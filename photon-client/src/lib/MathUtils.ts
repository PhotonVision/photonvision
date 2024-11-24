export const mean = (values: number[]): number | undefined => {
  if (values.length === 0) return undefined;
  return values.reduce((acc, num) => acc + num, 0) / values.length;
};

export const angleModulus = (valueRad: number): number => {
  while (valueRad < -Math.PI) valueRad += Math.PI * 2;
  while (valueRad > Math.PI) valueRad -= Math.PI * 2;
  return valueRad;
};

export const toDeg = (val: number) => val * (180.0 / Math.PI);
export const toRad = (val: number) => val * (Math.PI / 180.0);
