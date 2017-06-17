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

package xryusha.onlinedebug.config.conditions;

import xryusha.onlinedebug.config.Configuration;
import xryusha.onlinedebug.config.values.RValue;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * isNull predicate
 */
@XmlType(name="isnull")
@XmlRootElement(name= Configuration.Elements.CONDITION_ISNULL)
public class IsNullConditionSpec extends AbstractConditionSpec
{
    /**
     * Value reference to be checked
     */
    @XmlElement(name="value")
    protected RValue value;


    public RValue getValue()
    {
        return value;
    }

    public void setValue(RValue value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("IsNullConditionSpec");
        if ( inverse )
            sb.append(":inversed");
        sb.append(":{value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
