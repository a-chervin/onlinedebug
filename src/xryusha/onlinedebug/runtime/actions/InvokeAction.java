package xryusha.onlinedebug.runtime.actions;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.InvokeSpec;
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.config.values.Ref;
import xryusha.onlinedebug.config.values.RefChain;
import xryusha.onlinedebug.runtime.ExecutionContext;

public class InvokeAction extends Action<InvokeSpec>
{
    private Ref invocationPath;

    public InvokeAction(InvokeSpec spec)
    {
        super(spec);
        CallSpec call = new CallSpec(spec.getType(), spec.getMethod());
        call.getParams().addAll(spec.getParams());
        Ref target = spec.getAccessPath();
        if ( target != null ) {
            RefChain chain = new RefChain();
            chain.getRef().add(target);
            chain.getRef().add(call);
            invocationPath = chain;
        } else
            invocationPath = call;
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        Value retval = super.getValue(thread, invocationPath);
    }


}
