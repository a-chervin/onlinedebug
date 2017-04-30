package xryusha.onlinedebug.testcases.other;

import xryusha.onlinedebug.testcases.Flow;

import java.util.concurrent.Exchanger;

public class MultithreadingPrintTest extends Flow
{
    @Override
    public Object call() throws Exception
    {
        f();
        return null;
    }

    void f() throws Exception
    {
        final Exchanger<String> randezVous = new Exchanger<>();
        final int iterations = 2;
        Thread th1 = new Thread(new Runnable() {
                                    @Override
                                    public void run()
                                    {
                                        for(int inx = 0; inx < iterations; inx++) {
                                            String BP = "erwerw"; // BP
                                            try {
                                                randezVous.exchange("d");
                                                BP = "erwerw"; // at this point 2nd is waiting for 2nd randezVous
                                                randezVous.exchange("d");
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                });
        Thread th2 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                for(int inx = 0; inx < iterations; inx++ ) {
                    String BP = "erwerw"; // BP
                    try {
                        randezVous.exchange("d");
                        randezVous.exchange("d");
                        BP = "erwerw";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        th1.setName("TH-1");
        th2.setName("TH-2");

        th1.start();
        th2.start();

        th1.join();
        th2.join();
    } // f
}
