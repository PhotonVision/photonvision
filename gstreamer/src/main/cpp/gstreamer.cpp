#include <iostream>
#include <opencv2/opencv.hpp>
#include "jni_Gstreamer.h"

std::string jstringToCppString(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

JNIEXPORT jlong JNICALL Java_jni_Gstreamer_initCam(JNIEnv *env, jobject thisObject, jstring jpipe)
{

  std::string pipe(jstringToCppString(env, jpipe));
  std::cout << "\n\n" << pipe << "\n\n";
  cv::VideoCapture* cap = new cv::VideoCapture(pipe, cv::CAP_GSTREAMER);
  return (jlong)cap;
}

JNIEXPORT void JNICALL Java_jni_Gstreamer_readMat(JNIEnv *env, jobject thisObject, jlong pcap, jlong pmat)
{
  cv::VideoCapture* cap = reinterpret_cast<cv::VideoCapture*>(pcap);
  cv::Mat* mat =  reinterpret_cast<cv::Mat*>(pmat);
  cap->read(*mat);
}

JNIEXPORT void JNICALL Java_jni_Gstreamer_releaseCam(JNIEnv *env, jobject thisObject, jlong pcap)
{
  cv::VideoCapture* cap =  reinterpret_cast<cv::VideoCapture*>(pcap);
  cap->release();
}

