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

package xryusha.onlinedebug.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jdi.*;
import xryusha.onlinedebug.config.values.*;
import xryusha.onlinedebug.config.values.eventspecific.CurrentException;
import xryusha.onlinedebug.util.Log;
import xryusha.onlinedebug.exceptions.RemoteDataAccessException;
import xryusha.onlinedebug.exceptions.RemoteFieldNotFoundException;
import xryusha.onlinedebug.exceptions.RemoteMethodNotFoundException;


/**
 * Base class for all types performing actions (queries/calls) on remote VM
 */
public class RemotingBase
{
    protected final static Logger log = Log.getLogger();//Logger.getGlobal();
    private final Map<Class, AccessorFactory> accessorFactories = new IdentityHashMap<Class, AccessorFactory>() {{
        put(Const.class, (th, type, ref, spec)->createAccessConstValue(th,type,ref,spec));
        put(RefPath.class, (th,type,ref,spec)->createAccessRefPathValue(th,type,ref,spec));
        put(CallSpec.class, (th, type, ref, spec)->createMethodValueAcessor(th,type,ref,spec));
        put(Constructor.class, (th, type, ref, spec)->createMethodValueAcessor(th,type,ref,spec));
        put(SyntheticRValue.class, (th,type,ref,spec)->createAccessSynthetic(th,type,ref,spec));
        put(CurrentException.class, (th, type, ref, spec)->createAccessException(th,type,ref,spec));
        put(ArrayIndex.class, (th, type, ref, spec)->createAccessArray(th,type,ref,spec));
    }};
    /**
     * cache for remote instanceOf check results
     */
    private static ConcurrentMap<String,Boolean> instanceofCache = new ConcurrentHashMap<>();
    private Map<CacheKey, List<Accessor>> accessors = new ConcurrentHashMap<>();
    private AtomicReference<CallSpec> remoteToString = new AtomicReference<>();



    // just a simple utility method of toString on remote VM
    protected String toString(ThreadReference thread, Value value) throws Exception
    {
        String res;
        if (value == null) {
            res = null;
        } else if (PrimitiveValueFactory.canConvert(value.type())) {
            res = PrimitiveValueFactory.convert(value).toString();
        } else {
            CallSpec call = constructToStringCall();
            ((SyntheticRValue) call.getParams().get(0)).setValue(value);
            StringReference strval = (StringReference) getValue(thread, call);
            res = strval.value();
        }
        return res;
    } //  toString


    protected Value getValue(ThreadReference thread, RValue value) throws Exception
    {
        return getValue(thread, value, false);
    }

    protected Value getValue(ThreadReference thread, RValue value, boolean swallowException) throws Exception
    {
        try {
            return _getValue(thread, Arrays.asList(value));
        } catch (Exception ex) {
            if ( swallowException ) {
                log.log(Level.FINE, "ignorring failure to evaluate : " + value);
                return null;
            }
            throw ex;
        }
    }

    protected Value getValue(ThreadReference thread, List<RValue> variablePath) throws Exception
    {
        try {
            return _getValue(thread, variablePath);
        } catch (Throwable th) {
            log.log(Level.SEVERE, "failed to evaluate : " + variablePath, th);
            throw (Exception) th;
        }
    } // getValue

    /**
     * Performs evaluation of {@link RValue} element, used as well for remote call execution.
     * @param thread
     * @param variablePath ref path may contain single element (for example {@link Const} "a"
     *                     or reference to variable var ({@link RefPath}) or static method
     *                     invocation ({@link CallSpec}), but may be constructed
     *                     for more complicated chains: {@code a.b.d} or {@code a.d(var2).f[inx].K} etc.
     *                     In such case each chain link is an evaluation element.
     *                     <i>Note</i>: accessed fields or methods don't have to be necessary public
     * @return evaluation result
     * @throws Exception
     */
    private Value _getValue(final ThreadReference thread, List<RValue> variablePath) throws Exception
    {
        String prematureMessageFormat = "Access premature termination on entry %1$s for path %2$s";
        StackFrame currFrame = thread.frame(0);
        // check if this same request was performed previously, if yes using cached
        // execution path, if not build new one and cache for next usages
        CacheKey key = new CacheKey(thread, currFrame, variablePath);
        List<Accessor> accessorsList = accessors.get(key);
        Value currVal = currFrame.thisObject();/*null*/;
        boolean continueLoop = true;
        ListIterator<Accessor> itr = accessorsList != null ? accessorsList.listIterator() : null;
        while( accessorsList != null && continueLoop  && itr.hasNext() ) {
            Accessor acc = itr.next();
            currVal = acc.getValue(currVal);
            continueLoop = currVal != null;
        }

        if ( itr != null ) {
            if (  itr.hasNext()) // premature termination because val is null
                log.log(Level.FINE, () -> String.format(prematureMessageFormat,
                                          variablePath.get(itr.nextIndex()-1), variablePath));
            return currVal;
        }

        // normalize ref, i.e. ref(a.f.g) to ref<a>,ref<f>,ref<g>
        ReferenceType currType = currFrame.location().declaringType();
        continueLoop = true;
        accessorsList = new ArrayList<>();

        List<RValue> normalizedPath = this.normalize(variablePath);
        ListIterator<RValue> pathItr = normalizedPath.listIterator();
        while(continueLoop && pathItr.hasNext()) {
            RValue curRValue = pathItr.next();
            if ( currVal != null )
              currType = (ReferenceType)currVal.type();
            AccessorFactory fac = accessorFactories.get(curRValue.getClass());
            if ( fac == null ) {
                log.log(Level.SEVERE, "Unexpected RVal type: {0} of path({1})",
                        new Object[]{curRValue, variablePath});
                throw new RemoteDataAccessException("Unexpected RVal type: "+curRValue, normalizedPath);
            } // if Unknown type
            // building accessor (lambda) for this particular chain link, accessor embeds all link data
            Accessor acc = fac.createAccessor(thread, (ObjectReference) currVal, currType, curRValue);
            currVal = acc.getValue(currVal);
            accessorsList.add(acc);
            continueLoop = currVal != null;
        } // for pathItr
        if ( pathItr.hasNext() )
            log.log(Level.INFO, () -> String.format(prematureMessageFormat,
                    normalizedPath.get(pathItr.nextIndex() - 1), variablePath));
        else
            accessors.put(key, accessorsList);
        return currVal;
    } // _getValue

    protected List<RValue> normalize(List<RValue> variablePath)
    {
        List<RValue> normalizedPath = new ArrayList<>();
        normalize(variablePath, normalizedPath);
        return normalizedPath;
    } //normalize



    private void normalize(List<? extends RValue> variablePath, List<RValue> normalizedPath)
    {
        for (RValue value : variablePath) {
            if (value instanceof RefPath) {
                RefPath path = (RefPath) value;
                String varpath = path.getValue();
                boolean isFirst = true;
                for (StringTokenizer tkz = new StringTokenizer(varpath, "."); tkz.hasMoreElements(); ) {
                    String var = tkz.nextToken();
                    RefPath r = new RefPath(var);
                    if (isFirst)
                        r.setType(path.getType());
                    normalizedPath.add(r);
                } // for tokenz
            } else if (value instanceof RefChain) {
                RefChain chain = (RefChain) value;
                normalize(chain.getRef(), normalizedPath);
            } else {
                normalizedPath.add(value);
            }
        } // for variablePath
    } // normalize

    // executes remote Object.toString
    private CallSpec constructToStringCall()
    {
        CallSpec spec = remoteToString.get();
        if (spec != null)
            return spec;
        // verify magic strings locally before using in remote calls
        java.lang.reflect.Method methodValueOf;
        try {
            methodValueOf = String.class.getMethod("valueOf", new Class[]{Object.class});
        } catch (NoSuchMethodException nfex) {
            log.log(Level.SEVERE, "code error: String.valusOF(Object) not found");
            throw new RuntimeException(nfex);
        }
        CallSpec call = new CallSpec();
        call.setTargetClass(methodValueOf.getDeclaringClass().getName());
        call.setMethod(methodValueOf.getName());
        SyntheticRValue object = new SyntheticRValue();
        object.setType(Object.class.getName());
        call.getParams().add(object);
        if (remoteToString.compareAndSet(null, call))
            return call;
        return remoteToString.get();
    } // constructCall

    // remote Class.forName() call
    protected ReferenceType getClass(ThreadReference thread, String className) throws Exception
    {
        List<ReferenceType> classes = thread.virtualMachine().classesByName(className);
        if (classes.size() > 0)
            return classes.get(0);
        CallSpec loadingCall = new CallSpec(java.lang.Class.class.getName(), "forName");
        loadingCall.getParams().add(new Const(className));
        ClassObjectReference clazz = (ClassObjectReference) getValue(thread, loadingCall);
        return clazz != null ? clazz.reflectedType() : null;
    } // getClass

    /**
     *  Traverses down to execution thread frame to next class. Required in case of inner/anonymous
     *  classes with visibility to variables (local, instance and static) of enclosing class
     * @param thread
     * @param frameDepth current depth.
     * @return frame depth of previous class or -1
     * @throws Exception
     */
    private int scrollFrameToAnotherClass(ThreadReference thread, int frameDepth) throws Exception
    {
        int nextDepth = frameDepth;
        ReferenceType currentType = thread.frame(frameDepth).location().declaringType();
        ReferenceType encloserType = null;
        StackFrame frameDown = null;
        while(encloserType == null && nextDepth < thread.frameCount()) {
            frameDown = thread.frame(nextDepth);
            ReferenceType nxt = frameDown.location().declaringType();
            if ( !nxt.name().equals(currentType.name()))
                encloserType = nxt;
            else
                nextDepth++;
        }
        return encloserType != null ? nextDepth : -1;
    }

    /**
     * All class methodswhich can match specific {@link CallSpec} definition.
     * @param targetClass
     * @param call
     * @return
     * @throws Exception
     */
    protected List<Method> getMatchingMethods(ReferenceType targetClass, CallSpec call) throws Exception
    {
        String methodName = call.getMethod();
        List<Method> methods = targetClass.methodsByName(methodName);
        if (methods.size() == 0 ) {
            RemoteMethodNotFoundException mef =
                    new RemoteMethodNotFoundException("", methodName, targetClass.name());
            log.throwing(getClass().getName(), "getMatchingMethods", mef);
            throw mef;
        }

        ArrayList<Method> result = new ArrayList<>();
        List<RValue> params = call.getParams();
        for (Method mth : methods) {
            List<String> margs = mth.argumentTypeNames();
            if (margs.size() != params.size())
                continue;
            boolean match = true;
            for (int inx = 0; match && inx < margs.size(); inx++) {
                String argtype = margs.get(inx);
                RValue carg = params.get(inx);
                if (!match(argtype, carg))
                    match = false;
            } // for margs
            if (match) {
                result.add(mth);
            }
        } // for methods

        if (result.isEmpty()) {
            RemoteMethodNotFoundException mef =
                    new RemoteMethodNotFoundException("specified signature not found",
                            methodName, targetClass.name());
            log.throwing(getClass().getName(), "getMatchingMethods", mef);
            throw mef;
        }
        return result;
    } // findMatching

    /**
     * Very rough estimation used to filter out most obvious mismatches, more accurate evaluations
     * performed later
     * @param type
     * @param rValue
     * @return
     */
    private boolean match(String type, RValue rValue)
    {
        if (type.equals(rValue.getType()))
            return true;
        if (rValue instanceof Const && PrimitiveValueFactory.canConvert(type))
            return true;
        // todo: check deeper ref types, meanwhile enought
        if ( rValue instanceof Ref)
            return true;
        return false;
    } // match

    /**
     * Fine grained method verification against real parameter values. Executes primitive
     * type conversions if needed (configuration origined {@link Const} values to required type)
     *
     * @param thread
     * @param method remote method object
     * @param call call definition
     * @param args
     * @param convertedIfNeeded in case of match output for final call arguments with
     *                          performed conversions.
     * @return match or not
     * @throws Exception
     */
    private boolean match(ThreadReference thread,
                          Method method, CallSpec call,
                          List<Value> args, List<Value> convertedIfNeeded) throws Exception
    {
        if ( method.argumentTypeNames().size() == 0 )
            return true;

        List<Type> argtypes = method.argumentTypes();
        if ( argtypes.size() != args.size() ) // sanity
            return false;

        boolean[] matchedArgs = new boolean[args.size()];

        // first try direct match
        boolean directMatch = true;
        for(int inx = 0; inx < argtypes.size(); inx++) {
            Type expectedType = argtypes.get(inx);
            Value value = args.get(inx);
            Type valtype = value != null ? value.type() : null;
            // direct match
            directMatch = expectedType.equals(valtype);
            // or value is null and expected is referencetype
            directMatch |= expectedType instanceof ReferenceType && value == null;
            if ( !directMatch &&
                    value instanceof ObjectReference &&
                       expectedType instanceof ReferenceType) { // still mb arg obj extends expecetd type
                directMatch = isInstanceOf(thread, (ObjectReference) value, (ReferenceType) expectedType);
            }
            matchedArgs[inx] = directMatch;
        } // for

        // try match converted const values
        List<Value> convertingResult = new ArrayList<>(args);
        boolean convertedMatch = false;
        try {
            convertedMatch = matchConsts(thread, method, call, matchedArgs, convertingResult);
        } catch (Exception ex) {
            // actually dznt matter. it just didn't work to convert, i.e. no match
            String str = ex.toString();
        }
        boolean match = directMatch || convertedMatch;
        if ( match ) {
            convertedIfNeeded.clear();
            convertedIfNeeded.addAll(convertingResult);
        }
        return match;
    } // match

    /**
     * Performs matching of {@link Const} arguments to specific types
     * @param thread
     * @param method
     * @param call
     * @param alreadyMatched indexes of elements wich alredy matched and should be ignored
     * @param convertedArgs conversion result in case of match
     * @return
     * @throws Exception
     */
    private boolean matchConsts(ThreadReference thread,
                                Method method, CallSpec call,
                                boolean[] alreadyMatched,
                                List<Value> convertedArgs) throws Exception
    {
        List<String> argtypes = method.argumentTypeNames();
        boolean convertedMatch = true;
        List<RValue> argspecs = call.getParams();
        for(int inx = 0; convertedMatch && inx < argtypes.size(); inx++) {
            if ( alreadyMatched != null && alreadyMatched[inx] )
                continue;
            String expectedType = argtypes.get(inx);
            RValue spec = argspecs.get(inx);
            if ( spec instanceof Const ) { // check if Consts need be converted
                if ( PrimitiveValueFactory.canConvert(expectedType)) {
                    Value converted = PrimitiveValueFactory.convert(thread, expectedType, ((Const) spec).getValue());
                    convertedArgs.set(inx, converted);
                    continue;
                }
                else // it should, but we can't
                    convertedMatch = false;
            }
            else // if no validations required, just converting consts
                convertedMatch = alreadyMatched == null;
        } // match with consts convertion
        return convertedMatch;
    } // matchConsts


    /**
     * Performs remote {@link Class#isInstance(Object)} calls
     * @param thread
     * @param ref
     * @param type
     * @return
     * @throws Exception
     */
    protected boolean isInstanceOf(ThreadReference thread, ObjectReference ref, ReferenceType type) throws Exception
    {
        if ( ref == null )
            return false;

        if ( type.name().equals(Object.class.getName()))
            return true;

        String key = type.name() + "//" + ref.referenceType().name();
        Boolean known = instanceofCache.get(key);
        if ( known != null )
            return known.booleanValue();

        ClassObjectReference clazz = type.classObject();
        SyntheticRValue clazzVal = new SyntheticRValue(clazz);
        CallSpec call = new CallSpec(null, "isInstance");
        call.getParams().add(new SyntheticRValue(ref));
        RefChain request = new RefChain(Arrays.asList(clazzVal, call));
        BooleanValue boolvalue = (BooleanValue) getValue(thread, request);
        boolean res = boolvalue.booleanValue();
        instanceofCache.put(key, Boolean.valueOf(res));
        return res;
    } // isInstanceof

    //=========================
    // aux types
    //=========================

    /**
     * Evaluates (retrieves) value basing on it's owner object.
     */
    @FunctionalInterface
    interface Accessor
    {
        Value getValue(Value owner) throws Exception;
    }

    /**
     * Creates case specific accessor with all required data
     * to evaluate particular rValue element
     */
    @FunctionalInterface
    interface AccessorFactory
    {
        Accessor createAccessor(ThreadReference thread,
                                ObjectReference targetObject, ReferenceType targetType,
                                RValue valueSpec)  throws Exception;
    }

    //--------------------
    // accessor factories
    //--------------------
    private Accessor createAccessConstValue(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        Const aConst = (Const) valueSpec;
        String constType = aConst.getType() != null ?
                aConst.getType() : String.class.getName();
        Value value = PrimitiveValueFactory.convert(thread, constType, aConst.getValue());
        Accessor acc = (v)->value;
        return acc;
    } // createAccessConstValue

    private Accessor createAccessRefPathValue(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        return createAccessRefPathValue(thread, 0, targetObject, targetType, valueSpec);
    }

    private Accessor createAccessRefPathValue(ThreadReference thread, int frameDepth, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        RefPath path = (RefPath) valueSpec;
        if ("this".equals(path.getValue()))
            return (v)-> accessThisValue(thread);

        StackFrame frame = thread.frame(frameDepth);
        LocalVariable local = frame.visibleVariableByName(path.getValue());;
        if ( local != null ) {
            Method method = frame.location().method();
            return (v) -> accessLocalValue(thread, frameDepth, method, local);
        }

        ReferenceType tgtType = path.getType() != null?
                  getClass(thread, path.getType()) :
                    targetType != null ? targetType :
                      targetObject != null ? (ReferenceType)targetObject.type() :
                        frame.location().declaringType();

        Field field = tgtType.fieldByName(path.getValue());
        // if it's not an inner class, that's the end.
        if (field == null && !tgtType.name().contains("$")) {
            log.log(Level.SEVERE, "Can't find field " + path.getValue() + " on type " + tgtType);
            throw new RemoteFieldNotFoundException("", path.getValue(), tgtType.name());
        }
        // if nothing is found it could be a configuration mistake or reference to element defined out of
        // current frame (enclosing class data referenced by inner class)
        if (field == null ) {
            int nextDepth = scrollFrameToAnotherClass(thread, frameDepth);
            StackFrame nextFrame = thread.frame(nextDepth);
            ObjectReference enclosingObject = nextFrame.thisObject();
            ReferenceType encloserType = nextFrame.location().declaringType();
            Accessor steppedAcc = createAccessRefPathValue(thread, nextDepth, enclosingObject, encloserType, path);
            return  steppedAcc;
        }

        Accessor acc;
        if ( field.isStatic() )
            acc = (v)->accessStaticValue(tgtType, field);
        else {
            Method method = thread.frame(frameDepth).location().method();
            acc = (v) -> accessInstanceValue(thread, v, frameDepth, method, field);
        }
        return acc;
    } // createAccessRefPathValue

    private Accessor createMethodValueAcessor(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        return createMethodValueAcessor(thread, 0, targetObject, targetType, valueSpec);
    }

    /**
     * Accessor to particular method evaluation result. Includes frame depth as it may contain calls ot encloser
     * methods performed from inner class
     */
    private Accessor createMethodValueAcessor(ThreadReference thread, int frameDepth, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        CallSpec call = (CallSpec) valueSpec;
        ReferenceType targetClass = targetType;
        if (call.getTargetClass() != null) {
            targetClass = getClass(thread, call.getTargetClass());
        }
        targetClass = targetClass != null ?
                          targetClass : targetObject != null ?
                                           targetObject.referenceType() :
                                               thread.frame(frameDepth).location().declaringType();

        List<Method> possiblyMatching = null;
        int nextDepth = frameDepth;
        ReferenceType nextTargetClass = targetClass;
        while (possiblyMatching == null) {
            try {
                possiblyMatching = getMatchingMethods(nextTargetClass, call);
            } catch (RemoteMethodNotFoundException rnf ) {
                // nothing found, a config mistake or case or inner/encloser classes
                nextDepth = scrollFrameToAnotherClass(thread, nextDepth);
                nextTargetClass = nextDepth != -1 ?
                                  thread.frame(nextDepth).location().declaringType() : null;
                if ( nextTargetClass != null )
                   return createMethodValueAcessor(thread, nextDepth, null, nextTargetClass, valueSpec);
                else
                    throw rnf;
            }
        } // while

        List<Method> possiblyMatchingFin = possiblyMatching;
        int nextDepthFin = nextDepth;
//        Location finlocation = thread.frame(nextDepthFin).location();
        Method method = thread.frame(nextDepthFin).location().method();
        AtomicReference<Method> finalMatch = new AtomicReference<>();
        Accessor acc = (v)->accessMethodValue(thread, nextDepthFin, method, v, finalMatch, possiblyMatchingFin, call);
        return acc;
    } // createMethodValueAcessor

    private Accessor createAccessException(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec)  throws Exception
    {
        Accessor acc = (v)->accessException(thread);
        return acc;
    }

    private Accessor createAccessSynthetic(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec) throws Exception
    {
        SyntheticRValue syntheticRValue = (SyntheticRValue)valueSpec;
        return (v)->accessSyntheticValue(syntheticRValue);
    }

    private Accessor createAccessArray(ThreadReference thread, ObjectReference targetObject, ReferenceType targetType, RValue valueSpec)  throws Exception
    {
        ArrayIndex inx = (ArrayIndex) valueSpec;
        Accessor acc = (v)->accessArray(thread, v, inx);
        return acc;
    }

    //----------------
    // valueAccessors
    //----------------
    Value accessThisValue(ThreadReference thread) throws IncompatibleThreadStateException
    {
        StackFrame currFrame = thread.frame(0);
        Value currVal = currFrame.thisObject();
        return currVal;
    }

    /**
     *  frameDepth is not enought as when it called from extender method depth value will be bigger.
     *  On another hand, just fieldLocation is enought but scrolling from depth 0 is non-necessary
     *  operations we could save. So we start with minimal depth
     */
    Value accessLocalValue(ThreadReference thread, int frameDepth, Method method, LocalVariable local) throws IncompatibleThreadStateException
    {
        int currDepth = frameDepth;
        StackFrame currFrame;
        while(!(currFrame=thread.frame(currDepth)).location().method().equals(method))
            currDepth++;
        Value currVal = currFrame.getValue(local);
        return currVal;
    }

    /**
     *  frameDepth is not enough as when it called from extender method depth value will be bigger.
     *  On another hand, just fieldLocation is enought but scrolling from depth 0 is non-necessary
     *  operations we could save. So we start with minimal depth
     */
    Value accessInstanceValue(ThreadReference thread, Value owner, int frameDepth, Method method, Field field) throws IncompatibleThreadStateException
    {
        int currentDepth = frameDepth;
        StackFrame currFrame;
        while(!(currFrame=thread.frame(frameDepth)).location().method().equals(method))
            frameDepth++;

        ObjectReference ref = currentDepth > 0 ?
                                 currFrame.thisObject() :
                                    owner != null ? (ObjectReference) owner :
                                         currFrame.thisObject();
        Value val = ref.getValue(field);
        return val;
    }

    Value accessStaticValue(ReferenceType type, Field field) throws IncompatibleThreadStateException
    {
        Value val = type.getValue(field);
        return val;
    }


    /**
     * Performes final remote method invocation.
     * @param method see description for
     *                     {@link #accessInstanceValue(ThreadReference, Value, int, Method, Field)
     *                     accessInstanceValue} : frameDepth may be not enough,  location is enough,
     *                     but frameDepth can save some time if used as a start point
     * @param possibleMatch all overloaded methods with possibly matching signatures
     *                      During 1st execution the exactly matching method is selected
     *                      and stored in match parameter
     * @param match output for finally exact matching method
     */
    Value accessMethodValue(ThreadReference thread,
                            int frameDepth,
                            Method method,
                            Value owner,
                            AtomicReference<Method> match,
                            List<Method> possibleMatch,
                            CallSpec call) throws Exception
    {
        List<Value> args = new ArrayList<>(call.getParams().size());
        for(int inx = 0; inx < call.getParams().size(); inx++) {
            RValue param = call.getParams().get(inx);
            Value val = getValue(thread, param);
            args.add(val);
        } // for args

        // should we convert rvalue consts to real values?
        // shouldn't if it was performed during
        // !match(thread, m, call, args, converted)
        boolean matchConstsExplicitly = true;
        ArrayList<Value> convertedIfNeeded = new ArrayList<>(args);
        Method targetMethod = match.get();
        if ( targetMethod == null ) {
            for(Method m: possibleMatch) {
                if ( !match(thread, m, call, args, convertedIfNeeded) )
                    continue;
                args.clear();
                args.addAll(convertedIfNeeded);
                match.set(m);
                targetMethod = m;
                matchConstsExplicitly = false;
                break;
            }
        }

        if ( matchConstsExplicitly )
            matchConsts(thread, targetMethod, call, null, convertedIfNeeded);

        if ( targetMethod == null )
            throw new RemoteMethodNotFoundException("No matching signatures found",
                                                     possibleMatch.get(0).name(),
                                                     possibleMatch.get(0).declaringType().name());

        Value result;
        ReferenceType ownerType = targetMethod.declaringType();
        if ( targetMethod.isStatic() && targetMethod.isDefault()) {
            InterfaceType intf = (InterfaceType) ownerType;
            result = intf.invokeMethod(thread, targetMethod, convertedIfNeeded, ClassType.INVOKE_SINGLE_THREADED);
        }
        else if ( targetMethod.isConstructor() ) {
            ClassType clazz = (ClassType) ownerType;
            result = clazz.newInstance(thread, targetMethod, convertedIfNeeded, ClassType.INVOKE_SINGLE_THREADED);
        }
        else if ( targetMethod.isStatic()) {
            ClassType clazz = (ClassType) ownerType;
            result = clazz.invokeMethod(thread, targetMethod, convertedIfNeeded, ClassType.INVOKE_SINGLE_THREADED);
        } else {
            int currDepth = frameDepth;
            StackFrame currFrame;
            while(!(currFrame=thread.frame(currDepth)).location().method().equals(method))
                currDepth++;

            Value realOwner = currDepth == 0 ?
                                 (ObjectReference) owner :
                                      currFrame.thisObject();
            ObjectReference ref = (ObjectReference) realOwner;
            result = ref.invokeMethod(thread, targetMethod, convertedIfNeeded, ClassType.INVOKE_SINGLE_THREADED);
        }
        return result;
    } // accessMethodValue

    Value accessSyntheticValue(SyntheticRValue value)
    {
        return value.getValue();
    }

    Value accessArray(ThreadReference thread, Value owner, ArrayIndex index) throws Exception
    {
        ArrayReference array = (ArrayReference) owner;
        int indexVal;
        if ( index.getDynamicIndex() != null ) {
            RValue dynamicIndex = index.getDynamicIndex();
            Value dynamicIndexValue = getValue(thread, dynamicIndex);
            Object obj  = PrimitiveValueFactory.convert(dynamicIndexValue);
            Integer converted = (Integer) obj;
            indexVal = converted.intValue();
        }
        else
            indexVal = index.getFixedIndex();

        return array.getValue(indexVal);
    }

    Value accessException(ThreadReference thread)
    {
        ExecutionContext ctx = ExecutionContext.getContext(thread);
        Value exception = (Value) ctx.getEventSpecificValue(CurrentException.class);
        return exception;
    }

    // ---------
    // cache K
    // ---------
    private class CacheKey
    {
        final long threadId;
        final String point;
        final String path;

        public CacheKey(ThreadReference thread, StackFrame frame, List<RValue> variablesPath) throws Exception
        {
            this.threadId = thread.uniqueID();
            Location location = frame.location();
            this.point = location.sourcePath() + "-" + location.lineNumber();
            StringBuilder sb = new StringBuilder();
            for(RValue val: variablesPath ) {
                String id = val.uniqueID();
                sb.append('-').append(id);
            }
            this.path = sb.toString();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey key = (CacheKey) o;

            if (threadId != key.threadId) return false;
            if (point != null ? !point.equals(key.point) : key.point != null) return false;
            return !(path != null ? !path.equals(key.path) : key.path != null);
        }

        @Override
        public int hashCode()
        {
            int result = (int) (threadId ^ (threadId >>> 32));
            result = 31 * result + (point != null ? point.hashCode() : 0);
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }
    } // class CacheKey
}