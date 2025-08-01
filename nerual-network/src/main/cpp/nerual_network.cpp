#include <NvInfer.h>
#include <cassert>
#include <cstdio>
#include <cuda_runtime_api.h>
#include <fstream>
#include <iostream>
#include <opencv2/core/cuda.hpp>
#include <opencv2/cudaarithm.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/opencv.hpp>
#include <vector>
#include "jni_NerualNetwork.h"

using namespace nvinfer1;

typedef struct Model {
  IRuntime* runtime = nullptr;
  ICudaEngine* engine = nullptr;
  IExecutionContext* context = nullptr;
  cudaStream_t inferenceCudaStream;
  void* output_buffer;
  size_t output_size = 1;
} model_t;

class Logger : public ILogger {
  void log(Severity severity, const char* msg) noexcept override {
    if (severity <= Severity::kWARNING) std::cout << msg << std::endl;
  }
};

std::vector<char> loadEngineFile(const std::string& filename) {
  std::ifstream file(filename, std::ios::binary);
  if (!file) throw std::runtime_error("Engine file not found");
  return std::vector<char>((std::istreambuf_iterator<char>(file)),
                           std::istreambuf_iterator<char>());
}

size_t getOutputSize(ICudaEngine* engine) {
  Dims output_shape = engine->getTensorShape(engine->getIOTensorName(1));
  size_t output_size = 1;
  for (int i = 0; i < output_shape.nbDims; i++) {
    output_size *= output_shape.d[i];
    std::cout << output_shape.d[i] << " ";
  }
  std::cout << "\noutput size: " << output_size
            << "output nbDims: " << output_shape.nbDims << std::endl;
  return output_size;
}

cv::cuda::GpuMat preprocess(cv::Mat& img) {
  cv::cuda::GpuMat img_gpu;
  img_gpu.upload(img);
  cv::cuda::GpuMat gpu_dst(1, img_gpu.rows * img_gpu.cols * 1, CV_8UC3);

  size_t width = img_gpu.cols * img_gpu.rows;
  std::vector<cv::cuda::GpuMat> input_channels{
      cv::cuda::GpuMat(img_gpu.rows, img_gpu.cols, CV_8U, &(gpu_dst.ptr()[0])),
      cv::cuda::GpuMat(img_gpu.rows, img_gpu.cols, CV_8U,
                       &(gpu_dst.ptr()[width])),
      cv::cuda::GpuMat(img_gpu.rows, img_gpu.cols, CV_8U,
                       &(gpu_dst.ptr()[width * 2]))};
  cv::cuda::split(img_gpu, input_channels);  // HWC -> CHW

  cv::cuda::GpuMat output;
  gpu_dst.convertTo(output, CV_32FC3, 1.f / 255.f);

  return output;
}

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

JNIEXPORT jlong JNICALL Java_jni_NerualNetwork_initModel(JNIEnv* env,
                                                         jclass clazz,
                                                         jstring jpath) {
  Logger logger;
  std::string path(jstringToCppString(env, jpath));

  auto engine_data = loadEngineFile(path);

  model_t* model = new model_t();

  model->runtime = createInferRuntime(logger);
  assert(model->runtime != nullptr);

  model->engine = model->runtime->deserializeCudaEngine(engine_data.data(),
                                                        engine_data.size());
  assert(model->engine != nullptr);

  model->context = model->engine->createExecutionContext();
  assert(model->context != nullptr);

  model->output_size = getOutputSize(model->engine);
  cudaMalloc((void**)&(model->output_buffer),
             sizeof(float) * model->output_size);

  cudaStreamCreate(&(model->inferenceCudaStream));

  return (jlong)model;
}

JNIEXPORT jfloatArray JNICALL Java_jni_NerualNetwork_runModel(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong jmodel,
                                                              jlong jmat) {
  jfloatArray emptyArray = env->NewFloatArray(0);
  bool status;
  cv::Mat* mat = reinterpret_cast<cv::Mat*>(jmat);
  model_t* model = reinterpret_cast<model_t*>(jmodel);

  status = cv::imwrite("output.jpg", *mat);
  std::cout << status << std::endl;

  cv::cuda::GpuMat gpu_mat = preprocess(*mat);

  status = model->context->setTensorAddress(model->engine->getIOTensorName(0),
                                            (void*)gpu_mat.ptr<void>());
  std::cout << status << std::endl;

  status = model->context->setTensorAddress(model->engine->getIOTensorName(1),
                                            (void*)model->output_buffer);
  std::cout << status << std::endl;

  status = model->context->enqueueV3(model->inferenceCudaStream);
  std::cout << status << std::endl;

  cudaStreamSynchronize(model->inferenceCudaStream);
  std::cout << "status " << status << std::endl;

  std::vector<float> featureVector;
  featureVector.resize(model->output_size);
  cudaMemcpy(featureVector.data(), static_cast<char*>(model->output_buffer),
             model->output_size * sizeof(float),
             cudaMemcpyDeviceToHost);  // Probably do not need to cast to char*

  for (int i = 0; i < 10; i++) {
    for (int j = 0; j < 6; j++) {
      std::cout << featureVector[i * 6 + j] << " ";
    }
    std::cout << "\n";
  }
  jfloatArray output = env->NewFloatArray(featureVector.size());
  env->SetFloatArrayRegion(output, 0, featureVector.size(),
                           featureVector.data());

  return output;
}

JNIEXPORT void JNICALL Java_jni_NerualNetwork_releaseModel(JNIEnv* env,
                                                           jclass clazz,
                                                           jlong jmodel) {
  model_t* model = reinterpret_cast<model_t*>(jmodel);

  if (!model) return;

  if (model->context) {
    delete model->context;
    model->context = nullptr;
  }

  if (model->engine) {
    delete model->engine;
    model->engine = nullptr;
  }

  if (model->runtime) {
    delete model->runtime;
    model->runtime = nullptr;
  }

  if (model->output_buffer) {
    cudaFree(model->output_buffer);
    model->output_buffer = nullptr;
  }

  if (model->inferenceCudaStream) {
    cudaStreamDestroy(model->inferenceCudaStream);
    model->inferenceCudaStream = nullptr;
  }
}
