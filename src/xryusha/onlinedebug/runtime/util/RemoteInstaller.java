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
import xryusha.onlinedebug.config.values.*;
import xryusha.onlinedebug.runtime.RemotingBase;
import xryusha.onlinedebug.runtime.SyntheticRValue;



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
 *    of implementations. We return this file with name of onfly patched class {@link RemoteLoader_XXXXXXXXXXXXXXXX}
 *    where patching is changing it's name and replacing placeholder string with classnames
 *    The class implements the interface, but all it performes is calling {@link Class#forName(String)}
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
    private final static Charset oneByteCharset =  Charset.forName("ISO-8859-1");
    private final static AtomicInteger loadersCount = new AtomicInteger(0);
    private final String serverURL;
    private final InstallingHandler  patchinghandler;
    private final byte[] loaderTemplate;

    public static RemoteInstaller getInstance() throws IOException
    {
        return installerHolder.Instance.get();
    }

    public void init() throws Exception
    {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String resName =
                RemoteLoader_XXXXXXXXXXXXXXXX.class.getName().replace(".","/")+".class";
        try (InputStream is =
                  Thread.currentThread().getContextClassLoader().getResourceAsStream(resName)){
            for(int ch; (ch = is.read()) != -1; baos.write(ch));
        }

        loaderTemplate = baos.toByteArray();
    }

    public boolean install(ThreadReference thread, List<Class> classes) throws Exception
    {
        VirtualMachine vm = thread.virtualMachine();
        StringBuffer missingList = new StringBuffer();
        for(Class clazz: classes ) {
            if ( vm.classesByName(clazz.getName()).isEmpty() ) {
                missingList.append(clazz.getName()).append(" ");
            }
        }
        if ( missingList.length() == 0 )
            return true;
        if ( missingList.length() > RemoteLoader_XXXXXXXXXXXXXXXX.placeHolder.length() )
            throw new IllegalArgumentException("classes list is too long");

        // padding class list string to placeholder size
        for(int inx = missingList.length();
                 inx < RemoteLoader_XXXXXXXXXXXXXXXX.placeHolder.length(); inx++)
                    missingList.append(" ");

        String updatedSimpleClassName =
                RemoteLoader_XXXXXXXXXXXXXXXX.class.getSimpleName()
                        .replace("_XXXXXXXXXXXXXXXX",
                                 String.format("_%04d%012x",
                                        loadersCount.getAndIncrement(),
                                        System.currentTimeMillis()).toUpperCase());
        String updatedClassName =
                RemoteLoader_XXXXXXXXXXXXXXXX.class.getName()
                               .replace(RemoteLoader_XXXXXXXXXXXXXXXX.class.getSimpleName(),
                                        updatedSimpleClassName);
        byte[] updatedClassContent =
                  new String(loaderTemplate, oneByteCharset)
                    .replace(RemoteLoader_XXXXXXXXXXXXXXXX.class.getSimpleName(),
                                     updatedSimpleClassName)
                    .replace(RemoteLoader_XXXXXXXXXXXXXXXX.placeHolder,
                                     missingList.toString())
                           .getBytes(oneByteCharset);

        ObjectReference urlClassloader = getClassLoader(thread);
        // Is it URLClassloader? If not - we can't patch it..
        if ( !isInstanceOf(thread,
                           urlClassloader,
                           thread.virtualMachine()
                                 .classesByName(URLClassLoader.class.getName()).get(0))) {
            log.log(Level.INFO, () -> "RemoteInstaller.install: classloader is not URLClassLoader, exitting");
            return false; //
        }
        patchClassLoaderIfNeeded(thread, urlClassloader);
        patchinghandler.publish(CharsetProvider.class, updatedClassName, updatedClassContent, () -> {
                    CallSpec charsetForName = new CallSpec(Charset.class.getName(), "forName");
                    charsetForName.getParams().add(new Const("NOSUCH"+System.currentTimeMillis()));
                    try {
                        getValue(thread, charsetForName, true);
                    } catch (Exception ex) { // that's ok, it fails but the call triggers loading
                    }
                });
        log.log(Level.FINE, "RemoteInstaller.install: initlized successfully");
        return true;
    } // install


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

        URL serverURLObj = new URL(serverURL);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos =  new ObjectOutputStream(baos);
        oos.writeObject(serverURLObj);
        byte[] data = baos.toByteArray();
        ArrayType arrType = (ArrayType)
                  thread.virtualMachine().classesByName("byte[]").get(0);
        ArrayReference arr = arrType.newInstance(data.length);
        for(int inx = 0; inx < data.length; inx++)
            arr.setValue(inx, thread.virtualMachine().mirrorOf(data[inx]));

        // URL u = new ObjectInputStream(new ByteArrayInputStream(arr)).readObject();
        // Though this way significantly more complicated than just new URL(String),
        // this way reduces probability of collision since new URL()
        // may be monitored itself
        Constructor bais = new Constructor(ByteArrayInputStream.class.getName());
        bais.getParams().add(new SyntheticRValue(arr));
        Constructor ois = new Constructor(ObjectInputStream.class.getName());
        ois.getParams().add(bais);
        RefChain readChain = new RefChain();
        readChain.getRef().add(ois);
        readChain.getRef().add(new CallSpec(null, "readObject"));
        ObjectReference url = (ObjectReference) getValue(thread, readChain);

        // Adding to URLLoader
        RefChain addUrlChain = new RefChain();
        addUrlChain.getRef().add(new SyntheticRValue(urlClassLoader));
        addUrlChain.getRef().add(new RefPath("ucp.urls"));
        CallSpec addUrl = new CallSpec(null, "add");
        addUrl.getParams().add(new SyntheticRValue(url));
        addUrlChain.getRef().add(addUrl);
        getValue(thread, addUrlChain);
    } // patchClassLoaderIfNeeded

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
                try (InputStream is = cl.getResourceAsStream(path)) {
                    if ( is != null ) {
                        baos = new ByteArrayOutputStream();
                        for(int ch = is.read(); ch != -1; ch = is.read())
                            baos.write(ch);
                    }
                }
            }
            byte[] res = baos != null && baos.size() > 0 ? baos.toByteArray() : null;
            return res;
        } // getResponse
    } // ServiceInstallingHandler

                                     /* length of hex digits of long */
    public static class RemoteLoader_XXXXXXXXXXXXXXXX extends CharsetProvider
    {
        // veeery long placeholder body to be replaced
        private final static String placeholderElement = "{placeholder}";
        private static String placeHolder =
                  placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                + placeholderElement + placeholderElement + placeholderElement
                ;
        private static String loaderID = "RemoteLoader_XXXXXXXXXXXXXXXX";

        @Override
        public Iterator<Charset> charsets()
        {
            load();
            return null;
        }

        @Override
        public Charset charsetForName(String charsetName)
        {
            load();
            return null;
        }

        private void load()
        {
            StringTokenizer st = new StringTokenizer(placeHolder, " ");
            ArrayList<String> failed = new ArrayList<>();
            ArrayList<String> suceeded = new ArrayList<>();
            while(st.hasMoreElements()) {
                String name = st.nextToken();
                try {
                    Class<?> clz = Class.forName(name);
                    suceeded.add(name);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    failed.add(name);
                }
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            pw.println("==== remote loader " + loaderID + " ===========");
            if (!suceeded.isEmpty())
                pw.println("  Successfully loaded: " + suceeded);
            if (!failed.isEmpty())
                pw.println("  Failed loading: " + failed);
            pw.println("=====================================================");
            pw.flush();
            System.out.println(sw);
        } // load
    } // RemoteLoader_XXXXXXXXXXXXXXXX
}
