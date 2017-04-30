package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.event.BreakpointEvent;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.LineBreakpointSpec;


public class LineBreakpointEventHandler extends EventHandlerBase<LineBreakpointSpec, BreakpointEvent>
{
    public LineBreakpointEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
    }
}
