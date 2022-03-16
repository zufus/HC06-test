#include <__config>
#include <jni.h>
#include <string>
#include <android/log.h>
#include <iostream>
#include <cmath>
#include <algorithm>
#include "witMotion.h"
#define TAG "bluetooth2"

#define ELEVATOR 0
#define WING 1

int mobileAvg (float *oldAngles, float *s, int p, int aLen, float nextAngle, float *average, float *dev);
float standardDev (const float *, float , int);


char dataString[256];
int k = 0;

float *angleBuffer;
int pos = 0;
float avg = 0;
float sum = 0;
float dev = 0;
char *bufferData = nullptr;
const int avgLen = 16;


witMotion *dataProcessor[2];

//TODO Extend mobile average calculation to all the sensors value



extern "C"
JNIEXPORT void JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_createDataProcessingObjects(JNIEnv *env, jobject thiz, jint l){

    dataProcessor[ELEVATOR] = new witMotion(l);
    dataProcessor[WING]     = new witMotion(l);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_handleData(JNIEnv *env, jobject thiz, jint id, jbyteArray data){

    int buffLen = env->GetArrayLength(data);
    char *buffData = (char *) malloc(buffLen + 1);
    jbyte *cData = env->GetByteArrayElements(data, nullptr);
    memcpy(buffData, cData, buffLen);


    dataProcessor[id]->readAngleFromSerialData(buffData);
    dataProcessor[id]->mobileAvg();
    env->ReleaseByteArrayElements(data, cData, JNI_ABORT);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_resetData(JNIEnv *env, jobject thiz, jint id, jint N){
    dataProcessor[id]->reset(N);
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_getData(JNIEnv *env, jobject thiz, jint id){
    return dataProcessor[id]->getAverage();
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_getStdDev(JNIEnv *env, jobject thiz, jint id){
    return dataProcessor[id]->getStdDev();
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_getAngle(JNIEnv *env, jobject thiz, jint id){
    return dataProcessor[id]->getAngle();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_deleteDataProcessingObjects(JNIEnv *env, jobject thiz){

}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_processData(JNIEnv *env, jobject thiz, jbyteArray data) {

    SAngle Angle = {0, 0, 0, 0};

    k += 1;

    if (angleBuffer == nullptr)
        angleBuffer = (float *) calloc (avgLen, sizeof (float));

    int bufferLen = env->GetArrayLength(data);
    if (bufferData == nullptr)
        bufferData = (char *) malloc(bufferLen + 1);

    jbyte *cData = env->GetByteArrayElements(data, nullptr);

    if (!cData) {
        //TODO Find a better way to handle this error
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                            "BufferLen: %d -- ERROR Retriving Elements", bufferLen);
        return env->NewStringUTF("Data Retrive Error");
    }

    memcpy(bufferData, cData, bufferLen);
    memcpy(&Angle, &bufferData[2], 8);

    auto angle = (float) Angle.Angle[1] / 32768.0 * 180;

    pos = mobileAvg(angleBuffer, &sum, pos, avgLen, angle, &avg, &dev);

    // TODO: Add code to update the text string if _and_only_if_ a valid value has been received
    sprintf(dataString, "Angles: %3.2f +/- %3.2f", (float) avg, (float) dev);

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", dataString);

    env->ReleaseByteArrayElements(data, cData, JNI_ABORT);

    return env->NewStringUTF(dataString);
}


int mobileAvg (float *oldAngles, float *s, int p, int aLen, float nextAngle, float *average, float *d){

    if (k > 16 && (abs(nextAngle - *average) > 8*(*d))) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ops:  %f//%f//%f", (float) nextAngle , *average, *d);
            return p;
    }

    if (k < aLen)
        aLen = k;

    *s = *s - oldAngles[p] + nextAngle;
    oldAngles[p] = nextAngle;
    *average = *s / (float) aLen;
    *d = standardDev(oldAngles, *average, aLen);

    p++;
    if (p > avgLen)
        p = 0;

    return p;
}

float standardDev (const float *x, float xm, int N){
    float sDev = 0;
    for (int i = 0; i < N; i++)
        sDev += (x[i] - xm) * (x[i] - xm);

    sDev = sqrt(sDev / (float)N);
    sDev = (sDev > 0.001) ? sDev : 0.001;
    return sqrtf(sDev);
}