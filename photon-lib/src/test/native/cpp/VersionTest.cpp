#include "gtest/gtest.h"
#include "PhotonVersion.h"
#include <iostream>

TEST(VersionTest, PrintVersion) {
    std::cout << photonlib::PhotonVersion::versionString << std::endl; 
}