module.exports = {
  preset: 'ts-jest',
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
  setupFilesAfterEnv: ['./src/support/setup-jest.ts'],
}
