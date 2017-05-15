package xryusha.onlinedebug.runtime.handlers.optimize;

import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.ActionSpec;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.actions.Action;

public abstract class AbstractOptimizedAction extends Action
{
    private final String remoteActionClass;

    public AbstractOptimizedAction(ActionSpec spec, String remoteActionClass)
    {
        super(spec);

        this.remoteActionClass = remoteActionClass;
    }
}
