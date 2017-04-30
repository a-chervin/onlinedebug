package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class FromStaticInner extends Flow
{
    private static int statCounter = 0;

    @Override
    public void reset()
    {
        statCounter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        StaticInner in = new StaticInner();
        in.innF();

    }

    static class StaticInner
    {
        int st_innerInt = 8;
        static int st_innerStaticInt = 8;

        void innF()
        {
            String BP = "rwerwerew";
        }
    }
}
