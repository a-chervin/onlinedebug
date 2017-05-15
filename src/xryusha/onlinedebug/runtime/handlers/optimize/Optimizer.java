package xryusha.onlinedebug.runtime.handlers.optimize;

import com.sun.jdi.ThreadReference;
import xryusha.onlinedebug.runtime.HandlerData;

public interface Optimizer
{
    public void condition(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception;
    public void actions(ThreadReference thread, HandlerData.RuntimeConfig runtimeConfig) throws Exception;
}
