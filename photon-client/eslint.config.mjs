import pluginVue from "eslint-plugin-vue";
import { defineConfigWithVueTs, vueTsConfigs } from "@vue/eslint-config-typescript";

import skipFormattingConfig from "@vue/eslint-config-prettier/skip-formatting";

export default defineConfigWithVueTs(
  pluginVue.configs["flat/recommended-error"],
  vueTsConfigs.recommendedTypeChecked,
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
      eqeqeq: "error",
      "no-useless-concat": "error",
      "object-curly-spacing": ["error", "always"],
      "quote-props": ["error", "as-needed"],
      "no-case-declarations": "off",
      "vue/eqeqeq": "error",
      "vue/no-useless-concat": "error",
      "vue/no-constant-condition": "error",
      "vue/no-empty-pattern": "error",
      "vue/no-undef-directives": "error",
      "vue/no-undef-properties": "error",
      "vue/no-unused-properties": "error",
      "vue/no-unused-refs": "error",
      "vue/no-use-v-else-with-v-for": "error",
      "vue/no-useless-mustaches": "error",
      "vue/no-useless-v-bind": "error",
      "vue/prefer-use-template-ref": "error",
      "vue/require-default-prop": "off",
      "vue/require-typed-ref": "error",
      "vue/v-for-delimiter-style": "error",
      "vue/v-on-event-hyphenation": "off",
      "@typescript-eslint/no-empty-object-type": "error",
      "@typescript-eslint/no-explicit-any": "error",
      "vue/valid-v-slot": ["error", { allowModifiers: true }]
    }
  }
);
