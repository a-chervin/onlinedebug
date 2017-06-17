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

import javax.xml.bind.annotation.*;

/**
 * Field modification event
 */
@XmlRootElement(name= Configuration.Elements.BREAKPOINT_FIELDMODIFICATION)
@XmlType(name="fieldModification-breakpoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class FieldModificationBreakPointSpec extends  AbstractBreakPointSpec
{
    @XmlAttribute(name="class", required = true)
    private String targetClass;

    @XmlAttribute(name="field", required = true)
    private String targetField;

    public String getTargetClass()
    {
        return targetClass;
    }

    public void setTargetClass(String targetClass)
    {
        this.targetClass = targetClass;
    }

    public String getTargetField()
    {
        return targetField;
    }

    public void setTargetField(String targetField)
    {
        this.targetField = targetField;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("FieldModificationBreakPointSpec{");
        sb.append("targetClass='").append(targetClass).append('\'');
        sb.append(", targetField='").append(targetField).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
