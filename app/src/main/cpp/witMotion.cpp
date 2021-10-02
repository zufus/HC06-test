//
// Created by zufus on 10/2/21.
//

#include "witMotion.h"

witMotion::witMotion(int averageLength) {
    this->avgLen = averageLength;
    this->k = 0;
    this->pos = 0;
    this->avg = 0;
    this->sum = 0;
    this->dev = 0;

    //allocate buffer for mobile average
    this->angleBuffer = (float *) calloc(avgLen, sizeof(float));
    this->stcAngle = {0, 0, 0, 0};
}

witMotion::witMotion(int averageLength) {}

void witMotion::readAngleFromSerialData(char *ucData, unsigned short usLength) {
    this->k++;
    memcpy(this->stcAngle, ucData[2], 8);
}

void witMotion::mobileAvg(void){
    int N = this->avgLen;
    double newAngle = (float) this->stcAngle.Angle[0] / 32768.0 * 180;
    if ((this->k > this->avgLen) && (abs(newAngle - this->avg) > 8*dev)){
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Ops:  %f//%f//%f", nextAngle , this->avg, this->d);
        return p;
    }


    if (this->k < this->avgLen)
        N = this->k;


    this->sum = this->sum - this->angleBuffer + nextAngle;
    this->angleBuffer[pos] = newAngle;
    this->avg = this->sum / (float) N;

    standardDev(angleBuffer, this->avg, this->avgLen);

    this->pos += 1;
    if (this->pos >= avgLen)
        this->pos = 0;
}

void witMotion::standardDev(void) {
    double sd = 0;
    for (int i = 0; i < this->avgLen; i++)
        sd += (this->angleBuffer[i] - this->avg) * (this->angleBuffer[i] - this->avg);

    sd = sqrt(sd/(float) this->avgLen);
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


