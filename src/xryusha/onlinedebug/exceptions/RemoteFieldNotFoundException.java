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

package xryusha.onlinedebug.exceptions;

public class RemoteFieldNotFoundException extends RemoteExceptionBase
{
    private String field;
    private String clazz;

    public RemoteFieldNotFoundException(String message, String field, String clazz)
    {
        super(message);
        this.field = this.field;
        this.clazz = clazz;
    }

    public RemoteFieldNotFoundException(String message, Throwable cause, String method, String clazz)
    {
        super(message, cause);
        this.field = method;
        this.clazz = clazz;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("RemoteFieldNotFoundException{");
        sb.append("field='").append(field).append('\'');
        sb.append(", clazz='").append(clazz).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
