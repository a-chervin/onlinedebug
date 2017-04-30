package xryusha.onlinedebug.config.breakpoints;



import javax.xml.bind.annotation.*;


/**
 * Base breakpoint definition.
 */
@XmlType
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({LineBreakpointSpec.class, ExceptionBreakpointSpec.class,
             MethodTargetingBreakPoint.class, FieldModificationBreakPointSpec.class})
public abstract class AbstractBreakPointSpec
{
}

