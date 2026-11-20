export type ThemeName = "LightTheme" | "DarkTheme";

export type ThemeBase = {
  background: string;
  surface: string;
  primary: string;
  secondary: string;
};

const LightThemeDefaults: ThemeBase = {
  background: "#232C37",
  surface: "#006492",
  primary: "#FFD843",
  secondary: "#39A4D5"
};

const DarkThemeDefaults: ThemeBase = {
  background: "#151515",
  surface: "#1c232c",
  primary: "#39A4D5",
  secondary: "#FFD843"
};

export const themeDefaults: Record<ThemeName, ThemeBase> = {
  LightTheme: LightThemeDefaults,
  DarkTheme: DarkThemeDefaults
};

const cssVarMap: Record<keyof ThemeBase, string> = {
  background: "--pv-background",
  surface: "--pv-surface",
  primary: "--pv-primary",
  secondary: "--pv-secondary"
};

const colorVarMap: Record<keyof ThemeBase, string> = {
  background: "--color-pv-background",
  surface: "--color-pv-surface",
  primary: "--color-pv-primary",
  secondary: "--color-pv-secondary"
};

const getStoredThemeName = (): ThemeName => {
  const stored = localStorage.getItem("theme");
  return stored === "DarkTheme" ? "DarkTheme" : "LightTheme";
};

const getThemeType = (themeName: ThemeName) => (themeName === "DarkTheme" ? "dark" : "light");

const getThemeColorValue = (themeName: ThemeName, key: keyof ThemeBase): string => {
  const themeType = getThemeType(themeName);
  return localStorage.getItem(`${themeType}-${key}`) ?? themeDefaults[themeName][key];
};

const applyTheme = (themeName: ThemeName) => {
  if (typeof document === "undefined") return;

  const root = document.documentElement;
  root.dataset.theme = themeName === "DarkTheme" ? "dark" : "light";

  (Object.keys(cssVarMap) as Array<keyof ThemeBase>).forEach((key) => {
    const value = getThemeColorValue(themeName, key);
    root.style.setProperty(cssVarMap[key], value);
    root.style.setProperty(colorVarMap[key], value);
  });
};

export const resetTheme = () => {
  const themeName = getStoredThemeName();
  const themeType = getThemeType(themeName);

  (Object.keys(themeDefaults[themeName]) as Array<keyof ThemeBase>).forEach((key) => {
    localStorage.removeItem(`${themeType}-${key}`);
  });

  applyTheme(themeName);
};

export const getThemeColor = (color: keyof ThemeBase): string => {
  return getThemeColorValue(getStoredThemeName(), color);
};

export const setThemeColor = (color: keyof ThemeBase, value: string | null) => {
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
