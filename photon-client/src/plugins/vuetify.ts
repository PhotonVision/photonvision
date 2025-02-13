import Vue from "vue";
import Vuetify from "vuetify";
import "vuetify/dist/vuetify.min.css";
import "@mdi/font/css/materialdesignicons.css";
import type { VuetifyThemeVariant } from "vuetify/types/services/theme";

Vue.use(Vuetify);

const darkTheme: VuetifyThemeVariant = Object.freeze({
  primary: "#006492",
  secondary: "#39A4D5",
  accent: "#FFD843",
  background: "#232C37",
  error: "#b80000",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107"
});

const lightTheme: VuetifyThemeVariant = Object.freeze({
  primary: "#006492",
  secondary: "#39A4D5",
  accent: "#FFD843",
  background: "#232C37",
  error: "#b80000",
  info: "#2196F3",
  success: "#4CAF50",
  warning: "#FFC107"
});

export default new Vuetify({
  theme: {
    themes: {
      light: lightTheme,
      dark: darkTheme
    }
  },
  breakpoint: {
    thresholds: {
      md: 1460,
      lg: 2000
    }
  }
});
