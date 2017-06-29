package xryusha.onlinedebug.runtime.util.remote;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;


public class StackMinerTest
{
    public static void main(String[] args) throws Exception
    {
        System.load(
             "c:\\java\\projects\\onlinedebug\\src\\jni\\c\\Debug\\cygwin1.dll");
        System.load(
             "c:\\java\\projects\\onlinedebug\\src\\jni\\netbeans\\stackminer\\dist\\Debug\\Cygwin-Windows\\libstackminer.dll "
        );
                
        StackMinerTest test = new StackMinerTest();
        System.out.println("press enter");
        System.in.read();
        while(true) {
            int wait = 3;
            for(int ii = 0; ii < wait; ii++) {
                System.out.println("waiting: " + ii);
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }
            test.testLocals(17, 'c', true, "Str-ARG");
        }
        
    }

    void testLocals(int iiarg, char charag, boolean barag, String stararg)
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
        
        print("Args", StackMiner.getMethodArguments());
        print("Locals", StackMiner.getMethodLocals());
        print("All", StackMiner.getAllVisible());
        print("byteval, strval", StackMiner.getMethodLocals(Arrays.asList("byteval", "strval")));
        
        StackMiner.setLocalValue("byteval", Byte.valueOf((byte)0x00));
        print("byteval", StackMiner.getMethodLocals(Arrays.asList("byteval")));
        
        StackMiner.setLocalValue("shortval", Short.valueOf((short)15));
        print("shortval", StackMiner.getMethodLocals(Arrays.asList("shortval")));
        
        StackMiner.setLocalValue("intval", Integer.valueOf(200));
        print("intval", StackMiner.getMethodLocals(Arrays.asList("intval")));

        StackMiner.setLocalValue("longval", Long.valueOf(2000));
        print("longval", StackMiner.getMethodLocals(Arrays.asList("longval")));

        StackMiner.setLocalValue("floatval", Float.valueOf(0.159f));
        print("floatval", StackMiner.getMethodLocals(Arrays.asList("floatval")));

        StackMiner.setLocalValue("doubleval", Double.valueOf(2.317d));
        print("doubleval", StackMiner.getMethodLocals(Arrays.asList("doubleval")));

        StackMiner.setLocalValue("boolval", Boolean.TRUE);
        print("boolval", StackMiner.getMethodLocals(Arrays.asList("boolval")));

        StackMiner.setLocalValue("strval", "New Xernya");
        print("strval", StackMiner.getMethodLocals(Arrays.asList("strval")));

        StackMiner.setLocalValue("longWrapperVal", Long.valueOf(745));
        print("longWrapperVal", StackMiner.getMethodLocals(Arrays.asList("longWrapperVal")));
        
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

    void print(String title, Map<String,?> map)
    {
        System.out.println("=======================");
        System.out.println(title);
        for (Map.Entry<String,?> e : map.entrySet()) {
            System.out.println("   " + e.getKey() + ":(" + e.getValue() + ")");
        }System.out.println("^^^^^^^^^^^^^^^^^^^^^^^");        
    }


}
