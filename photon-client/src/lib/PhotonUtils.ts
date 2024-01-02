import type { Resolution } from "@/types/SettingTypes";

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
