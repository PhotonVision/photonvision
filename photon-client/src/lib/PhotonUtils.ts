import { useStateStore } from "@/stores/StateStore";
import { CalibrationPaperTypes, CalibrationTagFamilies, type Resolution } from "@/types/SettingTypes";
import { ARUCO_4X4_1000 } from "aruco-marker/dictionaries/aruco_4x4_1000";
import { ARUCO_5X5_1000 } from "aruco-marker/dictionaries/aruco_5x5_1000";
import { ARUCO_6X6_1000 } from "aruco-marker/dictionaries/aruco_6x6_1000";
import { ARUCO_7X7_1000 } from "aruco-marker/dictionaries/aruco_7x7_1000";
import axios, { type AxiosRequestConfig } from "axios";
import { unit } from "mathjs";

export const resolutionsAreEqual = (a: Resolution, b: Resolution) => {
  return a.height === b.height && a.width === b.width;
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

export const arucoTagDictionaryFor = (tagFamily: CalibrationTagFamilies) => {
  switch (tagFamily) {
    case CalibrationTagFamilies.Dict_4X4_1000:
      return ARUCO_4X4_1000;
    case CalibrationTagFamilies.Dict_5X5_1000:
      return ARUCO_5X5_1000;
    case CalibrationTagFamilies.Dict_6X6_1000:
      return ARUCO_6X6_1000;
    case CalibrationTagFamilies.Dict_7X7_1000:
      return ARUCO_7X7_1000;
    default:
      return undefined;
  }
};

export const paperDimensionsFor = (paperType: CalibrationPaperTypes) => {
  switch (paperType) {
    case CalibrationPaperTypes.Letter:
      return [unit(8.5, "in"), unit(11, "in")];
    case CalibrationPaperTypes.Legal:
      return [unit(8.5, "in"), unit(14, "in")];
    case CalibrationPaperTypes.Tabloid:
      return [unit(11, "in"), unit(17, "in")];
    case CalibrationPaperTypes.A4:
      return [unit(210, "mm"), unit(297, "mm")];
    case CalibrationPaperTypes.A3:
      return [unit(297, "mm"), unit(420, "mm")];
    case CalibrationPaperTypes.A2:
      return [unit(420, "mm"), unit(594, "mm")];
    default:
      return [unit(0, "mm"), unit(0, "mm")];
  }
};
