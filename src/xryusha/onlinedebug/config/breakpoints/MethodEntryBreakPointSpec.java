package xryusha.onlinedebug.config.breakpoints;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies method entry breakpoint
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_METHODENTRY)
@XmlType(name="method-entry")
public class MethodEntryBreakPointSpec extends MethodTargetingBreakPoint
{
}
