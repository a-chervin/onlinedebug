package xryusha.onlinedebug.runtime.handlers;

import com.sun.jdi.event.ModificationWatchpointEvent;
import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.FieldModificationBreakPointSpec;
import xryusha.onlinedebug.config.values.eventspecific.ModificationCurrent;
import xryusha.onlinedebug.config.values.eventspecific.ModificationNew;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.HandlerData;


public class FieldModificationEventHandler extends EventHandlerBase<FieldModificationBreakPointSpec, ModificationWatchpointEvent>
{
    public FieldModificationEventHandler(Configuration configuration) throws Exception
    {
        super(configuration);
    }

    @Override
    protected void onStart(ModificationWatchpointEvent event, HandlerData data, ExecutionContext ctx)
    {
        ctx.setEventSpecificValue(ModificationCurrent.class, event.valueCurrent());
        ctx.setEventSpecificValue(ModificationNew.class, event.valueToBe());
    }
}
