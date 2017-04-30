package xryusha.onlinedebug.testcases.actions;

import xryusha.onlinedebug.testcases.Flow;

public class ReturnValue extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        int retVal = 7;
        String BP = "sdfsdg";
        retVal = deep(2);
        BP = "rtyrtyr";
    }

    private int deep(int i)
    {
        String BP = "1234";
        return 8;
    }
}
