package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * isNull predicate
 */
@XmlType(name="isnull")
@XmlRootElement(name= Configuration.Elements.CONDITION_ISNULL)
public class IsNullConditionSpec extends AbstractConditionSpec
{
    /**
     * Value reference to be checked
     */
    @XmlElement(name="value")
    protected RValue value;


    public RValue getValue()
    {
        return value;
    }

    public void setValue(RValue value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("IsNullConditionSpec");
        if ( inverse )
            sb.append(":inversed");
        sb.append(":{value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
