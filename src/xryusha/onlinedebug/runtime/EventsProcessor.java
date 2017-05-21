package xryusha.onlinedebug.runtime;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.*;
import java.util.stream.Collectors;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.runtime.handlers.*;
import xryusha.onlinedebug.runtime.util.AsyncRemoteExecutor;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.runtime.util.DaemonThreadFactory;

/**
 * starts and manages event handling process
 */
public class EventsProcessor
{
    public final static String HANDLER_DATA_PN = "handlerData";

    private final static Logger log = Log.getLogger();
    private final VirtualMachine virtualMachine;
    private final ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations;
    private final Map<Class<? extends Event>, HandlerCall> eventHandlers = new HashMap<>();
    private final ConcurrentMap<String,ExecutorService> eventExecutors = new ConcurrentHashMap<>();
    private ThreadReference nullThread = null;
    private AsyncRemoteExecutor async = null;


    public EventsProcessor(VirtualMachine virtualMachine, Configuration config,
                           ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations) throws Exception
    {
        if ( virtualMachine == null )
            throw new IllegalArgumentException("remote VM is null");

        this.virtualMachine = virtualMachine;
        this.postponedRegistrations = postponedRegistrations;
        nullThread = createNullThread();

        LineBreakpointEventHandler lineBPH = new LineBreakpointEventHandler(config);
        eventHandlers.put(BreakpointEvent.class,  (e,rt,cx)->lineBPH.handle((BreakpointEvent) e, rt, cx));

        ExceptionBreakpointEventHandler exBPH = new ExceptionBreakpointEventHandler(config);
        eventHandlers.put(ExceptionEvent.class, (e,rt,cx)->exBPH.handle((ExceptionEvent) e, rt, cx));

        MethodEntryEventHandler methodEntryBPH = new MethodEntryEventHandler(config);
        eventHandlers.put(MethodEntryEvent.class, (e,rt,cx)->methodEntryBPH.handle((MethodEntryEvent) e, rt, cx));

        MethodExitEventHandler methodExitBPH = new MethodExitEventHandler(config);
        eventHandlers.put(MethodExitEvent.class, (e,rt,cx)->methodExitBPH.handle((MethodExitEvent) e, rt, cx));

        FieldModificationEventHandler modificationBPH = new FieldModificationEventHandler(config);
        eventHandlers.put(ModificationWatchpointEvent.class,
                          (e,rt,cx)->modificationBPH.handle((ModificationWatchpointEvent) e,rt,cx));

        eventHandlers.put(ClassPrepareEvent.class, (e,rt,cx)->handleClassPrepareEvent((ClassPrepareEvent) e,rt,cx));
    }


    public void handlePendingEvents() throws InterruptedException
    {
        EventQueue queue = virtualMachine.eventQueue();
        log.fine("--waiting for events--");
        EventSet events = queue.remove();
        try {
            ArrayList<Event> pending = new ArrayList<>();
            for (EventIterator itr = events.eventIterator(); itr.hasNext(); )
                pending.add(itr.nextEvent());
            Map<ThreadReference,List<Event>> eventGroups =
                    pending.stream().collect(Collectors.groupingBy(
                                        event-> ( event instanceof LocatableEvent )?
                                                ((LocatableEvent)event).thread() : nullThread
            ));
            // if thread non-bounded events (e.g. ClassPrepareEvent) fired,
            // full events.resume() should be called (no thread for thread.resume())
            final boolean nonBoundExists = eventGroups.containsKey(nullThread);
            final CountDownLatch cdl = /*nonBoundExists ?
                                         new CountDownLatch(pending.size()) :
                                           null*/
                                       new CountDownLatch(pending.size())
                    ;
            for(Map.Entry<ThreadReference,List<Event>> group : eventGroups.entrySet()) {
                ThreadReference thread = group.getKey();
                List<Event> threadEvents = group.getValue();
                ExecutionContext ctx =
                        ExecutionContext.createThreadBoundedContext(thread,
                                System.currentTimeMillis());
                ExecutorService executor =
                        eventExecutors.computeIfAbsent(
                                thread.name(), (k) ->
                                        Executors.newSingleThreadExecutor(new DaemonThreadFactory()));

                for (Event e : threadEvents) {
                    executor.submit(() -> {
                        try {
                            // no locks required. if async.init() called twice
                            // 2nd call is just noop. Anyway, try init just once,
                            // if fails - don't retry
                            if ( async == null && thread != nullThread) {
                                async = AsyncRemoteExecutor.getInstance();
                                try {
                                    async.init(thread);
                                } catch (Throwable th) {
                                    log.log(Level.SEVERE,
                                            "AsyncRemoteExecutor initialization fail", th);
                                }
                            }
                            log.fine(()->"handling event: " + e);
                            handleEvent(e, ctx);
                        } catch (Throwable th) {
                            log.log(Level.SEVERE, "Failed handling of " + e, th);
                        } finally {
                            if ( cdl != null )
                                cdl.countDown();
                        }
                    });
                } // for threadEvents
                executor.submit(()->thread.resume());
            } // for groups
            if ( nonBoundExists ) {
                cdl.await();
                events.resume();
            }
            log.fine("--ended events loop--");
        } catch (Throwable th) {
            log.log(Level.SEVERE, "handlePendingEvents.handleEventLoop", th);
        }
    } // handlePendingEvents


    private void handleEvent(Event event, ExecutionContext ctx) throws Exception
    {
        Class eventTypeKey = null;
        // find type of particular *Impl class
        for(Class type: eventHandlers.keySet()) {
            if ( type.isInstance(event) ) {
                eventTypeKey = type;
                break;
            }
        }
        if ( eventTypeKey == null ) {
            log.severe(()->"Unsupported event type: " + event);
            return;
        }

        HandlerCall call = eventHandlers.get(eventTypeKey);
        HandlerData data =
                (HandlerData) event.request().getProperty(HANDLER_DATA_PN);
        call.handle(event, data, ctx);
    } // handleEvent


    /**
     * class prepared event is monitored when during applying breakpoints relevant class was not loaded yet.
     * in such case delayed breakpoint creation registered and performed when required class is loaded
     */
    private void handleClassPrepareEvent(ClassPrepareEvent event, HandlerData data, ExecutionContext ctx)
    {
        if ( postponedRegistrations == null )
            return;
        ReferenceType loadedType = event.referenceType();
        Function<List<ReferenceType>,Boolean> postponed = postponedRegistrations.get(loadedType.name());
        if ( postponed == null ) {
            // may be it was one of it's inners?
            String name = loadedType.name();
            for(int innernameEnd = name.indexOf('$', 0);
                          innernameEnd > -1 && postponed == null;
                              innernameEnd = name.indexOf('$', innernameEnd)) {
                String innername = name.substring(0, innernameEnd+1)+"*";
                postponed = postponedRegistrations.get(innername);
            }
        }
        if ( postponed == null ) {
            return;
        }
        try {
            boolean success = postponed.apply(Arrays.asList(loadedType));
        } catch (Throwable th) {
            log.log(Level.SEVERE, "postponed action failed", th);
        }
    } // handleClassPrepareEvent

    private ThreadReference createNullThread()
    {
        ThreadReference thread = (ThreadReference)
                Proxy.newProxyInstance(ThreadReference.class.getClassLoader(),
                        new Class[]{ThreadReference.class},
                        (proxy, method, args) -> {
                            Object result = null;
                            switch (method.getName()) {
                                case "name":
                                    result = "";
                                    break;
                                case "uniqueID":
                                    result = Long.MIN_VALUE;
                                    break;
                                case "hashCode":
                                    result = Integer.MIN_VALUE;
                                    break;
                                case "equals":
                                    result = args[0] == this;
                                    break;
                            }
                            return result;
                        });
        return thread;
    } //

    /**
     * defines invocation of event handler
     * @param <E> event type
     */
    @FunctionalInterface
    public interface HandlerCall<E extends Event>
    {
        void  handle(E event, HandlerData data, ExecutionContext ctx) throws Exception;
    }
}
