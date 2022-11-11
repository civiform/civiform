module.exports = {
  testMatch: ['**/?(*.)+(spec|test).+(ts|js)'],
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
