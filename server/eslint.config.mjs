import noUnsanitized from "eslint-plugin-no-unsanitized";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import js from "@eslint/js";
import html from "@html-eslint/eslint-plugin";

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
      "config.d.ts"
    ],
  },
  {
    ...js.configs.recommended,
    files: ["**/*.{js,mjs,cjs,ts}"],
  },

  // Rules for all files
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

  // Rule overrides for unit test files
  {
    files: ["app/assets/javascripts/**/*.test.ts"],
    rules: {
      // It is common to need to set the innerHTML in typescript unit tests to
      // build html to test against. It is safe to ignore this rule in our sandbox test code.
      "no-unsanitized/property": "off",
    },
  },

  // Thymeleaf templates
  {
    files: ["app/views/**/*.html"],
    // Twirl templates embed Scala that the HTML parser can't read.
    ignores: ["app/views/**/*.scala.html"],
    ...html.configs.recommended,
    language: "html/html",
    languageOptions: {
      // Thymeleaf inlined expressions: [[...]] (escaped) and [(...)] (unescaped).
      templateEngineSyntax: {
        "[[": "]]",
        "[(": ")]",
      },
    },
    rules: {
      ...html.configs.recommended.rules,

      // Prettier formats these files (see formatter/fmt), so turn off the
      // stylistic rules that would fight it.
      "html/indent": "off",
      "html/attrs-newline": "off",
      "html/element-newline": "off",
      "html/no-extra-spacing-tags": "off",
      "html/quotes": "off",

      // Prettier writes void elements as `<meta />`. Empty Thymeleaf and
      // CiviForm custom tags (`<th:block />`, `<cf:icon />`) self-close too.
      "html/require-closing-tags": ["error", {
        selfClosing: "always",
        selfClosingCustomPatterns: ["^th:", "^cf:"],
      }],

      // Thymeleaf sets alt text through th:alt or th:attr.
      "html/require-img-alt": ["error", {
        substitute: ["th:alt", "th:attr", "th:alt-title"],
      }],

      // Layouts set these at render time (th:lang, th:text on <title>), which
      // the rules can't see.
      "html/require-lang": "off",
      "html/require-title": "off",

      // Ids repeated across mutually exclusive th:if/th:unless branches are
      // reported as duplicates.
      "html/no-duplicate-id": "off",

      // Fragments are linted in isolation, so an <li> fragment has no <ul>
      // parent until it is inserted into a page.
      "html/require-li-container": "off",
    },
  },
];
