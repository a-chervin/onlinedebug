package xryusha.onlinedebug.config.breakpoints;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies method exit breakpoint
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_METHODEXIT)
@XmlType(name="method-exit")
public class MethodExitBreakPointSpec extends MethodTargetingBreakPoint
{
}
