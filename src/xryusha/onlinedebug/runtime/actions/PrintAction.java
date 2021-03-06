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

package xryusha.onlinedebug.runtime.actions;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.PrintSpec;
import xryusha.onlinedebug.config.values.eventspecific.CurrentException;
import xryusha.onlinedebug.config.values.eventspecific.ModificationCurrent;
import xryusha.onlinedebug.config.values.eventspecific.ModificationNew;
import xryusha.onlinedebug.config.values.eventspecific.ReturnValue;
import xryusha.onlinedebug.runtime.PrimitiveValueFactory;
import xryusha.onlinedebug.runtime.util.RemoteInstaller;
import xryusha.onlinedebug.config.values.*;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.SyntheticRValue;


public class PrintAction extends Action<PrintSpec>
{
    public static final String RUN_SYNC = "action.print.sync";

    private final static Object remotePrinterCreationLock = new Object();
    private final static Map<String,Ref> remoteAppender = new ConcurrentHashMap<>();
    private final static boolean useSync = Boolean.getBoolean(RUN_SYNC);

    private final BlockingDeque<ArrayReference> arraysPool = new LinkedBlockingDeque<>();
    private final Map<Class<? extends RValue>, ValueAccessor> valueAccessors = createValueAccessors();
    private final String localFile;
    private final String remoteFile;
    private final long localOffset = ZoneId.systemDefault().getRules()
                                           .getOffset(LocalDateTime.now())
                                           .getTotalSeconds() * 1000;
    private SyntheticRValue printfFormatRValue;
    private SyntheticRValue printfArgsRValue;
    private StringReference printfFormat;
    private ArrayType arrayType;
    private Ref remotePrintingDispatcher = null;
    private volatile Boolean isAsyncWriterInstalled;


    public PrintAction(ThreadReference thread, PrintSpec actionConfig) throws Exception
    {
        super(actionConfig);
        localFile = makeInternal(actionConfig.getLocalLogFile());
        remoteFile = makeInternal(actionConfig.getRemoteLogFile());

        printfFormatRValue = new SyntheticRValue();
        printfFormatRValue.setType(String.class.getName());
        printfArgsRValue = new SyntheticRValue();
        printfArgsRValue.setType(Object[].class.getCanonicalName());

        if ( !shouldPrintRemote(actionConfig.getLocation()) )
            return;

        arrayType = (ArrayType) getClass(thread, Object[].class.getCanonicalName());

        RemoteInstaller installer = RemoteInstaller.getInstance();
        try {
            if ( isAsyncWriterInstalled == null ) {
                boolean installed =
                  installer.install(thread, Arrays.asList(AsyncWriterDispatcher.class ));
                if ( installed && !useSync )
                  remotePrintingDispatcher =
                     createRemoteAsyncPrinter(thread, remoteFile, printfFormatRValue, printfArgsRValue);
                isAsyncWriterInstalled = Boolean.valueOf(remotePrintingDispatcher != null);
                log.log(Level.INFO, "remote AsyncWriter installed {0}", installed);
            }
        } catch (Throwable ex) {
            log.log(Level.SEVERE, "running in synchronous mode as remote AsyncWriter installing failed", ex);
            isAsyncWriterInstalled = Boolean.FALSE;
        }
    } // ctor

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        StringBuilder formatSB = new StringBuilder();
        List<LazyValueHolder> data = collectData(thread, ctx, formatSB);
        formatSB.append("\n");
        String format = formatSB.toString();
        // if remote logging was enabled and message was
        // already formatted remotely, we can use the
        // omit additiona formatting and use the ready one,
        // so release the thread faster
        String remotelyEvaluated = null;
        if ( shouldPrintRemote(spec.getLocation()))
            remotelyEvaluated = printRemote(thread, data, format);
        if ( shouldPrintLocal(spec.getLocation())) {
//System.out.println("Received from remote: " + remotelyEvaluated);
            printLocal(thread, data, format, remotelyEvaluated);
        }
    } // execute

    @Override
    public void shutdown()
    {
        super.shutdown();
        remoteAppender.clear();
        AsyncWriterDispatcher.shutdown();
        printfFormat = null;
        printfFormatRValue = null;
        printfArgsRValue = null;
    }

    private String makeInternal(String loc) throws IOException
    {
        if ( loc != null ) loc = loc.trim();
        if ( loc != null && !"".equals(loc))
            loc = new File(loc).getCanonicalFile().toURI().getPath();
        if ( loc == null )
            loc = "";
        loc = loc.intern();
        return loc;
    } // makeInternal

    private boolean shouldPrintLocal(PrintSpec.LoggingVM location)
    {
        return location == PrintSpec.LoggingVM.local ||
                location == PrintSpec.LoggingVM.both ;
    }

    private boolean shouldPrintRemote(PrintSpec.LoggingVM location)
    {
        return location == PrintSpec.LoggingVM.remote ||
                location == PrintSpec.LoggingVM.both ;
    }


    private String printRemote(ThreadReference thread, List<LazyValueHolder> values, String format) throws Exception
    {
        boolean arrayWasJustCreated = false;
        ArrayReference array = arraysPool.poll();
        if ( array == null ) {
            array = arrayType.newInstance(values.size());
            array.disableCollection();
            arrayWasJustCreated = true;
        }

        // threadsafe is irrelevant here as it is just a
        // caching of same format value,
        // overridden by another thread in worth case
        if ( printfFormat == null )
            printfFormat = thread.virtualMachine().mirrorOf(format);
//System.out.println("FORMAT: " + printfFormat);
        for(int inx = 0; inx <values.size(); inx++) {
            RValue rval = this.spec.getParams().get(inx);
            LazyValueHolder holder = values.get(inx);

            // const values already converted and set in previous usages, if it is not the 1st time
            if ( !(rval instanceof Const) || arrayWasJustCreated ) {
                Value val = holder.asValue(thread);
                if ( val != null && PrimitiveValueFactory.canConvert(val.type())) {
                    // TODO: replace fast&dirty T->String->StringReference to reasonable solution
                    String str = toString(thread, val);
                    val = PrimitiveValueFactory.convert(thread, String.class.getName(), str);
                }
                array.setValue(inx, val);
            } // if array just created
        } // for values

        printfFormatRValue.setValue(printfFormat);
        printfArgsRValue.setValue(array);
//System.out.println("Sendigg format: " + printfFormatRValue.getValue()
//                   +" value: " + printfArgsRValue.getValue());
        Ref writer = getCreatePrintChain(thread,remoteFile, printfFormatRValue, printfArgsRValue);
        Value result = null;
        try {
            result = getValue(thread, writer);
//} catch(Exception ex) {
//System.err.println("::"+writer);
//ex.printStackTrace();
//throw ex;
        } finally {
            arraysPool.add(array);
        }
        if ( result instanceof StringReference )
            return ((StringReference)result).value();
        return null;
    } // printRemote


    private void printLocal(ThreadReference thread, List<LazyValueHolder> data, String format, String alreadyEvaluated) throws Exception
    {
        Object[] args;
        if ( alreadyEvaluated != null ) {
            args = new Object[]{alreadyEvaluated};
            format = "%s";
        }
        else {
            args = new Object[data.size()];
            for(int inx = 0; inx < data.size(); inx++) {
                LazyValueHolder tuple = data.get(inx);
                if ( tuple.strvalue != null ) {
                    args[inx] = tuple.strvalue;
                } else {
                    args[inx] = toString(thread, tuple.jdivalue);
                }
            } // for data
        }
        AsyncWriterDispatcher.submit(localFile, format, args);
    } // printLocal


    private boolean verifyCanWrite(ThreadReference thread, String file) throws Exception
    {
        LogTarget target = LogTarget.getTarget(file);
        if ( target != LogTarget.FILE )
            return true;
        Constructor ctor = new Constructor(File.class.getName());
        ctor.getParams().add(new Const(remoteFile, String.class.getName()));
        ObjectReference fileRef = (ObjectReference) getValue(thread, ctor);
        SyntheticRValue fileRVal = new SyntheticRValue(fileRef);
        // file exists and writable
        RefChain refChain = new RefChain();
        refChain.getRef().add(fileRVal);
        refChain.getRef().add(new CallSpec(null, "canWrite"));
        BooleanValue can = (BooleanValue) getValue(thread, refChain);
        if ( can.booleanValue() )
            return true;
        // file does not exist
        RefChain writableParentChain = new RefChain();
        writableParentChain.getRef().add(fileRVal);
        writableParentChain.getRef().add(new CallSpec(null, "getParentFile"));
        writableParentChain.getRef().add(new CallSpec(null, "canWrite"));
        can = (BooleanValue) getValue(thread, writableParentChain);
        return can.booleanValue();
    } // verifyCanWrite


    private Ref getCreatePrintChain(ThreadReference thread, String path,
                                    SyntheticRValue  format, SyntheticRValue  arrayData) throws Exception
    {
        if ( remotePrintingDispatcher != null ) {
//System.err.println("***Dispather used");
            return remotePrintingDispatcher;
        }
//System.err.println("***Sync used");
        // for some reason async was not uploaded,
        // going to use synchronous log file writing
        Ref writer = remoteAppender.get(path/*remoteFile*/);
        if ( writer != null )
            return writer;

        synchronized (remotePrinterCreationLock) {
            if ( (writer=remoteAppender.get(path/*remoteFile*/ )) == null ) {
                writer = createRemoteSyncPrinter(thread,path,format,arrayData);
                remoteAppender.put(path/*remoteFile*/, writer);
            } // inner if
        } // synch
        return writer;
    } // getCreatePrintChain


    private Ref createRemoteAsyncPrinter(ThreadReference thread,
                                         String path,
                                         SyntheticRValue format,
                                         SyntheticRValue data) throws Exception
    {
        boolean canWrite = verifyCanWrite(thread, path);
        if ( !canWrite )
            throw new IllegalArgumentException("can't write remote file " + remoteFile);

        CallSpec call = new CallSpec(AsyncWriterDispatcher.class.getName(), "submit");
        call.setMethod("submit");
        call.getParams().add(new Const(path, String.class.getName()));
        call.getParams().add(format);
        call.getParams().add(data);
        return call;
    } // createRemoteAsyncPrinter


    private Ref createRemoteSyncPrinter(ThreadReference thread,
                                        String path,
                                        SyntheticRValue format,
                                        SyntheticRValue data) throws Exception
    {
        ObjectReference printStream;

        LogTarget targetType = LogTarget.getTarget(path);
        switch (targetType) {
            case FILE:
               printStream = createRemotePrintStream(thread, path);
               break;
            default:
                printStream = (ObjectReference)
                        getValue(thread, new RefPath(System.class.getName(), targetType.systemStream));
        }
        printStream.disableCollection();

        RefChain refChain = new RefChain();
        refChain.getRef().add(new SyntheticRValue(printStream, true));

        CallSpec call = new CallSpec();
        call.setMethod("printf");
        call.getParams().add(format);
        call.getParams().add(data);

        refChain.getRef().add(call);
        return refChain;
    } // createRemoteSyncPrinter


    private ObjectReference createRemotePrintStream(ThreadReference thread, String path) throws Exception
    {
        Const _true = new Const(Boolean.TRUE.toString(), boolean.class.getName());
        Constructor fos = new Constructor(FileOutputStream.class.getName());
        fos.getParams().add(new Const(path/*remoteFile*/, String.class.getName()));
        fos.getParams().add(_true);

        Constructor bos = new Constructor(BufferedOutputStream.class.getName());
        bos.getParams().add(fos);

        Constructor newpos = new Constructor(PrintStream.class.getName());
        newpos.getParams().add(bos);
        newpos.getParams().add(_true);
        ObjectReference printstream = (ObjectReference) getValue(thread, newpos);
        printstream.disableCollection();
        return printstream;
    } // createRemotePrintStream


    private List<LazyValueHolder> collectData(ThreadReference thread, ExecutionContext ctx, StringBuilder formatBuilder) throws Exception
    {
        List<LazyValueHolder> data = new ArrayList<>(spec.getParams().size());
        for(int inx = 0; inx < spec.getParams().size(); inx++) {
            RValue arg = spec.getParams().get(inx);
            LazyValueHolder holder = new LazyValueHolder();
            ValueAccessor accessor = valueAccessors.get(arg.getClass());
            data.add(holder);
            if ( accessor == null )
                accessor = valueAccessors.get(Ref.class);
            String format = accessor.extract(thread, ctx, arg, holder);
            formatBuilder.append('%')
                         .append(inx+1)
                         .append('$')
                         .append(format);
        } // for params
        return data;
    } // collectData


    private String generalRefValueFormatter(ThreadReference thread, RValue arg, LazyValueHolder holder) throws Exception
    {
        Value value = super.getValue(thread, arg);
        String type = value!= null ? value.type().name() : "";
        String string = null;
        if ( value != null && PrimitiveValueFactory.canConvert(value.type())) {
            string  = PrimitiveValueFactory.convert(value).toString();
            value = null;
            type = String.class.getName();
        }
        String format = "s";
        holder.setJdivalue(value);
        holder.setStrvalue(string);
        return format;
    } // generalRefValueFormatter


    private String stackToString(ThreadReference thread) throws IncompatibleThreadStateException
    {
        List<StackFrame> frames = thread.frames();
        StringBuilder sb = new StringBuilder();
        boolean isfirst = true;
        for(StackFrame frame: frames) {
            if ( !isfirst )
                sb.append(System.lineSeparator());
            isfirst = false;
            sb.append("  ").append(frame.location());
        } // frames
        return sb.toString();
    } // stackToString


    class Line { String name, type; Value value; }


    private String dumpToString(ThreadReference thread, PrintSpec.Dump arg) throws Exception
    {
        StackFrame frame = thread.frame(0);
        StringBuilder sb = new StringBuilder();

        if ( arg.getSource().contains(PrintSpec.Dump.DumpSource.args)) {
            List<Value> args = frame.getArgumentValues();
            if ( args.size() > 0 ) {
                List<Line> result = new ArrayList<>();
                List<String> types = frame.location().method().argumentTypeNames();
                for(int inx = 0; inx < args.size(); inx++) {
                    Line line = new Line();
                    line.name = ""+(inx+1);
                    line.value = args.get(inx);
                    line.type =  types.get(inx);
                    result.add(line);
                }
                append2Buffer(thread, "method", result, sb);
            }
        } // args
        if ( arg.getSource().contains(PrintSpec.Dump.DumpSource.visible)) {
            List<LocalVariable> lvars = frame.visibleVariables();
            if ( lvars.size() > 0 ) {
                //<name,<type,value>>
                List<Line> reslist = new ArrayList<>();
                for(LocalVariable lvar : lvars) {
                    if ( lvar.isArgument())
                        continue;
                    Line line = new Line();
                    line.name = lvar.name();
                    line.type = lvar.typeName();
                    line.value = frame.getValue(lvar);
                    reslist.add(line);
                } // for lvars
                append2Buffer(thread, "visible", reslist, sb);
            } // if size > 0
        } // if DumpSource.visible
        String res = sb.toString();
        return res;
    } // dumpToString


    private void append2Buffer(ThreadReference thread, String title, List<Line> data, StringBuilder buffer)
    {
        buffer.append("--- ").append(title).append(" args --- ");
        boolean first = true;
        for(Line line: data) {
            String name = line.name;
            String type = line.type;
            Value value = line.value;
            if ( type == null && value!= null )
                type = value.type().name();
            if ( type == null )
                type = "unaccessible";
            buffer.append(System.lineSeparator())
                    .append("  ")
                    .append(name)
                    .append(": ")
                    .append(type)
                    .append(": ");
            try {
                String asString = value != null ? this.toString(thread, value) : "null";
                boolean string = type.equals(String.class.getName()) && value != null;
                if ( string )
                    buffer.append('"');
                buffer.append(asString);
                if ( string )
                    buffer.append('"');
            } catch (Throwable th) {
                buffer.append("::failed to obtain value: ")
                        .append(th);
            }
        } // for vars
        buffer.append(System.lineSeparator())
                .append("------------------- ");
    } // append2Buffer

    private String toTimeString(long time)
    {
        long localtime = time + localOffset;
        long msec = localtime % 1000;

        long totalsec = localtime / 1000;
        long seconds = totalsec % 60;

        long totalmin = totalsec / 60;
        long minutes = totalmin % 60;

        long totalh = totalsec / 3600;
        long hours = totalh % 24;

        StringBuilder sb = new StringBuilder();
        append(hours, 2, sb);
        sb.append(":");
        append(minutes, 2, sb);
        sb.append(":");
        append(seconds, 2, sb);
        sb.append(":");
        append(msec, 3, sb);
        return sb.toString();
    } // toTimeString

    private void append(long value, int digits, StringBuilder sb)
    {
        String[][] padds = new String[][]{
                new String[] {"0", "", ""},
                new String[] {"00", "0", ""},
                new String[] {"000", "00", "0"}
        } ;

        if ( value == 0 ) {
            sb.append(padds[digits - 1][0]);
            return;
        }
        if ( value < 10 )
            sb.append(padds[digits-1][1]);
        else if ( value < 100 )
            sb.append(padds[digits-1][2]);
        sb.append(value);
    }

    private  Map<Class<? extends RValue>, ValueAccessor> createValueAccessors()
    {
        return new HashMap<Class<? extends RValue>, ValueAccessor>() {{
            put(CurrentException.class,
                 (t, ctx, v, h)->{
                     h.setJdivalue((Value) ctx.getEventSpecificValue(CurrentException.class));
                     return "s";
                   });
            put(ReturnValue.class,
                 (t, ctx, v, h)-> {
                     h.setJdivalue((Value) ctx.getEventSpecificValue(ReturnValue.class));
                     return "s";
                 });
            put(ModificationCurrent.class,
                (t, ctx, v, h)->{
                    h.setJdivalue((Value) ctx.getEventSpecificValue(ModificationCurrent.class));
                    return "s";
                });
            put(ModificationNew.class,
                (t, ctx, v, h)->{
                    h.setJdivalue((Value) ctx.getEventSpecificValue(ModificationNew.class));
                    return "s";
                });
            put(Const.class,
                    (t, ctx, v, h)->{
                     h.setStrvalue(((Const)v).getValue());
                     return "s";
                    });
            put(PrintSpec.ThreadStack.class,
                (t, ctx, v, h)-> {
                    h.setStrvalue(stackToString(t));
                    return "s";
                });
            put(PrintSpec.Dump.class,
                (t,ctx, v,h)->{
                    h.setStrvalue(dumpToString(t, (PrintSpec.Dump)v));
                    return "s";
                });
            put(PrintSpec.ThreadName.class,
                (t,ctx,v,h)->{
                    h.setStrvalue(t.name());
                    return "s";
                });
            put(PrintSpec.ReceiveTime.class,
                    (t,ctx,v,h)->{
                        h.setStrvalue(toTimeString(ctx.getReceiveTime()));
                        return "s";
                    });
            put(Ref.class,
                    (t,ctx,v,h)-> generalRefValueFormatter(t, v, h) );
        }};
    } // createValueAccessors

    @FunctionalInterface
    private interface ValueAccessor
    {
        String extract(ThreadReference th, ExecutionContext ctx, RValue value, LazyValueHolder targer) throws Exception;
    }

    // avoiding non-necessary string->value convertion
    private static class LazyValueHolder
    {
        private String strvalue;
        private Value jdivalue;

        public LazyValueHolder()
        {
        }

        public LazyValueHolder(String strvalue, Value jdivalue)
        {
            this.strvalue = strvalue;
            this.jdivalue = jdivalue;
        }

        public String getStrvalue()
        {
            return strvalue;
        }

        public void setStrvalue(String strvalue)
        {
            this.strvalue = strvalue;
        }

        public Value getJdivalue()
        {
            return jdivalue;
        }

        public void setJdivalue(Value jdivalue)
        {
            this.jdivalue = jdivalue;
        }

        public Value asValue(ThreadReference thread) throws Exception
        {
            if ( jdivalue != null )
                return jdivalue;
            if ( strvalue != null )
              return PrimitiveValueFactory.convert(thread, String.class.getName(), strvalue);
            return null;
        }
    } //  class LazyValueHolder

    public static class AsyncWriterDispatcher
    {
        static {
            Runtime.getRuntime().addShutdownHook( new Thread() {
                                                    public synchronized void start() {
                                                        shutdown();
                                                    }}
                                                );
        }
        private static ConcurrentMap<String,AsyncWriter> writers = new ConcurrentHashMap<>();

        public static String submit(String path, String format, Object[] args) throws Exception
        {
//System.err.println("Submit: path<" + path + ">\n"+
//                   " format < " + format + ">\n"+
//                   " args <" + Arrays.toString(args)+">");
            AsyncWriter wr = writers.get(path);
            // not using lambda or writers.computeIfAbsent
            // to make it run on remote 1.7 as well
            if ( wr == null ) {
                synchronized (writers) {
                    wr = writers.get(path);
                    if ( wr == null ) {
                        wr = AsyncWriter.newInstance(path);
                        writers.put(path, wr);
                    }
                }
            } // eof if outer wr == null
            return wr.submit(format, args);
        } // submit

        public static void shutdown()
        {
            for(AsyncWriter writer: writers.values() ) {
                try {
                    writer.shutdown();
                } catch(Throwable th) {
                    th.printStackTrace();
                }
            }
            writers.clear();
        } // disconnect
    } // AsyncWriterDispatcher


    public static class AsyncWriter implements ThreadFactory
    {
        private final PrintStream target;
        private final ExecutorService appender;
        private final ExecutorService verifier;
        private final String targetPath;

        public static AsyncWriter newInstance(String targetPath) throws IOException
        {
            return new AsyncWriter(targetPath);
        }

        private AsyncWriter(String targetPath) throws IOException
        {
            this.targetPath = targetPath;
            LogTarget logTarget = LogTarget.getTarget(targetPath);
            switch ( logTarget ) {
                case STDOUT:
                    this.target = System.out;
                    break;
                case STDERR:
                    this.target = System.err;
                    break;
                default:
                    FileOutputStream os = new FileOutputStream(targetPath, true);
                    this.target = new PrintStream(os, true);
            } // switch

            appender = Executors.newSingleThreadExecutor(this);
            verifier = Executors.newSingleThreadExecutor(this);
        } // AsyncWriter


        public String submit(String format, Object[] args) throws Exception
        {
            final String message = String.format(format, args);
            final Future future = appender.submit(new Callable<Void>() {
                                                public Void call() throws Exception {
                                                    target.print(message);
                                                    target.flush();
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
        } // submit

        public void shutdown()
        {
            appender.shutdownNow();
            verifier.shutdownNow();
            target.flush();
            target.close();
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread th = new Thread(r);
            th.setDaemon(true);
            return th;
        }
    }

    enum LogTarget
    {
        STDOUT("out"), STDERR("err"), FILE(null);

        private final String systemStream;

        LogTarget(String systemStream)
        {
            this.systemStream = systemStream;
        }

        static LogTarget getTarget(String targetPath)
        {
            LogTarget target;
            switch ( targetPath ) {
                case "":
                case "out":
                case "stdout":
                    target = STDOUT;
                    break;
                case "err":
                case "stderr":
                    target = STDERR;
                    break;
                default :
                    target = FILE;
            }
            return target;
        }  // getTarget
    } // LogTarget
}

