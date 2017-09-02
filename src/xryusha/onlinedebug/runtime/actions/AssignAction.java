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

        // RefPath with class and var, static var
        //-----------------------
        if ( fieldPath.getType() != null ) {
            ClassType type = (ClassType) super.getClass(thread, fieldPath.getType());
            WrappedValueSetter wrap = getForStatic(type, fieldPath.getValue());
            return wrap;
        }

        //
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
            WrappedValueSetter cachedWrap = new WrappedValueSetter(toBeCached, local.type());
            valueSetterRef.set(cachedWrap);

            ValueSetter toBeUsedNow = (t,v) -> curFrame.setValue(local, v);
            WrappedValueSetter nowwrap = new WrappedValueSetter(toBeUsedNow, local.type());
            return nowwrap;
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
            WrappedValueSetter wrap = getForStatic(ownerClazz, fieldPath.getValue());
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
        WrappedValueSetter toBeUsedNowWrap = new WrappedValueSetter(toBeUsedNow, field.type());
        return toBeUsedNowWrap;
    } // buildCache

    private WrappedValueSetter getForStatic(ClassType ownerClazz, String fieldName) throws Exception
    {
        Field field = ownerClazz.fieldByName(fieldName);
        if ( field == null ) {
            String msg = "Field " + fieldName + " not found on class " + ownerClazz.name();
            log.severe(msg);
            throw new RemoteDataAccessException(msg, normalizedTargetPath);
        }

        ValueSetter toBeCached = (t,v)->ownerClazz.setValue(field, v);
        WrappedValueSetter wrap = new WrappedValueSetter(toBeCached, field.type());
        valueSetterRef.set(wrap);
        return wrap;
    } // getForStatic

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
