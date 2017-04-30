package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.Method;
import com.sun.jdi.event.MethodExitEvent;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.MethodExitBreakPointSpec;
import xryusha.onlinedebug.config.values.eventspecific.ReturnValue;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;

public class MethodExitEventHandler extends MethodRelatedEventHandler<MethodExitBreakPointSpec, MethodExitEvent>
{
    public MethodExitEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
    }

    @Override
    protected void onStart(MethodExitEvent event, HandlerData data, ExecutionContext ctx)
    {
        ctx.setEventSpecificValue(ReturnValue.class, event.returnValue());
    }

    @Override
    protected Method getMethod(MethodExitEvent event)
    {
        return event.method();
    }
}
