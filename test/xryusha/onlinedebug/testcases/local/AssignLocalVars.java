package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class AssignLocalVars extends Flow
{
    int ii_inst;

    @Override
    public void reset()
    {
        ii_inst = 0;
    }

    @Override
    public Object call() throws Exception
    {
        f(30);
        return null;
    }


    void f(int arg)
    {
        int ii = 0;
        String base="vv-";
        String st = base;

        String BP = st+ii+arg; // expected: arg:0, ii:0, st:"vv-"
        ii_inst++;
        BP = st+ii+arg; // invoked assign
        BP = st+ii+arg; // expected: ii:"1, st:"vv+"
    }
}
