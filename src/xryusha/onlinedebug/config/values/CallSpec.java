package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.actions.ActionSpec;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifies value returned by invocation of defined method.
 */
@XmlType(name="call")
@XmlRootElement(name= Configuration.Elements.VALUE_CALL)
@XmlSeeAlso(Constructor.class)
@XmlAccessorType(XmlAccessType.FIELD)
public class CallSpec extends Ref implements ActionSpec
{
    /**
     * method name
     */
    @XmlAttribute(name="method", required = false)
    protected String method = null;

    /**
     * method arguments on any type (const, variables, method calls)
     */
    @XmlElementWrapper(name="params")
    @XmlElementRef
    protected List<RValue> params;


    public CallSpec()
    {
        params = new ArrayList<>();
    }

    public CallSpec(String clazz, String method)
    {
        this();
        this.type/*targetClass*/ = clazz;
        this.method = method;
    }

    public String getTargetClass()
    {
        return type /*targetClass*/;
    }

    public void setTargetClass(String targetClass)
    {
        this.type/*targetClass*/ = targetClass;
    }

    public String getMethod()
    {
        return method ;
    }

    public void setMethod(String method)
    {
        //this.method = method;
        this.method = method;
    }

    public List<RValue> getParams()
    {
        return params;
    }

    @Override
    protected String reference()
    {
        StringBuilder sb = new StringBuilder();
        if ( type /*targetClass*/ != null )
            sb.append(type/*targetClass*/).append(':');
        sb.append(method).append("()");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CallSpec{" +
                "targetClass='" + type/*targetClass*/ + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                '}';
    }
}
