/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   StackMiner_private.h
 *
 * Created on June 20, 2017, 11:19 PM
 */

#ifndef STACKMINER_PRIVATE_H
#define STACKMINER_PRIVATE_H

#ifdef __cplusplus
extern "C" {
#endif

#define ALL_ARGS       "all:args"
#define ALL_LOCAL_VARS "all:locals"
    
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
} valueContext;

typedef enum { SETTER, RETURN_VAL } changeType;


void logError(JNIEnv *env, char* message, jvmtiError error);
void logAccessError(JNIEnv* env, char* accessor, jvmtiError error, char* entryName);

int initAccessors(JNIEnv *env);
int initCollectionAPI(JNIEnv *env);
int initConvertes(JNIEnv *env);
int initConverter(JNIEnv *env, char* className, char primitiveType, jclass* globalClass, jmethodID* valueOf);
jvmtiError initRequest(JNIEnv *env, jthread* currentThread, jvmtiFrameInfo* frames, jint* framesDepth);

jvmtiError checkBounds(JNIEnv *env, jvmtiFrameInfo frame, jobject* list, jint* startIndex, jint* endIndex);

jvmtiError findInLocalContext(JNIEnv* env, /*jvmtiLocalVariableEntry** localValues,*/
                              jstring name, jthread currentThread, 
                              jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[], 
                              int currentDepth, int maxDepth, valueContext* result);

jvmtiError getValue(JNIEnv* env, jstring name, jthread currentThread, jvmtiFrameInfo* frames, 
                    cachedLocalVarsEntry cachedLocals[],  int startDepth, int maxDepth, 
                    valueContext* value);

jvmtiError setValue(JNIEnv* env, jstring name, jobject value, jthread currentThread, 
                    jvmtiFrameInfo* frames, cachedLocalVarsEntry cachedLocals[], 
                    int currentDepth, int maxDepth, valueContext* result);

jvmtiError enforceReturn(JNIEnv* env, jobject value, jthread currentThread, jvmtiFrameInfo* frames);

jobject asJObject(JNIEnv* env, jthread currentThread, int depth, jvmtiLocalVariableEntry* locals, int index);

jvmtiError applyChange(JNIEnv* env, jthread currentThread, changeType usage, 
                       char sign, char* entryName, int currentDepth, int slot, jobject value);


static jvmtiEnv *g_jvmti = NULL;
static jclass    g_declaringClass = NULL;
static jclass    g_linkedHashMapClass = NULL;
static jmethodID g_linkedHashMapCtor = NULL;
static jmethodID g_mapPut = NULL;
static jmethodID g_listSize = NULL;
static jmethodID g_listGet = NULL;

/* primitive wrapper classes */
static jclass g_ByteClass = NULL;
static jclass g_ShortClass = NULL;
static jclass g_IntClass = NULL;
static jclass g_LongClass = NULL;
static jclass g_FloatClass = NULL;
static jclass g_DoubleClass = NULL;
static jclass g_CharClass = NULL;
static jclass g_BooleanClass = NULL;

/* valueOf() static methods */
static jmethodID g_log = NULL;
static jmethodID g_byteValueToObject = NULL;
static jmethodID g_shortValueToObject = NULL;
static jmethodID g_intValueToObject = NULL;
static jmethodID g_longValueToObject = NULL;
static jmethodID g_floatValueToObject = NULL;
static jmethodID g_doubleValueToObject = NULL;
static jmethodID g_charValueToObject = NULL;
static jmethodID g_booleanValueToObject = NULL;

/* methods of java.lang.Number (byteValue, intValue etc) */
static jmethodID g_objectToByteValue = NULL;
static jmethodID g_objectToShortValue = NULL;
static jmethodID g_objectToIntValue = NULL;
static jmethodID g_objectToLongValue = NULL;
static jmethodID g_objectToFloatValue = NULL;
static jmethodID g_objectToDoubleValue = NULL;
static jmethodID g_objectToBooleanValue = NULL;



typedef jvmtiError (* intApplier) (jvmtiEnv*,jthread,jint,jint,jint);
typedef jvmtiError (* longApplier) (jvmtiEnv*,jthread,jint,jint,jlong);
typedef jvmtiError (* floatApplier) (jvmtiEnv*,jthread,jint,jint,jfloat);
typedef jvmtiError (* doubleApplier) (jvmtiEnv*,jthread,jint,jint,jdouble);
typedef jvmtiError (* objectApplier) (jvmtiEnv*,jthread,jint,jint,jobject);
typedef jvmtiError (* voidApplier) (jvmtiEnv*,jthread,jint,jint,jobject);


JNICALL jvmtiError returnLongWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jlong value);
JNICALL jvmtiError returnFloatWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jfloat value);
JNICALL jvmtiError returnDoubleWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jdouble value);
JNICALL jvmtiError returnObjectWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jobject value);
JNICALL jvmtiError returnVoidWrapper(jvmtiEnv* env, jthread thread, jint depth, jint slot, jobject unused);

intApplier getIntApplier(jvmtiEnv* env, changeType type, char* applierName);
longApplier getLongApplier(jvmtiEnv* env, changeType type, char* applierName);
floatApplier getFloatApplier(jvmtiEnv* env, changeType type, char* applierName);
doubleApplier getDoubleApplier(jvmtiEnv* env, changeType type, char* applierName);
objectApplier getObjectApplier(jvmtiEnv* env, changeType type, char* applierName);
voidApplier getVoidApplier(jvmtiEnv* env, changeType type, char* applierName);

#ifdef __cplusplus
}
#endif

#endif /* STACKMINER_PRIVATE_H */

