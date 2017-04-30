package xryusha.onlinedebug.testcases.local;

import xryusha.onlinedebug.testcases.Flow;

import java.util.Arrays;
import java.util.stream.IntStream;

public class LocalArrayVar extends Flow

{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        int[] ints = new int[]{1,2,3,4,5};
        String bp = "A";
        IntStream stream = Arrays.stream(ints);
        ints = stream.map(i->i+1).toArray();
        bp = "A";
    }

    int dynamicInx(int inx)
    {
        return inx;
    }
}
