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

package xryusha.onlinedebug.config.values;

import xryusha.onlinedebug.config.Configuration;

import javax.xml.bind.annotation.*;

/**
 * Specifies constant value, defined by configuration
 */
@XmlType(name="const")
@XmlRootElement(name= Configuration.Elements.VALUE_CONST)
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Const extends RValue
{
    /**
     * constant value
     */
    private String value;

    public Const()
    {
    }

    public Const(String value)
    {
        setValue(value);
    }

    public Const(String value, String type)
    {
        this(value);
        this.type = type;
    }

    @XmlAttribute(name="value")
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        if ( value != null ) {
            value = value.replace("\\n", "\n");
            value = value.replace("\\t", "\t");
        }
        this.value = value;
    }

    @Override
    public void setType(String type)
    {
        super.setType(type);
    }

    @Override
    public String toString()
    {
        return "Const{" +
                "value='" + value + '\'' +
                "type=" + type +
                '}';
    }
}
