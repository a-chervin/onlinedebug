package xryusha.onlinedebug.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.*;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.runtime.handlers.*;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.runtime.util.DaemonThreadFactory;

/**
 * starts and manages event process handling process
 */
public class EventsProcessor
{
    public final static String HANDLER_DATA_PN = "handlerData";

    private final static Logger log = Log.getLogger();
    private final VirtualMachine virtualMachine;
    private final ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations;
    private final ConcurrentMap<String,ExecutorService> treadSpecificExecutors = new ConcurrentHashMap<>();
    private final ExecutorService generalExecutor;

    /**
     * defines invocation of event handler
     * @param <E> event type
     */
    @FunctionalInterface
    public interface HandlerCall<E extends Event>
    {
        void  handle(E event, HandlerData data, ExecutionContext ctx) throws Exception;
    }

    private final Map<Class<? extends Event>, HandlerCall> eventHandlers = new HashMap<>();


    public EventsProcessor(VirtualMachine virtualMachine, Configuration config,
                           ConcurrentMap<String,Function<List<ReferenceType>,Boolean>> postponedRegistrations) throws Exception
    {
        if ( virtualMachine == null )
            throw new IllegalArgumentException("remote VM is null");

        this.virtualMachine = virtualMachine;
        this.postponedRegistrations = postponedRegistrations;
        generalExecutor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());

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
            ArrayList<Future> pending = new ArrayList<>();
            for (EventIterator itr = events.eventIterator(); itr.hasNext(); ) {
                Event event = itr.next();
                long eventTime = System.currentTimeMillis();
                if ( event instanceof LocatableEvent ) {
                    // thread bounded events are processed in
                    // parallel way, separate executor for events of
                    // each thread
                    LocatableEvent threadOwned = (LocatableEvent) event;
                    final ThreadReference thread = threadOwned.thread();
                    ExecutionContext ctx = ExecutionContext.createThreadBoundedContext(thread, eventTime);
                    String executorK = thread.name();
                    ExecutorService executor =
                            treadSpecificExecutors.computeIfAbsent(executorK,
                                    (k)->Executors.newSingleThreadExecutor(new DaemonThreadFactory()));
                    Callable cl = ()-> {   try {
                                              handleEvent(event, ctx);
                                              return null;
                                            } finally {
                                               ExecutionContext.closeContext(thread);
                                           }
                                        };
                    Future future = executor.submit(cl);
                    pending.add(future);
                } else {
                    ExecutionContext ctx = new ExecutionContext(eventTime);
                    Callable cl = ()->{ handleEvent(event, ctx); return null; };
                    Future future = generalExecutor.submit(cl);
                    pending.add(future);
                }
                log.fine(()->"handling event: " + event);
            } // for events
            // waiting until all event processing completed before resumin vm
            for(Future fu : pending) {
                try {
                    fu.get();
                } catch (Throwable th) {
                    log.log(Level.SEVERE, "handlePendingEvents.handleEvent", th);
                }
            }
            log.fine("--ended events loop--");
        } catch (Throwable th) {
            log.log(Level.SEVERE, "handlePendingEvents.handleEventLoop", th);
        } finally {
            events.resume();
        }
    } // handleNextEvent



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
}