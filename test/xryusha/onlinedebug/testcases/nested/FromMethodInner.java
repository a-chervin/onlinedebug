package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class FromMethodInner extends Flow
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
        counter = 4;
        statCounter = 5;
        f(9);
        return null;
    }

    void f(int arg)
    {
        final int val = 6;
        class N
        {
            void nf()
            {
                String BP = "rwerwerew";
            }
        }

        N n = new N();
        n.nf();
    } // methodInn
}
