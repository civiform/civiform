module.exports = {
  preset: 'ts-jest',
  testMatch: ['**/__tests__/**/*.+(ts|js)', '**/?(*.)+(spec|test).+(ts|js)'],
  transform: {
    '^.+\\.(ts)$': [
      'ts-jest',
      {
        tsconfig: 'src/tsconfig.json',
      },
    ],
  },
  globalSetup: './src/delete_database.ts',
  setupFilesAfterEnv: ['./src/support/setup-jest.ts'],
}
