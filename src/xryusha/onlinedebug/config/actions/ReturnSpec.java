package xryusha.onlinedebug.config.actions;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.*;

/**
 * Specifies action of early return with specifying value, i.e. enforcing method return at
 * arbitrary point and not by "return" statement. For example, may be useful during dev. to simulate
 * desired scenario or just a short-time w/a for methods throwing exception
 */
@XmlRootElement(name= Configuration.Elements.ACTION_RETURN)
@XmlType(name="earlyReturn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReturnSpec implements ActionSpec
{
    /**
     * Value to be returned by method (applicable for non-void)
     */
    @XmlElementRef
    private RValue returnValue;

    public ReturnSpec()
    {
    }

    public ReturnSpec(RValue returnValue)
    {
        this.returnValue = returnValue;
    }

    public RValue getReturnValue()
    {
        return returnValue;
    }

    public void setReturnValue(RValue returnValue)
    {
        this.returnValue = returnValue;
    }
}
