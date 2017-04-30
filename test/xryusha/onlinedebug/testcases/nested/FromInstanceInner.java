package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class FromInstanceInner extends Flow
{
    private int counter = 0;
    private static int statCounter = 0;

    @Override
    public void reset()
    {
        counter = 0;
        statCounter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        counter = 5;
        statCounter = 6;
        f();
        return null;
    }

    void f()
    {
        Inner in = new Inner();
        in.innF();
    }

    class Inner
    {
        int innerInt = 7;
        int innerStaticInt = 8;

        void innF()
        {
            String BP = "rwerwerew";
        }
    }
}
