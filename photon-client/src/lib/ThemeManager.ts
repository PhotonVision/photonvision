type ThemeName = "LightTheme" | "DarkTheme";

type ThemeColors = {
  background: string;
  onBackground: string;
  surface: string;
  surfaceVariant: string;
  onSurface: string;
  primary: string;
  secondary: string;
  accent: string;
  error: string;
  info: string;
  success: string;
  warning: string;
  buttonActive: string;
  buttonPassive: string;
  logsBackground: string;
  sidebar: string;
};

const LightThemeDefaults: ThemeColors = {
  background: "#232C37",
  onBackground: "#ffffff",
  surface: "#006492",
  surfaceVariant: "#8f8f8f",
  onSurface: "#f0f0f0",
  primary: "#FFD843",
  secondary: "#39A4D5",
  accent: "#FFD843",
  error: "#b80000",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
  buttonActive: "#FFD843",
  buttonPassive: "#39A4D5",
  logsBackground: "#232C37",
  sidebar: "#006492"
};

const DarkThemeDefaults: ThemeColors = {
  background: "#151515",
  onBackground: "#f0f0f0",
  surface: "#1c232c",
  surfaceVariant: "#485b70",
  onSurface: "#f0f0f0",
  primary: "#39A4D5",
  secondary: "#FFD843",
  accent: "#006492",
  error: "#ff2e2e",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107",
  buttonActive: "#FFD843",
  buttonPassive: "#39A4D5",
  logsBackground: "#151515",
  sidebar: "#151515"
};

const themeDefaults: Record<ThemeName, ThemeColors> = {
  LightTheme: LightThemeDefaults,
  DarkTheme: DarkThemeDefaults
};

const cssVarMap: Record<keyof ThemeColors, string> = {
  background: "--pv-background",
  onBackground: "--pv-on-background",
  surface: "--pv-surface",
  surfaceVariant: "--pv-surface-variant",
  onSurface: "--pv-on-surface",
  primary: "--pv-primary",
  secondary: "--pv-secondary",
  accent: "--pv-accent",
  error: "--pv-error",
  info: "--pv-info",
  success: "--pv-success",
  warning: "--pv-warning",
  buttonActive: "--pv-button-active",
  buttonPassive: "--pv-button-passive",
  logsBackground: "--pv-logs-background",
  sidebar: "--pv-sidebar"
};

const colorVarMap: Record<keyof ThemeColors, string> = {
  background: "--color-pv-background",
  onBackground: "--color-pv-on-background",
  surface: "--color-pv-surface",
  surfaceVariant: "--color-pv-surface-variant",
  onSurface: "--color-pv-on-surface",
  primary: "--color-pv-primary",
  secondary: "--color-pv-secondary",
  accent: "--color-pv-accent",
  error: "--color-pv-error",
  info: "--color-pv-info",
  success: "--color-pv-success",
  warning: "--color-pv-warning",
  buttonActive: "--color-pv-button-active",
  buttonPassive: "--color-pv-button-passive",
  logsBackground: "--color-pv-logs-background",
  sidebar: "--color-pv-sidebar"
};

const getStoredThemeName = (): ThemeName => {
  const stored = localStorage.getItem("theme");
  return stored === "DarkTheme" ? "DarkTheme" : "LightTheme";
};

const getThemeType = (themeName: ThemeName) => (themeName === "DarkTheme" ? "dark" : "light");

const getThemeColorValue = (themeName: ThemeName, key: keyof ThemeColors): string => {
  const themeType = getThemeType(themeName);
  return localStorage.getItem(`${themeType}-${key}`) ?? themeDefaults[themeName][key];
};

const applyTheme = (themeName: ThemeName) => {
  if (typeof document === "undefined") return;

  const root = document.documentElement;
  root.dataset.theme = themeName === "DarkTheme" ? "dark" : "light";

  (Object.keys(cssVarMap) as Array<keyof ThemeColors>).forEach((key) => {
    const value = getThemeColorValue(themeName, key);
    root.style.setProperty(cssVarMap[key], value);
    root.style.setProperty(colorVarMap[key], value);
  });
};

export const resetTheme = () => {
  const themeName = getStoredThemeName();
  const themeType = getThemeType(themeName);

  (Object.keys(themeDefaults[themeName]) as Array<keyof ThemeColors>).forEach((key) => {
    localStorage.removeItem(`${themeType}-${key}`);
  });

  applyTheme(themeName);
};

export const getThemeColor = (color: keyof ThemeColors): string => {
  return getThemeColorValue(getStoredThemeName(), color);
};

export const setThemeColor = (color: keyof ThemeColors, value: string | null) => {
  const themeName = getStoredThemeName();
  const themeType = getThemeType(themeName);

  if (value) localStorage.setItem(`${themeType}-${color}`, value);
  else localStorage.removeItem(`${themeType}-${color}`);

  applyTheme(themeName);
};

export const toggleTheme = () => {
  const nextTheme = getStoredThemeName() === "LightTheme" ? "DarkTheme" : "LightTheme";
  localStorage.setItem("theme", nextTheme);
  applyTheme(nextTheme);
};

export const restoreThemeConfig = () => {
  applyTheme(getStoredThemeName());
};
