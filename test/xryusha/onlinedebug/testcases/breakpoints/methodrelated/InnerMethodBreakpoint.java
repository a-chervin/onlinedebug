package xryusha.onlinedebug.testcases.breakpoints.methodrelated;

import xryusha.onlinedebug.testcases.Flow;

public class InnerMethodBreakpoint extends Flow
{
    int counter = 0;

    @Override
    public void reset()
    {
        counter = 0;
    }

    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        Inner in = new Inner();
        String BP = "e53453";
        in.innerF();
        BP = "e53453";
        reset();
        in.innerF();
        BP = "e53453";
    }

    class Inner
    {
        void innerF()
        {
            String bp = "eretrtrt";
        }
    }
}
