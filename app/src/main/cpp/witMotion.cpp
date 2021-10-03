//
// Created by zufus on 10/2/21.
//

#include "witMotion.h"
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <android/log.h>
#define TAG "bluetooth2"

#define HC06_DEBUG 0

witMotion::witMotion(int averageLength) {
    this->avgLen = averageLength;
    this->k = 0;
    this->pos = 0;
    this->avg = 0;
    this->sum = 0;
    this->dev = 0;

    //allocate buffer for mobile average
    this->angleBuffer = (float *) calloc(avgLen, sizeof(float));
    for (int i = 0; i < this->avgLen; i++)
        angleBuffer[i] = 0;
    this->stcAngle = {0, 0, 0, 0};
}

void witMotion::readAngleFromSerialData(const char *ucData) {
    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "In readAngleFromSerialData, k: %d" , k);
    this->k++;
    this->stcAngle.Angle[0] = (short) (ucData[3]<<8 | ucData[2]);
    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "In readAngleFromSerialData, data copied, k: %d" , k);
    memcpy((void *) &this->stcAngle, (const void*) &ucData[2], 8);

}

void witMotion::mobileAvg(){
    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "In mobileAvg, k: %d" , k);

    int N = this->avgLen;
    float newAngle = (float) this->stcAngle.Angle[0] / 32768 * 180;
    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                        "In mobileAvg, newAngle= %f" , newAngle);

    if ((this->k > this->avgLen) && (abs(newAngle - this->avg) > 100*dev)){
        if (HC06_DEBUG)
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ops:  %f//%f//%f", newAngle , this->avg, this->dev);
        return;
    }



    if (this->k < this->avgLen)
        N = this->k;


    this->sum = this->sum - this->angleBuffer[pos] + newAngle;
    this->angleBuffer[pos] = (float) newAngle;
    this->avg = this->sum / (float) N;

    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "%f//%f//%f (%d/%d)", newAngle , this->avg, this->dev, this->k, this->pos);

    this->pos += 1;
    if (this->pos >= avgLen)
        this->pos = 0;

    standardDev(N);


}

void witMotion::standardDev(int N) {
    if (HC06_DEBUG)
        __android_log_print(ANDROID_LOG_DEBUG, TAG,
                            "In standardDev, N:%d", N);

    float sd = 0;
    for (int i = 0; i < N; i++)
        sd += (this->angleBuffer[i] - this->avg) * (this->angleBuffer[i] - this->avg);

    sd = sqrt(sd/(float) N);
    sd = (sd > 0.001) ? sd : 0.001;
    this->dev = sd;
}

void witMotion::reset(int N) {

    this->k = 0;
    this->pos = 0;
    this->avg = 0;
    this->sum = 0;
    this->dev = 0;

    this->stcAngle = {0, 0, 0, 0};

    if (this->avgLen != N) {
        //re-allocate buffer for mobile average
        this->avgLen = N;
        free(this->angleBuffer);
        this->angleBuffer = (float *) calloc(avgLen, sizeof(float));
    }
}

float witMotion::getAverage() {
    return this->avg;
}

float witMotion::getStdDev() {
    return this->dev;
}


