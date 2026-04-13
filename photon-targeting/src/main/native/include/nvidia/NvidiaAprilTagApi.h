/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#pragma once

#include <stddef.h>
#include <stdint.h>

struct uint3 {
  unsigned int x;
  unsigned int y;
  unsigned int z;

  constexpr uint3(unsigned int x_ = 1, unsigned int y_ = 1, unsigned int z_ = 1)
      : x(x_), y(y_), z(z_) {}
};

struct dim3 : public uint3 {
  constexpr dim3(unsigned int x_ = 1, unsigned int y_ = 1, unsigned int z_ = 1)
      : uint3(x_, y_, z_) {}
  constexpr dim3(uint3 value) : uint3(value) {}
};

struct uchar3 {
  unsigned char x;
  unsigned char y;
  unsigned char z;
};

struct alignas(8) float2 {
  float x;
  float y;
};

typedef int cudaError_t;

enum cudaMemcpyKind {
  cudaMemcpyHostToHost = 0,
  cudaMemcpyHostToDevice = 1,
  cudaMemcpyDeviceToHost = 2,
  cudaMemcpyDeviceToDevice = 3,
  cudaMemcpyDefault = 4
};

constexpr cudaError_t cudaSuccess = 0;

extern "C" {
cudaError_t cudaGetDeviceCount(int* count);
cudaError_t cudaMallocPitch(void** devPtr, size_t* pitch, size_t width, size_t height);
cudaError_t cudaFree(void* devPtr);
cudaError_t cudaMemcpy2D(void* dst, size_t dpitch, const void* src, size_t spitch,
                         size_t width, size_t height, cudaMemcpyKind kind);
const char* cudaGetErrorString(cudaError_t error);
}

struct CUstream_st;

typedef struct cuAprilTagsID_st {
  float2 corners[4];
  uint16_t id;
  uint8_t hamming_error;
  float orientation[9];
  float translation[3];
} cuAprilTagsID_t;

typedef struct cuAprilTagsImageInput_st {
  uchar3* dev_ptr;
  size_t pitch;
  uint16_t width;
  uint16_t height;
} cuAprilTagsImageInput_t;

typedef struct cuAprilTagsCameraIntrinsics_st {
  float fx;
  float fy;
  float cx;
  float cy;
} cuAprilTagsCameraIntrinsics_t;

typedef enum {
  NVAT_TAG36H11,
  NVAT_ENUM_SIZE = 0x7fffffff
} cuAprilTagsFamily;

typedef struct cuAprilTagsHandle_st* cuAprilTagsHandle;

extern "C" {
int nvCreateAprilTagsDetector(cuAprilTagsHandle* hApriltags, uint32_t img_width,
                              uint32_t img_height, uint32_t tile_size,
                              cuAprilTagsFamily tag_family,
                              const cuAprilTagsCameraIntrinsics_t* cam,
                              float tag_dim);
int cuAprilTagsDetect(cuAprilTagsHandle hApriltags,
                      const cuAprilTagsImageInput_t* img_input,
                      cuAprilTagsID_t* tags_out, uint32_t* num_tags,
                      uint32_t max_tags, CUstream_st* input_stream);
int cuAprilTagsDestroy(cuAprilTagsHandle hApriltags);
}
