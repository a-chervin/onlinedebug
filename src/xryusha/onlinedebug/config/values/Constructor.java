package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;

/**
 * Specifies constructor invocation
 */
@XmlType(name="constructor")
@XmlRootElement(name = Configuration.Elements.VALUE_CONSTRUCTOR)
@XmlAccessorType(XmlAccessType.FIELD)
public class Constructor extends CallSpec
{
    public Constructor()
    {
        setMethod("<init>");
    }

    public Constructor(String targetClass)
    {
        this();
        setTargetClass(targetClass);
    }
}
