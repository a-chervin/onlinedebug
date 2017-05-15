package xryusha.onlinedebug.runtime.handlers.optimize;

import xryusha.onlinedebug.config.conditions.AbstractConditionSpec;

public class OptimizedCondition extends AbstractConditionSpec
{
    private final String remoteConditionClass;

    public OptimizedCondition(String remoteConditionClass)
    {
        this.remoteConditionClass = remoteConditionClass;
    }

    public String getRemoteConditionClass()
    {
        return remoteConditionClass;
    }
}
