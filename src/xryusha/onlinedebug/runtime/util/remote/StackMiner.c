#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <jvmti.h>
#include "xryusha_onlinedebug_runtime_util_StackMiner.h"


#ifdef __cplusplus
extern "C" {
#endif

#define MAX_LOCAL_FRAME_DEPTH 10
#define UNDEFINED 0x1FFFFFFF

typedef struct {
    jvmtiLocalVariableEntry* locals;
    int localsCount;
} cachedLocalVarsEntry;

typedef enum {
    OK, FAIL, IGNORE
} getValueStatus;

typedef struct {
    getValueStatus status;
    int slot;
    char* name;
    jobject value;
    int valueDepth;
} getValueResult;

void logError(JNIEnv *env, char* message, jvmtiError error);
int initCollectionAPI(JNIEnv *env);
int initConvertes(JNIEnv *env);
int initConverter(JNIEnv *env, char* className, char primitiveType, jclass* globalClass, jmethodID* valueOf);
jvmtiError getValue(JNIEnv* env, jstring name, jthread currentThread, jvmtiFrameInfo* frames,
                    cachedLocalVarsEntry cachedLocals[],  int startDepth, int maxDepth,
                    getValueResult* value);
jobject asJObject(JNIEnv* env, jthread currentThread, int depth, jvmtiLocalVariableEntry* locals, int index);


static jvmtiEnv *g_jvmti = NULL;
static jclass    g_declaringClass = NULL;
static jclass    g_hashMapClass = NULL;
static jmethodID g_hashMapCtor = NULL;
static jmethodID g_hashMapPut = NULL;
static jmethodID g_listSize = NULL;
static jmethodID g_listGet = NULL;
/* convertors valueOf() */
static jclass g_ByteClass = NULL;
static jclass g_ShortClass = NULL;
static jclass g_IntClass = NULL;
static jclass g_LongClass = NULL;
static jclass g_FloatClass = NULL;
static jclass g_DoubleClass = NULL;
static jclass g_CharClass = NULL;
static jclass g_BooleanClass = NULL;

static jmethodID g_log = NULL;
static jmethodID g_valueOfByte = NULL;
static jmethodID g_valueOfShort = NULL;
static jmethodID g_valueOfInt = NULL;
static jmethodID g_valueOfLong = NULL;
static jmethodID g_valueOfFloat = NULL;
static jmethodID g_valueOfDouble = NULL;
static jmethodID g_valueOfChar = NULL;
static jmethodID g_valueOfBoolean = NULL;


JNIEXPORT jboolean JNICALL
Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_init
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
}  /* init */


JNIEXPORT jobject JNICALL
Java_xryusha_onlinedebug_runtime_util_remote_StackMiner_extract
  (JNIEnv *env, jclass clazz, jobject list)
{
   jint size;
   jvmtiError error;
   jobject hashMap = NULL;
   jthread currentThread;
   jvmtiFrameInfo frames[MAX_LOCAL_FRAME_DEPTH];
   jint framesDepth;
   jobject value = NULL;
   cachedLocalVarsEntry cachedLocals[MAX_LOCAL_FRAME_DEPTH];


    error = (*g_jvmti)->GetCurrentThread(g_jvmti, &currentThread);
    if ( error != JVMTI_ERROR_NONE) {
        logError(env, "GetCurrentThread error", error);
        return NULL;
    }

    error = (*g_jvmti)->GetStackTrace(g_jvmti, currentThread,
                                      0, MAX_LOCAL_FRAME_DEPTH,
                                      frames, &framesDepth);
    if (error != JVMTI_ERROR_NONE) {
        logError(env, "GetStackTrace error", error);
        return NULL;
    }

    hashMap = (*env)->NewObject(env, g_hashMapClass, g_hashMapCtor, 1);
    size = list != NULL ? (*env)->CallIntMethod(env, list, g_listSize) : UNDEFINED;
    if (size == 0)
        return hashMap;

    memset(cachedLocals, 0, sizeof(cachedLocals));
    for (int ii = 0; ii < size; ii++) {
        int index = ii;
        getValueResult result;
        jstring name = NULL;
        if ( list != NULL ) {
            name = (jstring) (*env)->CallObjectMethod(env, list, g_listGet, ii);
            index = UNDEFINED;
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

        if ( size == UNDEFINED ) {
            size = cachedLocals[1].localsCount;
        }

        jstring globalName = name != NULL ?
                               (*env)->NewGlobalRef(env, name) :
                                  (*env)->NewStringUTF(env, result.name);

        (*env)->CallObjectMethod(env, hashMap, g_hashMapPut, globalName, result.value);
        (*env)->DeleteLocalRef(env, name);
    }
/*
    for(int inx = 0; inx < MAX_FRAME_DEPTH && cachedLocals[inx].locals != NULL; inx++ )
        (*g_jvmti)->Deallocate(g_jvmti,  (void*)cachedLocals[inx].locals);
    for(int inx = 0; inx < framesDepth ; inx++ )
        (*g_jvmti)->Deallocate(g_jvmti,  (void*)&frames[inx]);
*/
    return hashMap;
 } /* extract */


jvmtiError getValue(JNIEnv* env, jstring name, jthread currentThread,
                    jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[],
                    int currentDepth, int maxDepth, getValueResult* result)
{
    jvmtiError error;
    jvmtiLocalVariableEntry* locals;
    jint localsCounter;
    jobject retrieved;
    jobject globalizedRetrieved;
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
    } /* locals == NULL */

    if ( name != NULL )
        nameAr = (*env)->GetStringUTFChars(env, name, 0);

    index = UNDEFINED;
    for(int inx = 0; nameAr != NULL && inx < localsCounter; inx++) {
        if ( !strcmp(nameAr,  locals[inx].name) )
            index = inx;
        result->slot = index;
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

    /* variable is can't be used earlie than its declaration location */
    if ( frames[currentDepth].location < locals[index].start_location ) {
        result->status = IGNORE;
        return JVMTI_ERROR_NONE;
    }

    globalizedRetrieved = NULL;
    retrieved  = asJObject(env, currentThread, currentDepth, locals, index);
    if (retrieved != NULL) {
        globalizedRetrieved = (*env)->NewGlobalRef(env, retrieved);
        (*env)->DeleteLocalRef(env, retrieved);
    }
    result->value = globalizedRetrieved;
    return JVMTI_ERROR_NONE;
} /* getValue */

jobject asJObject(JNIEnv* env, jthread currentThread, int depth, jvmtiLocalVariableEntry* locals, int index)
{
    jvmtiError error;
    jobject retrieved;

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
             logError(env, "GetLocalInt error", error);
             return NULL;
          }
          retrieved = sgn == 'B' ? (*env)->CallStaticObjectMethod(env, g_ByteClass, g_valueOfByte, (jbyte)value) :
                      sgn == 'C' ? (*env)->CallStaticObjectMethod(env, g_CharClass, g_valueOfChar, (jchar)value) :
                      sgn == 'I' ? (*env)->CallStaticObjectMethod(env, g_IntClass, g_valueOfInt, value) :
                      sgn == 'S' ? (*env)->CallStaticObjectMethod(env, g_ShortClass, g_valueOfShort, (jshort)value) :
                                   (*env)->CallStaticObjectMethod(env, g_ShortClass, g_valueOfShort, (jboolean)value);
          break;
        } /* ints */
        case 'J' : {
           jlong value;
           error =  (*g_jvmti)->GetLocalLong(g_jvmti, currentThread, depth,
                                             locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logError(env, "GetLocalLong error", error);
             return NULL;
           }
           retrieved = (*env)-> CallStaticObjectMethod(env, g_LongClass, g_valueOfLong, value);
           break;
        } /* long */
        case 'D' : {
            jdouble value;
           error =  (*g_jvmti)->GetLocalDouble(g_jvmti, currentThread, depth,
                                               locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logError(env, "GetLocalDouble error", error);
             return NULL;
           }
           retrieved = (*env)-> CallStaticObjectMethod(env, g_DoubleClass, g_valueOfDouble, value);
           break;
        } /* double */
        case 'F' : {
           jfloat value;
           error =  (*g_jvmti)->GetLocalFloat(g_jvmti, currentThread, depth,
                                               locals[index].slot, &value);
           if ( error != JVMTI_ERROR_NONE) {
             logError(env, "GetLocalFloat error", error);
             return NULL;
           }
           retrieved = (*env)-> CallStaticObjectMethod(env, g_FloatClass, g_valueOfFloat, value);
           break;
        } /* double */
        case 'L' : {
           error =  (*g_jvmti)->GetLocalObject(g_jvmti, currentThread, depth,
                                               locals[index].slot, &retrieved);
           if ( error != JVMTI_ERROR_NONE) {
             logError(env, "GetLocalObject error", error);
             return NULL;
           }
           break;
        }
        default: {
            char* msg = "Unexpected field signature:  ";
            msg[strlen(msg)-1] = sgn;
            logError(env, msg, 0);
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
    if ( JNI_ERR == initConverter(env, "java.lang.Byte", 'B', &g_ByteClass, &g_valueOfByte) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Short", 'S', &g_ShortClass, &g_valueOfShort) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Integer", 'I', &g_IntClass, &g_valueOfInt) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Long", 'J', &g_LongClass, &g_valueOfLong) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Float", 'F', &g_FloatClass, &g_valueOfFloat) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Double", 'D', &g_DoubleClass, &g_valueOfDouble) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Boolean", 'Z', &g_BooleanClass, &g_valueOfBoolean) )
        return JNI_ERR;
    if ( JNI_ERR == initConverter(env, "java.lang.Character", 'C', &g_CharClass, &g_valueOfChar) )
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

int initCollectionAPI(JNIEnv *env)
{
    jclass hashMapClass = NULL;
    jclass mapClass = NULL;
    jclass listClass = NULL;
    
    hashMapClass = (*env)->FindClass(env, "java/util/HashMap");
    if (hashMapClass == NULL) {
        logError(env, "Unable to access HashMap class", 0);
      return JNI_ERR;
    }    
    g_hashMapClass = (*env)->NewGlobalRef(env,hashMapClass);
    (*env)->DeleteLocalRef(env, hashMapClass);
    
    g_hashMapCtor = (*env)->GetMethodID(env, g_hashMapClass, "<init>", "(I)V");
    if ( g_hashMapCtor == NULL ) {
        logError(env, "Unable to access HashMap constructor", 0);
        return JNI_ERR;
    }

    mapClass = (*env)->FindClass(env, "java/util/Map");
    if (  mapClass == NULL ) {
        logError(env, "Unable to access java.util.Map class", 0);
        return JNI_ERR;    
    }
    g_hashMapPut = (*env)->GetMethodID(env, mapClass, "put", 
                                       "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if ( g_hashMapPut == NULL ) {
        logError(env, "Unable to access HashMap constructor", 0);         
        return JNI_ERR;
    }

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
    return JNI_OK;
} /* initCollectionAPIs */

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
       errorMsg = "erro code evaluation not evailable";
    jstring messageStr = (*env)->NewStringUTF(env, message);
    jstring errStr = (*env)->NewStringUTF(env, errorMsg);
    jstring level = (*env)->NewStringUTF(env, "SEVERE");
    (*env)->CallStaticVoidMethod(env, g_declaringClass, g_log, level, errStr, messageStr);
    (*env)->DeleteLocalRef(env, level);
    (*env)->DeleteLocalRef(env, errStr);
    (*env)->DeleteLocalRef(env, messageStr);
}

#ifdef __cplusplus
}
#endif
