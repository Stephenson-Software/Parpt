# Contributing to Parpt
Thank you for your interest in contributing to Parpt!

This project aims to help developers and creators focus their time on high-impact work through clear project prioritization.

We welcome all kinds of contributions, whether you're fixing a bug, writing documentation, adding a new feature or providing feedback.

## Getting Started
1. Fork the repository
2. Set up the project

## Running Tests
To run tests locally:
```bash
./gradlew test
```

To run the full build (including tests):
```bash
./gradlew build
```

## Continuous Integration
This project uses GitHub Actions for continuous integration. All pull requests and pushes to main/develop branches will automatically:
- Build the project with Java 21
- Run all unit tests (currently 61 tests)
- Generate test reports and artifacts
- Validate the Gradle wrapper

The CI pipeline ensures code quality by:
- ✅ Running all unit tests including markdown export functionality
- ✅ Compiling both main and test code
- ✅ Generating test coverage reports
- ✅ Uploading build artifacts

Make sure all tests pass locally before submitting a pull request. The CI checks must pass before merging.

## License
By contributing, you agree that your contributions will be licensed under the MIT License.
