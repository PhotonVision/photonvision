import { useStateStore } from "@/stores/StateStore";
import type { Resolution } from "@/types/SettingTypes";
import axios from "axios";

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
      await axios.get(ip ? `http://${ip}/status` : "/status");
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

export const parseJsonFile = async <T extends Record<string, any>>(file: File): Promise<T> => {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = (event) => {
      const target: FileReader | null = event.target;
      if (target === null) reject();
      else resolve(JSON.parse(target.result as string) as T);
    };
    fileReader.onerror = (error) => reject(error);
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
 * @returns A promise that resolves when the POST request is complete
 */
export const axiosPost = (url: string, description: string, data?: any, config?: any): Promise<void> => {
  return axios
    .post(url, data, config)
    .then(() => {
      useStateStore().showSnackbarMessage({
        message: "Successfully dispatched the request to " + description + ". Waiting for backend to respond",
        color: "success"
      });
    })
    .catch((error) => {
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
    });
};
