package xryusha.onlinedebug.runtime.actions;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.*;
import xryusha.onlinedebug.runtime.func.ErrorproneBiFunction;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.RemotingBase;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;


public abstract class Action<T extends ActionSpec> extends RemotingBase
{
    protected final T spec;
    protected static final ConcurrentMap<Class,
            ErrorproneBiFunction<ThreadReference,ActionSpec,Action>> actionFactory =

                           new ConcurrentHashMap<Class, ErrorproneBiFunction<ThreadReference,ActionSpec,Action>>() {{
                                   put(PrintSpec.class, (t, c)->new PrintAction(t,(PrintSpec)c));
                                   put(AssignSpec.class,(t, c)->new AssignAction((AssignSpec)c));
                                   put(ReturnSpec.class,(t, c)->new ReturnAction(t, (ReturnSpec)c));
                                   put(InvokeSpec.class,(t, c)->new InvokeAction((InvokeSpec)c));
                               }};


    public static Action compile(ThreadReference thread, ActionSpec actionConfig) throws Exception
    {
        ErrorproneBiFunction<ThreadReference,ActionSpec,Action> factory =
                                                         actionFactory.get(actionConfig.getClass());
        if ( factory == null ) {
            log.log(Level.SEVERE, "Unexpected action: " + actionConfig);
            throw new UnsupportedOperationException("Unexpected action: " + actionConfig);
        }
        Action action = factory.apply(thread, actionConfig);
        return action;
    }

    protected Action(T spec)
    {
        this.spec = spec;
    }

    public abstract void execute(LocatableEvent event, ExecutionContext ctx) throws Exception;
}
