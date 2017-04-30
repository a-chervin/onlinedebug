package xryusha.onlinedebug.testcases.specialvalues;

import xryusha.onlinedebug.testcases.Flow;

public class ReturnValue extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    int f()
    {
        return 168;
    }
}
