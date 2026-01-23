import "vuetify/styles";
import("@mdi/font/css/materialdesignicons.css");
import type { ThemeDefinition } from "vuetify/lib/composables/theme";
import { createVuetify } from "vuetify";

const CommonColors = {
  photonBlue: "#006492",
  photonYellow: "#FFD843",
  lightBlue: "#39A4D5",
  darkGray: "#151515",
  gray: "#1c232c",
  lightGray: "#232C37"
};

export const DarkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    background: CommonColors.darkGray,
    sidebar: CommonColors.darkGray,

    surface: CommonColors.gray,
    primary: CommonColors.lightBlue,
    secondary: CommonColors.photonYellow,
    accent: CommonColors.photonBlue,

    toggle: CommonColors.photonBlue,
    logsBackground: CommonColors.darkGray,

    buttonActive: CommonColors.photonYellow,
    buttonPassive: CommonColors.lightBlue,

    "surface-variant": "#485b70",
    "on-surface-variant": "#f0f0f0",

    error: "#ff2e2e",
    info: "#2196F3",
    success: "#4CAF50",
    warning: "#FFC107"
  }
};

export const LightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    background: CommonColors.lightGray,
    sidebar: CommonColors.photonBlue,

    surface: CommonColors.photonBlue,
    primary: CommonColors.photonYellow,
    secondary: CommonColors.lightBlue,
    accent: CommonColors.photonYellow,

    toggle: CommonColors.lightBlue,
    logsBackground: CommonColors.lightGray,

    buttonActive: CommonColors.photonYellow,
    buttonPassive: CommonColors.lightBlue,

    "surface-variant": "#8f8f8fff",

    error: "#b80000",
    info: "#2196F3",
    success: "#4CAF50",
    warning: "#FFC107"
  },
  variables: { "medium-emphasis-opacity": 1, "high-emphasis-opacity": 1 }
};

export default createVuetify({
  theme: { defaultTheme: "LightTheme", themes: { LightTheme: LightTheme, DarkTheme: DarkTheme } },
  display: { thresholds: { md: 1460, lg: 2000 } }
});
