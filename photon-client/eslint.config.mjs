import pluginVue from "eslint-plugin-vue";
import { defineConfigWithVueTs, vueTsConfigs } from "@vue/eslint-config-typescript";

import skipFormattingConfig from "@vue/eslint-config-prettier/skip-formatting";

export default defineConfigWithVueTs(
  pluginVue.configs["flat/recommended"],
  vueTsConfigs.recommended,
  skipFormattingConfig,
  {
    ignores: ["**/dist/**", "playwright-report"]
  },
  {
    //extends: ["js/recommended"],
    rules: {
      quotes: ["error", "double"],
      "comma-dangle": ["error", "never"],

      "comma-spacing": [
        "error",
        {
          before: false,
          after: true
        }
      ],

      semi: ["error", "always"],
      "eol-last": "error",
      "object-curly-spacing": ["error", "always"],
      "quote-props": ["error", "as-needed"],
      "no-case-declarations": "off",
      "vue/require-default-prop": "off",
      "vue/v-on-event-hyphenation": "off",
      "vue/require-typed-ref": "error",
      "@typescript-eslint/no-empty-object-type": "error",
      "@typescript-eslint/no-explicit-any": "error",
      "vue/valid-v-slot": ["error", { allowModifiers: true }]
    }
  }
);
