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

package xryusha.onlinedebug.runtime.util.remote;

import xryusha.onlinedebug.util.Log;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StackMiner
{
    private final static Logger log = Log.getLogger();
    native public static boolean init();
    native public static Map<String,Object> extract(List<String> names);

    static {
        init();
    }


    static void log(String severity, String errType, String errMessage)
    {
        Level level = Level.parse(severity);
        log.log(level, "{0}: {1}", new Object[]{errMessage, errType});
    }
}
