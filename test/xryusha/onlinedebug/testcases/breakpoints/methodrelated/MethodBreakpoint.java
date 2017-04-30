package xryusha.onlinedebug.testcases.breakpoints.methodrelated;

import xryusha.onlinedebug.testcases.Flow;

public class MethodBreakpoint extends Flow
{
    int counter = 7;
    int staticcounter = 7;

    @Override
    public void reset()
    {
        counter = 7;
        staticcounter = 7;
    }

    @Override
    public Object call() throws Exception
    {
        root();
        return null;
    }

    void root()
    {
        String BP = "dsfdsfsd";
        f(7,"AS");
        BP = "dsfdsfsd";
        reset();
        f(7,"AS");
        BP = "dsfdsfsd";
        fStat();
        BP = "dsdfdfgdfgd";
        reset();
        fStat();
        BP = "dsdfdfgdfgd";
        f(8,"XP",false);
    }

    int  f(int ii, String str)
    {
        return 15;
    }

    int  f(int ii, String str, boolean b)
    {
        return 16;
    }

    int  fN(int ii, String str)
    {
        return 17;
    }

    static int fStat()
    {
        String BP = "dsfdsfsd";
        return 18;
    }
}
