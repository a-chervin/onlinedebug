#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <jvmti.h>
#include "StackMiner.h"
#include "StackMiner_private.h"

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


#ifdef __cplusplus
extern "C" {
#endif

#define MAX_LOCAL_FRAME_DEPTH 10
#define UNDEFINED 0x1FFFFFFF

static jmethodID g_logObject = NULL;    

JNIEXPORT jboolean JNICALL Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_init
  (JNIEnv *env, jclass clazz)
{
   JavaVM *jvm = NULL;
   jvmtiError error;
   jclass hashMapClass = NULL;
   jvmtiCapabilities capabilities;
   
           
   g_declaringClass = clazz;
   g_log = (*env)->GetStaticMethodID(env, g_declaringClass, "log", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
   if ( g_log == NULL ) 
       return JNI_FALSE;

   g_logObject = (*env)->GetStaticMethodID(env, g_declaringClass, "log", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
   if ( g_log == NULL ) 
       return JNI_FALSE;
   
   error = (*env)->GetJavaVM(env, &jvm);
    if ( error != JNI_OK || jvm == NULL ) {
        logError(env, "GetJavaVM failed", error);
        return JNI_FALSE;
    }
    
    error = (*jvm)->GetEnv(jvm, (void**)&g_jvmti, JVMTI_VERSION_1_0);
    if ( error != JNI_OK || g_jvmti == NULL ) {
        logError(env, "Unable to access JVMTI", error);
        return JNI_FALSE;
    }

    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_access_local_variables = 1;
    error = (*g_jvmti)-> AddCapabilities(g_jvmti, &capabilities);
    if ( error != JVMTI_ERROR_NONE ) {
      logError(env, "Unable to set can_access_local_variables capability", error);
      return JNI_FALSE;        
    }
    
    capabilities.can_force_early_return = 1;
    error = (*g_jvmti)-> AddCapabilities(g_jvmti, &capabilities);
    if ( error != JVMTI_ERROR_NONE ) {
      logError(env, "Unable to set can_force_early_return, forced return will be diabled", error);
    }

    if ( initCollectionAPI(env) != JNI_OK )
        return JNI_FALSE;
    
    if ( initConvertes(env) != JNI_OK )
        return JNI_FALSE;

    return JNI_TRUE;
}  /* Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_init */


JNIEXPORT jobject JNICALL Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_extract
  (JNIEnv *env, jclass clazz, jobject list)
{
   jint size;
   jvmtiError error;
   jobject hashMap = NULL; 
   jthread currentThread;
   jint framesDepth;
   jvmtiFrameInfo frames[MAX_LOCAL_FRAME_DEPTH];
   cachedLocalVarsEntry cachedLocals[MAX_LOCAL_FRAME_DEPTH];
   
   
   memset(frames, 0, sizeof(frames));
   memset(cachedLocals, 0, sizeof(cachedLocals));

    if ( initRequest(env, &currentThread, frames, &framesDepth) != JVMTI_ERROR_NONE )
        return NULL;
   
    hashMap = (*env)->NewObject(env, g_linkedHashMapClass, g_linkedHashMapCtor);
    if ( (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return NULL;
    }
    
    jint startIndex, endIndex;    
    if ( checkBounds(env, frames[1], &list, &startIndex, &endIndex) != JVMTI_ERROR_NONE )
        return NULL;
    
    if ( startIndex == 0 && endIndex == 0 )
        return hashMap;
    
    
    for (int ii = startIndex; ii < endIndex; ii++) {
        int index = ii;
        valueContext result;
        jstring name = NULL;       
        
        memset(&result, 0, sizeof(result));
        /* name could be already obrained in case of size == 1  */
        if ( list != NULL  ) { 
            name = (jstring) (*env)->CallObjectMethod(env, list, g_listGet, ii);            
            if ( (*env)->ExceptionCheck(env)) {
              (*env)->ExceptionDescribe(env);
              return NULL;
            }
        }
        else
            result.slot = index;
        
        error = getValue(env, name, currentThread, frames, cachedLocals, 
                         1, /* depth-0 is this native method */
                         framesDepth, &result);
        if ( error != JVMTI_ERROR_NONE )
            return NULL;
        if ( result.status == IGNORE )
            continue;
         /* if jni's fine should it fail or continue? */
        if ( result.status == FAIL )
            continue;
        
        if ( endIndex == UNDEFINED ) {
            endIndex = cachedLocals[1].localsCount;
        }
        
        jstring globalName = name != NULL ?
                               (*env)->NewGlobalRef(env, name) :
                                  (*env)->NewStringUTF(env, result.name);
        
        (*env)->CallObjectMethod(env, hashMap, g_mapPut, globalName, result.value);
        if ( name != NULL )
           (*env)->DeleteLocalRef(env, name);
    } /* for startIndex -> endIndex */
    for(int inx = 0; inx < MAX_LOCAL_FRAME_DEPTH && cachedLocals[inx].locals != NULL; inx++ )
        (*g_jvmti)->Deallocate(g_jvmti,  (void*)cachedLocals[inx].locals);

/* 
    for(int inx = 0; inx < framesDepth ; inx++ )
        (*g_jvmti)->Deallocate(g_jvmti,  (void*)&frames[inx]);
 */    

    return hashMap;
 } /* Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_extract */


JNIEXPORT jboolean JNICALL Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_setValue
  (JNIEnv* env, jclass clazz, jstring name, jobject value)
{
   jvmtiError error;
   jthread currentThread;
   jint framesDepth;
   jvmtiFrameInfo frames[MAX_LOCAL_FRAME_DEPTH];
   cachedLocalVarsEntry cachedLocals[MAX_LOCAL_FRAME_DEPTH];
   valueContext context;

   memset(frames, 0, sizeof(frames));
   memset(cachedLocals, 0, sizeof(cachedLocals));
   
   error = initRequest(env, &currentThread, frames, &framesDepth);
    if ( error != JVMTI_ERROR_NONE )
       return JNI_FALSE;

   error = setValue(env, name, value, currentThread, frames, cachedLocals, 1, framesDepth, &context);
   return error == JVMTI_ERROR_NONE ? JNI_OK : JNI_FALSE;
} /* Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_setValue */


JNIEXPORT jboolean JNICALL Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_enforceReturn
  (JNIEnv* env, jclass clazz, jobject value)
{
   jvmtiError error;
   jthread currentThread;
   jint framesDepth;
   jvmtiFrameInfo frames[MAX_LOCAL_FRAME_DEPTH];
   cachedLocalVarsEntry cachedLocals[MAX_LOCAL_FRAME_DEPTH];
   valueContext context;

   memset(frames, 0, sizeof(frames));
   memset(cachedLocals, 0, sizeof(cachedLocals));
   
   error = initRequest(env, &currentThread, frames, &framesDepth);
    if ( error != JVMTI_ERROR_NONE )
       return JNI_FALSE;

   error = enforceReturn(env, value, currentThread, frames);
   return error == JVMTI_ERROR_NONE ? JNI_OK : JNI_FALSE;    
} /* Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_enforceReturn */



jvmtiError enforceReturn(JNIEnv* env, jobject value, jthread currentThread, jvmtiFrameInfo* frames)
{
    jvmtiError error;
    char* methodName;
    char* signature;
    char* genericSignature;
    int index;
    char sign;
    error = (*g_jvmti)->GetMethodName(g_jvmti, frames[0].method, 
                                      &methodName, &signature, &genericSignature);
//    index = strchr(signature, ')');
    sign =  *(strchr(signature, ')') + 1); //signature[index+1];
    error = applyChange(env, currentThread, SETTER,  sign, methodName, -1, -1, value);

    (*g_jvmti)->Deallocate(g_jvmti, methodName);
    (*g_jvmti)->Deallocate(g_jvmti, signature);
    (*g_jvmti)->Deallocate(g_jvmti, genericSignature);

    return error;    
} /* returnValue */

jvmtiError setValue(JNIEnv* env, jstring name, jobject value, jthread currentThread, 
                    jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[], 
                    int currentDepth, int maxDepth, valueContext* result)
{
    jvmtiError error;
    jvmtiLocalVariableEntry* locals;
          
    error = findInLocalContext(env, name, currentThread, 
                               frames, cachedLocals,  
                               currentDepth, maxDepth,  result);
    
    if ( error != JVMTI_ERROR_NONE)
        return error;
            
    if ( result->status == IGNORE )
        return JVMTI_ERROR_NONE;

    locals = cachedLocals[currentDepth].locals;
    char sign = *(locals[result->slot].signature);
    error = applyChange(env, currentThread, SETTER,  sign, locals[result->slot].name, currentDepth, locals[result->slot].slot, value);
    return error;
} /* setValue */

jvmtiError applyChange(JNIEnv* env, jthread currentThread, changeType usage, 
                       char sign, char* entryName, 
                       int currentDepth, int slot, jobject value)
{
    jvmtiError error;
//    jvmtiLocalVariableEntry* locals;
    char applierName[50] = {0};
          
    switch( sign ) {
        case 'B' : 
        case 'C':
        case 'I':
        case 'S':                
        case 'Z': {
          jint intvalue = sign == 'B' ? (*env)->CallByteMethod(env, value, g_objectToByteValue) :
                       sign == 'C' ? (*env)->CallShortMethod(env, value, g_objectToShortValue) :
                       sign == 'I' ? (*env)->CallIntMethod(env, value, g_objectToIntValue) :
                       sign == 'S' ? (*env)->CallShortMethod(env, value, g_objectToShortValue) :
                                   (*env)->CallBooleanMethod(env, g_ShortClass, g_objectToBooleanValue);
          intApplier applier = getIntApplier(g_jvmti, usage, applierName);
          error = applier(g_jvmti, currentThread, currentDepth,  slot, intvalue) ;
          break;
        } /* ints */
        case 'J' : {
           jlong longvalue =(*env)->CallLongMethod(env, value, g_objectToLongValue);
           longApplier applier = getLongApplier(g_jvmti, usage, applierName);
           error = applier(g_jvmti, currentThread, currentDepth,  slot, longvalue) ;
           break;
        } /* long */
        case 'D' : {
           jdouble doublevalue = (*env)->CallDoubleMethod(env, value, g_objectToDoubleValue);
           doubleApplier applier = getDoubleApplier(g_jvmti, usage, applierName);
           error = applier(g_jvmti, currentThread, currentDepth,  slot, doublevalue) ;
           break;
        } /* double */
        case 'F' : {
           jfloat floatvalue  = (*env)->CallFloatMethod(env, value, g_objectToFloatValue);
           floatApplier applier = getFloatApplier(g_jvmti, usage, applierName);
           error = applier(g_jvmti, currentThread, currentDepth,  slot, floatvalue) ;
           break;
        } /* double */
        case 'L' : 
        case '[' : 
        {
           objectApplier applier = getObjectApplier(g_jvmti, usage, applierName);
           error = applier(g_jvmti, currentThread, currentDepth,  slot, value) ;
           break;
        }
        default: {
            char* msg = "Unexpected field signature:  ";
            logAccessError(env, "Unexpected field signature", 0, entryName);                              
            return JVMTI_ERROR_TYPE_MISMATCH;
        }
    } /* switch signature */
    
    if ( (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }

    if ( error != JVMTI_ERROR_NONE) {
       logAccessError(env, "setter", error, entryName);                              
    }   
    
    return error;
} /* setValue */


jvmtiError initRequest(JNIEnv *env, jthread* currentThread, jvmtiFrameInfo* frames, jint* framesDepth)
{
    jvmtiError error;
    
    error = (*g_jvmti)->GetCurrentThread(g_jvmti, currentThread);
    if ( error != JVMTI_ERROR_NONE) {
        logError(env, "GetCurrentThread error", error);
        return error;
    }

    error = (*g_jvmti)->GetStackTrace(g_jvmti, *currentThread, 
                                      0, MAX_LOCAL_FRAME_DEPTH, 
                                      frames, framesDepth);
    if (error != JVMTI_ERROR_NONE) {
        logError(env, "GetStackTrace error", error);
    }
    
    return error;
} /* initRequest */

jvmtiError checkBounds(JNIEnv *env, jvmtiFrameInfo frame, jobject* list, jint* startIndex, jint* endIndex) 
{
    jvmtiError error = JVMTI_ERROR_NONE;
    int size;

    size = *list != NULL ? (*env)->CallIntMethod(env, *list, g_listSize) : UNDEFINED;
    if ( (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JVMTI_ERROR_INTERNAL;
    }
    else if (size == 0) {
        *startIndex = 0;
        *endIndex = 0;
        return JVMTI_ERROR_NONE;
    }
    else if (size == UNDEFINED) {
        *startIndex = 0;
        *endIndex = UNDEFINED;
        return JVMTI_ERROR_NONE;
    }
    if (size > 1) {
        *startIndex = 0;
        *endIndex = size;
        return JVMTI_ERROR_NONE;
    }

    jstring name = (jstring) (*env)->CallObjectMethod(env, *list, g_listGet, 0);
    if ( (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JVMTI_ERROR_INTERNAL;
    }
    
    const char* singleNameStr = (*env)->GetStringUTFChars(env, name, 0);
    if (!strcmp(ALL_ARGS, singleNameStr)) {
        error = (*g_jvmti)->GetArgumentsSize(g_jvmti, frame.method, endIndex);
        *startIndex = 1;
        *list = NULL;
    } /* if "all:args" */
    else if (!strcmp(ALL_LOCAL_VARS, singleNameStr)) {
        error = (*g_jvmti)->GetArgumentsSize(g_jvmti, frame.method, startIndex);
        *endIndex = UNDEFINED;
        *list = NULL;
    } /* if all locals */
    else {
        *startIndex = 0;
        *endIndex = size; /* i.e. 1 */
    }

    (*env)->DeleteLocalRef(env, singleNameStr);

    if (error != JVMTI_ERROR_NONE) {
        logError(env, "GetArgumentsSize failed", error);
    }
    return error;
} /* checkBounds */

jvmtiError getValue(JNIEnv* env, jstring name, jthread currentThread, 
                    jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[], 
                    int currentDepth, int maxDepth, valueContext* result)
{
    jvmtiError error;
    jvmtiLocalVariableEntry* locals;
    jobject retrieved;
    jobject globalizedRetrieved;
          
    error = findInLocalContext(env, name, currentThread, 
                               frames, cachedLocals,  
                               currentDepth, maxDepth,  result);
    
    locals = cachedLocals[currentDepth].locals;
    
    if ( error != JVMTI_ERROR_NONE)
        return error;
            
    if ( result->status == IGNORE )
        return JVMTI_ERROR_NONE;
    
    globalizedRetrieved = NULL;
    retrieved  = asJObject(env, currentThread, currentDepth, locals, result->slot);
    if (retrieved != NULL) {
        globalizedRetrieved = (*env)->NewGlobalRef(env, retrieved);
        (*env)->DeleteLocalRef(env, retrieved);
    }
    result->value = globalizedRetrieved;
    return JVMTI_ERROR_NONE;
} /* getValue */



jvmtiError findInLocalContext(JNIEnv* env /*, jvmtiLocalVariableEntry** localValues */,
                              jstring name, jthread currentThread, 
                              jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[], 
                              int currentDepth, int maxDepth, valueContext* result)
{
    jvmtiError error;
    jvmtiLocalVariableEntry* locals;
    jint localsCounter;
    char buffer[50];
    int index;
    char* nameAr = NULL;
    
    /* if this depth is searched 1st time retrieve locals table */
    locals = cachedLocals[currentDepth].locals;
    localsCounter = cachedLocals[currentDepth].localsCount;
    if ( locals == NULL ) {
        error = (*g_jvmti)->GetLocalVariableTable(g_jvmti, 
                                                  frames[currentDepth].method,
                                                  &localsCounter, &locals);        
        if ( error != JVMTI_ERROR_NONE) {
            logError(env, "jvmtiLocalVariableEntry error", error);
            result->status = FAIL;
            return error;
        }

        cachedLocals[currentDepth].locals = locals;
        cachedLocals[currentDepth].localsCount = localsCounter;
//        *localValues = locals;
    } /* locals == NULL */

    if ( name != NULL )
        nameAr = (*env)->GetStringUTFChars(env, name, 0);
        
    index = UNDEFINED;
    for(int inx = 0; nameAr != NULL && index == UNDEFINED && inx < localsCounter; inx++) {
        if ( !strcmp(nameAr,  locals[inx].name) ) 
          result->slot = index = inx;
    }
    /* entry not found, try in next frame if possible */
    if ( nameAr != NULL && index == UNDEFINED  ) {
        if ( currentDepth < maxDepth  ) {
            return  getValue(env, name, currentThread, frames, 
                             cachedLocals, currentDepth+1, maxDepth, result);
            } else {
                memset(buffer, 0, sizeof (buffer));
                strcpy(buffer, "Field not found: ");
                strcpy(buffer + strlen(buffer), nameAr);
                logError(env, buffer, 0);
                result->status = FAIL;
                return JVMTI_ERROR_NONE;
            }
    } // nameAr != NULL && index == -1
    else {
        index = result->slot;
        result->name = locals[index].name;
        result->valueDepth = currentDepth;
    }
    
    /* variable is can't be used before its declaration location */
    if ( frames[currentDepth].location < locals[index].start_location ) {
        result->status = IGNORE;
    }
    return JVMTI_ERROR_NONE;
} /* findInLocalContext */

jobject asJObject(JNIEnv* env, jthread currentThread, int depth, jvmtiLocalVariableEntry* locals, int index)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    jobject retrieved;
    
    char* getter;
    char sgn = *(locals[index].signature);
    switch( sgn ) {
        case 'B':
        case 'C':
        case 'I':
        case 'S':                
        case 'Z': {
          jint value;
          error =  (*g_jvmti)->GetLocalInt(g_jvmti, currentThread, depth, 
                                           locals[index].slot, &value);
          if ( error != JVMTI_ERROR_NONE) {
             logAccessError(env, "GetLocalInt", error, locals[index].name);
             return NULL;
          }   
          retrieved = sgn == 'B' ? (*env)->CallStaticObjectMethod(env, g_ByteClass, g_byteValueToObject, (jbyte)value) :
                      sgn == 'C' ? (*env)->CallStaticObjectMethod(env, g_CharClass, g_charValueToObject, (jchar)value) :
                      sgn == 'I' ? (*env)->CallStaticObjectMethod(env, g_IntClass, g_intValueToObject, value) :
                      sgn == 'S' ? (*env)->CallStaticObjectMethod(env, g_ShortClass, g_shortValueToObject, (jshort)value) :
                                   (*env)->CallStaticObjectMethod(env, g_BooleanClass, g_booleanValueToObject, (jboolean)value);
          break;
        } /* ints */
        case 'J' : {
           jlong value;
           error =  (*g_jvmti)->GetLocalLong(g_jvmti, currentThread, depth, 
                                             locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logAccessError(env, "GetLocalLong", error, locals[index].name);
             return NULL;
           }   
           retrieved = (*env)-> CallStaticObjectMethod(env, g_LongClass, g_longValueToObject, value);
           break;
        } /* long */
        case 'D' : {
            jdouble value;
           error =  (*g_jvmti)->GetLocalDouble(g_jvmti, currentThread, depth, 
                                               locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logAccessError(env, "GetLocalDouble", error, locals[index].name);               
             return NULL;
           }   
           retrieved = (*env)-> CallStaticObjectMethod(env, g_DoubleClass, g_doubleValueToObject, value);                
           break;
        } /* double */
        case 'F' : {
           jfloat value;
           error =  (*g_jvmti)->GetLocalFloat(g_jvmti, currentThread, depth, 
                                               locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logAccessError(env, "GetLocalFloat", error, locals[index].name);                              
             return NULL;
           }   
           retrieved = (*env)-> CallStaticObjectMethod(env, g_FloatClass, g_floatValueToObject, value);                
           break;
        } /* double */
        case 'L' : 
        case '[' : 
        {
           error =  (*g_jvmti)->GetLocalObject(g_jvmti, currentThread, depth, 
                                               locals[index].slot, &retrieved);
           if ( error != JVMTI_ERROR_NONE) {
             logAccessError(env, "GetLocalObject", error, locals[index].name);                              
             return NULL;
           }   
           break;
        }
        default: {
            char msg[100];
            logAccessError(env, "Unexpected field signature", 0, locals[index].name);                              
            return NULL;
        }
    } /* switch signature */
    if ( (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return NULL;
    }    
    return retrieved;    
} /* asJObject */

int initConvertes(JNIEnv *env)
{
    if ( JNI_ERR == initConverter(env, "java.lang.Byte", 'B', &g_ByteClass, &g_byteValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Short", 'S', &g_ShortClass, &g_shortValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Integer", 'I', &g_IntClass, &g_intValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Long", 'J', &g_LongClass, &g_longValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Float", 'F', &g_FloatClass, &g_floatValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Double", 'D', &g_DoubleClass, &g_doubleValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Boolean", 'Z', &g_BooleanClass, &g_booleanValueToObject) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Character", 'C', &g_CharClass, &g_charValueToObject) )
        return JNI_ERR;      
    if ( JNI_ERR == initAccessors(env) )
        return JNI_ERR;  
    return JNI_OK;
} /* initConverters */

int initConverter(JNIEnv *env, char* className, char primitiveType, jclass* globalClass, jmethodID* valueOf )
{
    jclass wrapperClass = NULL;
    jmethodID converter = NULL;
    char fixedClassName[40];
    char signature[50];
    
    memset(fixedClassName, 0, sizeof(signature));
    memset(signature, 0, sizeof(signature));

    for(int inx = 0; inx < sizeof(fixedClassName) && className[inx] != 0; inx++)
        fixedClassName[inx] = className[inx] == '.' ? '/' : className[inx];
        
    memset(signature, 0, sizeof(signature));
    signature[0] = '(';
    signature[1] = primitiveType;
    signature[2] = ')';
    signature[3] = 'L';
    strcpy(signature+4, fixedClassName);
    signature[strlen(signature)] = ';';
    
    wrapperClass = (*env)->FindClass(env, fixedClassName);
    if ( wrapperClass == NULL )
        return JNI_ERR;
     converter = (*env)->GetStaticMethodID(env, wrapperClass, "valueOf", signature);        
     if ( converter == NULL )
         return JNI_ERR;
     
    *globalClass = (*env)->NewGlobalRef(env,wrapperClass);
    (*env)->DeleteLocalRef(env, wrapperClass);
    
     *valueOf = converter;
     return JNI_OK;
} /* initConverter */

int initAccessors(JNIEnv *env)
{
    g_objectToByteValue = (*env)->GetMethodID(env, g_ByteClass, "byteValue", "()B");
    if ( g_objectToByteValue == NULL ) {
        logError(env, "Unable to access Byte.byteValue", 0);         
        return JNI_ERR;
    }

    g_objectToShortValue = (*env)->GetMethodID(env, g_ShortClass, "shortValue", "()S");
    if ( g_objectToShortValue == NULL ) {
        logError(env, "Unable to access Short.shortValue", 0);         
        return JNI_ERR;
    }

    g_objectToIntValue = (*env)->GetMethodID(env, g_IntClass, "intValue", "()I");
    if ( g_objectToIntValue == NULL ) {
        logError(env, "Unable to access Integer.intValue", 0);         
        return JNI_ERR;
    }

    g_objectToLongValue = (*env)->GetMethodID(env, g_LongClass, "longValue", "()J");
    if ( g_objectToLongValue == NULL ) {
        logError(env, "Unable to access Long.longValue", 0);         
        return JNI_ERR;
    }

    g_objectToFloatValue = (*env)->GetMethodID(env, g_FloatClass, "floatValue", "()F");
    if ( g_objectToFloatValue == NULL ) {
        logError(env, "Unable to access Float.floatValue", 0);         
        return JNI_ERR;
    }

    g_objectToDoubleValue = (*env)->GetMethodID(env, g_DoubleClass, "doubleValue", "()D");
    if ( g_objectToDoubleValue == NULL ) {
        logError(env, "Unable to access Double.doubleValue", 0);         
        return JNI_ERR;
    }

    g_objectToBooleanValue = (*env)->GetMethodID(env, g_BooleanClass, "booleanValue", "()Z");
    if ( g_objectToBooleanValue == NULL ) {
        logError(env, "Unable to access Boolean.booleanValue", 0);         
        return JNI_ERR;
    }
    
    return JNI_OK;
} /* initAccessors */


int initCollectionAPI(JNIEnv *env)
{
    jclass linkedHashMapClass = NULL;
    jclass mapClass = NULL;
    jclass listClass = NULL;

    /* Map.put */
    mapClass = (*env)->FindClass(env, "java/util/Map");
    if (  mapClass == NULL ) {
        logError(env, "Unable to access java.util.Map class", 0);
        return JNI_ERR;    
    }
    
    g_mapPut = (*env)->GetMethodID(env, mapClass, "put", 
                                       "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if ( g_mapPut == NULL ) {
        logError(env, "Unable to access Map.put", 0);         
        return JNI_ERR;
    }
    (*env)->DeleteLocalRef(env, mapClass);
    
    /* LinkedHashMap new() */
    linkedHashMapClass = (*env)->FindClass(env, "java/util/HashMap");
    if (linkedHashMapClass == NULL) {
        logError(env, "Unable to access LinkedHashMap class", 0);
      return JNI_ERR;
    }    
    g_linkedHashMapClass = (*env)->NewGlobalRef(env,linkedHashMapClass);
    (*env)->DeleteLocalRef(env, linkedHashMapClass);
    
    g_linkedHashMapCtor = (*env)->GetMethodID(env, g_linkedHashMapClass, "<init>", "()V");
    if ( g_linkedHashMapCtor == NULL ) {
        logError(env, "Unable to access LinkedHashMap constructor", 0);
        return JNI_ERR;
    }

    /* List.get(), List.size() */
    listClass = (*env)->FindClass(env, "java/util/List");
    if ( listClass == NULL ) {
        logError(env, "Unable to access List class", 0);         
        return JNI_ERR;    
    }    
    g_listSize = (*env)->GetMethodID(env, listClass, "size", "()I");
    if ( g_listSize == NULL ) {
        logError(env, "Unable to access list.size()", 0);         
        return JNI_ERR;
    }
    g_listGet = (*env)->GetMethodID(env, listClass, "get", "(I)Ljava/lang/Object;");    
    if ( g_listGet == NULL ) {
        logError(env, "Unable to access list.get()", 0);         
        return JNI_ERR;
    }
    (*env)->DeleteLocalRef(env, listClass);
    
    return JNI_OK;
} /* initCollectionAPIs */

void logAccessError(JNIEnv* env, char* accessor, jvmtiError error, char* entryName)
{
       char buff[100] = {0};
       strcpy(buff, accessor);
       buff[strlen(buff)] = ' ';
       strcpy(buff+strlen(buff), entryName);
       logError(env, buff, error);    
}

void logError(JNIEnv *env, char* message, jvmtiError error)
{
    char* errorMsg = NULL;
    jvmtiError err;
    if ( g_jvmti != NULL ) {
       err = (*g_jvmti)->GetErrorName(g_jvmti, error, &errorMsg);
       if ( err != JVMTI_ERROR_NONE )
           errorMsg = "error code evaluation failed";
    }
    else 
       errorMsg = "error code evaluation not evailable";
    jstring messageStr = (*env)->NewStringUTF(env, message);
    jstring errStr = (*env)->NewStringUTF(env, errorMsg);
    jstring level = (*env)->NewStringUTF(env, "SEVERE");
    (*env)->CallStaticVoidMethod(env, g_declaringClass, g_log, level, errStr, messageStr);
    (*env)->DeleteLocalRef(env, level);
    (*env)->DeleteLocalRef(env, errStr);
    (*env)->DeleteLocalRef(env, messageStr);
}

void logValue(JNIEnv *env, char* level, char* message, jobject object)
{
    jstring levelString = (*env)->NewStringUTF(env, level);
    jstring title = (*env)->NewStringUTF(env, message);
    (*env)->CallStaticVoidMethod(env, g_declaringClass, g_logObject, levelString, title, object);
    (*env)->DeleteLocalRef(env, levelString);
    (*env)->DeleteLocalRef(env, title);
}

jvmtiError returnIntWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jint value)
{
    return (*env)->ForceEarlyReturnInt(env, thread, value);
}
intApplier getIntApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    if ( type == SETTER ) {
        strcpy(applierName, "SetLocalInt");
        return (*env)->SetLocalInt;
    }
    strcpy(applierName, "returnInt");
    return &returnIntWrapper;    
}


jvmtiError returnLongWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jlong value)
{
    return (*env)->ForceEarlyReturnLong(env, thread, value);
}
longApplier getLongApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    if ( type == SETTER ) {
        strcpy(applierName, "SetLocalLong");
        return (*env)->SetLocalLong;
    }
    strcpy(applierName, "returnLong");
    return &returnLongWrapper;    
}


jvmtiError returnFloatWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jfloat value)
{
    return (*env)->ForceEarlyReturnFloat(env, thread, value);
}
floatApplier getFloatApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    if ( type == SETTER ) {
        strcpy(applierName, "SetLocalFloat");
        return (*env)->SetLocalFloat;
    }
    strcpy(applierName, "returnFloatWrapper");
    return &returnFloatWrapper;    
}


jvmtiError returnDoubleWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jdouble value)
{
    return (*env)->ForceEarlyReturnDouble(env, thread, value);
}
doubleApplier getDoubleApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    if ( type == SETTER ) {
        strcpy(applierName, "SetLocalDouble");
        return (*env)->SetLocalDouble;
    }
    strcpy(applierName, "returnDoubleWrapper");
    return &returnDoubleWrapper;    
}


jvmtiError returnObjectWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jobject value)
{
    return (*env)->ForceEarlyReturnObject(env, thread, value);
}
objectApplier getObjectApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    if ( type == SETTER ) {
        strcpy(applierName, "SetLocalObject");
        return (*env)->SetLocalObject;
    }
    strcpy(applierName, "returnObjectWrapper");
    return &returnObjectWrapper;    
}

jvmtiError returnVoidWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jobject unused)
{
    return (*env)->ForceEarlyReturnVoid(env, thread);
}
voidApplier getVoidApplier(jvmtiEnv* env, changeType type, char* applierName)
{
    strcpy(applierName, "returnVoidWrapper");
    return type == SETTER ? NULL : &returnVoidWrapper;    
}


#ifdef __cplusplus
}
#endif
