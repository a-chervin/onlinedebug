package xryusha.onlinedebug.config.actions;


import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.eventspecific.ReturnValue;
import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.*;
import java.util.*;

/**
 * Specifies action of printing formatted message including references to values,
 * constant values and special data (event time, thread name, method return value, thread stack,
 * dump of visible variables or method arguments etc)
 * The message may be logged on monitored VM side, local side or both to
 * standard streams (stdout/stderr) or to files.
 */
@XmlRootElement(name= Configuration.Elements.ACTION_PRINT)
@XmlType(name="print")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({PrintSpec.ThreadStack.class, PrintSpec.Dump.class, ReturnValue.class, PrintSpec.ThreadName.class})
public class PrintSpec implements ActionSpec
{
    /**
     * Where the message shold be logged: here, by remove VM or both
     */
    @XmlAttribute
    private LoggingVM location;

    /**
     * Path of local log file, console for "", "stdout" or null, "stderr" to stderr (obviously) or path
     * used if local printing enabled
     */
    @XmlAttribute(name="localFile")
    private String localLogFile;

    /**
     * Path of remote log file, console for "", "stdout" or null, "stderr" to stderr (obviously) or path
     * used if remote printing enabled
     */
    @XmlAttribute(name="remoteFile")
    private String remoteLogFile;

    /**
     * Elements combining the message
     */
    @XmlElementWrapper(name = "params")
    @XmlElementRef
    protected List<RValue> params;

    public PrintSpec()
    {
        params = new ArrayList<>();
    }

    public PrintSpec(LoggingVM location)
    {
        this();
        this.location = location;
    }

    public LoggingVM getLocation()
    {
        return location;
    }

    public String getLocalLogFile()
    {
        return localLogFile;
    }

    public void setLocalLogFile(String localLogFile)
    {
        this.localLogFile = localLogFile;
    }

    public String getRemoteLogFile()
    {
        return remoteLogFile;
    }

    public void setRemoteLogFile(String remoteLogFile)
    {
        this.remoteLogFile = remoteLogFile;
    }

    public void setLocation(LoggingVM remote)
    {
        this.location = remote;
    }

    public List<RValue> getParams()
    {
        return params;
    }



    @Override
    public String toString()
    {
        return "PrintSpec{" +
                "location=" + location +
                ", params=" + params +
                '}';
    }

    /**
     * Enum name is LoggingVM and not location as it could be
     * to avoid ambiguous name (com.sun.jdi.Location widely used here)
     */
    @XmlType(name="location")
    public enum LoggingVM
    {
        local, remote, both
    }

    @XmlType(name="")
    @XmlRootElement(name=Configuration.Elements.VALUE_THREADNAME)
    public static class ThreadName extends RValue
    {
    }

    @XmlType(name="")
    @XmlRootElement(name= Configuration.Elements.VALUE_THREADSTACK)
    public static class ThreadStack extends RValue
    {
    }

    @XmlType(name="")
    @XmlRootElement(name=Configuration.Elements.VALUE_TIME)
    public static class ReceiveTime extends RValue
    {
    }

    @XmlType(name="")
    @XmlRootElement(name=Configuration.Elements.VALUE_DUMP)
    public static class Dump extends RValue
    {
        public enum DumpSource { args, visible }

        @XmlList()
        @XmlAttribute(name="source")
        private Set<DumpSource> source = new HashSet<>();

        public Set<DumpSource> getSource()
        {
            return source;
        }

        public void setSource(Set<DumpSource> source)
        {
            this.source = source;
        }
    }
}
