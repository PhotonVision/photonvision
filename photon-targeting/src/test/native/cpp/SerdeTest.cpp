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

#include "photon/dataflow/structures/Packet.h"

#include "photon/targeting/BoolTestMessage.h"
#include "photon/targeting/Int8TestMessage.h"
#include "photon/targeting/Int16TestMessage.h"
#include "photon/targeting/Int32TestMessage.h"
#include "photon/targeting/Int64TestMessage.h"
#include "photon/targeting/Float32TestMessage.h"
#include "photon/targeting/Float64TestMessage.h"
#include "photon/targeting/Transform3dTestMessage.h"

#include "gtest/gtest.h"

#include <optional>

using namespace photon;

template <typename T>
    requires(PhotonStructSerializable<T>)
inline bool test_serde(const T& data) {
    Packet p;
    p.Pack<T>(data);
    T unpackedData = p.Unpack<T>();
    return data == unpackedData;
}

TEST(SerdeTest, Int8Test) {
    std::cout << "Running int8 Test\n";
    // Default Test
    Int8TestMessage test1{};
    ASSERT_TRUE(test_serde(test1));
    std::cout << "Running int8 Test\n";
    // Optional Test
    Int8TestMessage test2{};
    test2.optTest = {3};
    ASSERT_TRUE(test_serde(test2));
}