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


@XmlRootElement(name= Configuration.Elements.VALUE_REFPATH)
@XmlType(name="ref")
@XmlAccessorType(XmlAccessType.FIELD)
public class RefPath extends Ref
{
    @XmlAttribute(name="var", required = true)
    protected String value;

    public RefPath()
    {
    }

    public RefPath(String value)
    {
        this();
        this.value = value;
    }

    public RefPath(String clazz, String value)
    {
        this();
        this.type = clazz;
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

//    public String getClazz()
//    {
//        return clazz;
//    }
//
//    public void setClazz(String clazz)
//    {
//        this.clazz = clazz;
//    }

    @Override
    protected String reference()
    {
        if ( type  == null)
            return value;

        StringBuilder sb = new StringBuilder(type)
                .append('.')
                .append(value);
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "RefPath{" +
                "class='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

