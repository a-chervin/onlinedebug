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
import xryusha.onlinedebug.config.actions.ActionSpec;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifies value returned by invocation of defined method.
 */
@XmlType(name="call")
@XmlRootElement(name= Configuration.Elements.VALUE_CALL)
@XmlSeeAlso(Constructor.class)
@XmlAccessorType(XmlAccessType.FIELD)
public class CallSpec extends Ref implements ActionSpec
{
    /**
     * method name
     */
    @XmlAttribute(name="method", required = false)
    protected String method = null;

    /**
     * method arguments on any type (const, variables, method calls)
     */
    @XmlElementWrapper(name="params")
    @XmlElementRef
    protected List<RValue> params;


    public CallSpec()
    {
        params = new ArrayList<>();
    }

    public CallSpec(String clazz, String method)
    {
        this();
        this.type/*targetClass*/ = clazz;
        this.method = method;
    }

    public String getTargetClass()
    {
        return type /*targetClass*/;
    }

    public void setTargetClass(String targetClass)
    {
        this.type/*targetClass*/ = targetClass;
    }

    public String getMethod()
    {
        return method ;
    }

    public void setMethod(String method)
    {
        //this.method = method;
        this.method = method;
    }

    public List<RValue> getParams()
    {
        return params;
    }

    @Override
    protected String reference()
    {
        StringBuilder sb = new StringBuilder();
        if ( type /*targetClass*/ != null )
            sb.append(type/*targetClass*/).append(':');
        sb.append(method).append("()");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "CallSpec{" +
                "targetClass='" + type/*targetClass*/ + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                '}';
    }
}
