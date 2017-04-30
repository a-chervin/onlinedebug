package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class AssignInstanceVars extends Flow
{
    int ii_inst;
    static int ii_stat;
    int cnt=0;

    @Override
    public void reset()
    {
        ii_inst = 0;
        ii_stat = 0;
        cnt=0;
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
        cnt++;
        BP = st+ii+arg; // invoked assign
        BP = st+ii+arg; // expected: ii:"1, st:"vv+"
    }
}
