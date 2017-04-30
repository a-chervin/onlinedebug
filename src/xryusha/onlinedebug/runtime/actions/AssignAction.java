package xryusha.onlinedebug.runtime.actions;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import xryusha.onlinedebug.config.actions.AssignSpec;
import xryusha.onlinedebug.config.values.Const;
import xryusha.onlinedebug.exceptions.RemoteDataAccessException;
import xryusha.onlinedebug.runtime.ExecutionContext;
import xryusha.onlinedebug.runtime.PrimitiveValueFactory;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.config.values.RefPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AssignAction extends Action<AssignSpec>
{
    private List<RValue> normalizedTargetPath;
    private AtomicReference<WrappedValueSetter> valueSetterRef = new AtomicReference<>();

    public AssignAction(AssignSpec spec)
    {
        super(spec);
        List<RValue> normalized = normalize(Arrays.asList(spec.getTarget()));
        normalizedTargetPath = Collections.unmodifiableList(normalized);
    }

    @Override
    public void execute(LocatableEvent event, ExecutionContext ctx) throws Exception
    {
        ThreadReference thread = event.thread();
        WrappedValueSetter cachedSetter = valueSetterRef.get();
        if ( cachedSetter == null )
            cachedSetter = buildCache(thread);
        ValueSetter setter =  cachedSetter.setter;
        Value value = null;
        if ( spec.getValue() instanceof Const) {
            // check if type set for const configuration, fix if needed
            Type expectedType = cachedSetter.type;
            Const cnst = (Const) spec.getValue();
            if ( !expectedType.name().equals(cnst.getType()) &&
                    PrimitiveValueFactory.canConvert(expectedType)) {
                // fix current value
                value = PrimitiveValueFactory.convert(thread, expectedType, cnst.getValue());
                // update const type definition, so next type this intervention won't be required
                cnst.setType(expectedType.name());
            }
        }
        if ( value == null )
            value = getValue(thread, spec.getValue());
        setter.set(thread, value);
    } // execute

    private WrappedValueSetter buildCache(ThreadReference thread) throws Exception
    {
        List<RValue> targetpath = new ArrayList<>(normalizedTargetPath);
        RefPath fieldPath = (RefPath) targetpath.remove(targetpath.size()-1);
        if ( targetpath.size() > 0 ) { // regular path
            ObjectReference owner =  (ObjectReference) getValue(thread, targetpath);
            if ( owner == null )
                throw new RemoteDataAccessException("Field owner can't be accessed",  targetpath);
            ClassType ownerClazz =(ClassType) owner.type();
            Field targetField = ownerClazz.fieldByName(fieldPath.getValue());
            if ( targetField == null )
                throw new RemoteDataAccessException("Target field not found",  normalizedTargetPath);
            ValueSetter toBeCached = (t,v)-> {
                                            ObjectReference tgt =  (ObjectReference) getValue(thread, targetpath);
                                            if ( tgt == null )
                                                throw new RemoteDataAccessException("owner can't be accessed",  targetpath);
                                            owner.setValue(targetField, v);
                                       };
            WrappedValueSetter cachewrap = new WrappedValueSetter(toBeCached, targetField.type());
            valueSetterRef.set(cachewrap);
            ValueSetter toBeUsedNow = (t,v)->owner.setValue(targetField, v);
            WrappedValueSetter nowwrap = new WrappedValueSetter(toBeUsedNow, targetField.type());
            return nowwrap;
        } // if normal path

        // no path, just variable name, i.e. it's a
        // local, static of current class or this instance variable
        //-----------
        // local:
        StackFrame curFrame = thread.frame(0);
        LocalVariable local = curFrame.visibleVariableByName(fieldPath.getValue());
        if ( local != null ) {
            ValueSetter toBeCached = (t,v) -> {
                                        StackFrame frame = t.frame(0);
                                        frame.setValue(local, v);
                                     };
            ValueSetter toBeUsedNow = (t,v) -> curFrame.setValue(local, v);
            WrappedValueSetter wrap = new WrappedValueSetter(toBeUsedNow, local.type());
            valueSetterRef.set(wrap);
            return wrap ;
        } // if local

        ClassType ownerClazz = (ClassType)curFrame.location().declaringType();
        ObjectReference thiis = curFrame.thisObject();
        Field field = ownerClazz.fieldByName(fieldPath.getValue());
        if ( field == null ) {
            String msg = "Field " + fieldPath.getValue() + " not found on class " + ownerClazz.name();
            log.severe(msg);
            throw new RemoteDataAccessException(msg, normalizedTargetPath);
        }

        if ( thiis == null ) { // static field
            ValueSetter toBeCached = (t,v)->ownerClazz.setValue(field, v);
            WrappedValueSetter wrap = new WrappedValueSetter(toBeCached, field.type());
            valueSetterRef.set(wrap);
            return wrap;
        }
        // instance var
        ValueSetter toBeCached = (t,v) -> {
                                      ObjectReference ref = t.frame(0).thisObject();
                                      ref.setValue(field, v);
                                  };
        WrappedValueSetter wrap = new WrappedValueSetter(toBeCached, field.type());
        valueSetterRef.set(wrap);
        ValueSetter toBeUsedNow = (t,v)->thiis.setValue(field, v);
        WrappedValueSetter nowwrap = new WrappedValueSetter(toBeUsedNow, field.type());
        return nowwrap;
    } // buildCache


    static class WrappedValueSetter
    {
        private final ValueSetter setter;
        private final Type type;

        public WrappedValueSetter(ValueSetter setter, Type type)
        {
            this.setter = setter;
            this.type = type;
        }
    }

    @FunctionalInterface
    interface ValueSetter
    {
        void set(ThreadReference thread, Value value) throws Exception;
    }
}