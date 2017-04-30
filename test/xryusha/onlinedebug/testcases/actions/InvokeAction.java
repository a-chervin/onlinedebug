package xryusha.onlinedebug.testcases.actions;

import xryusha.onlinedebug.testcases.Flow;

import java.util.concurrent.atomic.AtomicInteger;

public class InvokeAction extends Flow
{
    private AtomicInteger intHolder = new AtomicInteger(0);

    @Override
    public void reset()
    {
        intHolder.set(0);
    }

    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f()
    {
        String BP = "2343554"; // check val
        BP = "2343554"; // invoke intHolder.addAndGet(2)
        BP = "21345"; // check val
    }
}
