package xryusha.onlinedebug.runtime.util;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory
{
    @Override
    public Thread newThread(Runnable r)
    {
        Thread th = new Thread(r);
        th.setDaemon(true);
        return th;
    }
}
