import Vue from "vue";
import Vuetify from "vuetify";
import "vuetify/dist/vuetify.min.css";
import "@mdi/font/css/materialdesignicons.css";

Vue.use(Vuetify);

const theme = Object.freeze({
    primary: "#006492",
    secondary: "#39A4D5",
    accent: "#FFD843",
    background: "#232C37"
});

export default new Vuetify({
    theme: {
        themes: {
            light: theme,
            dark: theme
        }
    },
    breakpoint: {
        thresholds: {
            md: 1460,
            lg: 2000
        }
    }
});
