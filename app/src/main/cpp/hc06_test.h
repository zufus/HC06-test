//
// Created by zufus on 9/26/21.
//

#include <__config>
#include <jni.h>

#ifndef HC06_TEST_HC06_TEST_H
#define HC06_TEST_HC06_TEST_H

extern "C"
JNIEXPORT jstring JNICALL Java_com_skylabmodels_hc06_1test_MainActivity_processData(JNIEnv *env, jobject thiz, jbyteArray data);

#endif //HC06_TEST_HC06_TEST_H
