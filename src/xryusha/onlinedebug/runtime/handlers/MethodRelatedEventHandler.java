package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.Method;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.MethodTargetingBreakPoint;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * base class for method related breakpoint (entry/exit) handlers
 * @param <P> Breakpoint configuration type
 * @param <E> Event type
 */
public abstract class MethodRelatedEventHandler<P extends MethodTargetingBreakPoint, E extends LocatableEvent> extends EventHandlerBase<P, E>
{
    protected abstract Method getMethod(E event);

    protected final ConcurrentMap<String,List<ConfigEntry>> methodsOfClass;
    // caching decisions regarding particular method handling
    protected final ConcurrentMap<MethodKey,Optional<HandlerData.RuntimeConfig>> signatures;


    protected MethodRelatedEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
        List<ConfigEntry> methodBpEntries =
                configuration.getEntries().stream()
                        .filter(e->configSpecClass.isInstance(e.getBreakPoint()))
                        .collect(Collectors.toList());
        methodBpEntries.stream().forEach(c-> {
                                     MethodTargetingBreakPoint bp =
                                                  (MethodTargetingBreakPoint)c.getBreakPoint();
                                     if ( bp.isConstructor() )
                                         bp.setMethod("<init>");
                                });

        // TODO: check zero-actions entries
        methodsOfClass = methodBpEntries.stream().collect(Collectors.groupingBy(
                                    e->((MethodTargetingBreakPoint)e.getBreakPoint()).getTargetClass(),
                                    ConcurrentHashMap::new,
                                    Collectors.toList()
                            ));

        signatures = new ConcurrentHashMap<>();
    }


    @Override
    protected void handle(E event, HandlerData.RuntimeConfig entry, ExecutionContext ctx) throws Exception
    {
        Method m = getMethod(event);
        // not necessary exact BP config. if there are more than 1 overloaded method
        // just the last call filter is applied. exact signature must be retrieved explicitly
        // return fast as possible, skip signatures check if not needed
        MethodTargetingBreakPoint mbp = (MethodTargetingBreakPoint) entry.getConfigEntry().getBreakPoint();
        MethodKey key = new MethodKey(mbp, m.declaringType().name(), m.name(), m.signature());
        Optional<HandlerData.RuntimeConfig> cached = signatures.get(key);

        // we already checked it previously and decision was to reject
        if ( cached != null && !cached.isPresent())
            return;

        HandlerData.RuntimeConfig matched = null;
        if ( cached == null) {
            // we see it 1st time
            boolean match = match(m, mbp);
            if (match) {
                matched = entry;
                Optional<HandlerData.RuntimeConfig> signatureSpecific = Optional.ofNullable(entry);
                signatures.put(key, signatureSpecific);
            } // check all configs
            else {
                signatures.put(key, Optional.empty());
                return;
            }
        } // if !cached.isPresent()
        else
            matched = cached.get();

        super.handle(event, matched, ctx);
    } // handle

    protected boolean match(Method m, ConfigEntry  entry)
    {
        if ( !(entry.getBreakPoint() instanceof MethodTargetingBreakPoint) )
            return false;
        boolean res = match(m, (MethodTargetingBreakPoint)entry.getBreakPoint());
        return res;
    }

    protected boolean match(Method m, MethodTargetingBreakPoint breakPoint)
    {
        if ( !m.name().equals(breakPoint.getMethod()) )
            return false;

        if ( breakPoint.isAnySignature() )
            return true;

        List<String> argTypes = m.argumentTypeNames();
        boolean match = argTypes.size() == breakPoint.getParams().size();
        for(int inx = 0; match && inx < argTypes.size(); inx++) {
            match = argTypes.get(inx).equals(breakPoint.getParams().get(inx).getType());
        }
        return match;
    } // match


    // Map key class
    class MethodKey
    {
        private String clazz, method, signature;
        private MethodTargetingBreakPoint bp;

        public MethodKey(MethodTargetingBreakPoint bp, String clazz, String method, String signature)
        {
            this.clazz = clazz;
            this.method = method;
            this.signature = signature;
            this.bp = bp;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodKey methodKey = (MethodKey) o;

            if (clazz != null ? !clazz.equals(methodKey.clazz) : methodKey.clazz != null) return false;
            if (method != null ? !method.equals(methodKey.method) : methodKey.method != null) return false;
            if (signature != null ? !signature.equals(methodKey.signature) : methodKey.signature != null) return false;
            return bp == methodKey.bp/*!(bp != null ? !bp.equals(methodKey.bp) : methodKey.bp != null)*/;
        }

        @Override
        public int hashCode()
        {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (method != null ? method.hashCode() : 0);
            result = 31 * result + (signature != null ? signature.hashCode() : 0);
            result = 31 * result + (bp != null ? bp.hashCode() : 0);
            return result;
        }
    } // CheckResultKey
}
