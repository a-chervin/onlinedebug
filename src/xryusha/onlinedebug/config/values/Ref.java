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

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Specifies base for references to remote VM's references (vars, methods etc)
 */
@XmlType(name = "abstractRef")
@XmlSeeAlso({CallSpec.class, RefChain.class, RefPath.class, ArrayIndex.class})
public abstract class Ref extends RValue
{
    @Override
    public String uniqueID()
    {
        return super.uniqueID()+"<" + reference()+">";
    }

    protected String reference()
    {
        return "";
    };
}
