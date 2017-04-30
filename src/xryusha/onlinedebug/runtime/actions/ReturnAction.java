package xryusha.onlinedebug.runtime.actions;

import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.ReturnSpec;
import xryusha.onlinedebug.config.values.Const;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.PrimitiveValueFactory;


public class ReturnAction extends Action<ReturnSpec>
{
    public ReturnAction(ThreadReference thread, ReturnSpec spec) throws Exception
    {
        super(spec);
        if ( !thread.virtualMachine().canForceEarlyReturn() )
            throw new UnsupportedOperationException("Remove JVM does not support earlyReturn operation");
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        RValue rvalSpec = spec.getReturnValue();
        if ( rvalSpec instanceof Const ) {
            Method m = event.location().method();
            Const cnst = (Const) rvalSpec;
            if ( !m.returnType().name().equals(cnst.getType()) &&
                    PrimitiveValueFactory.canConvert(m.returnType()))
                cnst.setType(m.returnType().name());
        }

        Value returnValue = getValue(thread, rvalSpec);
        thread.forceEarlyReturn(returnValue);
    }
}
