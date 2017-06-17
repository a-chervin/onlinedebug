/**
 * Licensed to the a-chervin (ax.chervin@gmail.com) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * a-chervin licenses this file under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xryusha.onlinedebug;

import com.sun.jdi.ReferenceType;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.runtime.RemoteJVM;
import xryusha.onlinedebug.runtime.EventsProcessor;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.*;

/*
TODO:
 add <caller>
 add <callerType>
 add encloser access
 */
public class Main
{
    public final static String USAGE =
            "Usage: main [-debug] <remove jvm's host:port> <configuration_file_path>"+
            "\nUse -help for this message. "+
            "\nUse '-help schema [output_file]' to get config schema"+
            "\nUse '-help example [output_file]' to get config example"+
            "\nMonitored JVM flags: "+
            "\n-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%PORT%";

    public static final String DEBUG_FLAG = "-debug";
    public static final String SKIP_SCHEMAVALIDATION_FLAG = "-skip";
    public static final String SCHEMA_HELP_FLAG = "schema";
    public static final String EXAMPLE_HELP_FLAG = "schema";
    public static final String DEFAULT_SCHEMA = "./configuration.xsd";
    public static final String EXAMPLE = "example.xml";


    public static void main(String[] args) throws Exception
    {
/* */
/*
        args = new String[]{
                 "-debug",
//                 "-skip",
                 "localhost:5005",
                 "C:\\java\\projects\\onlinedebug\\test\\" +
//                "onlinedebug\\testcases\\nested\\FromAnonymousMethodInner_accessMethod.xml"
//                  "xryusha\\onlinedebug\\testcases\\breakpoints\\modification\\ModificationBreakpoint.xml"
//                    "onlinedebug\\testcases\\breakpoints\\methodrelated\\InnerMethodEntryBreakpoint.xml"
//                    "onlinedebug\\testcases\\local\\LocalArrayDynamicIndex.xml"
//                    "xryusha\\onlinedebug\\testcases\\actions\\ReturnOnException.xml"
//                      "xryusha\\onlinedebug\\testcases\\local\\LocalVars.xml"
//                      "xryusha\\onlinedebug\\testcases\\nested\\NestedCallChain.xml"
//                      "xryusha\\onlinedebug\\testcases\\jdk\\LocaleAccess.xml"
//                        "xryusha\\onlinedebug\\testcases\\nested\\FromConstructor.xml"
                        "xryusha\\onlinedebug\\testcases\\nested\\FromMethodInner.xml"
        };
*/
        if ( args == null || args.length == 0 ) {
            doHelp(null);
            return;
        };

        ArrayList<String> argslist = new ArrayList<>(Arrays.asList(args));
        String arg = argslist.get(0);
        if ( arg.equals("-h") || arg.equals("-help")) {
            argslist.remove(0);
            doHelp(argslist);
            return;
        }

        // mb added new k's
        Set<String> justFlags = new HashSet<>(Arrays.asList(DEBUG_FLAG,SKIP_SCHEMAVALIDATION_FLAG));
        Map<String,String> argmap = new HashMap<>();
        int inx = 0;
        for(; inx < args.length && args[inx].startsWith("-"); inx++) {
            String k = args[inx];
            String val = "";
            if (!justFlags.contains(k) &&
                     inx+1<args.length &&
                        !args[inx+1].startsWith("-"))
                val = args[inx++];
            argmap.put(k,val);
        }
        if ( inx > args.length - 2 ) {
            doHelp(argslist);
            return;
        }

        boolean debug = argmap.containsKey("-debug");
        boolean skip = argmap.containsKey("-skip");
        String addrStr = args[inx++];
        String configStr = args[inx++];

        Level level = debug? Level.FINE : Level.INFO;
        Logger log = Logger.getLogger("onlinedebug");
        log.setLevel(level);
        log.setUseParentHandlers(false);

        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%6$s%n");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(level);
        log.addHandler(consoleHandler);

        Path configFile = Paths.get(configStr);
        if ( !Files.exists(configFile) )
            throw new FileNotFoundException(configStr);

        Configuration config = Configuration.load(configFile, !skip);
        log.config(config.toString());

        String[] parts = addrStr.split(":");
        if ( parts.length != 2 )
            throw new IllegalArgumentException("Invalid remote addr");

        InetSocketAddress addr = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
        RemoteJVM jvm = null;
        while ( jvm == null ) {
            log.info("Connecting to JVM on " + addr);
            try {
                jvm = new RemoteJVM(addr);
            } catch (Exception ex) {
                log.warning("Connection attempt failed: " + ex.getMessage() +
                            "\nKeeping retrying..");
                Thread.sleep(3000);
            }
        }
        log.info("Connected.");

        ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponed = jvm.apply(config);
        final RemoteJVM finvm = jvm;
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        finvm.disconnect();
                    }
                }
        );
        System.out.println("Settings applied.");
        EventsProcessor handler = new EventsProcessor(jvm.getRemoteVM(), config, postponed);
        while(true) {
            handler.handlePendingEvents();
        }
    } // main

    private static boolean doHelp(ArrayList<String> argslist) throws IOException
    {
        System.out.println(USAGE);
        if ( argslist == null || argslist.isEmpty() )
            return true;
        String arg = argslist.remove(0);
        String target = "";
        InputStream source = null;
        if (arg.equals(SCHEMA_HELP_FLAG)) {
            source = Configuration.getSchemaAsStream();
            if ( !argslist.isEmpty())
                target = argslist.remove(0);
            else
                target = DEFAULT_SCHEMA;
        } else if ( arg.equals(EXAMPLE_HELP_FLAG)) {
            class D{};
            Class thisclass = D.class.getEnclosingClass();
            source = thisclass.getResourceAsStream(EXAMPLE);
            if ( !argslist.isEmpty())
                target = argslist.remove(0);
            else
                target = EXAMPLE;
        }
        else
            return true;
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
        for(int ch = source.read(); ch != -1; ch = source.read())
            bos.write(ch);
        return true;
    } // doHelp
}
