package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.event.*;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.ExceptionBreakpointSpec;
import xryusha.onlinedebug.config.values.eventspecific.CurrentException;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;


public class ExceptionBreakpointEventHandler extends EventHandlerBase<ExceptionBreakpointSpec, ExceptionEvent>
{
    public ExceptionBreakpointEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
    }

    @Override
    protected void onStart(ExceptionEvent event, HandlerData data, ExecutionContext ctx)
    {
        ctx.setEventSpecificValue(CurrentException.class, event.exception());
    }
}
