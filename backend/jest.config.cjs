const commonProjectConfig = {
  preset: "ts-jest",
  testEnvironment: "node",
  moduleFileExtensions: ["ts", "js", "json"],
  transform: {
    "^.+\\.ts$": [
      "ts-jest",
      {
        tsconfig: "<rootDir>/tsconfig.test.json",
      },
    ],
  },
  clearMocks: true,
  testPathIgnorePatterns: ["/node_modules/", "/dist/"],
};

module.exports = {
  projects: [
    {
      ...commonProjectConfig,
      displayName: "unit",
      testMatch: ["<rootDir>/tests/unit/**/*.test.ts"],
    },
    {
      ...commonProjectConfig,
      displayName: "integration",
      testMatch: ["<rootDir>/tests/integration/**/*.test.ts"],
      maxWorkers: 1,
      testTimeout: 30000,
    },
  ],
};
