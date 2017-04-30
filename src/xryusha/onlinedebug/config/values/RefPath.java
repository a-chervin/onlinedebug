package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;


@XmlRootElement(name= Configuration.Elements.VALUE_REFPATH)
@XmlType(name="ref")
@XmlAccessorType(XmlAccessType.FIELD)
public class RefPath extends Ref
{
    @XmlAttribute(name="var", required = true)
    protected String value;

    public RefPath()
    {
    }

    public RefPath(String value)
    {
        this();
        this.value = value;
    }

    public RefPath(String clazz, String value)
    {
        this();
        this.type = clazz;
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

//    public String getClazz()
//    {
//        return clazz;
//    }
//
//    public void setClazz(String clazz)
//    {
//        this.clazz = clazz;
//    }

    @Override
    protected String reference()
    {
        if ( type  == null)
            return value;

        StringBuilder sb = new StringBuilder(type)
                .append('.')
                .append(value);
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "RefPath{" +
                "class='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

