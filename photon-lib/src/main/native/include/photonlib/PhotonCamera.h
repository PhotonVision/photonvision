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

#include <memory>
#include <string>

#include <networktables/NetworkTable.h>
#include <networktables/NetworkTableEntry.h>
#include <networktables/NetworkTableInstance.h>
#include <wpi/deprecated.h>

#include "photonlib/PhotonPipelineResult.h"

namespace photonlib {

enum LEDMode : int { kDefault = -1, kOff = 0, kOn = 1, kBlink = 2 };

/**
 * Represents a camera that is connected to PhotonVision.ß
 */
class PhotonCamera {
 public:
  /**
   * Constructs a PhotonCamera from a root table.
   * @param rootTable The root table that the camera is broadcasting information
   * over.
   */
  explicit PhotonCamera(std::shared_ptr<nt::NetworkTable> rootTable);

  /**
   * Constructs a PhotonCamera from the name of the camera.
   * @param cameraName The nickname of the camera (found in the PhotonVision
   * UI).
   */
  explicit PhotonCamera(const std::string& cameraName);

  /**
   * Returns the latest pipeline result.
   * @return The latest pipeline result.
   */
  PhotonPipelineResult GetLatestResult() const;

  /**
   * Toggles driver mode.
   * @param driverMode Whether to set driver mode.
   */
  void SetDriverMode(bool driverMode);

  /**
   * Returns whether the camera is in driver mode.
   * @return Whether the camera is in driver mode.
   */
  bool GetDriverMode() const;

  /**
   * Request the camera to save a new image file from the input
   * camera stream with overlays.
   * Images take up space in the filesystem of the PhotonCamera.
   * Calling it frequently will fill up disk space and eventually
   * cause the system to stop working.
   * Clear out images in /opt/photonvision/photonvision_config/imgSaves
   * frequently to prevent issues.
   */
  void TakeInputSnapshot(void);

  /**
   * Request the camera to save a new image file from the output
   * stream with overlays.
   * Images take up space in the filesystem of the PhotonCamera.
   * Calling it frequently will fill up disk space and eventually
   * cause the system to stop working.
   * Clear out images in /opt/photonvision/photonvision_config/imgSaves
   * frequently to prevent issues.
   */
  void TakeOutputSnapshot(void);

  /**
   * Allows the user to select the active pipeline index.
   * @param index The active pipeline index.
   */
  void SetPipelineIndex(int index);

  /**
   * Returns the active pipeline index.
   * @return The active pipeline index.
   */
  int GetPipelineIndex() const;

  /**
   * Returns the current LED mode.
   * @return The current LED mode.
   */
  LEDMode GetLEDMode() const;

  /**
   * Sets the LED mode.
   * @param led The mode to set to.
   */
  void SetLEDMode(LEDMode led);

  /**
   * Returns whether the latest target result has targets.
   * This method is deprecated; {@link PhotonPipelineResult#hasTargets()} should
   * be used instead.
   * @deprecated This method should be replaced with {@link
   * PhotonPipelineResult#HasTargets()}
   * @return Whether the latest target result has targets.
   */
  WPI_DEPRECATED(
      "This method should be replaced with PhotonPipelineResult::HasTargets()")
  bool HasTargets() const { return GetLatestResult().HasTargets(); }

 private:
  std::shared_ptr<nt::NetworkTable> mainTable =
      nt::NetworkTableInstance::GetDefault().GetTable("photonvision");

 protected:
  nt::NetworkTableEntry rawBytesEntry;
  nt::NetworkTableEntry driverModeEntry;
  nt::NetworkTableEntry inputSaveImgEntry;
  nt::NetworkTableEntry outputSaveImgEntry;
  nt::NetworkTableEntry pipelineIndexEntry;
  nt::NetworkTableEntry ledModeEntry;

  mutable Packet packet;
};

}  // namespace photonlib
