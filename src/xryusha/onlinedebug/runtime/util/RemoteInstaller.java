package xryusha.onlinedebug.runtime.util;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.sun.jdi.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import xryusha.onlinedebug.runtime.RemotingBase;
import xryusha.onlinedebug.runtime.SyntheticRValue;
import xryusha.onlinedebug.config.values.*;


/**
 * Installs classes on target VM.<br/>
 * Generally, VM prohibits it by throwing {@link com.sun.jdi.ClassNotLoadedException ClassNotLoadedException}
 * if class installed using remote reflaction by  {@link ClassLoader#defineClass(String, byte[], int, int)}  as it may create
 * lock and incosistency problems (see {@link com.sun.jdi.ClassNotLoadedException ClassNotLoadedException} description.
 * To overcome this problem the strategy is not so streiht forward but :
 * <pre>
 *  - starting local simple {@link com.sun.net.httpserver.HttpServer }
 *  - patching remote classloader by adding to its URL list server's URL
 *  - invoking remotely {@link Charset#forName(String)}} with non-existing charset name.
 *    As the {@link ServiceLoader} is called it makes requests to ours server to obtain
 *    META-INF/services/java.nio.charset.spi.CharsetProvider file which should contain list
 *    of implementations. We return this file with name of onfly generated and compiled class
 *    implementing the interface, but all it performes is calling {@link Class#forName(String)}
 *    for each one of classes to be installed. As a result remote VM decides by itself to download
 *    these classes.
 *    <i>Note</i>: Basically we could use another service interface (even {@link Runnable} or
 *    {@link java.util.concurrent.Callable}, but this way makes sure it won't have any side effects
 *    (it could be another Runnable's service implementation)
 *
 *</pre>
 */
public class RemoteInstaller extends RemotingBase
{
    private final static String INSTALLER_ROOT = "/installer";
    private final static String TEMPLATE_FILE = "RemoteLoader.template";
    private final static String PACKAGE_TOKEN = "{{PACKAGE}}";
    private final static String FILENAME_TOKEN = "{{CLASSNAME}}";
    private final static String CLASSES_TOKEN = "{{CLASSES}}";
    private final static AtomicInteger loadersCount = new AtomicInteger(1);

    private static RemoteInstaller instance;
    private final String serverURL;
    private final InstallingHandler  patchinghandler;

    public static RemoteInstaller getInstance() throws IOException
    {
        return installerHolder.Instance.get();
    }

    // java out-of-box singleton implementation
    private enum installerHolder {
        Instance;

        private final RemoteInstaller installer;

        installerHolder() {
            try {
                installer = new RemoteInstaller();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        RemoteInstaller get() { return installer; }
    } // installerHolder


    private RemoteInstaller() throws IOException
    {
        ServerSocket seso = new ServerSocket(0);
        int freePort = seso.getLocalPort();
        seso.close();
        String host = InetAddress.getLocalHost().getHostName();
        patchinghandler = new InstallingHandler();
        serverURL = "http://"+host+":"+freePort+ INSTALLER_ROOT + "/";
        HttpServer server = HttpServer.create(new InetSocketAddress(host, freePort), 0);
        server.createContext(INSTALLER_ROOT, patchinghandler);
        server.start();
        new Thread(()->warmUp()).start();
    }

    public void install(ThreadReference thread, List<Class> classes) throws Exception
    {
        VirtualMachine vm = thread.virtualMachine();
        ArrayList<Class> missingClasses = new ArrayList<>();
        for(Class clazz: classes ) {
            if ( vm.classesByName(clazz.getName()).isEmpty() )
                missingClasses.add(clazz);
        }
        if ( missingClasses.isEmpty() )
            return;
        String[] generated = generateLoaderCode(missingClasses);
        String className = generated[0];
        String code = generated[1];
        byte[] compiled = Compiler.INSTANCE.compile(className, code, "1.6");
        ObjectReference urlClassloader = getClassLoader(thread);
        patchClassLoaderIfNeeded(thread, urlClassloader);
        patchinghandler.publish(CharsetProvider.class, className, compiled, () -> {
                    CallSpec charsetForName = new CallSpec(Charset.class.getName(), "forName");
                    charsetForName.getParams().add(new Const("NOSUCH"+System.currentTimeMillis()));
                    try {
                        getValue(thread, charsetForName, true);
                    } catch (Exception ex) { // that's ok, it fails but the call triggers loading
                    }
                });
    } // install

    private void warmUp()
    {
        try {
            String[]generated = generateLoaderCode(Arrays.asList(this.getClass()));
            String className = generated[0];
            String code = generated[1];
            Compiler.INSTANCE.compile(className, code, "1.6");
        } catch (Throwable ex){
            System.err.println("RemoteInstaller.warmUp() fail: ");
            ex.printStackTrace();
        }
    }

    private ObjectReference getClassLoader(ThreadReference thread) throws Exception
    {
        RefChain getLoaderChain = new RefChain();
        getLoaderChain.getRef().add(new CallSpec(Thread.class.getName(), "currentThread"));
        getLoaderChain.getRef().add(new CallSpec(null, "getContextClassLoader"));
        ObjectReference urlClassLoader = (ObjectReference) getValue(thread, getLoaderChain);
        return urlClassLoader;
    }

    private void patchClassLoaderIfNeeded(ThreadReference thread, ObjectReference urlClassLoader) throws Exception
    {
        RefChain chain = new RefChain();
        chain.getRef().add(new SyntheticRValue(urlClassLoader));
        // check if our URL is already installed (i.e. it's not a 1st call)
        chain.getRef().add(new CallSpec(null, "getURLs"));
        ArrayReference array = (ArrayReference) getValue(thread, chain);
        boolean patched = false;
        for(Value val : array.getValues()) {
            String url = toString(thread, val);
            if ( (patched = serverURL.equals(url)) ) {
                break;
            }
        } // for urls
        if ( patched ) {
            log.log(Level.FINE, "remote jvm classloader already patched");
            return;
        }

        Constructor urlCtor = new Constructor(URL.class.getName());
        urlCtor.getParams().add(new Const(serverURL, String.class.getName()));
        ObjectReference url = (ObjectReference) getValue(thread, urlCtor);
        // Adding to URLLoader
        RefChain addUrlChain = new RefChain();
        addUrlChain.getRef().add(new SyntheticRValue(urlClassLoader));
        addUrlChain.getRef().add(new RefPath("ucp.urls"));
        CallSpec addUrl = new CallSpec(null, "add");
        addUrl.getParams().add(new SyntheticRValue(url));
        addUrlChain.getRef().add(addUrl);
        getValue(thread, addUrlChain);
    } // patchClassLoaderIfNeeded


    private String[] generateLoaderCode(List<Class> classes) throws IOException
    {
        String loadedTemplate;
        try (InputStream template = getClass().getResourceAsStream(TEMPLATE_FILE)){
            if ( template == null )
                throw new FileNotFoundException(TEMPLATE_FILE);
            StringWriter swr = new StringWriter();
            for(int ch = template.read(); ch != -1; ch = template.read())
                swr.write(ch);
            loadedTemplate = swr.toString();
        }
        String rnd = Long.toHexString(System.currentTimeMillis()).toUpperCase();
        String className = "RemoteLoader_"
                + classes.get(0).getSimpleName()
                + "_" + loadersCount.getAndIncrement()
                + "_" + rnd;
        String packaged = loadedTemplate.replace(PACKAGE_TOKEN, "onlinedebug.runtime.installer");
        String renamed = packaged.replace(FILENAME_TOKEN, className);
        String qualifiedName = "onlinedebug.runtime.installer." + className;


        StringBuilder sb = new StringBuilder();
        for(int inx = 0; inx < classes.size(); inx++ ) {
            Class clazz = classes.get(inx);
            if ( inx > 0 )
                sb.append(", ");
            sb.append('"')
                    .append(clazz.getName())
                    .append('"');
        } // all classes
        String classesList = sb.toString();
        String ready = renamed.replace(CLASSES_TOKEN, classesList);
        return new String[]{qualifiedName, ready};
    } // generateLoaderCode


    private class InstallingHandler implements HttpHandler
    {
        private ConcurrentMap<String,byte[]> pendingUploads = new ConcurrentHashMap<>();


        public synchronized void publish(Class serviceClass, String implementationClassName, byte[] implementation, Runnable loadingTrigger) throws Exception
        {
            String metaName = "META-INF/services/"+serviceClass.getName();
            pendingUploads.put(metaName, implementationClassName.getBytes());
            String implClassFilename = implementationClassName.replace('.', '/') + ".class";
            pendingUploads.put(implClassFilename, implementation.clone());
            try {
                loadingTrigger.run();
            } finally {
                pendingUploads.remove(metaName);
                pendingUploads.remove(implClassFilename);
            }
        } //  publish

        @Override
        public void handle(HttpExchange httpExchange) throws IOException
        {
            URI request = httpExchange.getRequestURI();
            String root = httpExchange.getHttpContext().getPath();
            String requestpath = request.getPath();
            String resource = requestpath.substring(root.length());
            while ( resource.charAt(0) == '/' )
                resource = resource.substring(1);

            byte[] responseData = getResponse(resource);
            OutputStream os = httpExchange.getResponseBody();
            if ( responseData != null ) {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseData.length);
                os.write(responseData);
            }
            else
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0);

            log.log(Level.FINE, "ServiceInstallingHandler::handle request: {0} , response.size: {1}",
                    new Object[]{resource, responseData != null ? responseData.length : 0});
            os.flush();
            os.close();
        }

        private String[] localOnly = new String[] {
                "java/",
                "javax/",
                "jdk/",
                "com/oracle/",
                "com/sun/",
                "sun/",
        };

        byte[] getResponse(String path) throws IOException
        {
            for(String name: localOnly) {
                if ( path.startsWith(name) ) {
                    log.log(Level.FINE,
                             ()->"ServiceInstallingHandler.getResponse: ignorring request for "
                                  + path);
                    return null;
                }
            }
            ByteArrayOutputStream baos = null;
            byte[] pendings = pendingUploads.get(path);
            if ( pendings != null ) {
                baos = new ByteArrayOutputStream();
                baos.write(pendings);
            }
            else { // one of classes referenced by implmenetor
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                InputStream is = cl.getResourceAsStream(path);
                if ( is != null ) {
                    baos = new ByteArrayOutputStream();
                    for(int ch = is.read(); ch != -1; ch = is.read())
                        baos.write(ch);
                    is.close();
                }
            }
            byte[] res = baos != null && baos.size() > 0 ? baos.toByteArray() : null;
            return res;
        } // getResponse
    } // ServiceInstallingHandler
}
