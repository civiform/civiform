import { defineConfig } from "eslint/config";
import playwright from 'eslint-plugin-playwright';
import noUnsanitized from "eslint-plugin-no-unsanitized";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import js from "@eslint/js";
import prettier from "eslint-config-prettier";
import google from "eslint-config-google";

export default defineConfig([{

    plugins: {
        js,
        noUnsanitized,
        typescriptEslint, 
        playwright,
    },

    extends: [
        "js/recommended",
        "typescriptEslint/recommended-type-checked",
        google,
        prettier,
        "noUnsanitized/recommended",
    ],

    languageOptions: {
        globals: {
                ...globals.browser,
                ...globals.commonjs,
                "window.htmx": "writable",
            },
        parser: tsParser,
        ecmaVersion: 5,
        sourceType: "script",

        parserOptions: {
            project: ["./src/tsconfig.json"],
        },
    },

    rules: {
        ...playwright.configs['flat/recommended'].rules,
        "require-jsdoc": "off",
        "valid-jsdoc": "off",
        "no-unused-vars": "off",
        "@typescript-eslint/no-unused-vars": "error",
        "@typescript-eslint/unbound-method": ["error", {
            ignoreStatic: true,
        }],
        "@typescript-eslint/no-unsafe-enum-comparison": "off",
        "@typescript-eslint/no-non-null-assertion": "off",
        // Browser tests probably won't directly set `.innerHTML`, but if they
        // do have a reason to it's fine
        "no-unsanitized/method": "off",
        "no-unsanitized/property": "off",
        // Playwright specific: off until we can clean it all up
        "playwright/expect-expect": "off",
        "playwright/no-conditional-expect": "off",
        "playwright/no-conditional-in-test": "off",
        // We shouldn't go crazy with nesting test.step, but it's a hard no
        // on enforcing it. There are times when it makes complex tests more
        // easier to read.
        "playwright/no-nested-step": "off",
        "playwright/no-skipped-test": "off",
        "playwright/no-wait-for-selector": "off"
    },

    ignores: [
        "tmp/**",
        "eslint.config.mjs"
    ]   
 }]);
