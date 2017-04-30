package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;

/**
 * Specifies constant value, defined by configuration
 */
@XmlType(name="const")
@XmlRootElement(name= Configuration.Elements.VALUE_CONST)
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Const extends RValue
{
    /**
     * constant value
     */
    private String value;

    public Const()
    {
    }

    public Const(String value)
    {
        setValue(value);
    }

    public Const(String value, String type)
    {
        this(value);
        this.type = type;
    }

    @XmlAttribute(name="value")
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        if ( value != null ) {
            value = value.replace("\\n", "\n");
            value = value.replace("\\t", "\t");
        }
        this.value = value;
    }

    @Override
    public void setType(String type)
    {
        super.setType(type);
    }

    @Override
    public String toString()
    {
        return "Const{" +
                "value='" + value + '\'' +
                "type=" + type +
                '}';
    }
}
