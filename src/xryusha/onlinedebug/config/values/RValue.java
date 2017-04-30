package xryusha.onlinedebug.config.values;

import javax.xml.bind.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base value type
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({ Const.class, Ref.class})
public abstract class RValue
{
    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * base evaluated element class
     */
    @XmlAttribute(name="class", required = false)
    protected String type;

    @XmlTransient
    private final String id;

    public RValue()
    {
        id = this.getClass().getSimpleName() + "-" + counter.incrementAndGet();
    }

    public String uniqueID()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
