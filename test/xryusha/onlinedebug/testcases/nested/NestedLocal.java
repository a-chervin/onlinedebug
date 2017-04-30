package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class NestedLocal extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f(9);
        return null;
    }

    void f(int arg)
    {
        Nested_T1 t1 = new Nested_T1();
        String BP = "-bp"+arg;
        t1.inst_ii = 100;
        t1.inst_t2.inst_ii = 200;
        BP = "-bp"+arg;
    }
}
