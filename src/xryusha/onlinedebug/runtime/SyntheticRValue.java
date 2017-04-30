package xryusha.onlinedebug.runtime;

import com.sun.jdi.Value;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.config.values.Ref;

/**
 * {@link RValue} type created during run-time and not based on configuration,
 *                used to keep remote object values
 */
public class SyntheticRValue extends Ref
{
    private ThreadLocal<Value> threadLocal = new ThreadLocal<Value>();
    private Value value;
    private boolean shared;

    public SyntheticRValue()
    {
        this(false);
    }

    public SyntheticRValue(Value value)
    {
        this(false);
        setValue(value);
    }

    public SyntheticRValue(boolean shared)
    {
        this.shared = shared;
    }

    public SyntheticRValue(Value value, boolean shared)
    {
        this.shared = shared;
        setValue(value);
    }

    public void setValue(Value value)
    {
        if ( shared )
           this.value = value;
        else
           threadLocal.set(value);
    }

    public Value getValue()
    {
        if ( shared )
            return value;
        return threadLocal.get();
    }
} // SyntheticRValue
