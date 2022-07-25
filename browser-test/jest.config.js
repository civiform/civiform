module.exports = {
  preset: 'jest-playwright-preset',
  testMatch: ['**/__tests__/**/*.+(ts|js)', '**/?(*.)+(spec|test).+(ts|js)'],
  transform: {
    '^.+\\.(ts)$': 'ts-jest',
  },
  globalSetup: './src/delete_database.ts',
  globals: {
    'ts-jest': {
      tsconfig: 'src/tsconfig.json',
    },
  },
}
