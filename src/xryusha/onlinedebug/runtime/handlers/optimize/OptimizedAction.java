package xryusha.onlinedebug.runtime.handlers.optimize;

import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.ActionSpec;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.actions.Action;

public class OptimizedAction extends Action
{
    private final String remoteActionClass;

    public OptimizedAction(ActionSpec spec, String remoteActionClass)
    {
        super(spec);

        this.remoteActionClass = remoteActionClass;
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {

    }
}
