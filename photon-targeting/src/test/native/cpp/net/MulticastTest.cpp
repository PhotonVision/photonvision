// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

#include <wpinet/MulticastServiceAnnouncer.h>
#include <wpinet/MulticastServiceResolver.h>

#include <array>
#include <chrono>
#include <string>
#include <string_view>
#include <thread>
#include <utility>

#include <gtest/gtest.h>
#include <wpi/timestamp.h>

TEST(MulticastServiceAnnouncerTest, EmptyText) {
  const std::string_view serviceName = "TestServiceNoText";
  const std::string_view serviceType = "_wpinotxt";
  const int port = std::rand();
  wpi::MulticastServiceAnnouncer announcer(serviceName, serviceType, port);
  wpi::MulticastServiceResolver resolver(serviceType);

  if (announcer.HasImplementation() && resolver.HasImplementation()) {
    announcer.Start();
    resolver.Start();

    std::this_thread::sleep_for(std::chrono::seconds(5));

    resolver.Stop();
    announcer.Stop();
  }
}
