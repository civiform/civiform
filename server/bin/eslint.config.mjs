import noUnsanitized from "eslint-plugin-no-unsanitized";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import js from "@eslint/js";

export default [
  // Ignore patterns - must be first
  {
    ignores: [
      "app/assets/javascripts/**/*.js",
      "app/assets/dist/**",
      "target/**",
      "dist/**",
      "build/**",
      "public/**",
      "node_modules/**",
      "eslint.config.mjs",
      "vitest.setup.ts",
      "postcss.config.js",
      "tailwind.config.js",
      "config.d.ts"
    ],
  },
  js.configs.recommended,

  {
    files: ["app/assets/javascripts/**/*.ts"],

    plugins: {
      "@typescript-eslint": typescriptEslint,
      "no-unsanitized": noUnsanitized,
    },

    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.commonjs,
        "window.htmx": "writable",
      },
      parser: tsParser,
      ecmaVersion: 2022,
      sourceType: "module",

      parserOptions: {
        project: ["./tsconfig.json"],
      },
    },

    rules: {
      // TODO(#3361): enable "@typescript-eslint/strict-type-checked" rules
      ...typescriptEslint.configs["recommended-type-checked"].rules,
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
  },
];
