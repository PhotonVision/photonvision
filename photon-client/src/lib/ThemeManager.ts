import { type ThemeInstance } from "vuetify";
import { LightTheme, DarkTheme } from "@/plugins/vuetify";

export const resetTheme = (theme: ThemeInstance) => {
  const themeType = theme.global.name.value === "LightTheme" ? "light" : "dark";
  localStorage.removeItem(`${themeType}-background`);
  localStorage.removeItem(`${themeType}-primary`);
  localStorage.removeItem(`${themeType}-secondary`);
  localStorage.removeItem(`${themeType}-accent`);

  restoreThemeConfig(theme);
};

export const getThemeColor = (theme: ThemeInstance, color: string): string => {
  const themeType = theme.global.name.value === "LightTheme" ? "light" : "dark";
  const defaultTheme = theme.global.name.value === "LightTheme" ? LightTheme : DarkTheme;
  return localStorage.getItem(`${themeType}-${color}`) ?? defaultTheme.colors![color]!;
};

export const setThemeColor = (theme: ThemeInstance, color: string, value: string | null) => {
  const themeType = theme.global.name.value === "LightTheme" ? "light" : "dark";
  if (value) localStorage.setItem(`${themeType}-${color}`, value);
  else localStorage.removeItem(`${themeType}-${color}`);

  restoreThemeConfig(theme);
};

export const toggleTheme = (theme: ThemeInstance) => {
  const currentTheme = localStorage.getItem("theme");
  localStorage.setItem("theme", currentTheme === "LightTheme" ? "DarkTheme" : "LightTheme");

  restoreThemeConfig(theme);
};

export const restoreThemeConfig = (theme: ThemeInstance) => {
  // Restore theme preference
  const storedTheme = localStorage.getItem("theme");
  if (storedTheme) theme.global.name.value = storedTheme;

  // Restore custom theme colors
  const themeType = theme.global.name.value === "LightTheme" ? "light" : "dark";
  const defaultTheme = theme.global.name.value === "LightTheme" ? LightTheme : DarkTheme;

  const customBackground = localStorage.getItem(`${themeType}-background`);
  const customPrimary = localStorage.getItem(`${themeType}-primary`);
  const customSecondary = localStorage.getItem(`${themeType}-secondary`);
  const customAccent = localStorage.getItem(`${themeType}-accent`);

  theme.themes.value[theme.global.name.value].colors.background = customBackground ?? defaultTheme.colors!.background!;
  theme.themes.value[theme.global.name.value].colors.sidebar = customBackground ?? defaultTheme.colors!.sidebar!;

  theme.themes.value[theme.global.name.value].colors.primary = customPrimary ?? defaultTheme.colors!.primary!;
  theme.themes.value[theme.global.name.value].colors.buttonActive = customPrimary ?? defaultTheme.colors!.buttonActive!;

  theme.themes.value[theme.global.name.value].colors.secondary = customSecondary ?? defaultTheme.colors!.secondary!;
  theme.themes.value[theme.global.name.value].colors.buttonPassive =
    customSecondary ?? defaultTheme.colors!.buttonPassive!;

  theme.themes.value[theme.global.name.value].colors.accent = customAccent ?? defaultTheme.colors!.accent!;
  theme.themes.value[theme.global.name.value].colors.toggle = customAccent ?? defaultTheme.colors!.toggle!;
  console.log("restored theme:", storedTheme);
};
