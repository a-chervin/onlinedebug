package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.*;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.MethodEntryRequest;
import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.LineBreakpointSpec;
import xryusha.onlinedebug.config.breakpoints.MethodEntryBreakPointSpec;
import xryusha.onlinedebug.runtime.EventsProcessor;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MethodEntryEventHandler extends MethodRelatedEventHandler<MethodEntryBreakPointSpec, MethodEntryEvent>
{
    private final int MAX_REPLACING_ATTEMPTS = 3;

    protected ConcurrentMap<String, AtomicInteger> successCounts = new ConcurrentHashMap<>();

    public MethodEntryEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
    }

    @Override
    protected Method getMethod(MethodEntryEvent event)
    {
        return event.method();
    }

    @Override
    protected void handle(MethodEntryEvent event, HandlerData.RuntimeConfig runtimeData, ExecutionContext ctx) throws Exception
    {
        super.handle(event, runtimeData, ctx);

        // let's replace all method breakpoints of this class
        // by line breakpoints to make things faster.
        // Performed on 1st time particular method approached
        ThreadReference thread = event.thread();
        ReferenceType currentType = thread.frame(0).location().declaringType();
        AtomicInteger defaultVal = new AtomicInteger(0);
        AtomicInteger currentCount = successCounts.putIfAbsent(currentType.name(), defaultVal);
        if ( currentCount == null )
            currentCount = defaultVal;

        // too many fails.
        if ( currentCount != null && currentCount.get() < -1*MAX_REPLACING_ATTEMPTS ) {
            if ( currentCount.get() > Integer.MIN_VALUE ) {
                log.log(Level.WARNING, "Stopping attempts to replace MethodBreakPoints by LineBreakpoints due to error treshold");
                currentCount.set(Integer.MIN_VALUE);
            }
            return;
        }

        int successCount = currentCount.get();
        if  ( successCount == 1 /* aready succeeded*/
                || successCount == Integer.MAX_VALUE) /* currently performed by smb else */
            return;

        boolean weownit = currentCount.compareAndSet(successCount, Integer.MAX_VALUE);
        if ( !weownit )
            return;

        List<ConfigEntry> addedConfigs = new ArrayList<>();
        try {
            applyLineBreakpoints(event, runtimeData, addedConfigs);
            currentCount.set(1);
        } catch (Throwable th) {
            log.log(Level.SEVERE, "Failed to apply LineBreakpoint", th);
            try {
            } catch (Throwable throll) {
                rollbackChanges(event, addedConfigs);
                log.log(Level.SEVERE, "Failed to rollback added LineBreakpoints", throll);
            }
            currentCount.set(successCount-1);
        }
    } // handle


    private void applyLineBreakpoints(MethodEntryEvent event,
                                      HandlerData.RuntimeConfig runtimeData,
                                      List<ConfigEntry> addedEntries) throws Exception
    {
        ThreadReference thread = event.thread();
        ReferenceType currentType = thread.frame(0).location().declaringType();
        Method method = event.method();

        Map<Method, Set<ConfigEntry>> methodConfigs = getMethodConfigurations(currentType);
        for(Map.Entry<Method, Set<ConfigEntry>> mapentry : methodConfigs.entrySet()) {
            Method m = mapentry.getKey();
            Set<ConfigEntry> configs = mapentry.getValue();
            if ( configs.isEmpty() )
                continue;

            List<Location> locations = m.allLineLocations();
            if ( locations.isEmpty() ) {
                // something wrong
                log.log(Level.WARNING, "Cant find locations for method " + m);
                continue;
            }

            String name = m.declaringType().name();
            // check if not inner, if yes find encloser
            int inx = name.indexOf('$');
            String thisOrEncloser = inx == -1 ? name : name.substring(0, inx);
            Location loc = locations.get(0);
            for(ConfigEntry entry : configs) {
                ConfigEntry newEntry = entry.clone();
                LineBreakpointSpec bp = new LineBreakpointSpec(thisOrEncloser, loc.lineNumber());
                newEntry.setBreakPoint(bp);
                runtimeData.getJvm().apply(newEntry);
                addedEntries.add(newEntry);
            }
        } // eof for matching methods
        MethodEntryRequest req = (MethodEntryRequest) event.request();
        req.disable();
        req.addClassExclusionFilter(currentType.name());
        req.enable();
    } // applyLineBreakpoints

    private Map<Method, Set<ConfigEntry>> getMethodConfigurations(ReferenceType currentType)
    {
        Set<ReferenceType> typesToCheck = new HashSet<>();
        Queue<ReferenceType> toCheck = new ArrayDeque<>();
        toCheck.add(currentType);
        while(!toCheck.isEmpty()) {
            ReferenceType type = toCheck.poll();
            if ( methodsOfClass.containsKey(type.name()))
                typesToCheck.add(type);
            if ( type instanceof ClassType) {
                ClassType clazz = (ClassType) type;
                type = clazz.superclass();
                if ( type == null ) // Object.class
                    continue;
                toCheck.add(type);
                toCheck.addAll(clazz.allInterfaces());
            } else if (type instanceof InterfaceType) {
                InterfaceType intf = (InterfaceType)type;
                toCheck.addAll(intf.superinterfaces());
            }
        } // while toCheck

        Set<ConfigEntry> relatedConfs =
                typesToCheck.stream()
                        .filter(t->methodsOfClass.containsKey(t.name()))
                        .flatMap(t-> methodsOfClass.get(t.name()).stream())
                        .collect(Collectors.toSet());

        List<Method> declaredInCurrent =
                currentType.allMethods().stream()
                                        .filter(m-> !m.isAbstract() && m.declaringType().equals(currentType))
                                        .collect(Collectors.toList());

        return declaredInCurrent.stream().collect(Collectors.toMap(
                Function.identity(),
                m -> relatedConfs.stream()
                                 .filter(c->match(m,c))
                                 .collect(Collectors.toSet())
            ) // toMap
        );
    } // getMethodConfigurations

    private void rollbackChanges(MethodEntryEvent event,  List<ConfigEntry> addedConfigs)
    {
        List<BreakpointRequest> allBPs = event.virtualMachine().eventRequestManager().breakpointRequests();
        List<BreakpointRequest> related = allBPs.stream()
                .filter(bp-> isRelated(bp, addedConfigs))
                .collect(Collectors.toList());
        event.virtualMachine().eventRequestManager().deleteEventRequests(related);
    } // rollbackChanges

    private boolean isRelated(BreakpointRequest request, List<ConfigEntry> addedConfigs)
    {
        HandlerData data =
                (HandlerData) request.getProperty(EventsProcessor.HANDLER_DATA_PN);
        if ( data == null )
            return false;
        List<ConfigEntry> configs = data.getConfig().stream()
                .map(r->r.getConfigEntry())
                .collect(Collectors.toList());
        boolean found = addedConfigs.stream().anyMatch(c->configs.contains(c));
        return found;
    } // isRelated
}
