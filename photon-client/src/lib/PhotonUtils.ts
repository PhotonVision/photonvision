import type { Resolution } from "@/types/SettingTypes";
import { PipelineType } from "@/types/PipelineTypes";

// Common RegEx used for naming both pipelines and cameras
export const nameChangeRegex = /^[A-Za-z0-9_ \-)(]*[A-Za-z0-9][A-Za-z0-9_ \-)(.]*$/;

export const resolutionsAreEqual = (a: Resolution, b: Resolution) => {
  return a.height === b.height && a.width === b.width;
};

export const getResolutionString = (resolution: Resolution): string => `${resolution.width}x${resolution.height}`;

export const parseJsonFile = async <T extends Record<string, any>>(file: File): Promise<T> => {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = (event) => {
      const target: FileReader | null = event.target;
      // eslint-disable-next-line prefer-promise-reject-errors
      if (target === null) reject();
      else resolve(JSON.parse(target.result as string) as T);
    };
    fileReader.onerror = (error) => reject(error);
    fileReader.readAsText(file);
  });
};

export const getCalImageUrl = (host: string, resolution: Resolution, idx: number, cameraIdx: number) => {
  const url = new URL(`http://${host}/api/utils/getCalSnapshot`);
  url.searchParams.set("width", Math.round(resolution.width).toFixed(0));
  url.searchParams.set("height", Math.round(resolution.height).toFixed(0));
  url.searchParams.set("snapshotIdx", Math.round(idx).toFixed(0));
  url.searchParams.set("cameraIdx", Math.round(cameraIdx).toFixed(0));

  return url.href;
};
export const getCalJSONUrl = (host: string, resolution: Resolution, cameraIdx: number) => {
  const url = new URL(`http://${host}/api/utils/getCalibrationJSON`);
  url.searchParams.set("width", Math.round(resolution.width).toFixed(0));
  url.searchParams.set("height", Math.round(resolution.height).toFixed(0));
  url.searchParams.set("cameraIdx", Math.round(cameraIdx).toFixed(0));

  return url.href;
};

export const pipelineTypeToString = (type: PipelineType): string => {
  switch (type) {
    case PipelineType.Calib3d:
      return "Calibration";
    case PipelineType.DriverMode:
      return "Driver Mode";
    case PipelineType.ColoredShape:
      return "Colored Shape";
    case PipelineType.Reflective:
      return "Reflective";
    case PipelineType.AprilTag:
      return "AprilTag";
    case PipelineType.Aruco:
      return "Aruco";
    case PipelineType.ObjectDetection:
      return "Object Detection";
  }
};
