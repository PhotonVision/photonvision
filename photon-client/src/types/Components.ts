export interface DropdownSelectItem<V> {
  name: string | number;
  value: V;
  disabled?: boolean;
}

export interface RadioItem<T> {
  name: string;
  value: T;
  tooltip?: string;
  disabled?: boolean;
}

export type ValidationRule = (value: any) => string | boolean;

export interface RGBColor {
  r: number;
  g: number;
  b: number;
}
