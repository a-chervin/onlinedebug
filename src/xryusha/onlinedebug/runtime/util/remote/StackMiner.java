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
    private final static String GET_ARGS = "all:args";
    private final static String GET_LOCALS = "all:locals";
    private static Logger log;
    
    native private static boolean init();
    native private static Map<String,Object> getValues(List<String> names, int callerFrame);
    native private static boolean setValue(String name, Object value, int callerFrame);
    native private static boolean enforceReturn(Object value, Thread targetThread);

    static {
        log = Logger.getGlobal();
//        System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
//        System.load(
//                "c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll "
//        );
//        System.load(
//                "e:\\cygwin\\insted\\bin\\cygwin1.dll");
//        System.load(
//                "c:\\java\\projects\\onlinedebug\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll"
//        );

System.out.println("StachMiner: before init");
        boolean inited = init();
System.out.println("StachMiner: after init: " + inited);
        if ( !inited )
            throw new IllegalStateException("StackMiner natives initialization failed");
    }

    

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
        return setValue(name, value, 2);
    }

    public static boolean enforceEarlyReturn(Thread targetThread, Object value)
    {
        return enforceReturn(value, targetThread);
    }

    private static Map<String,Object> getValues(List<String> names)
    {
        // frame 0 : extract()
        // frame 1 : current ( getValues )
        // frame 2 : getMethodArguments() or getMethodLocals() etc
        // frame 3 : caller
        Map<String,Object> res = getValues(names, 3);
        return res;
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



///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xryusha.onlinedebug.runtime.util.remote;
//
//import java.lang.reflect.Method;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// *
// * @author Lena
// */
//public class StackMiner 
//{
//    public static final String GET_ARGS = "all:args";
//    public static final String GET_LOCALS = "all:locals";    
//    private static final boolean inited;
//    static final Logger log;
//
//    static {
//        log = Logger.getGlobal();
//        inited = init();
//    }
//
//    public static native boolean init();
//
//    public static native Map<String, Object> extract(List<String> names);
//
//    public static native boolean setValue(String name, Object value);
//
//    public static native boolean enforceReturn(Object value);
//
//    static void log(String severity, String errType, String errMessage) {
//        Level level = Level.parse(severity);
//        log.log(level, "{0}: {1}", new Object[]{errMessage, errType});
//    }
//
//    static void log(String severity, String title, Object object) {
//        Level level = Level.parse(severity);
//        log.log(level, "{0}: {1}", new Object[]{title, object});
//    }
//
//    public static void main(String[] args) throws Exception {
//        Level level = Level.parse("SEVERE");
//        String st = "libstackminer_jni.dll";
//        Method put = Map.class.getMethod("put", new Class[]{Object.class, Object.class});
//        System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
//        //       System.loadLibrary(st);
//        System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll ");
//        System.out.println("Enter to continue");
//        System.in.read();
//        init();
//        Zhop zh = new Zhop();
//        while (true) {
//            for (int i = 0; i < 3; i++) {
//                try {
//                    System.out.println("Waiting- " + i);
//                    Thread.currentThread().sleep(3000);
//                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//            }
//            zh.f(17, 'e', true, "xep-arg");
//            System.out.println("\n - - - - - - - - - - - - - - - -\n\n");
//        }
//    }
//    
//}
