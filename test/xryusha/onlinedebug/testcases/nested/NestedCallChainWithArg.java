package xryusha.onlinedebug.testcases.nested;

import xryusha.onlinedebug.testcases.Flow;

public class NestedCallChainWithArg extends Flow
{
    Nested_T1 t1 = new Nested_T1();

    @Override
    public void reset()
    {
        t1 = new Nested_T1();
    }

    @Override
    public Object call() throws Exception
    {
        f(9);
        return null;
    }

    void f(int arg)
    {
        String BP = "-bp"+arg;
        BP = "-bp"+arg;
        String res = BP+"---";
    }

    Nested_T1 getT1()
    {
        return t1;
    }
}
