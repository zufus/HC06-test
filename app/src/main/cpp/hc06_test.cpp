#include <__config>
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());

}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_skylabmodels_hc06_1test_MainActivity_processData(JNIEnv *env, jobject thiz,
                                                          jbyteArray data) {
    // TODO: implement processData()
    jboolean isCopy;
    jbyte *cData = env->GetByteArrayElements(data, &isCopy);
    env->ReleaseByteArrayElements(data, cData, JNI_ABORT);

    std::string s((char*) cData, sizeof((char *) cData));
    return env->NewStringUTF(s.c_str());

}