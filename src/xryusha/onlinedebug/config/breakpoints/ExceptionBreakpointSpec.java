/**
 * Licensed to the a-chervin (ax.chervin@gmail.com) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * a-chervin licenses this file under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xryusha.onlinedebug.config.breakpoints;

import xryusha.onlinedebug.config.Configuration;

import java.util.*;
import javax.xml.bind.annotation.*;


/**
 * Specifies breakpoint fired by thrown exception.
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_EXCEPTION)
@XmlType(name="exception-breakpoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExceptionBreakpointSpec extends  AbstractBreakPointSpec
{
    /**
     * List of related exception classes
     */
    @XmlElement(name="exception")
    private List<String> exceptions = new ArrayList<>();

    /**
     * If just one exception is mentioned it may be defined by attribute
     */
    @XmlAttribute(name="exception")
    private String exception;

    public String getException()
    {
        return exception;
    }

    public void setException(String exception)
    {
        this.exception = exception;
    }

    public List<String> getExceptions()
    {
        return exceptions;
    }

    public void setExceptions(List<String> exception)
    {
         this.exceptions = exception;
    }

    public List<String> allExceptions()
    {
        if ( exception == null && exceptions == null )
            return null;
        List<String> result = new ArrayList<>();
        if ( exceptions != null )
            result.addAll(exceptions);
        if ( exception != null && !result.contains(exception))
            result.add(exception);
        return result;
    }

    @Override
    public String toString()
    {
        return "ExceptionBreakpoint: " + allExceptions();
    }
}
