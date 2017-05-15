package xryusha.onlinedebug.runtime;

import com.sun.jdi.ThreadReference;
import xryusha.onlinedebug.config.values.eventspecific.BaseEventSpecificValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Execution context data
 */
public class ExecutionContext
{
    private static ConcurrentMap<String,ExecutionContext> contexts = new ConcurrentHashMap<>();

    private Map<String,Object> contextData = new HashMap<>();
    private long receiveTime;

    public static ExecutionContext createThreadBoundedContext(ThreadReference thread, long processingStartTime)
    {
        String k = getThreadKey(thread);
        ExecutionContext ctx = new ExecutionContext(System.currentTimeMillis());
        contexts.put(k, ctx);
        return ctx;
    }

    public static ExecutionContext getContext(ThreadReference thread)
    {
        String k = getThreadKey(thread);
        return contexts.get(k);
    }

    public static void closeContext(ThreadReference thread)
    {
        String k = getThreadKey(thread);
        contexts.remove(k);
    }

    public ExecutionContext(long receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    public long getReceiveTime()
    {
        return receiveTime;
    }

    public void setValue(String key, Object value)
    {
        contextData.put(key, value);
    }

    public Object getValue(String key)
    {
        return contextData.get(key);
    }

    public void setEventSpecificValue(Class<? extends BaseEventSpecificValue> type, Object value)
    {
        contextData.put(type.getName(), value);
    }

    public Object getEventSpecificValue(Class<? extends BaseEventSpecificValue> type)
    {
        return contextData.get(type.getName());
    }


    private static String getThreadKey(ThreadReference thread)
    {
        return thread.name() + "_" + Long.toHexString(thread.uniqueID());
    }

} // ExecutionContext
