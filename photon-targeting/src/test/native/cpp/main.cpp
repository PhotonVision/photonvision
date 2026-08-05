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

#include <string_view>

#include <catch2/catch_session.hpp>
#include <catch2/catch_test_macros.hpp>
#include <wpi/hal/HAL.h>

namespace {

bool IsCatchListCommand(int argc, char** argv) {
  for (int i = 1; i < argc; ++i) {
    std::string_view arg{argv[i]};
    if (arg == "--list-tests" || arg == "--list-tags" ||
        arg == "--list-reporters" || arg == "--list-listeners") {
      return true;
    }
  }
  return false;
}

}  // namespace

int main(int argc, char** argv) {
  if (!IsCatchListCommand(argc, argv)) {
    HAL_Initialize();
  }
  int ret = Catch::Session().run(argc, argv);
  if (!IsCatchListCommand(argc, argv)) {
    HAL_Shutdown();
  }
  return ret;
}
