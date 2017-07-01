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
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.config.values.Ref;

import javax.xml.bind.annotation.*;

/**
 * Specifies action of invoking required method.
 * The definition contains just one parameter {@link #accessPath}, rest specified
 * in parent class {@link CallSpec}
 * @see #accessPath
 */
@XmlRootElement(name= Configuration.Elements.ACTION_INVOKE)
@XmlType(name="invoke")
@XmlAccessorType(XmlAccessType.FIELD)
public class InvokeSpec extends CallSpec implements ActionSpec
{
    /**
     * Path to required methods
     */
    @XmlElementRef(required = false)
    private Ref accessPath;

    public Ref getAccessPath()
    {
        return accessPath;
    }

    public void setAccessPath(Ref accessPath)
    {
        this.accessPath = accessPath;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("InvokeSpec{");
        sb.append("accessPath=").append(accessPath);
        sb.append(", call=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
