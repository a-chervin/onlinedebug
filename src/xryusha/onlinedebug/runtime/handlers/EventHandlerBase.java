package xryusha.onlinedebug.runtime.handlers;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;


import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.actions.ActionSpec;
import xryusha.onlinedebug.config.breakpoints.AbstractBreakPointSpec;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;
import xryusha.onlinedebug.runtime.RemotingBase;
import xryusha.onlinedebug.runtime.actions.Action;
import xryusha.onlinedebug.runtime.conditions.ConditionEvaluator;


public abstract class EventHandlerBase<P extends AbstractBreakPointSpec, E extends LocatableEvent> extends RemotingBase
{
    protected final static Logger log = Log.getLogger();

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    protected final Class<? extends AbstractBreakPointSpec> configSpecClass;
    protected final Class<? extends LocatableEvent> handledEventClass;


    protected EventHandlerBase(Configuration configuration) throws Exception
    {
        Class clazz;
        for ( clazz = this.getClass();
                !(clazz.getGenericSuperclass() instanceof ParameterizedType);
                             clazz = clazz.getSuperclass() );

        ParameterizedType handlerBaseType = (ParameterizedType) clazz.getGenericSuperclass();
        Type realSpecType = handlerBaseType.getActualTypeArguments()[0];
        configSpecClass = (Class<? extends AbstractBreakPointSpec>) Class.forName(realSpecType.getTypeName());
        Type eventType = handlerBaseType.getActualTypeArguments()[1];
        handledEventClass = (Class<? extends LocatableEvent>) Class.forName(eventType.getTypeName());
    } // ctor


    public void handle(E event, HandlerData data, ExecutionContext ctx) throws Exception
    {
        if ( data == null ) {
            log.severe(getClass().getSimpleName() + ": missing HandlerData: " + event);
            return;
        }
        onStart(event, data, ctx);
        for(HandlerData.RuntimeConfig runtimeData: data.getConfig()) {
            handle(event, runtimeData, ctx);
        }
    } // handle

    protected void onStart(E event, HandlerData data, ExecutionContext ctx)
    {
    }

    protected void handle(E event, HandlerData.RuntimeConfig runtimeConfig, ExecutionContext ctx) throws Exception
    {
        ConfigEntry config = runtimeConfig.getConfigEntry();
        if ( config.getCondition() != null ) {
            try {
                boolean match = conditionEvaluator.evaluate(event.thread(), config.getCondition());
                if ( !match ) {
                    log.fine(()->getClass().getSimpleName()
                            + " : event " + event
                            + " did not match condition " + config.getCondition());
                    return;
                }
            } catch (Throwable th) {
                log.log(Level.SEVERE, "event " + event
                        +" ignorred as evaluation failed for condition: "
                        + config.getCondition(), th);
                return;
            }
        } // if condition
        ThreadReference thread = event.thread();
        List<Action> actions;
        if ( (actions = runtimeConfig.getActions())== null ) {
            actions = new ArrayList<>();
            for(ActionSpec spec : config.getActionList()) {
                Action action = Action.compile(thread, spec);
                actions.add(action);
            }
            runtimeConfig.setActions(actions);
        } // if runtimeConfig.getActions()== null

        for(Action action: actions) {
            try {
                action.execute(event, ctx);
            } catch (Throwable th) {
                log.log(Level.SEVERE, th, ()->"action failed on: " + event);
            }
        }
    } // handle
}