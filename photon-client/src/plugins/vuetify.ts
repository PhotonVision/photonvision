import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import type { ThemeDefinition } from "vuetify/lib/composables/theme";
import { createVuetify } from "vuetify";

const commonColors = { error: "#b80000", info: "#2196F3", success: "#4CAF50", warning: "#FFC107" };

const ThemeColors = {
  photonBlue: "#006492",
  photonYellow: "#FFD843",
  lightBlue: "#39A4D5",
  darkGray: "#151515",
  gray: "#1c232c",
  lightGray: "#232C37"
};


const DarkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    background: ThemeColors.darkGray,
    surface: ThemeColors.gray,
    sidebar: ThemeColors.darkGray,

    primary: ThemeColors.lightBlue,
    secondary: ThemeColors.photonYellow,
    accent: ThemeColors.lightBlue,


    // Card/info background
    "surface-variant": "#485b70",
    "on-surface-variant": "#f0f0f0",


    toggle: "#006492",

    // Misc/mismatches between themes

    ...{ error: "#9b0000", info: "#2196F3", success: "#4CAF50", warning: "#FFC107" } //b80000
  }
};

const LightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    background: ThemeColors.lightGray,
    surface: ThemeColors.photonBlue,
    sidebar: ThemeColors.photonBlue,

    primary: ThemeColors.photonBlue,
    secondary: ThemeColors.lightBlue,
    accent: ThemeColors.photonYellow,

    "surface-variant": "#358AB0",
    "surface-light": ThemeColors.photonYellow,
    toggle: "#39A4D5",

    ...commonColors
  },
  variables: { "medium-emphasis-opacity": 1, "high-emphasis-opacity": 1 }
};

export default createVuetify({
  theme: { defaultTheme: "DarkTheme", themes: { LightTheme: LightTheme, DarkTheme: DarkTheme } },
  display: { thresholds: { md: 1460, lg: 2000 } }
});

// const LightTheme: ThemeDefinition = {
//   dark: false,
//   colors: {
//     background: "#232C37",
//     primary: "#006492",
//     surface: "#006492",
//     secondary: "#39A4D5",
//     "surface-variant": "#358AB0",
//     accent: "#FFD843",
//     "surface-light": "#FFD843",
//     toggle: "#39A4D5",

//     // Misc/mismatches between themes
//     sidebar: "#006492",

//     ...commonColors
//   },
//   variables: { "medium-emphasis-opacity": 1, "high-emphasis-opacity": 1 }
// };
