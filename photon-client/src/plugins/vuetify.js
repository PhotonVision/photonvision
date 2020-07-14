import '@mdi/font/css/materialdesignicons.css';
import 'material-design-icons-iconfont/dist/material-design-icons.css'
import Vue from 'vue';
import Vuetify from 'vuetify/lib';
import theme from "../theme";

Vue.use(Vuetify, {});

// Although you *can* set up theming here, it's so frequently inappropriate that we do it in the markup
export default new Vuetify({
    theme: {
        themes: {
            light: theme,
            dark: theme,
        }
    },
    breakpoint: {
        thresholds: {
            md: 1460,
            lg: 2000,
        },
    }
});
