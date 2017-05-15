package xryusha.onlinedebug.runtime.handlers.optimize;

import xryusha.onlinedebug.config.conditions.AbstractConditionSpec;

public abstract class AbstractOptimizedCondition extends AbstractConditionSpec
{
    private final String remoteConditionClass;

    public AbstractOptimizedCondition(String remoteConditionClass)
    {
        this.remoteConditionClass = remoteConditionClass;
    }

    public String getRemoteConditionClass()
    {
        return remoteConditionClass;
    }
}
