import { defineConfig } from "eslint/config";
import globals from "globals";

export default defineConfig([{
    languageOptions: {
        globals: {
            ...globals.node,
        },

        ecmaVersion: 2020,
        sourceType: "module",
    },

    rules: {
        "guard-for-in": "off",
        "valid-jsdoc": "off",
        "@typescript-eslint/no-non-null-assertion": "off",
    },
}]);