import { useStateStore } from "@/stores/StateStore";
import {
  CalibrationPaperTypes,
  CalibrationTagFamilies,
  type BoardObservation,
  type CameraCalibrationResult,
  type Resolution
} from "@/types/SettingTypes";
import axios, { type AxiosRequestConfig } from "axios";
import { length, type Length } from "@adam-rocska/units-and-measurement/length";

export const resolutionsAreEqual = (a: Resolution, b?: Resolution) => {
  return a.height === b?.height && a.width === b?.width;
};

export const meanReprojectionError = (obs: BoardObservation): number => {
  const used = obs.reprojectionErrors.filter((_, i) => obs.cornersUsed[i]);
  if (used.length === 0) return NaN;
  return used.reduce((sum, pt) => sum + Math.hypot(pt.x, pt.y), 0) / used.length;
};

export interface CalibrationSummaryStatistics {
  // Mean overall reprojection error in pixels, averaged over each observation's mean error
  mean: number;
  horizontalFOV: number;
  verticalFOV: number;
  diagonalFOV: number;
}

export const getCalibrationSummaryStatistics = (cal: CameraCalibrationResult): CalibrationSummaryStatistics => {
  const fx = cal.cameraIntrinsics.data[0];
  const fy = cal.cameraIntrinsics.data[4];
  const { width, height } = cal.resolution;

  const observationMeans = cal.meanErrors ?? cal.observations?.map(meanReprojectionError) ?? [];
  const mean = observationMeans.length ? observationMeans.reduce((a, b) => a + b, 0) / observationMeans.length : NaN;

  return {
    mean,
    horizontalFOV: (2 * Math.atan2(width / 2, fx) * 180) / Math.PI,
    verticalFOV: (2 * Math.atan2(height / 2, fy) * 180) / Math.PI,
    // Scales the vertical axis by the pixel aspect ratio (fx / fy) to handle non-square pixels
    diagonalFOV: (2 * Math.atan2(Math.sqrt(width ** 2 + (height / (fy / fx)) ** 2) / 2, fx) * 180) / Math.PI
  };
};

/**
 * Checks the status of the backend by polling the "/status" endpoint.
 *
 * This function will repeatedly attempt to send a GET request to the backend
 * until a successful response is received or the specified timeout is reached.
 *
 * @param timeout - The maximum time in milliseconds to wait for a successful response.
 * @param ip - Optional IP address of the backend server. If not provided, the default endpoint is used. This is meant for the case where the backend is running on a different IP than the frontend.
 * @returns A promise that resolves to a boolean indicating whether the backend is responsive (true) or not (false).
 */
export const statusCheck = async (timeout: number, ip?: string): Promise<boolean> => {
  // Poll the backend until it's responsive or we hit the timeout
  let pollLimit = Math.floor(timeout / 100);
  while (pollLimit > 0) {
    try {
      pollLimit--;
      await axios.get(ip ? `http://${ip}/api/status` : "/status");
      return true;
    } catch {
      // Backend not ready yet, wait and retry
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
  }

  return false;
};

/**
 * Forces a page reload after a brief delay and a status check.
 */
export const forceReloadPage = async () => {
  await new Promise((resolve) => setTimeout(resolve, 1000));

  useStateStore().showSnackbarMessage({
    message: "Reloading the page to apply changes...",
    color: "success"
  });

  await statusCheck(20000);

  window.location.reload();
};

export const getResolutionString = (resolution: Resolution): string => `${resolution.width}x${resolution.height}`;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const parseJsonFile = async <T extends Record<string, any>>(file: File): Promise<T> => {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = (event) => {
      const target: FileReader | null = event.target;
      if (target === null) reject(new Error("FileReader event target is null"));
      else resolve(JSON.parse(target.result as string) as T);
    };
    fileReader.onerror = () => reject(new Error("Error reading file"));
    fileReader.readAsText(file);
  });
};

/**
 * A helper function to make POST requests using axios with standardized success and error handling.
 *
 * @param url The endpoint URL to which the POST request is sent
 * @param description A brief description of the request for users, e.g., "import object detection models".
 * @param data Payload to be sent in the POST request
 * @param config Optional axios request configuration
 * @returns A promise that resolves to true if the POST request is successful, or false if an error occurs.
 */
export const axiosPost = async (
  url: string,
  description: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data?: any,
  config?: AxiosRequestConfig
): Promise<boolean> => {
  try {
    await axios.post(url, data, config);
    useStateStore().showSnackbarMessage({
      message: "Successfully dispatched the request to " + description + ". Waiting for backend to respond",
      color: "success"
    });
    return true;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (error: any) {
    if (error.response) {
      useStateStore().showSnackbarMessage({
        message: "The backend is unable to fulfill the request to " + description + ".",
        color: "error"
      });
    } else if (error.request) {
      useStateStore().showSnackbarMessage({
        message: "Error while trying to process the request to " + description + "! The backend didn't respond.",
        color: "error"
      });
    } else {
      useStateStore().showSnackbarMessage({
        message: "An error occurred while trying to process the request to " + description + ".",
        color: "error"
      });
    }
    return false;
  }
};

export const arucoTagFamilyNameFor = (tagFamily: CalibrationTagFamilies) => {
  switch (tagFamily) {
    case CalibrationTagFamilies.Dict_4X4_1000:
      return "ArUco 4x4 1000";
    case CalibrationTagFamilies.Dict_5X5_1000:
      return "ArUco 5x5 1000";
    case CalibrationTagFamilies.Dict_6X6_1000:
      return "ArUco 6x6 1000";
    case CalibrationTagFamilies.Dict_7X7_1000:
      return "ArUco 7x7 1000";
    default:
      return "ArUco Original";
  }
};

export const arucoTagDictionaryFor = async (tagFamily: CalibrationTagFamilies) => {
  switch (tagFamily) {
    case CalibrationTagFamilies.Dict_4X4_1000:
      const { ARUCO_4X4_1000 } = await import("aruco-marker/dictionaries/aruco_4x4_1000");
      return ARUCO_4X4_1000;
    case CalibrationTagFamilies.Dict_5X5_1000:
      const { ARUCO_5X5_1000 } = await import("aruco-marker/dictionaries/aruco_5x5_1000");
      return ARUCO_5X5_1000;
    case CalibrationTagFamilies.Dict_6X6_1000:
      const { ARUCO_6X6_1000 } = await import("aruco-marker/dictionaries/aruco_6x6_1000");
      return ARUCO_6X6_1000;
    case CalibrationTagFamilies.Dict_7X7_1000:
      const { ARUCO_7X7_1000 } = await import("aruco-marker/dictionaries/aruco_7x7_1000");
      return ARUCO_7X7_1000;
    default:
      return undefined;
  }
};

export const paperDimensionsFor = (paperType: CalibrationPaperTypes): [Length, Length] => {
  switch (paperType) {
    case CalibrationPaperTypes.Letter:
      return [length.in(8.5), length.in(11)];
    case CalibrationPaperTypes.Legal:
      return [length.in(8.5), length.in(14)];
    case CalibrationPaperTypes.Tabloid:
      return [length.in(11), length.in(17)];
    case CalibrationPaperTypes.A4:
      return [length.mm(210), length.mm(297)];
    case CalibrationPaperTypes.A3:
      return [length.mm(297), length.mm(420)];
    case CalibrationPaperTypes.A2:
      return [length.mm(420), length.mm(594)];
    default:
      return [length.mm(0), length.mm(0)];
  }
};
