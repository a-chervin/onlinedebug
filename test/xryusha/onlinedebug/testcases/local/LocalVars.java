package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

public class LocalVars extends Flow
{
    @Override
    public Object call() throws Exception
    {
        func(0);
        return null;
    }

    void func(int arg)
    {
        int ii = 0;
        String base="vv-";
        String st = base;
        String BP = st+ii+arg; // expected: arg:0, ii:0, st:"vv-"

        ii++; //
        arg++;
        st = base + ii;
        BP = st+ii+arg; // expected: arg:1, ii:1, st:"vv-1"
    }
}
