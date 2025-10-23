import { useStateStore } from "@/stores/StateStore";
import type { Resolution } from "@/types/SettingTypes";
import axios from "axios";

export const resolutionsAreEqual = (a: Resolution, b: Resolution) => {
  return a.height === b.height && a.width === b.width;
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
