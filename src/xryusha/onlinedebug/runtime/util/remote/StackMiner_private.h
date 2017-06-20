/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   StackMiner_private.h
 * Author: Lena
 *
 * Created on June 20, 2017, 11:19 PM
 */

#ifndef STACKMINER_PRIVATE_H
#define STACKMINER_PRIVATE_H

#ifdef __cplusplus
extern "C" {
#endif

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



#ifdef __cplusplus
}
#endif

#endif /* STACKMINER_PRIVATE_H */

