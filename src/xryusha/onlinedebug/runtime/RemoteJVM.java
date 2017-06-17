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

package xryusha.onlinedebug.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.request.*;
import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.exceptions.RemoteFieldNotFoundException;
import xryusha.onlinedebug.runtime.util.AsyncRemoteExecutor;
import xryusha.onlinedebug.runtime.util.RemoteInstaller;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.exceptions.RemoteClassNotFoundException;
import xryusha.onlinedebug.runtime.func.ErrorproneFunction;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.*;


public class RemoteJVM
{
    private final static Logger log = Log.getLogger();
    private final static Map<VirtualMachine,RemoteJVM> vms = new ConcurrentHashMap<>();
    private final VirtualMachine remoteVM;
    private final ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponed = new ConcurrentHashMap<>();
    private final Map<Class,ErrorproneFunction<ConfigEntry,Boolean>> installers =
            new HashMap<Class,ErrorproneFunction<ConfigEntry,Boolean>>(){{
                put(LineBreakpointSpec.class, (sp-> installLineBP(sp)));
                put(ExceptionBreakpointSpec.class, (sp-> installExceptions(sp)));
                put(MethodEntryBreakPointSpec.class, (sp-> installMethodEntry(sp)));
                put(MethodExitBreakPointSpec.class, (sp-> installMethodExit(sp)));
                put(FieldModificationBreakPointSpec.class, (sp-> installFieldModification(sp)));
            }};
    private final Map<?,HandlerData> grouppedHandlerData = new ConcurrentHashMap<>();


    public static RemoteJVM lookup(VirtualMachine vm)
    {
        return vms.get(vm);
    }

    public RemoteJVM(InetSocketAddress address) throws IOException, IllegalConnectorArgumentsException
    {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List<AttachingConnector> connectors = vmm.attachingConnectors();
        AttachingConnector socConn = null;
        for(int inx = 0; inx < connectors.size() && socConn == null; inx++) {
            AttachingConnector cn = connectors.get(inx);
            if ( !"dt_socket".equals(cn.transport().name()))
                continue;
            socConn = cn;
        } // eof for
        if ( socConn == null )
            throw new RuntimeException("dt_socket connector not found");

        Map<String,Connector.Argument> params = socConn.defaultArguments();
        String host = address.getHostName();
        Connector.StringArgument hostArg = (Connector.StringArgument) params.get("hostname");
        hostArg.setValue(host);

        int port = address.getPort();
        Connector.IntegerArgument portArg = (Connector.IntegerArgument) params.get("port");
        portArg.setValue(port);
        remoteVM = socConn.attach(params);
        vms.put(remoteVM, this);
        try {
            RemoteInstaller.getInstance().init();
        } catch (Throwable th) {
            log.log(Level.SEVERE, "RemoteInstaller initialization fail", th);
        }
    }

    public VirtualMachine getRemoteVM()
    {
        return remoteVM;
    }


    /**
     * Applies configuration on remote jvm
     * @param configuration
     * @return delayed registration actions should be executed when relevant
     *                 classes are loaded (currently are still unavailable)
     * @throws Exception
     */
    public ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> apply(Configuration configuration) throws Exception
    {
        remoteVM.eventRequestManager().deleteAllBreakpoints();
        for(ConfigEntry configEntry : configuration.getEntries()) {
            apply(configEntry);
        }
        return postponed;
    } // compile Configuration

    public void disconnect()
    {
        if ( remoteVM == null )
            return;
        log.info("Disconnection from remoteVM gracefully");
        try {
            remoteVM.eventRequestManager().deleteAllBreakpoints();
            remoteVM.dispose();
        } catch (Throwable th) {
            log.info("Failed grace remoteVM disconnection");
        }
    }

    public void apply(ConfigEntry configEntry) throws Exception
    {
        if ( !configEntry.isEnabled()) {
            log.warning("Ignorring disabled entry: " + configEntry.getBreakPoint());
            return;
        }
        AbstractBreakPointSpec bp = configEntry.getBreakPoint();
        ErrorproneFunction<ConfigEntry,Boolean> installer = installers.get(bp.getClass());
        if ( installer == null ) {
            log.log(Level.SEVERE, "Unsupported breakpoint type: " + bp);
            return;
        }
        installer.apply(configEntry);
        log.info(()->"Successfully installed: " + bp);
    } // compile Action

    /**
     * Installs configured Line break point or registers it as delayed
     * action if relevant is not available yet
     * @param entry
     * @return
     * @throws Exception
     */
    private boolean installLineBP(final ConfigEntry entry) throws Exception
    {
        LineBreakpointSpec breakPoint = (LineBreakpointSpec) entry.getBreakPoint();
        checkIfJdk(breakPoint.getTargetClass(), null);
        List<ReferenceType> types = getClass(breakPoint.getTargetClass());
        if ( types.size() == 0 ) {
            registerPosponedInstall(breakPoint.getTargetClass(), (t) -> installLineBP(entry, t));
            return false;
        }
        else
            return installLineBP(entry, types);
    } // install LineBreakpointSpec

    /**
     * Installs configured line break point. Called when relevant class
     * is available (i.e. called by {@link #installLineBP(ConfigEntry)} or
     * as delayed registered action by {@link #} by EventsProcessor.handleClassPrepareEvent()}
     * triggered when relevant class is loaded
     */
    private boolean installLineBP(final ConfigEntry entry, List<ReferenceType> possibleTypes) throws Exception
    {
        ArrayList<Location> allVisibleLocations = new ArrayList<>();
        LineBreakpointSpec breakPoint = (LineBreakpointSpec) entry.getBreakPoint();
        Location location = null;
        for(int inx = 0; inx < possibleTypes.size() && location == null; inx++ ) {
            ReferenceType clazz = possibleTypes.get(inx);
            List<Location> locations = clazz.locationsOfLine(breakPoint.getLine());
            if ( locations.size() > 0  )
                location = locations.get(0);
            else {
                allVisibleLocations.addAll(clazz.allLineLocations());
            }
        }
        if ( location == null ) {
            // it may be config error or line inside one of inner classes
            Queue<ReferenceType> nestedForCheck = new LinkedList<>();
            List<ReferenceType> inners =
                    possibleTypes.stream().flatMap(t->t.nestedTypes().stream()).collect(Collectors.toList());
            nestedForCheck.addAll(inners);
            while( location == null && !nestedForCheck.isEmpty()) {
                ReferenceType inner = nestedForCheck.poll();
                List<Location> locations = inner.locationsOfLine(breakPoint.getLine());
                if ( locations.size() > 0 )
                    location = locations.get(0);
                else {
                    nestedForCheck.addAll(inner.nestedTypes());
                    allVisibleLocations.addAll(inner.allLineLocations());
                }
            }
        } // is inner?

        if ( location == null ) {
            if ( breakPoint.isFastFailOnMissing() ) {
                String msg = "Not found location " + breakPoint;
                RemoteClassNotFoundException cnf =
                        new RemoteClassNotFoundException(msg, breakPoint.getTargetClass());
                log.throwing(getClass().getSimpleName(), "install", cnf);
                throw cnf;
            }
            log.log(Level.SEVERE,
                    "installLineBP: Can not find location {0}, registering postponed action", breakPoint);
            for(ReferenceType type : possibleTypes ) {
                String possiblename = type.name()+"$*";
                registerPosponedInstall(possiblename, (t) -> installLineBP(entry, t));
            }
            return false;
        }

        Location finlock = location;
        init(location, (em)->em.createBreakpointRequest(finlock), null, entry);
        return true;
    } // install LineBreakpointSpec


    /**
     * Installs field modification breakpoint
     * See {@link #installLineBP(ConfigEntry)} description for line breakpoint installation
     */
    private boolean installFieldModification(final ConfigEntry entry) throws Exception
    {
        FieldModificationBreakPointSpec breakPoint = (FieldModificationBreakPointSpec) entry.getBreakPoint();
        List<ReferenceType> types = getClass(breakPoint.getTargetClass());
        if ( types.size() == 0 ) {
            registerPosponedInstall(breakPoint.getTargetClass(), (t) -> installFieldModification(entry, t));
            return false;
        }
        else
            return installFieldModification(entry, types);
    } // install

    /**
     * Installs field modification breakpoint
     * See {@link #installLineBP(ConfigEntry, List)} description for line breapoint installation
     */
    private boolean installFieldModification(final ConfigEntry entry, List<ReferenceType> possibleTypes) throws Exception
    {
        FieldModificationBreakPointSpec spec = (FieldModificationBreakPointSpec) entry.getBreakPoint();
        String key = spec.getTargetClass() + ":" + spec.getTargetField();
        for (ReferenceType type: possibleTypes) {
            Field field = type.fieldByName(spec.getTargetField());
            if ( field == null ) {
                log.log(Level.SEVERE, "Field modification breakpoind failure: field not found: " + key);
                throw new RemoteFieldNotFoundException("Field not found", spec.getTargetField(), spec.getTargetClass());
            }
            init(key, (em)->em.createModificationWatchpointRequest(field), null, entry);
            log.log(Level.INFO, ()->"Field modification event registered successfully : " + key);
        }
        return true;
    } // installExceptions

    /**
     * Installs exception thrown breakpoint
     * See {@link #installLineBP(ConfigEntry)} description for line breapoint installation
     */
    private boolean installExceptions(final ConfigEntry entry) throws Exception
    {
        ExceptionBreakpointSpec breakPoint = (ExceptionBreakpointSpec) entry.getBreakPoint();
        List<String> exceptions = breakPoint.allExceptions();
        boolean allsuccess = true;
        for (String exName : exceptions) {
            List<ReferenceType> types = getClass(exName);
            if (types.size() == 0) {
                registerPosponedInstall(exName, t -> installExceptions(entry, t));
                allsuccess = false;
            }
            installExceptions(entry, types);
        }
        return allsuccess;
    } // install

    /**
     * Installs thrown exception breakpoint
     * See {@link #installLineBP(ConfigEntry, List)} description for line breapoint installation
     */
    private boolean installExceptions(final ConfigEntry entry, List<ReferenceType> exceptions ) throws Exception
    {
        for (ReferenceType exception: exceptions) {
            init(exception.name(), (em)->em.createExceptionRequest(exception,true,true), null, entry);
            log.log(Level.INFO, ()->"Exception registered successfully : " + exception);
        }
        return true;
    } // installExceptions

    /**
     * Installs method entry breakpoint
     * See {@link #installLineBP(ConfigEntry)} description for line breapoint installation
     */
    private boolean installMethodEntry(final ConfigEntry entry) throws Exception
    {
        MethodEntryBreakPointSpec breakPoint = (MethodEntryBreakPointSpec) entry.getBreakPoint();
        if ( breakPoint.getTargetClass() == null )
            throw new IllegalArgumentException("class is not specified");
        checkIfJdk(breakPoint.getTargetClass(), breakPoint.getMethod());
        List<ReferenceType> types = getClass(breakPoint.getTargetClass());
        if ( types.size() == 0 ) {
            registerPosponedInstall(breakPoint.getTargetClass(), (t) -> installMethodEntry(entry,  t));
            return false;
        }
        else
            return installMethodEntry(entry, types);
    } // install installMethodEntry

    /**
     * Installs method entry breakpoint
     * See {@link #installLineBP(ConfigEntry, List)} description for line breapoint installation
     */
    private boolean installMethodEntry(final ConfigEntry entry, List<ReferenceType> possibleTypes) throws Exception
    {
        MethodEntryBreakPointSpec mev = (MethodEntryBreakPointSpec) entry.getBreakPoint();
        String k = "MethodEntry-"+mev.getTargetClass();
        init(k, em->em.createMethodEntryRequest(), r-> {
                                                         for(ReferenceType type : possibleTypes)
                                                             r.addClassFilter(type);
                                                   }, entry);
        return true;
    } // install MethodEntryBreakPointSpec

    /**
     * Installs method exit breakpoint
     * See {@link #installLineBP(ConfigEntry)} description for line breapoint installation
     */
    private boolean installMethodExit(final ConfigEntry entry) throws Exception
    {
        MethodExitBreakPointSpec breakPoint = (MethodExitBreakPointSpec) entry.getBreakPoint();
        if ( breakPoint.getTargetClass() == null )
            throw new IllegalArgumentException("class is not specified");
        checkIfJdk(breakPoint.getTargetClass(), breakPoint.getMethod());
        List<ReferenceType> types = getClass(breakPoint.getTargetClass());
        if ( types.size() == 0 ) {
            registerPosponedInstall(breakPoint.getTargetClass(), (t) -> installMethodExit(entry, t));
            return false;
        }
        else
            return installMethodExit(entry, types);
    } // install LineBreakpointSpec

    /**
     * Installs method exit breakpoint
     * See {@link #installLineBP(ConfigEntry, List)} description for line breapoint installation
     */
    private boolean installMethodExit(final ConfigEntry entry , List<ReferenceType> possibleTypes) throws Exception
    {
        MethodExitBreakPointSpec mev = (MethodExitBreakPointSpec) entry.getBreakPoint();
        String k = "MethodExit-"+mev.getTargetClass();
        init(k, em->em.createMethodExitRequest(), r-> {
                                                        for(ReferenceType type : possibleTypes)
                                                            r.addClassFilter(type);
                                                    }, entry);
        return true;
    } // install MethodEntryBreakPointSpec


/*

    // TODO L
    // TODO L
    // TODO L
    private boolean installRemoteExecutor() throws Exception
    {
        final ThreadReference finalizer =
                remoteVM.allThreads().stream()
                                     .filter(t->t.name().equals("Finalizer"))
                                     .findAny().get();
        finalizer.interrupt();
//        ReferenceType interrupEx =
//                remoteVM.classesByName(InterruptedException.class.getName()).get(0);
//        ExceptionRequest erx = remoteVM.eventRequestManager()
//                .createExceptionRequest(interrupEx, true, true);
//        erx.addThreadFilter(finalizer);
//        StepRequest erx = remoteVM.eventRequestManager()
//                .createStepRequest(finalizer, StepRequest.STEP_LINE,
//                        StepRequest.STEP_OVER);
//        erx.addThreadFilter(finalizer);
        MethodEntryRequest req =  remoteVM.eventRequestManager()
                .createMethodEntryRequest();
        log.fine(()->"RemoteJVM:  waiting for GC hook..");
        EventQueue eveq = remoteVM.eventQueue();
        while(Integer.parseInt("5") == 5) {
            EventSet eves = eveq.remove();
            try {
            Event ee =  eves.eventIterator().nextEvent();
                System.out.println("EVenet: " +ee);
//                AsyncRemoteExecutor.getInstance().init(mee.thread());
            } finally {
                eves.resume();
                req.disable();
                remoteVM.eventRequestManager().deleteEventRequests(Arrays.asList(req));
            }
        }
        log.fine(()->"RemoteJVM:  resumed.");
        return true;
    } // installRemoteExecutor

*/

    /**
     * Creates and registers required breakpoint event
     * @param key   key to be used for registration this event type
     * @param eventFactory relevant {@link EventRequest} object factory call
     * @param requestSetup initiation action (relevant field setting) to be
     *                     applied on created request
     * @param entry configuration entry
     * @return*/
    private <K, E extends EventRequest> HandlerData init(K key,
                                                         Function<EventRequestManager,E> eventFactory,
                                                         Consumer<E> requestSetup,
                                                         ConfigEntry entry)
    {
        Map<K,HandlerData> map = (Map<K, HandlerData>) grouppedHandlerData;
        HandlerData data = map.get(key);
        if ( data == null ) {
            E request = eventFactory.apply(remoteVM.eventRequestManager());
            if ( requestSetup != null )
                requestSetup.accept(request);
            request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            data = new HandlerData(this);
            map.put(key, data);
            request.putProperty(EventsProcessor.HANDLER_DATA_PN, data);
            request.enable();
        }
        data.addConfig(entry);
        return data;
    }


    /**
     * @param className   registers action to be performed when specified class is loaded
     * @param action
     */
    private void registerPosponedInstall(String className, ErrorproneFunction<List<ReferenceType>, Boolean> action)
    {
        EventRequestManager emg = remoteVM.eventRequestManager();
        ClassPrepareRequest classrequest = emg.createClassPrepareRequest();
        classrequest.addClassFilter(className);
        PostponedInstaller installer = (PostponedInstaller) postponed.get(className);
        if (installer == null)
            installer = new PostponedInstaller(className, classrequest);
        installer.addRequest(action);
        classrequest.enable();

        // was it loaded meanwhile? retrying installation action and
        // remove from postponed actions
        List<ReferenceType> types = remoteVM.classesByName(className);
        if (types.size() > 0) {
            try {
                boolean succeeded = installer.apply(types);
                if (succeeded) {
                    postponed.remove(className);
                } else {
                    log.log(Level.WARNING, "postopend installation  fail");
                }
            } catch (Throwable th) {
                log.log(Level.SEVERE, "postponed installation fail", th);
            }
        } // was it loaded meanwhile
        else
            postponed.put(className, installer);
    } // registerPosponedInstall


    private List<ReferenceType> getClass(String name) throws Exception
    {
        List<ReferenceType> types = remoteVM.classesByName(name);
        return types;
    }


    private void checkIfJdk(String clazz, String method)
    {
        if ( !clazz.startsWith("java."))
            return;
        StringBuilder sb = new StringBuilder("\n")
          .append("==============================================\n")
          .append("*  Warning: breaking on JDK class            *\n")
          .append("*  ").append(clazz).append("      *\n")
          .append("*  may bring to locks and unexpected results *\n");
        if ( clazz.equals(Locale.class.getName()) &&
                "getDefault".equals(method) )
            sb.append("* >Locale.getDefault()< is VERY discouraged  *\n")
              .append("*  break point as it's widely used in jdk    *\n")
              .append("*  code, this choise must be reconsidered    *\n");

        sb.append("==============================================\n");
        log.log(Level.WARNING, sb.toString());
    } // checkIfJdk

    private class PostponedInstaller implements Function<List<ReferenceType>,Boolean>
    {
        private final String clazz;
        private final ClassPrepareRequest prepareRequest;
        private final ArrayList<ErrorproneFunction<List<ReferenceType>, Boolean>> pendingRequests = new ArrayList<>();

        public PostponedInstaller(String clazz, ClassPrepareRequest prepareRequest)
        {
            this.clazz = clazz;
            this.prepareRequest = prepareRequest;
        }

        public void addRequest(ErrorproneFunction<List<ReferenceType>, Boolean> callable)
        {
            pendingRequests.add(callable);
        }

        @Override
        public Boolean apply(List<ReferenceType> referenceTypes)
        {
            boolean fullsuccess = true;
            for (int inx = pendingRequests.size() - 1; inx > -1; inx--) {
                ErrorproneFunction<List<ReferenceType>, Boolean> call = pendingRequests.get(inx);
                try {
                    boolean suceeded = call.apply(referenceTypes);
                    fullsuccess &= suceeded;
                    if (suceeded) {
                        pendingRequests.remove(call);
                    }
                } catch (Throwable th) {
                    log.log(Level.SEVERE, "postponed action on class " + clazz + " failed", th);
                }
                if (pendingRequests.size() == 0) {
                    log.log(Level.FINE, "postponed actions completed on class " + clazz);
                    prepareRequest.disable();
                    remoteVM.eventRequestManager().deleteEventRequest(prepareRequest);
                }
            } // for pendingRequests
            return fullsuccess;
        } // apply

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder("PostponedInstaller{");
            sb.append("id=").append(System.identityHashCode(this));
            sb.append(", clazz='").append(clazz).append('\'');
            sb.append(", pendingRequests=").append(pendingRequests);
            sb.append(", prepareRequest=").append(prepareRequest);
            sb.append('}');
            return sb.toString();
        }
    } // PostponedInstaller

//// ================ TODO : clean later ======================================
//// =================   archive        =======================================
//// ===========================================================================
//    public static ReferenceType upload(ThreadReference thread, List<Class> locals) throws Exception
//    {
//        List<byte[]> codes = new ArrayList<>();
//        for(Class clz : locals ) {
//            String name = clz.getName();
//            int lastdot = name.lastIndexOf('.');
//            String simplename = name.substring(lastdot+1);
//            InputStream is = clz.getResourceAsStream(simplename+".class");
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            for(int ch = is.read(); ch != -1; ch = is.read())
//                os.write(ch);
//            is.close();
//            byte[] code = os.toByteArray();
//            codes.add(code);
//        }
//
//
//        List<Method> getLoaders = thread.referenceType().methodsByName("getContextClassLoader");
//        Method getContextClassLoader = getLoaders.get(0);
//        ObjectReference classLoader = (ObjectReference)  thread.invokeMethod(thread,
//                                                                getContextClassLoader,
//                                                                Collections.EMPTY_LIST,
//                                                                ClassType.INVOKE_SINGLE_THREADED);
//
//        VirtualMachine remoteVM = thread.virtualMachine();
//        List<ReferenceType> arrayz = remoteVM.classesByName("byte[]");
//        ArrayType bytearrtype = (ArrayType) arrayz.get(0);
//        List<Method> defineClassZ = classLoader.referenceType().methodsByName("defineClass");
//        Optional<Method> defineClassOpt =
//                defineClassZ.stream().filter(m->m.argumentTypeNames().size()==4).findAny();
//        Method defineClass = defineClassOpt.get();
//        List<Method> resolveClassZ = classLoader.referenceType().methodsByName("resolveClass");
//        Method resolveClass = resolveClassZ.get(0);
//
//        List<ClassObjectReference> loaded = new ArrayList<>();
//        for(int inx = 0; inx < codes.size(); inx++) {
//            byte[] code = codes.get(inx);
//            String name = locals.get(inx).getName();
//            ArrayReference arref = bytearrtype.newInstance(code.length);
//            for(int ii = 0; inx<code.length;inx++)
//                arref.setValue(ii, remoteVM.mirrorOf(code[ii]));
//            ClassObjectReference myloadedclazz = (ClassObjectReference)
//                    classLoader.invokeMethod(thread, defineClass,
//                            Arrays.asList(
//                                    null,
////                                    remoteVM.mirrorOf(name),
//                                    arref,
//                                    remoteVM.mirrorOf(0),
//                                    remoteVM.mirrorOf(code.length)),
//                            ClassType.INVOKE_SINGLE_THREADED);
//            loaded.add(myloadedclazz);
//        }
//
//        ClassObjectReference root = loaded.get(0);
//        classLoader.invokeMethod(thread, resolveClass, Arrays.asList(root), ClassType.INVOKE_SINGLE_THREADED);
//        List<ReferenceType> mys = remoteVM.classesByName(root.reflectedType().name());
//        ReferenceType my = mys.get(0);
//        List<Method> ms = my.allMethods();
//        System.out.println("");
//        return my;
//    } // upload
//
//    private ReferenceType loadManually(String name, int maxAttempts) throws Exception
//    {
//        log.log(Level.INFO, "Loading class %s manually..", name);
//
//        // get remote Class.forName();
////        Method forNameMethod = getLoaderMethod();
//        StringReference nameReference = remoteVM.mirrorOf(name);
//        log.log(Level.INFO, "Starting attempts (may take a while) to fetch class %s", name);
//        ReferenceType result = null;
//        MethodEntryRequest met = remoteVM.eventRequestManager()
//                                         .createMethodEntryRequest();
//        met.enable();
//        EventQueue eveq = remoteVM.eventQueue();
//        for(int counter = 0; result == null && counter < maxAttempts; counter++) {
//            log.fine(()->"RemoteJVM:  Entering to awaiting mode... ");
//            EventSet eves = eveq.remove();
//            log.fine(()->"RemoteJVM:  resumed.");
//            HashSet<Long> checkedThreads = new HashSet<>();
//            for(EventIterator itr = eves.eventIterator(); result == null && itr.hasNext();) {
//                Event event = itr.nextEvent();
//                if ( !(event instanceof LocatableEvent) )
//                    continue;
//
//                LocatableEvent lev = (LocatableEvent) event;
//                long threadID = lev.thread().uniqueID();
//                if ( checkedThreads.contains(threadID))
//                    continue;
//                else
//                    checkedThreads.add(threadID);
//
//                ThreadReference thread = lev.thread();
///*
//                List<StackFrame> stack = thread.frames();
//                List<String> srcstack = stack.stream().map(sf-> {
//                    try {
//                        return sf.location().sourceName();
//                    } catch (Throwable th) {
//                        return null;
//                    }
//                }).collect(Collectors.toList());
//                boolean isloader1 = srcstack.stream().anyMatch((String s)->s.contains("ClassLoader"));
//                boolean isloader = stack.stream().anyMatch(f-> {
//                                            try {
//                                                LoggingVM loc = f.location();
//                                                Type declarer = loc.declaringType();
//                                                boolean res =
//                                                        !declarer.name().startsWith("java.") &&
//                                                        !declarer.name().startsWith("sun.");
//                                                return res;
//                                            } catch (Throwable th) {
//                                                String str = th.toString();
//                                                return false;
//                                            }
//                                       });
//                if ( isloader1 )
//                    continue;
//*/
//                met.disable();
//                try {
//
//                    List<Method> getLoaders = thread.referenceType().methodsByName("getContextClassLoader");
//                    Method getContextClassLoader = getLoaders.get(0);
//                    ObjectReference classLoader = (ObjectReference)
//                                                          thread.invokeMethod(thread,
//                                                                             getContextClassLoader,
//                                                                             Collections.EMPTY_LIST,
//                                                                             ClassType.INVOKE_SINGLE_THREADED);
//
///// Run new Thread(()->Class4Name))
//                    InputStream is =
//                            AsyncLoader.class.getResourceAsStream(AsyncLoader.class.getSimpleName()+".class");
////                            AsyncLoader.class.getResource("").openStream();
//                    ByteOutputStream baos= new ByteOutputStream();
//                    int ch;
//                    while ((ch=is.read())!=-1)
//                        baos.write(ch);
//                    is.close();
//                    byte[] data = baos.toByteArray();
//                    List<ReferenceType> arrayz = remoteVM.classesByName("byte[]");
//                    ArrayType bytearrtype = (ArrayType) arrayz.get(0);
//                    ArrayReference arref = bytearrtype.newInstance(data.length);
//                    for(int inx = 0; inx<data.length;inx++)
//                        arref.setValue(inx, remoteVM.mirrorOf(data[inx]));
//
//                    List<Method> defineClassZ = classLoader.referenceType().methodsByName("defineClass");
//                    Optional<Method> defineClassOpt =
//                            defineClassZ.stream().filter(m->m.argumentTypeNames().size()==4).findAny();
//                    Method defineClass = defineClassOpt.get();
//
//                    List<Method> resolveClassZ = classLoader.referenceType().methodsByName("resolveClass");
//                    Method resolveClass = resolveClassZ.get(0);
//
//                    ObjectReference myloaderclazz = (ObjectReference)
//                             classLoader.invokeMethod(thread, defineClass,
//                                                      Arrays.asList(null,
//                                                                    arref,
//                                                                    remoteVM.mirrorOf(0),
//                                                                    remoteVM.mirrorOf(data.length)),
//                                                                    ClassType.INVOKE_SINGLE_THREADED);
//
//                    classLoader.invokeMethod(thread, resolveClass,
//                                             Arrays.asList(myloaderclazz),
//                                             ClassType.INVOKE_SINGLE_THREADED);
//
//                    List<ReferenceType> myloaded = remoteVM.classesByName(AsyncLoader.class.getName());
//
//
//
///// end Run new Thread(()->Class4Name))
//
//                    List<Method> loadClassList = classLoader.referenceType().methodsByName("loadClass");
//                    Optional<Method> loadClassOpt = loadClassList.stream().filter(m->m.argumentTypeNames().size()==2).findAny();
//                    Method install = loadClassOpt.get();
//                    ClassObjectReference loadedClass = (ClassObjectReference)
//                                   classLoader.invokeMethod(thread, install,
//                                                            Arrays.asList(nameReference, remoteVM.mirrorOf(true)),
//                                                            ClassType.INVOKE_SINGLE_THREADED);
//                    Type ref = loadedClass.referenceType();
//                    Type refl = loadedClass.reflectedType();
//                    try {
//                        Object o = ((ReferenceType) refl).allLineLocations();
//                        System.out.println("O:"+o);
//                    } catch (Throwable th) {
//                        continue;
//                    }
//
//                    Method forname = getLoaderMethod();
//                    ObjectReference zz =(ObjectReference)
//                            ((ClassType)forname.declaringType())
//                                                .invokeMethod(thread, forname,
//                                                              Arrays.asList(nameReference), ClassType.INVOKE_SINGLE_THREADED);
//
//                    result = (ReferenceType) refl;
////
//
//                    if ( result != null ) {
//                        log.fine(()->"loadManually: successfully loaded class " + name);
//                    }
//                } catch (Exception ex) {
//                    log.log(Level.WARNING, ex, ()->"Loading attempt failed");
//                }
//            } // for eventset
//
//            eves.resume();
////            if ( result == null )
////                System.out.println("  attempt failed, keeping trying...");
//        } // for <loading>
//        remoteVM.eventRequestManager().deleteEventRequest(met);
//
//        if ( result == null ) {
//            RemoteClassNotFoundException cnf =
//                    new RemoteClassNotFoundException("after " + maxAttempts + " attempts: ", name);
//            log.throwing(getClass().getSimpleName(), "loadManually", cnf);
//        }
//
//        return result;
//    } // loadManually
//    private Method getLoaderMethod() throws Exception
//    {
//        List<ReferenceType> mainsList =
//                remoteVM.classesByName(Class.class.getName());
//        if ( mainsList.size() == 0 ) {
//            log.log(Level.SEVERE, "remote class java.lang.Class was not located");
//            RemoteClassNotFoundException cnf = new RemoteClassNotFoundException("", Class.class.getName());
//            log.throwing(getClass().getSimpleName(), "loadManually", cnf);
//            throw cnf;
//        }
//
//        ClassType classClass = (ClassType) mainsList.get(0);
//        List<Method> forNames =
//                classClass.methodsByName("forName",
//                        "(Ljava/lang/String;)Ljava/lang/Class;");
//        if ( forNames.size() == 0 ) {
//            log.log(Level.SEVERE, "remote java.lang.Class.forName() was not located");
//            RemoteMethodNotFoundException cnf =
//                    new RemoteMethodNotFoundException("", "forName", classClass.name());
//            log.throwing(getClass().getSimpleName(), "loadManually", cnf);
//            throw cnf;
//        }
//
//        Method forName = forNames.get(0);
//        return forName;
//    } // getLoaderMethod
}
