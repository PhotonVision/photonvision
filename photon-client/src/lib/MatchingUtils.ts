import { PVCameraInfo, type PVCSICameraInfo, type PVFileCameraInfo, type PVUsbCameraInfo } from "@/types/SettingTypes";

/**
 * Check if two cameras match by comparing properties.
 * For USB cameras, it checks the name, vendorId, productId, and uniquePath.
 * For CSI cameras, it checks the uniquePath and baseName.
 * For file cameras, it checks the uniquePath and name.
 */
export const camerasMatch = (camera1: PVCameraInfo, camera2: PVCameraInfo) => {
  if (camera1.PVUsbCameraInfo && camera2.PVUsbCameraInfo)
    return (
      camera1.PVUsbCameraInfo.name === camera2.PVUsbCameraInfo.name &&
      camera1.PVUsbCameraInfo.vendorId === camera2.PVUsbCameraInfo.vendorId &&
      camera1.PVUsbCameraInfo.productId === camera2.PVUsbCameraInfo.productId &&
      camera1.PVUsbCameraInfo.uniquePath === camera2.PVUsbCameraInfo.uniquePath
    );
  else if (camera1.PVCSICameraInfo && camera2.PVCSICameraInfo)
    return (
      camera1.PVCSICameraInfo.uniquePath === camera2.PVCSICameraInfo.uniquePath &&
      camera1.PVCSICameraInfo.baseName === camera2.PVCSICameraInfo.baseName
    );
  else if (camera1.PVFileCameraInfo && camera2.PVFileCameraInfo)
    return (
      camera1.PVFileCameraInfo.uniquePath === camera2.PVFileCameraInfo.uniquePath &&
      camera1.PVFileCameraInfo.name === camera2.PVFileCameraInfo.name
    );
  else return false;
};

/**
 * Get the connection-type-specific camera info from the given PVCameraInfo object.
 */
export const cameraInfoFor = (
  camera: PVCameraInfo | null
): PVUsbCameraInfo | PVCSICameraInfo | PVFileCameraInfo | any => {
  if (!camera) return null;
  if (camera.PVUsbCameraInfo) {
    return camera.PVUsbCameraInfo;
  }
  if (camera.PVCSICameraInfo) {
    return camera.PVCSICameraInfo;
  }
  if (camera.PVFileCameraInfo) {
    return camera.PVFileCameraInfo;
  }
  return {};
};

/**
 * Find the PVCameraInfo currently occupying the same uniquePath as the the given module
 */
export const getMatchedDevice = (allDevices: PVCameraInfo[], info: PVCameraInfo | undefined): PVCameraInfo => {
  if (!info) {
    return {
      PVFileCameraInfo: undefined,
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    };
  }
  return (
    allDevices.find((it) => cameraInfoFor(it).uniquePath === cameraInfoFor(info).uniquePath) || {
      PVFileCameraInfo: undefined,
      PVCSICameraInfo: undefined,
      PVUsbCameraInfo: undefined
    }
  );
};
