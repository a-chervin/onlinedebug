package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class NestedStaticFirstCall extends Flow
{
    static Nested_T1 t1 = new Nested_T1();

    @Override
    public Object call() throws Exception
    {
        f(9);
        return null;
    }

    public void reset()
    {
        t1 = new Nested_T1();
    }

    void f(int arg)
    {
        String BP = "-bp"+arg;
        t1.inst_ii = 100;
        t1.inst_t2.inst_ii = 200;
        BP = "-bp"+arg;
    }

    Nested_T1 getT1()
    {
        return t1;
    }
}
