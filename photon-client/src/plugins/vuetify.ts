import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import type { ThemeDefinition } from "vuetify/lib/composables/theme";
import { createVuetify } from "vuetify";

const commonColors = { error: "#b80000", info: "#2196F3", success: "#4CAF50", warning: "#FFC107" };

const DarkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    // Blue primary
    primary: "#39A4D5", // 006492

    // Yellow secondary
    secondary: "#FFD843",

    // Near-black background
    background: "#151515", // 232C37 (old) 121212 (very dark)
    "on-background": "",

    // Card/info background
    surface: "#232C37",
    "surface-variant": "#485b70",
    "on-surface-variant": "#f0f0f0",

    // Lighter blue
    accent: "#39A4D5",

    toggle: "#006492",

    // Misc/mismatches between themes
    sidebar: "#151515",

    ...{ error: "#b80000", info: "#2196F3", success: "#4CAF50", warning: "#FFC107" }
  }
};

const LightTheme: ThemeDefinition = {
  dark: false,
  colors: {
    background: "#232C37",
    primary: "#006492",
    surface: "#006492",
    secondary: "#39A4D5",
    "surface-variant": "#358AB0",
    accent: "#FFD843",
    "surface-light": "#FFD843",
    toggle: "#39A4D5",

    // Misc/mismatches between themes
    sidebar: "#006492",

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
