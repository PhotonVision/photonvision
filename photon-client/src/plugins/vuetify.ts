import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import type { ThemeDefinition } from "vuetify/lib/composables/theme";
import { createVuetify } from "vuetify";

const commonColors = {
  error: "#b80000",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107"
};

const DarkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    primary: "#006492",
    secondary: "#39A4D5",
    accent: "#FFD843",
    background: "#232C37",
    ...commonColors
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
    ...commonColors
  },
  variables: {
    "medium-emphasis-opacity": 1,
    "high-emphasis-opacity": 1
  }
};

export default createVuetify({
  theme: {
    defaultTheme: "LightTheme",
    themes: {
      LightTheme: LightTheme,
      DarkTheme: DarkTheme
    }
  },
  display: {
    thresholds: {
      md: 1460,
      lg: 2000
    }
  }
});
