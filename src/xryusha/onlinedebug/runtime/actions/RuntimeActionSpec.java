package xryusha.onlinedebug.runtime.actions;

import xryusha.onlinedebug.config.actions.ActionSpec;

public class RuntimeActionSpec implements ActionSpec
{
    private Action action;

    public RuntimeActionSpec()
    {
    }

    public Action getAction()
    {
        return action;
    }

    public void setAction(Action action)
    {
        this.action = action;
    }
}
