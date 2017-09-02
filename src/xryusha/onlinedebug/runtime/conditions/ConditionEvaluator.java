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

package xryusha.onlinedebug.runtime.conditions;


import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.sun.jdi.*;
import xryusha.onlinedebug.config.conditions.*;
import xryusha.onlinedebug.config.values.CallSpec;
import xryusha.onlinedebug.config.values.RValue;
import xryusha.onlinedebug.exceptions.EvaluationException;
import xryusha.onlinedebug.runtime.PrimitiveValueFactory;
import xryusha.onlinedebug.runtime.RemotingBase;
import xryusha.onlinedebug.runtime.SyntheticRValue;


/**
 * Evaluates condition statement
 */
public class ConditionEvaluator extends RemotingBase
{
    private  Map<Class<? extends AbstractConditionSpec>, BiPredicate<ThreadReference, AbstractConditionSpec>> evaluators;
    private  List<RValue> comparableCompareChain;




    public ConditionEvaluator()
    {
        evaluators = new IdentityHashMap<>();
        evaluators.put(AndGroupSpec.class, this::evaluateAnd);
        evaluators.put(OrGroupSpec.class, this::evaluateOr);
        evaluators.put(EqualsConditionSpec.class, this::evaluateEquals);
        evaluators.put(LessConditionSpec.class, this::evaluateLess);
        evaluators.put(IsNullConditionSpec.class, this::evaluateIsNull);
        evaluators.put(AtLocationConditionSpec.class, this::evaluateLocation);
        comparableCompareChain = constructCompareCall();
    }

    /**
     * Evaluates condition
     * @param thread
     * @param condition
     * @return evaluation result
     * @throws Exception
     */
    public boolean evaluate(ThreadReference thread, AbstractConditionSpec condition) throws Exception
    {
        try {
            boolean res = _evaluate(thread, condition);
            return res;
        } catch (RuntimeException rex) {
            Exception reported = rex.getCause() != null ? (Exception) rex.getCause() : rex;
            log.log(Level.SEVERE, "evaluation fail of " + condition, reported);
            log.throwing(getClass().getName(), "evaluate", reported);
            throw rex;
        }
    } // evaluate

    // yep, don't have to declare RuntimeException, just to emphasize it may happen here
    protected boolean _evaluate(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        BiPredicate<ThreadReference, AbstractConditionSpec> evaluator = evaluators.get(condition.getClass());
        if ( evaluator == null ) {
            String msg = "Unknown condition type: " + condition;
            log.severe(msg);
            EvaluationException ex = new EvaluationException(msg);
            log.throwing(getClass().getName(), "_evaluate", ex);
            throw new RuntimeException(ex);
        }
        boolean res = evaluator.test(thread, condition);
        if ( condition.isInverse())
            res = !res;

        return res;
    } // _evaluate

    protected boolean evaluateAnd(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        boolean res = evaluateGroup(thread, (GroupConditionSpec) condition, (a, b)->a&&b, (a)->!a);
        return res;
    } // evaluateAnd

    protected boolean evaluateOr(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        boolean res = evaluateGroup(thread, (GroupConditionSpec) condition, (a,b)->a||b, (a)->true);
        return res;
    } // evaluateOr

    protected boolean evaluateEquals(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        ComparationResult res = evaluateRelation(thread, (RelationConditionSpec) condition);
        if ( res.resultType == ComparationResult.ResultType.BothAreNull)
            return true;
        if ( res.resultType == ComparationResult.ResultType.LeftIsNull ||
                 res.resultType == ComparationResult.ResultType.RightIsNull )
            return false;
        if ( res.resultType == ComparationResult.ResultType.Compared )
            return res.result == 0;
        return false;
    }

    protected boolean evaluateLess(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        ComparationResult res = evaluateRelation(thread, (RelationConditionSpec) condition);
        if ( res.resultType == ComparationResult.ResultType.BothAreNull)
            return false;

        String nullSide = null;
        if ( res.resultType == ComparationResult.ResultType.LeftIsNull ) {
            nullSide = "left";
        } else if (  res.resultType == ComparationResult.ResultType.RightIsNull ) {
            nullSide = "right";
        }
        if ( nullSide != null ) {
            EvaluationException evax = new EvaluationException("can't evaluate relation as "
                                                               + nullSide + " side is null");
            log.throwing(getClass().getName(), "evaluateLess", evax);
            throw new RuntimeException(evax);
        }

        return res.result < 0;
    } // evaluateLess

    protected boolean evaluateIsNull(ThreadReference thread, AbstractConditionSpec condition) throws RuntimeException
    {
        IsNullConditionSpec isnull = (IsNullConditionSpec) condition;
        RValue rval = isnull.getValue();
        if ( rval == null )
            return true;
        try {
            Value val = getValue(thread, rval);
            return val == null;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Evaluation failed for " + condition, ex);
            throw new RuntimeException(ex);
        }
    } // evaluateIsNull

    protected boolean evaluateGroup(ThreadReference thread,
                                    GroupConditionSpec group,
                                    BiPredicate<Boolean, Boolean> op,
                                    Predicate<Boolean> loopingTerminationCondition) throws RuntimeException
    {
        if ( group.getConditions() == null )
            return true;
        Boolean match = null;
        boolean terminate = false;
        for(Iterator<AbstractConditionSpec> itr = group.getConditions().iterator(); !terminate && itr.hasNext(); ) {
            AbstractConditionSpec cond = itr.next();
            boolean e = _evaluate(thread, cond);
            if ( match == null )
                match = Boolean.valueOf(e);
            match = op.test(match.booleanValue(), e);
            terminate = loopingTerminationCondition.test(match);
        }
        return match;
    }

    /**
     * Evaluate relation
     * @param thread
     * @param relation (less/equal)
     * @return
     * @throws RuntimeException
     */
    protected ComparationResult evaluateRelation(ThreadReference thread, RelationConditionSpec relation) throws RuntimeException
    {
        Value lval, rval;
        try {
            lval = relation.getLeft() != null ? getValue(thread, relation.getLeft()) : null;
            rval = relation.getRight() != null ? getValue(thread, relation.getRight()) : null;
            if ( lval == null && rval == null  ) // both nulls
                return new ComparationResult(ComparationResult.ResultType.BothAreNull, 0);
            if ( lval==null )
                return new ComparationResult(ComparationResult.ResultType.LeftIsNull, 0);
            if ( rval==null )
                return new ComparationResult(ComparationResult.ResultType.RightIsNull, 0);

            Type leftype = lval.type(), rightype = rval.type();
            if ( PrimitiveValueFactory.canConvert(leftype) ) {
                Object lefobj = PrimitiveValueFactory.convert(lval);
                Object robj = PrimitiveValueFactory.convert(rval);
                int res = ((Comparable)lefobj).compareTo(robj);
                return new ComparationResult(ComparationResult.ResultType.Compared, res);
            } // if primitives

            // if it's not a primitive, execute on remote jvm
            ObjectReference leftRef = (ObjectReference) lval;
            ObjectReference rightRef = (ObjectReference) rval;
            SyntheticRValue thisVal = (SyntheticRValue) comparableCompareChain.get(0);
            thisVal.setValue(leftRef);
            CallSpec call = (CallSpec) comparableCompareChain.get(1);
            SyntheticRValue that = (SyntheticRValue)call.getParams().get(0);
            that.setValue(rightRef);
            Value compareResult = getValue(thread, comparableCompareChain);
            int res = ((IntegerValue)compareResult).value();
            return new ComparationResult(ComparationResult.ResultType.Compared, res);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Evaluation failed for " + relation, ex);
            log.throwing(getClass().getName(), "evaluateRelation", ex);
            throw new RuntimeException(ex);
        }
    } // evaluateEquals


    private boolean evaluateLocation(ThreadReference threadReference, AbstractConditionSpec locationSpec)
    {
        AtLocationConditionSpec location = (AtLocationConditionSpec)locationSpec;
        String targetClass = location.getInClass();
        String method = location.getMethod();
        Integer line = location.getLine();

        try {
            Location currLocation = threadReference.frame(0).location();
            ReferenceType clazz = currLocation.declaringType();
            String name = clazz.name();
            boolean match =
                    targetClass.endsWith("*") ?
                             name.startsWith(targetClass.substring(0,targetClass.length()-1)) :
                                 name.equals(targetClass);
            if ( !match ) {
                log.fine(()->"location spec name of" + location + " did not match " + currLocation);
                return false;
            }

            if ( method != null ) {
                match = currLocation.method().name().equals(method);
                if (!match) {
                    log.fine(()->"location spec method of " + location + " did not match " + currLocation);
                    return false;
                }
            } // if method
            if ( line != null ) {
                match = line.intValue() == currLocation.lineNumber();
                if (!match) {
                    log.fine(()->"location spec line of " + location + " did not match " + currLocation);
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Evaluation failed for " + location, ex);
            log.throwing(getClass().getName(), "evaluateLocation", ex);
            throw new RuntimeException(ex);
        }
    } // evaluateLocation


    /**
     * Constructs a.compareTo(b) call template
     * @return
     */
    private List<RValue> constructCompareCall()
    {
        SyntheticRValue thisVal = new SyntheticRValue();
        List<RValue> chain = new ArrayList<>(2);
        chain.add(thisVal);
        // verify magic strings
        java.lang.reflect.Method compareTo = null;
        try {
            compareTo = Comparable.class.getMethod("compareTo", new Class[]{Object.class});
        } catch (Exception ex) {
            String msg = "code error: Comparable.compare method not found";
            log.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }

        CallSpec compareCall = new CallSpec();
        compareCall.setMethod(compareTo.getName());
        compareCall.setTargetClass(compareTo.getDeclaringClass().getName());

        SyntheticRValue that = new SyntheticRValue();
        that.setType(Object.class.getName());
        compareCall.getParams().add(that);
        chain.add(compareCall);
        return chain;
    } // constructCompareCall

    protected static class ComparationResult
    {
        public final ResultType resultType;
        public final int result;
        public enum ResultType { Compared, NotComparable, LeftIsNull, RightIsNull, BothAreNull };

        public ComparationResult(ResultType resultType, int result)
        {
            this.resultType = resultType;
            this.result = result;
        }
    } // ComparationResult
} // ConditionEvaluator