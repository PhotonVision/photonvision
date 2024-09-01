import "@mdi/font/css/materialdesignicons.css";
import "vuetify/styles";

import { createVuetify, type ThemeDefinition } from "vuetify";

const dataColors = {
  error: "#FF5252",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107"
};

const PhotonVisionClassicTheme: ThemeDefinition = {
  dark: false,
  colors: {
    background: "#232C37",
    primary: "#006492",
    surface: "#006492",
    secondary: "#39A4D5",
    "surface-variant": "#39A4D5",
    accent: "#FFD843",
    "surface-light": "#FFD843",
    ...dataColors
  },
  variables: {
    icon: "mdi-controller-classic"
  }
};

const PhotonVisionDarkTheme: ThemeDefinition = {
  dark: true,
  colors: {
    primary: "#006492",
    secondary: "#39A4D5",
    accent: "#FFD843",
    ...dataColors
  },
  variables: {
    icon: "mdi-controller"
  }
};

export default createVuetify({
  theme: {
    defaultTheme: "PhotonVisionClassicTheme",
    themes: {
      PhotonVisionClassicTheme,
      PhotonVisionDarkTheme
    }
  },
  display: {
    mobileBreakpoint: "sm",
    thresholds: {
      xs: 0,
      sm: 600,
      md: 1400,
      lg: 2000,
      xl: 2560,
      xxl: 3200
    }
  }
});
