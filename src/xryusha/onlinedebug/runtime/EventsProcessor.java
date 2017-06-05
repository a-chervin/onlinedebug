package xryusha.onlinedebug.runtime;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final static int MAX_INITIALIZATION_ATTEMPTS = 5;
    private final static Logger log = Log.getLogger();
    private final static AtomicInteger asyncInitializationAttempts = new AtomicInteger(0);
    private final VirtualMachine virtualMachine;
    private final ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations;
    private final Map<Class<? extends Event>, HandlerCall> eventHandlers = new HashMap<>();
    private final ConcurrentMap<String,ExecutorService> eventExecutors = new ConcurrentHashMap<>();
    private final Map<String,EventProcessingEntry> currentlyProcessedEvent = new ConcurrentHashMap<>();
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
            // collect events and group by tread
            ArrayList<Event> pending = new ArrayList<>();
            for (EventIterator itr = events.eventIterator(); itr.hasNext(); ) {
                Event e = itr.nextEvent();
                pending.add(e);
                log.log(Level.FINE, () -> "handlePendingEvents: retrieved event " + e);
            }



            ThreadReference[] threadHolder = new ThreadReference[1];
            Map<String, List<EventProcessingEntry>> eventGroups =
                    pending.stream()
                           .map(e -> {
                                ThreadReference thread =  nullThread;
                                if ( e instanceof LocatableEvent) {
                                    thread = ((LocatableEvent) e).thread();
                                    threadHolder[0] = thread;
                                }
                                EventProcessingEntry ee = new EventProcessingEntry(e, thread);
                                return ee;
                             })
                             .collect(Collectors.groupingBy(entry -> entry.thread.name()));

            //  Try to init async executor if it was not yet.
            if (async == null && threadHolder[0] != null
                          && asyncInitializationAttempts.get() < MAX_INITIALIZATION_ATTEMPTS ) {
                asyncInitializationAttempts.incrementAndGet();
                async = AsyncRemoteExecutor.getInstance();
                try {
                    AsyncRemoteExecutor inst = AsyncRemoteExecutor.getInstance();
                    if (!inst.init(threadHolder[0]))
                        async = null;
                } catch (Throwable th) {
                    log.log(Level.SEVERE, "AsyncRemoteExecutor initialization fail", th);
                }
                if (asyncInitializationAttempts.get() < MAX_INITIALIZATION_ATTEMPTS)
                    log.log(Level.SEVERE, "Stopping attempts to install async executor");
            }

            // if thread non-bounded events (e.g. ClassPrepareEvent) fired,
            // full events.resume() should be called (no thread for thread.resume())
            final boolean nonBoundExists = eventGroups.containsKey(nullThread.name());
            final CountDownLatch cdl = nonBoundExists ? new CountDownLatch(pending.size()) : null;
                for (Map.Entry<String, List<EventProcessingEntry>> group : eventGroups.entrySet()) {
                    ThreadReference thread = group.getValue().get(0).thread;
                    List<EventProcessingEntry> threadEventEntries = group.getValue();
                    AtomicInteger threadEventsCnt = new AtomicInteger(threadEventEntries.size());
                    ExecutionContext ctx =
                            ExecutionContext.createThreadBoundedContext(thread, System.currentTimeMillis());
                    ExecutorService executor =
                            eventExecutors.computeIfAbsent(
                                    thread.name(), (k) ->
                                            Executors.newSingleThreadExecutor(new DaemonThreadFactory()));

                    for (EventProcessingEntry eventProcessingEntry : threadEventEntries) {
                        EventProcessingEntry beingProcessed = null;
                        if (thread != nullThread &&
                                (beingProcessed = currentlyProcessedEvent.putIfAbsent(thread.name(), eventProcessingEntry)) != null) {
                            // previous event handling on this thread is not terminated yet, which means that
                            // the event was fired by event processing itself. Potentially deadlock condition.
                            EventProcessingEntry beinfin = beingProcessed;
                            StringBuffer sb = new StringBuffer();
                            sb.append("\n=======================================================\n")
                              .append("*  WARNING: handlePendingEvents: retrieved event         *\n")
                              .append("*   ").append(eventProcessingEntry.event).append(" *\n")
                              .append("*   was fired during handling of                         *\n")
                              .append("*   ").append(beinfin.event).append(" *\n")
                              .append("*   To prevent deadlocks previous event handling         *\n")
                              .append("*   will be terminated.                                  *\n")
                              .append("=========================================================*\n");
                            log.log(Level.WARNING, sb.toString());
                            beingProcessed.handlingThread.interrupt();
                            thread.resume();
                            continue;
                        }
                        executor.submit(() -> {
                            try {
                                eventProcessingEntry.handlingThread = Thread.currentThread();
                                log.log(Level.FINE, ()->"Starting processing for " + eventProcessingEntry.event);
                                handleEvent(eventProcessingEntry.event, ctx);
                            } catch (Throwable th) {
                                log.log(Level.SEVERE, "Failed handling of " + eventProcessingEntry.event, th);
                            } finally {
                                if (cdl != null)
                                    cdl.countDown();
                                log.log(Level.FINE, ()->"Completed processing for " + eventProcessingEntry.event);
                                currentlyProcessedEvent.remove(thread.name());
                                if ( threadEventsCnt.decrementAndGet() == 0 )
                                   thread.resume();
                            }
                        });
                    } // for threadEvents
                } // for groups
                if (nonBoundExists) {
                    cdl.await();
                    events.resume();
                }
                log.fine("--ended events loop--");
        } catch(Throwable th){
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
                                    result = "==synthetic-tread==";
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

    private class EventProcessingEntry
    {
        Event event;
        ThreadReference thread;
        Thread handlingThread;

        EventProcessingEntry(Event event, ThreadReference thread)
        {
            this.event = event;
            this.thread = thread;
        }
    }

}
