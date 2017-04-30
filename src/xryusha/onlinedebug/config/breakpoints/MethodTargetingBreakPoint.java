package xryusha.onlinedebug.config.breakpoints;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.config.values.Ref;
import xryusha.onlinedebug.util.Util;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Base type for method entry/exit break points.
 */
@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({MethodEntryBreakPointSpec.class, MethodExitBreakPointSpec.class, MethodTargetingBreakPoint.Arg.class})
public abstract class MethodTargetingBreakPoint extends AbstractBreakPointSpec implements Util
{
    /**
     * Class or interface defining the method declaration or implementation
     */
    @XmlAttribute(name="class")
    private String targetClass;

    /**
     * Target method name. In most of cases must be specified,
     * not relevant when constructor is targeted (see {@link #isConstructor})
     */
    @XmlAttribute(name="method", required = false)
    private String method;

    /**
     * Specifies it is a constructor (in such case {@link #method method name} is not needed)
     */
    @XmlAttribute(name="constructor", required = false)
    private Boolean isConstructor;

    /**
     * If specified the breakpoint applies on all overloaded methods with this name regardless
     * of method signature.
     */
    @XmlAttribute(name="anySignature", required = false)
    private Boolean anySignature;

    /**
     * Defines method signature by {@link Arg "arg" } elements. e.g.
     * <pre>
     * {@code
     *  <params>
     *     <arg class="int"/>
     *     <arg class="java.lang.String"/>
     *  </params>
     * }
     * </pre>
     * @see Arg
     */
    @XmlElementWrapper(name="params")
    @XmlElementRef
    protected List<RValue> params = new ArrayList<>();

    public String getTargetClass()
    {
        return targetClass;
    }

    public void setTargetClass(String targetClass)
    {
        this.targetClass = targetClass;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public boolean isAnySignature()
    {
        return valueOf(anySignature, false);
    }

    public void setAnySignature(boolean anySignature)
    {
        this.anySignature = anySignature;
    }

    public boolean isConstructor()
    {
        return valueOf(isConstructor, false);
    }

    public void setConstructor(boolean constructor)
    {
        isConstructor = constructor;
    }

    public List<RValue> getParams()
    {
        return params;
    }


    /**
     * Used to specify method signature
     * @see #params
     */
    @XmlType(name="")
    @XmlRootElement(name= Configuration.Elements.VALUE_ARGPLACEHOLDER)
    public static class Arg extends Ref
    {
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("MethodTargetingBreakPoint{");
        sb.append("targetClass='").append(targetClass).append('\'');
        sb.append(", method='").append(method).append('\'');
        sb.append(", params=").append(params);
        sb.append('}');
        return sb.toString();
    }
}
