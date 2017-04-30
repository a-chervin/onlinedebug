package xryusha.onlinedebug.config.breakpoints;


import xryusha.onlinedebug.util.Util;
import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;

/**
 * Specifies the simplest breakpoint type, specific source code line
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_LOCATION)
@XmlType(name="line-breakpoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class LineBreakpointSpec extends  AbstractBreakPointSpec implements Util
{
    /**
     * Qualified class name
     */
    @XmlAttribute(name="class", required = true)
    private String targetClass;

    /**
     * Line number
     */
    @XmlAttribute(name="line", required = true)
    private int line;

    /**
     * Prevents breakpoint application fail if relevant line is not found,
     * which may happen if line is located within inner/local/anonymous classl.
     * The reason is that inner/local/anonymous classes may be loaded much later
     * than encloser class, as a result lines of it are not visible during
     * encloser class loading
     */
    @XmlAttribute(name="fastFailOnMissing", required = false)
    private Boolean fastFailOnMissing;

    public LineBreakpointSpec()
    {
    }

    public LineBreakpointSpec(String targetClass, int line)
    {
        this.targetClass = targetClass;
        this.line = line;
    }

    public String getTargetClass()
    {
        return targetClass;
    }

    public void setTargetClass(String targetClass)
    {
        this.targetClass = targetClass;
    }

    public int getLine()
    {
        return line;
    }

    public void setLine(int line)
    {
        this.line = line;
    }

    public boolean isFastFailOnMissing()
    {
        return valueOf(fastFailOnMissing, true);
    }

    public void setFastFailOnMissing(boolean fastFailOnMissing)
    {
        this.fastFailOnMissing = fastFailOnMissing;
    }

    @Override
    public String toString()
    {
        return "Breakpoint{" + targetClass + ":" + line+"}";
    }
}
