package xryusha.onlinedebug.config.values.eventspecific;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.breakpoints.ExceptionBreakpointSpec;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Used to present pending exception value in case of
 * {@link ExceptionBreakpointSpec exception-based} break point
 * @see ExceptionBreakpointSpec
 */
@XmlType(name="thrownException")
@XmlRootElement(name= Configuration.Elements.VALUE_THROWN_EXCEPTION)
public class CurrentException extends BaseEventSpecificValue
{
    @Override
    public String toString()
    {
        return "ThrownException{}";
    }
}
