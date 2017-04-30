package xryusha.onlinedebug.config.actions;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.config.values.Ref;

import javax.xml.bind.annotation.*;

/**
 * Specifies action of invoking required method.
 * The definition contains just one parameter {@link #accessPath}, rest specified
 * in parent class {@link CallSpec}
 * @see #accessPath
 */
@XmlRootElement(name= Configuration.Elements.ACTION_INVOKE)
@XmlType(name="invoke")
@XmlAccessorType(XmlAccessType.FIELD)
public class InvokeSpec extends CallSpec implements ActionSpec
{
    /**
     * Path to required methods
     */
    @XmlElementRef
    private Ref accessPath;

    public Ref getAccessPath()
    {
        return accessPath;
    }

    public void setAccessPath(Ref accessPath)
    {
        this.accessPath = accessPath;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("InvokeSpec{");
        sb.append("accessPath=").append(accessPath);
        sb.append(", call=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
