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
