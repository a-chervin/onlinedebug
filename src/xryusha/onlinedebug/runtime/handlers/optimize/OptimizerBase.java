package xryusha.onlinedebug.runtime.handlers.optimize;

import com.sun.jdi.ThreadReference;
import xryusha.onlinedebug.runtime.HandlerData;

public class OptimizerBase implements Optimizer
{
    protected int optimizationTreshold = 20;

    @Override
    public void condition(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception
    {
        if (  runtimeConfig.getConfigEntry().getCondition() == null ||
               runtimeConfig.getConditionChecks() < optimizationTreshold ||
                 runtimeConfig.getConfigEntry().getCondition() instanceof OptimizedCondition )
        return;
        _condition(thread, runtimeConfig);
        runtimeConfig.conditionChecked();
    }

    @Override
    public void actions(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception
    {
        if ( runtimeConfig.getActionUse() < optimizationTreshold ||
                runtimeConfig.getActions() == null ||
                runtimeConfig.getActions().size() == 0 ||
                runtimeConfig.getActions().get(0) instanceof OptimizedAction )
            return;

        _actions(thread, runtimeConfig);
        runtimeConfig.actionUsed();
    }

    // NOOP. Implemented in extenders
    protected void _condition(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig)
    {
    }

    // NOOP. Implemented in extenders
    protected void _actions(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig)
    {
    }
}
