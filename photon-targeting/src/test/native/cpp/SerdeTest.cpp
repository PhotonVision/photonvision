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

#include <iostream>
#include <optional>

#include <wpi/units/angle.hpp>

#include "gtest/gtest.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/targeting/BoolTestMessage.h"
#include "photon/targeting/Float32TestMessage.h"
#include "photon/targeting/Float64TestMessage.h"
#include "photon/targeting/Int16TestMessage.h"
#include "photon/targeting/Int32TestMessage.h"
#include "photon/targeting/Int64TestMessage.h"
#include "photon/targeting/Int8TestMessage.h"
#include "photon/targeting/Transform3dTestMessage.h"

using namespace photon;

template <typename T>
  requires(PhotonStructSerializable<T>)
inline bool test_serde(const T& data) {
  Packet p;
  p.Pack<T>(data);
  T unpackedData = p.Unpack<T>();
  return data == unpackedData;
}

TEST(SerdeTest, Int8) {
  std::cout << "Running int8 Test\n";
  // Default Test
  Int8TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Int8TestMessage test2{};
  test2.optTest = {3};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Int8TestMessage test3{};
  test3.vlaTest = {1, 2, 3};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Int8TestMessage test4{};
  test4.test = 1;
  test4.vlaTest = {1, 2, 3};
  test4.optTest = {3};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Int16) {
  std::cout << "Running int16 Test\n";
  // Default Test
  Int16TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Int16TestMessage test2{};
  test2.optTest = {3};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Int16TestMessage test3{};
  test3.vlaTest = {1, 2, 3};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Int16TestMessage test4{};
  test4.test = 1;
  test4.vlaTest = {1, 2, 3};
  test4.optTest = {3};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Int32) {
  std::cout << "Running int32 Test\n";
  // Default Test
  Int32TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Int32TestMessage test2{};
  test2.optTest = {3};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Int32TestMessage test3{};
  test3.vlaTest = {1, 2, 3};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Int32TestMessage test4{};
  test4.test = 1;
  test4.vlaTest = {1, 2, 3};
  test4.optTest = {3};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Int64) {
  std::cout << "Running int64 Test\n";
  // Default Test
  Int64TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Int64TestMessage test2{};
  test2.optTest = {3};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Int64TestMessage test3{};
  test3.vlaTest = {1, 2, 3};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Int64TestMessage test4{};
  test4.test = 1;
  test4.vlaTest = {1, 2, 3};
  test4.optTest = {3};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Float32) {
  std::cout << "Running float32 Test\n";
  // Default Test
  Float32TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Float32TestMessage test2{};
  test2.optTest = {3.0};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Float32TestMessage test3{};
  test3.vlaTest = {1.0, 2.0, 3.0};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Float32TestMessage test4{};
  test4.test = 1.0;
  test4.vlaTest = {1.0, 2.0, 3.0};
  test4.optTest = {3.0};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Float64) {
  std::cout << "Running float64 Test\n";
  // Default Test
  Float64TestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Float64TestMessage test2{};
  test2.optTest = {3.0};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Float64TestMessage test3{};
  test3.vlaTest = {1.0, 2.0, 3.0};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Float64TestMessage test4{};
  test4.test = 1.0;
  test4.vlaTest = {1.0, 2.0, 3.0};
  test4.optTest = {3.0};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Bool) {
  std::cout << "Running bool Test\n";
  // Default Test
  BoolTestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  BoolTestMessage test2{};
  test2.optTest = {true};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  BoolTestMessage test3{};
  test3.vlaTest = {true, false, true};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  BoolTestMessage test4{};
  test4.test = true;
  test4.vlaTest = {false, true, false};
  test4.optTest = {true};
  ASSERT_TRUE(test_serde(test4));
}

TEST(SerdeTest, Transform3d) {
  std::cout << "Running Transform3d Test\n";
  // Default Test
  Transform3dTestMessage test1{};
  ASSERT_TRUE(test_serde(test1));
  // Optional Test
  Transform3dTestMessage test2{};
  test2.optTest = {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}};
  ASSERT_TRUE(test_serde(test2));
  // VLA Test
  Transform3dTestMessage test3{};
  test3.vlaTest = {
      {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}},
      {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}},
      {8_m, 2_m, 1_m, wpi::math::Rotation3d{0_deg, 1_deg, 88_deg}}};
  ASSERT_TRUE(test_serde(test3));
  // General Test
  Transform3dTestMessage test4{};
  test4.test = {8_m, 2_m, 1_m, wpi::math::Rotation3d{0_deg, 1_deg, 88_deg}};
  test4.vlaTest = {
      {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}},
      {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}},
      {8_m, 2_m, 1_m, wpi::math::Rotation3d{0_deg, 1_deg, 88_deg}}};
  test4.optTest = {1_m, 2_m, 3_m, wpi::math::Rotation3d{6_deg, 7_deg, 12_deg}};
  ASSERT_TRUE(test_serde(test4));
}
