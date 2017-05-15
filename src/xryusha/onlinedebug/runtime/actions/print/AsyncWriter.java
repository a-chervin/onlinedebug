package xryusha.onlinedebug.runtime.actions.print;


import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.*;

public class AsyncWriter implements ThreadFactory
{
    private final PrintStream target;
    private final ExecutorService appender;
    private final ExecutorService verifier;

    public static AsyncWriter newInstance(String targetPath) throws IOException
    {
        return new AsyncWriter(targetPath);
    }

    public AsyncWriter(String targetPath) throws IOException
    {
        switch (targetPath) {
            case "":
            case "stdout":
                this.target = System.out;
                break;
            case "stderr":
                this.target = System.err;
                break;
            default:
                Path path = new File(targetPath).toPath();
                FileChannel file = FileChannel.open(path,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
                OutputStream os = Channels.newOutputStream(file);
                target = new PrintStream(os, true);
        }

        appender = Executors.newSingleThreadExecutor(this);
        verifier = Executors.newSingleThreadExecutor(this);
    } // AsyncWriter


    public String submit(String format, Object[] args) throws Exception
    {
        final String message = String.format(format, args);
        Future future = appender.submit(new Callable<Void>() {
                                            public Void call() throws Exception
                                            {
                                                target.println(message) ;
                                                return null;
                                            }
                                        });
        verifier.submit(new Runnable() {
                            public void run() {
                                try {
                                    future.get();
                                } catch (Throwable th) {
                                    ByteArrayOutputStream baos =
                                               new ByteArrayOutputStream();
                                    PrintStream ps = new PrintStream(baos);
                                    ps.println("Append task failed: ");
                                    th.printStackTrace(ps);
                                    ps.flush();
                                    String str = baos.toString();
                                    System.err.println(str);
                                }
                    } // run
                } // Runnable
               );
        return message;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        Thread th = new Thread(r);
        th.setDaemon(true);
        return th;
    }
}
