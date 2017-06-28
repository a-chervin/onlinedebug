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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StackMiner
{
    private final static boolean inited;
    final static Logger log;
    static {
        log = Logger.getGlobal();
//        System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
//        System.load(
//                "c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll "
//        );
        inited = init();
    }

    private final static String GET_ARGS = "all:args";
    private final static String GET_LOCALS = "all:locals";
    
    native private static boolean init();
    native private static Map<String,Object> extract(List<String> names);
    native private static boolean setValue(String name, Object value);
    native private static boolean enforceReturn(Object value);

    public static Map<String,Object> getMethodArguments()
    {
        return getValues(Arrays.asList(GET_ARGS));
    }

    public static Map<String,Object> getMethodLocals()
    {
        return getValues(Arrays.asList(GET_LOCALS));
    }

    public static Map<String,Object> getMethodLocals(List<String> names)
    {
        return getValues(names);
    }

    public static Map<String,Object> getAllVisible()
    {
        return getValues(null);
    }

    public static boolean setLocalValue(String name, Object value)
    {
        checkState();
        return setValue(name, value);
    }

    public static boolean enforceEarlyReturn(Object value)
    {
        checkState();
        return enforceReturn(value);
    }

    private static Map<String,Object> getValues(List<String> names)
    {
        checkState();
        Map<String,Object> res = extract(names);
        return res;
    }


    private static void checkState() throws IllegalStateException
    {
        if ( !inited )
            throw new IllegalStateException("not initialized");
    }

    private static void log(String severity, String errType, String errMessage)
    {
        Level level = Level.parse(severity);
        log.log(level, "{0}: {1}", new Object[]{errMessage, errType});
    }

    private static void log(String severity, String title, Object object)
    {
        Level level = Level.parse(severity);
        log.log(level, "{0}: {1}", new Object[]{title, object});
    }
}

