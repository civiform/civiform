import { defineConfig } from "eslint/config";
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
    },
    
    extends: [
        "js/recommended",
        // TODO(#3361): enable "typescriptEslint/strict-type-checked"
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
            project: ["./tsconfig.json"],
        },
    },

    rules: {
        "require-jsdoc": "off",
        // valid-jsdoc is deprecated. If we want JSdoc checks we should use
        // https://github.com/gajus/eslint-plugin-jsdoc with TS-specific settings.
        "valid-jsdoc": "off",
        // default JS no-unused-vars causes false-positives on TS code. For example, enums.
        "no-unused-vars": "off",
        "@typescript-eslint/no-unused-vars": "error",
        "@typescript-eslint/no-require-imports": "off",
        "@typescript-eslint/unbound-method": ["error", {
            ignoreStatic: true,
        }],
        // See TypeScript best practices in
        // https://github.com/civiform/civiform/wiki/Development-standards#typescript-code
        "@typescript-eslint/no-non-null-assertion": "off",
        "@typescript-eslint/no-unsafe-enum-comparison": "off",
        // Add method that we use to sanitize our HTML
        "no-unsanitized/property": ["error", {
            escape: {
                methods: ["DOMPurify.sanitize"],
            },
        }],
    },

    ignores: [
        "app/assets/javascripts/**/*.js",
        "target/**",
        "dist/**",
        "build/**",
        "public/**",
        "node_modules/**",
        "eslint.config.mjs",
        "vitest.setup.ts",
        "postcss.config.js",
        "tailwind.config.js",
        "vite.config.ts"
    ]
}]);
