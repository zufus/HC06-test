#include <__config>
#include <jni.h>
#include <string>
#include <android/log.h>
#include <iostream>
#include <cmath>
#include "JY901.h"
#define TAG "bluetooth2"


int mobileAvg (float *oldAngles, float *s, int p, int aLen, float nextAngle, float *average, float *dev);
float standardDev (float *, int, float , int);


char dataString[256];
int k = 0;

float *angleBuffer;
int pos = 0;
float avg = 0;
float sum = 0;
float dev = 0;
char *bufferData = nullptr;
const int avgLen = 16;

//TODO Extend mobile average calculation to all the sensors value

extern "C"
JNIEXPORT jstring JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_processData(JNIEnv *env, jobject thiz,
                                                          jbyteArray data) {

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
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "BufferLen: %d -- ERROR Retriving Elements", bufferLen);
        return env->NewStringUTF("Data Retrive Error");
    }

    memcpy(bufferData, cData, bufferLen);
    memcpy(&Angle, &bufferData[2], 8);

    auto angle = (float) Angle.Angle[0] / 32768.0 * 180;

    pos = mobileAvg(angleBuffer, &sum, pos, avgLen, angle, &avg, &dev);

    // TODO: Add code to update the text string if _and_only_if_ a valid value has been received
    sprintf(dataString, "Angles: %3.2f +/- %3.2f (%d/%d)",
            (float) avg,
            (float) dev,
            k, pos);

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", dataString);

    env->ReleaseByteArrayElements(data, cData, JNI_ABORT);

    return env->NewStringUTF(dataString);
}

int mobileAvg (float *oldAngles, float *s, int p, int aLen, float nextAngle, float *average, float *dev){

    if (k > 16 && (abs(nextAngle - *average) > 8*(*dev))) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ops:  %f//%f//%f", (float) nextAngle , *average, *dev);
            return p;
    }

    if (k < aLen)
        aLen = k;

    *s = *s - oldAngles[p] + nextAngle;
    oldAngles[p] = nextAngle;
    *average = *s / aLen;
    *dev = standardDev(oldAngles, 0, *average, aLen);

    p++;
    if (p > avgLen)
        p = 0;

    return p;
}

float standardDev (float *x, int n, float xm, int N){
    float sdev = 0;
    for (int i = 0; i < N; i++)
        sdev += (x[i] - xm) * (x[i] - xm);

    sdev = sdev/(float)N;
    return sqrtf(sdev);
}