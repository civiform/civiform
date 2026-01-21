import typescriptEslint from "@typescript-eslint/eslint-plugin";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import js from "@eslint/js";
import google from "eslint-config-google";

export default [
    // Ignore patterns - must be first
    {
        ignores: [
            "eslint.config.mjs",
            "**/node_modules/**",
            "**/dist/**",
            "**/build/**",
            "**/coverage/**",
        ],
    },

    js.configs.recommended,
    google,

    {
        files: ["**/*.ts", "**/*.js", "**/*.mjs"],

        plugins: {
            "@typescript-eslint": typescriptEslint,
        },

        languageOptions: {
            globals: {
                ...globals.node,
            },
            parser: tsParser,
            ecmaVersion: "latest",
            sourceType: "module",

            parserOptions: {
                project: ["./tsconfig.json"],
            },
        },

        rules: {
            ...typescriptEslint.configs["recommended-type-checked"].rules,
            "max-len": ["error", { code: 100 }],
            "require-jsdoc": "off",
            "valid-jsdoc": "off",
            "no-unused-vars": "off",
            "@typescript-eslint/no-unused-vars": "error",
            "@typescript-eslint/unbound-method": ["error", {
                ignoreStatic: true,
            }],
            "@typescript-eslint/no-unsafe-enum-comparison": "off",
            "@typescript-eslint/no-non-null-assertion": "off",

            // Disabled for now - can re-enable when cleaning up types
            "@typescript-eslint/require-await": "off",
            "@typescript-eslint/no-unsafe-assignment": "off",
            "@typescript-eslint/no-explicit-any": "off",
            "@typescript-eslint/restrict-template-expressions": "off",
            "@typescript-eslint/no-unsafe-member-access": "off",
            "@typescript-eslint/no-unsafe-argument": "off",
        },
    },
];