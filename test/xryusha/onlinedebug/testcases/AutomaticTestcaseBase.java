package xryusha.onlinedebug.testcases;


import com.sun.jdi.ReferenceType;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.actions.PrintSpec;
import xryusha.onlinedebug.runtime.EventsProcessor;
import xryusha.onlinedebug.runtime.RemoteJVM;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AutomaticTestcaseBase
{
    protected void runTest(Class<? extends Flow> testFlow) throws Exception
    {
        runTest(testFlow, testFlow.getSimpleName()+".xml", testFlow.getSimpleName()+".txt");
    }

    protected void runTest(Class testFlow, String configFileName, String resultsFileName) throws Exception
    {
        Function<String, Path> getPath = (filename) -> {
            URL configURL = testFlow.getResource(filename);
            assert configURL != null : "file " + filename + " not found";
            String config = configURL.getPath();
            Path path = new File(config).toPath();
            return path;
        };
        Path configFile = getPath.apply(configFileName);
        Path expectedResult = getPath.apply(resultsFileName);


        if ( !Files.exists(configFile))
            throw new IllegalArgumentException("Config file not exists: " + configFile);
        if ( !Files.exists(expectedResult))
            throw new IllegalArgumentException("Expected results not exists: " + configFile);
        List<String> expectedLines = Files.readAllLines(expectedResult);

        // run remote test
        List<List<String>> realLines = runTest(configFile, testFlow);

        // collect results
        List<String> localLines = realLines.get(0);
        List<String> remoteLines = realLines.get(1);

        // trim all
        expectedLines = expectedLines.stream().map(s->s.trim()).collect(Collectors.toList());
        localLines = localLines.stream().map(s->s.trim()).collect(Collectors.toList());
        remoteLines = remoteLines.stream().map(s->s.trim()).collect(Collectors.toList());

        boolean loceq = expectedLines.equals(localLines);
        boolean remeq = expectedLines.equals(remoteLines);
        if ( loceq && remeq )
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("Expected result: ").append(System.lineSeparator());
        sb.append(expectedLines).append(System.lineSeparator());

        sb.append("Local log content: (asExpected: ").append(loceq).append(")").append(System.lineSeparator());
        sb.append(localLines).append(System.lineSeparator());

        sb.append("Remote log content (asExpected: ").append(remeq).append(")").append(System.lineSeparator());
        sb.append(remoteLines).append(System.lineSeparator());

        String msg = sb.toString();
        throw new Exception(msg);
    } // runTest

    protected List<List<String>> runTest(Path configFile, Class testFlow) throws Exception
    {
        Logger log = Logger.getLogger("onlinedebug");
        log.setLevel(Level.FINE);
        log.setUseParentHandlers(false);

        File localLog = File.createTempFile("xryusha-"+this.getClass().getSimpleName()+"-local", null);
        File remoteLog = File.createTempFile("xryusha-"+this.getClass().getSimpleName()+"-remote", null);
        Configuration patchedconfig = this.setupTargetFiles(configFile, localLog.toPath(), remoteLog.toPath());
        runTestProcess(patchedconfig, testFlow);
        List<String> localLines = Files.readAllLines(localLog.toPath());
        List<String> remoteLines = Files.readAllLines(remoteLog.toPath());
        localLog.delete();
        remoteLog.delete();
        List<List<String>> result = new ArrayList<>(Arrays.asList(localLines, remoteLines));
        return result;
    } // runTest


    protected void runTestProcess(Configuration config, Class testFlow) throws Exception
    {
        int remoteJvmWaitsForUs = 10000;
        int weWaitForRemoteJvm = 15000;
        ServerSocket seso = new ServerSocket(0);
        int port = seso.getLocalPort();
        seso.close();
        String exe = System.getProperty("java.home") +
                                        File.separator + "bin" +
                                        File.separator + "java";
        String debugX = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + port;
        String classpath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String delayStr = Integer.toString(remoteJvmWaitsForUs);
        boolean isdebug = false;
        List<String> args = new ArrayList<>(Arrays.asList(exe, debugX, "-cp", classpath,
                                                          AutomaticTestcaseDriver.class.getName()));
        if ( isdebug )
            args.add("-debug");
        args.addAll(Arrays.asList(delayStr, testFlow.getName()));
        ProcessBuilder pb = new ProcessBuilder( args.toArray(new String[0])
/*                          exe, debugX, "-cp", classpath,
                          AutomaticTestcaseDriver.class.getName(),
                          "-debug",
                          delayStr,
                          testFlow.getName()*/);

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
          .redirectError(ProcessBuilder.Redirect.INHERIT);

        Process proc = pb.start();
        try {
            runTest(config, weWaitForRemoteJvm, proc, port);
        } finally {
            proc.destroyForcibly();
        }
    } // runTestProcess


    private void runTest(Configuration config, int weWaitForRemoteJvm, Process proc, int port) throws Exception
    {
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        RemoteJVM jvm = null;
        long starttime = System.currentTimeMillis();
        while ( jvm == null && System.currentTimeMillis() - starttime < weWaitForRemoteJvm) {
            try {
                jvm = new RemoteJVM(addr);
            } catch (Exception ex) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            }
        }
        if ( jvm == null ) {
            System.out.println("failed to attach");
            proc.destroyForcibly();
            throw new Exception("failed to attach");
        }
        ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations = jvm.apply(config);
        EventsProcessor handler = new EventsProcessor(jvm.getRemoteVM(), config, postponedRegistrations);
        onStart(handler, jvm, config);
        // notify test process it may continue start
        proc.getOutputStream().write(new byte[]{(byte)'a', (byte)'\n'});
        proc.getOutputStream().flush();

        boolean docontinue = true;
        while(docontinue) {
            try {
               handler.handlePendingEvents();
               docontinue = proc.isAlive();
            } catch (Exception ex) {
                docontinue = false;
            }
        } // while
    } // runTest

    // callback
    protected void onStart(EventsProcessor handler, RemoteJVM jvm, Configuration config)
    {
    }

    protected Configuration setupTargetFiles(Path configurationFile, Path localLog, Path remoteLog) throws Exception
    {
        String targetLocal = localLog.toFile().getCanonicalFile().toURI().getPath();
        String targetRemote = remoteLog.toFile().getCanonicalFile().toURI().getPath();

        DocumentBuilderFactory docfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder bld = docfac.newDocumentBuilder();
        Document configDom = bld.parse(configurationFile.toFile());
        Element root = configDom.getDocumentElement();
        Queue<Element> pendingElements = new LinkedList<>();
        pendingElements.add(root);
        while(!pendingElements.isEmpty()) {
            Element nxt = pendingElements.poll();
            NodeList childs = nxt.getChildNodes();
            int len = childs.getLength();
            for(int inx = 0; inx < len; inx++) {
                Node child = childs.item(inx);
                if ( !(child instanceof Element) )
                    continue;
                Element childEl = (Element) child;
                if (!Configuration.Elements.ACTION_PRINT.equals(childEl.getTagName())) {
                    pendingElements.add(childEl);
                    continue;
                }
                childEl.setAttribute("location", PrintSpec.LoggingVM.both.name());
                childEl.setAttribute("localFile", targetLocal);
                childEl.setAttribute("remoteFile", targetRemote);
            } // for element childs
        } // for doc elements

        File tmpFile = File.createTempFile("odb", null);
        DOMSource src = new DOMSource(configDom);
        FileWriter fw = new FileWriter(tmpFile);
        StreamResult result = new StreamResult(fw);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(src, result);
        fw.close();
        Configuration config = Configuration.load(tmpFile.toPath());
        tmpFile.delete();
        return config;
    } // setupTargetFiles
}
