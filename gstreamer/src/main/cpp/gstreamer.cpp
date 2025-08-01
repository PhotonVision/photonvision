#include <iostream>
#include <opencv2/opencv.hpp>
#include "jni_Gstreamer.h"

std::string jstringToCppString(JNIEnv* env, jstring jStr) {
  if (!jStr) return "";

  const jclass stringClass = env->GetObjectClass(jStr);
  const jmethodID getBytes =
      env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
  const jbyteArray stringJbytes = (jbyteArray)env->CallObjectMethod(
      jStr, getBytes, env->NewStringUTF("UTF-8"));

  size_t length = (size_t)env->GetArrayLength(stringJbytes);
  jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

  std::string ret = std::string((char*)pBytes, length);
  env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

  env->DeleteLocalRef(stringJbytes);
  env->DeleteLocalRef(stringClass);
  return ret;
}

JNIEXPORT jlong JNICALL Java_jni_Gstreamer_initCam(JNIEnv* env, jclass clazz,
                                                   jstring jpipe) {
  std::string pipe(jstringToCppString(env, jpipe));
  cv::VideoCapture* cap = new cv::VideoCapture(pipe, cv::CAP_GSTREAMER);
  return (jlong)cap;
}

JNIEXPORT jboolean JNICALL Java_jni_Gstreamer_readMat(JNIEnv* env, jclass clazz,
                                                      jlong pcap, jlong pmat) {
  cv::VideoCapture* cap = reinterpret_cast<cv::VideoCapture*>(pcap);
  cv::Mat* mat = reinterpret_cast<cv::Mat*>(pmat);
  bool success = cap->read(*mat);
  return success;
}


JNIEXPORT void JNICALL Java_jni_Gstreamer_getGrayScale(JNIEnv* env,
                                                       jclass clazz, jlong praw,
                                                       jlong pprocessed) {
  cv::Mat* raw = reinterpret_cast<cv::Mat*>(praw);
  cv::Mat* processed = reinterpret_cast<cv::Mat*>(pprocessed);

  // std::cout << "Grayscaling" << std::endl;

  if (!raw || raw->empty()) {
    // Make a black image of size 1456x1088
    *processed = cv::Mat(1088, 1456, CV_8UC1, cv::Scalar(0));
  } else {
    cv::cvtColor(*raw, *processed, cv::COLOR_BGR2GRAY);
  }

  // std::cout << "Done" << std::endl;
}

JNIEXPORT void JNICALL Java_jni_Gstreamer_releaseCam(JNIEnv* env, jclass clazz,
                                                     jlong pcap) {
  cv::VideoCapture* cap = reinterpret_cast<cv::VideoCapture*>(pcap);
  cap->release();
}
