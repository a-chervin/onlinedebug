package xryusha.onlinedebug.runtime.util;

import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.ConfigEntry;
import xryusha.onlinedebug.config.breakpoints.MethodEntryBreakPointSpec;
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.RemoteJVM;
import xryusha.onlinedebug.runtime.RemotingBase;
import xryusha.onlinedebug.runtime.actions.Action;
import xryusha.onlinedebug.runtime.actions.RuntimeActionSpec;
import xryusha.onlinedebug.runtime.func.ErrorproneFunction;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class AsyncRemoteExecutor extends RemotingBase
{
    private enum State { Initial, Initialization, Initialized, Failed }
    private final static AsyncRemoteExecutor instance = new AsyncRemoteExecutor();

    private final AtomicReference<State> state = new AtomicReference<>(State.Initial);
    private InetSocketAddress slaveAddress = null;
    private Socket slaveConnection = null;
    private BlockingQueue<ErrorproneFunction<ThreadReference,Void>> pendings =
                                                        new LinkedBlockingQueue<>();

    public static AsyncRemoteExecutor getInstance()
    { return instance; }

    private AsyncRemoteExecutor()
    {
    }

    public void init(ThreadReference thread) throws Exception
    {
        if ( state.get() == State.Initialization ||
                     state.get() == State.Initialized )
            return;

        if ( !state.compareAndSet(State.Initial, State.Initialization) &&
                !state.compareAndSet(State.Failed, State.Initialization)  )
            return;

        try {
            Class slave = RemoteAgent.class;
            RemoteInstaller.getInstance().install(thread, Arrays.asList(slave));
            CallSpec init = new CallSpec(slave.getName(), RemoteAgent.initMethodName);
            Value sockAddr = getValue(thread, init);
            String addr = ((StringReference)sockAddr).value();
            log.log(Level.FINE, ()->"Remote agent address: " + addr);
            String[] addrparts = addr.split(":");
            int port = Integer.parseInt(addrparts[1]);
            slaveAddress = new InetSocketAddress(addrparts[0], port);
            ConfigEntry config = this.getConfig();
            RemoteJVM.lookup(thread.virtualMachine()).apply(config);
            state.set(State.Initialized);
        } catch (Throwable ex) {
            state.set(State.Failed );
            log.log(Level.SEVERE, getClass().getSimpleName() + ": fail", ex);
            throw ex;
        }
    }

    public void submit(ErrorproneFunction<ThreadReference,Void> task) throws IOException
    {
        if ( slaveAddress == null )
            throw new IllegalStateException("Not connected");

        if ( slaveConnection == null )
            slaveConnection = new Socket(slaveAddress.getHostName(), slaveAddress.getPort());
        try {
            slaveConnection.getOutputStream().write(0x01);
        } catch (IOException iex) {
            // may be meanwhile slave created new socket
            // renew and try to send ping - if fails again - nothing to do
            slaveConnection.close();
            slaveConnection = new Socket(slaveAddress.getHostName(), slaveAddress.getPort());
            slaveConnection.getOutputStream().write(0x01);
        }

        pendings.add(task);
        try {
            if ( slaveConnection == null )
                slaveConnection = new Socket(slaveAddress.getHostName(), slaveAddress.getPort());
            slaveConnection.getOutputStream().write(0x01);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "failed to notify remote slave", ex);
            pendings.remove(task);
            slaveConnection = null;
            throw ex;
        }
    } // submit

    private void play(ThreadReference thread)
    {
        ErrorproneFunction<ThreadReference,Void> task = null;
        while((task=pendings.poll()) !=  null) {
            try {
                ErrorproneFunction<ThreadReference,Void> fintask = task;
                log.log(Level.FINE, ()->"Submitting async task: " + fintask);
                task.apply(thread);
            } catch (Throwable ex) {
                log.log(Level.SEVERE,"Async task failed: " + task, ex);
            }
        }
    } // play

    private ConfigEntry getConfig()
    {
        MethodEntryBreakPointSpec mbp = new MethodEntryBreakPointSpec();
        mbp.setMethod(RemoteAgent.remoteExecutionHookName);
        mbp.setTargetClass(RemoteAgent.class.getName());
        RuntimeActionSpec actSpec = new RuntimeActionSpec();
        Action playAction = new Action(actSpec) {
            @Override
            public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception {
                play(event.thread());
            }
        };
        actSpec.setAction(playAction);
        ConfigEntry entry = new ConfigEntry();
        entry.setBreakPoint(mbp);
        entry.getActionList().add(actSpec);
        return entry;
    } // getConfig

    public static class RemoteAgent extends Thread
    {
        static RemoteAgent agent;
        private final ServerSocket serso;

        final static String initMethodName = "init";
        static String init() throws IOException
        {
            if ( agent == null )
                agent = new RemoteAgent();
            String res = toString(agent.getAddr());
            return res;
        }

        public RemoteAgent() throws IOException
        {
            super("OlDRemoteAgent");
            setDaemon(true);
            setPriority(3);
            ServerSocket sor = new ServerSocket(0);
            int port = sor.getLocalPort();
            sor.close();
            serso = new ServerSocket(port);
            start();
        }

        public void run()
        {
            InputStream is = null;
            Socket accepted = null;
            while(true) {
                try {
                    if ( is == null ) {
                        accepted = serso.accept();
                        is = accepted.getInputStream();
                    }
                    int ch = is.read();
                    if ( ch == -1 ) { // disconnected
                        closeSilently(accepted, is);
                        is = null;
                        continue;
                    }
                    remoteExecutionHook();
                } catch(Throwable sth) {
                    closeSilently(accepted, is);
                    is = null;
                } // catch
            } // while
        } // run

        public final static String remoteExecutionHookName = "remoteExecutionHook";
        void remoteExecutionHook()
        {
            return;
        }

        private ServerSocket getAddr()
        {
            return serso;
        }

        private void closeSilently(Socket sock, InputStream is)
        {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable th) {
                }
            }
            if (sock != null) {
                try {
                    sock.close();
                } catch (Throwable th) {
                }
            }
        }

        private static String toString(ServerSocket serso) throws IOException
        {
            Socket sk =
                    new Socket(serso.getInetAddress().getHostName(), serso.getLocalPort());
            String host = sk.getLocalAddress().getHostAddress();
            sk.close();
            return host + ":" + serso.getLocalPort();
        }
    } // RemoteAgent
}
