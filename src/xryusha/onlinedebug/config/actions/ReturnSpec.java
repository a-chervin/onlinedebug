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
