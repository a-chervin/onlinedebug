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
    final static Logger log;
    static {
        log = Logger.getGlobal();
//        System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
//        System.load(
//                "c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll "
//        );
//        init();
    }

    public final static String GET_ARGS = "all:args";
    public final static String GET_LOCALS = "all:locals";
    
    static int II;
    native public static boolean init();
    native public static Map<String,Object> extract(List<String> names);
    native public static boolean setValue(String name, Object value);
    native public static boolean enforceReturn(Object value);
    

    static void log(String severity, String errType, String errMessage)
    {
        Level level = Level.parse(severity);
        log.log(level, "{0}: {1}", new Object[]{errMessage, errType});
    }

    static void log(String severity, String title, Object object)
    {
        Level level = Level.parse(severity);
        log.log(level, "{0}: {1}", new Object[]{title, object});
    }

    public static void main(String[] args) throws Exception
    {
       Level level = Level.parse("SEVERE");
       String st = "libstackminer_jni.dll";
       java.lang.reflect.Method put = Map.class.getMethod("put", new Class[]{Object.class, Object.class});
       System.load("c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
//       System.loadLibrary(st);
       System.load(
               "c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll "
       );
       System.out.println("Enter to continue");
       System.in.read();
       init();
       Zhop zh = new Zhop();
       while(true) {
            for(int i = 0; i < 3; i++ )
                try {
                    System.out.println("Waiting- " + i);
                    Thread.currentThread().sleep(3000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            zh.f(17, 'e', true, "xep-arg");
            System.out.println("\n - - - - - - - - - - - - - - - -\n\n");
        }
    }
}

class Zhop
{
    void print(String title, Map<String,?> map)
    {
        System.out.println("=======================");
        System.out.println(title);
        for (Map.Entry<String,?> e : map.entrySet()) {
            System.out.println("   " + e.getKey() + ":(" + e.getValue() + ")");
        }System.out.println("^^^^^^^^^^^^^^^^^^^^^^^");        
    }

    void f(int iiarg, char charag, boolean barag, String stararg)
    {
        byte byteval = 120;
        short shortval = 1024;
        int intval = 10241024;
        long longval = 2024102410;
        float floatval = 3.14f;
        double doubleval = 3.14159d;
        boolean boolval = false;
        String strval = "Some String";
        Long longWrapperVal = Long.valueOf(100);
        int[] intarray = {1,2,3,4,5};
        
        print("Args", StackMiner.extract(Arrays.asList(StackMiner.GET_ARGS)));
        print("Locals", StackMiner.extract(Arrays.asList(StackMiner.GET_LOCALS)));
        print("All", StackMiner.extract(null));
        print("byteval, strval", StackMiner.extract(Arrays.asList("byteval", "strval")));
        
        StackMiner.setValue("byteval", Byte.valueOf((byte)0x00));
        print("byteval", StackMiner.extract(Arrays.asList("byteval")));
        
        StackMiner.setValue("shortval", Short.valueOf((short)15));
        print("shortval", StackMiner.extract(Arrays.asList("shortval")));
        
        StackMiner.setValue("intval", Integer.valueOf(200));
        print("intval", StackMiner.extract(Arrays.asList("intval")));

        StackMiner.setValue("longval", Long.valueOf(2000));
        print("longval", StackMiner.extract(Arrays.asList("longval")));

        StackMiner.setValue("floatval", Float.valueOf(0.159f));
        print("floatval", StackMiner.extract(Arrays.asList("floatval")));

        StackMiner.setValue("doubleval", Double.valueOf(2.317d));
        print("doubleval", StackMiner.extract(Arrays.asList("doubleval")));

        StackMiner.setValue("boolval", Boolean.TRUE);
        print("boolval", StackMiner.extract(Arrays.asList("boolval")));

        StackMiner.setValue("strval", "New Xernya");
        print("strval", StackMiner.extract(Arrays.asList("strval")));

        StackMiner.setValue("longWrapperVal", Long.valueOf(745));
        print("longWrapperVal", StackMiner.extract(Arrays.asList("longWrapperVal")));
        
//        System.out.println("")
//        Map<String,Object>  res1 = StackMiner.extract(null);
//            String zh_str_l = "Xernya";
//            int zh_ii_l = 5;
//            System.out.println("before native: " + (int_i++));
//            boolean zh_inited = StackMiner.init();
//            Map<String,Object>  res1 = StackMiner.extract(null);
//            print("(null): ", res1);    
//            
//            Map<String,Object>  res2 = StackMiner.extract(Arrays.asList("all:args"));
//            print("(all:args): ", res2);            
//            
//            Map<String,Object>  res3 = StackMiner.extract(Arrays.asList("all:locals"));
//            print("(all:locals): ", res3);            
//            
//            Map<String,Object>  res4 = StackMiner.extract(Arrays.asList("zh_str_l", "zh_ii_l"));
//            print("(zh_str_l, zh_ii_l): ", res4);            
//            
//            StackMiner.setValue("zh_ii_l", Integer.valueOf(18));
//            Map<String,Object>  res5 = StackMiner.extract(Arrays.asList("zh_str_l", "zh_ii_l"));
//            print("updated(int) (zh_str_l, zh_ii_l): ", res5);            
//
//            StackMiner.setValue("zh_str_l", "TAHUNAX");
//            Map<String,Object>  res6 = StackMiner.extract(Arrays.asList("zh_str_l", "zh_ii_l"));
//            print("updated(String) (zh_str_l, zh_ii_l): ", res6);            
//            
/*
            System.out.println("Inited: " + zh_inited);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    int r_zhp = 5;
                    int[] arr = {1,2,3,4,5};
                    String r_str = "xep" ;
                    List params = Arrays.asList("this", 
                                                "r_zhp", "r_str",
                                                "zh_str_l", "ii"
                                               );
                    Map<String,Object>  res = StackMiner.extract(params);
                    System.out.println("=======initial=======");
                    for(Map.Entry e: res.entrySet()) {
                        System.out.println("   " + e.getKey()+ ":("+e.getValue()+")");
                    }
//                    System.out.println("res: (isnull: " + (res==null) + ") :: " + res);
                    StackMiner.setValue("r_str", "TAHYHAX");
                    res = StackMiner.extract(null);
                    System.out.println("=======changed=======");
//                    System.out.println("res-updated: (isnull: " + (res==null) + ") :: " + res);
                    for(Map.Entry e: res.entrySet()) {
                        System.out.println("   " + e.getKey()+ ":("+e.getValue()+")");
                    }
                    System.out.println("--end--\n\n");
                }
            }; 
            
//            r.run();
*/    
    }
}
