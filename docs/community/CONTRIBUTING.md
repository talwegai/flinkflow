# Contributing to Flinkflow

First off, thank you for considering contributing to Flinkflow! It's people like you that make Flinkflow such a great tool for the streaming community.

## ⚖️ Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). We strive to maintain a welcoming, safe, and inclusive environment.

## 🚀 How to Contribute

There are many ways to contribute to Flinkflow, beyond just writing code:

*   **Report Bugs**: Found something that isn't working? [Open an issue](https://github.com/talweg/flinkflow/issues).
*   **Suggest Features**: Have an idea for a new Flowlet or a DSL improvement? Let us know!
*   **Improve Documentation**: Spot a typo or a confusing explanation? PRs for docs are always welcome.
*   **Write Code**: Pick an issue labelled `good first issue` and start hacking.

---

## 🛠️ Development Workflow

### 1. Fork and Clone
Fork the repository on GitHub and clone it to your local machine.

```bash
git clone https://github.com/YOUR_USERNAME/flinkflow.git
cd flinkflow
```

### 2. Set Up Your Environment
Flinkflow is a multi-language project:
*   **Java**: Requires JDK 17 and Maven 3.8+.
*   **Python**: Requires Python 3.9+ (for testing and some board components).
*   **Kubernetes**: Local tests use `kind` or `minikube` for CRD validation.

```bash
mvn clean install -DskipTests
```

### 3. Create a Branch
Always create a new branch for your work. Use a descriptive name like `feature/new-connector` or `fix/yaml-parser`.

```bash
git checkout -b feature/my-cool-feature
```

### 4. Coding Standards

#### **Java**
*   Follow the standard Java naming conventions.
*   Include **Apache 2.0 License Headers** at the top of every new file.
*   Write unit tests for new logic (we use JUnit 5 and AssertJ).

#### **YAML DSL**
*   Ensure that any changes to the pipeline schema are reflected in the `docs/GUIDE_CONFIGURATION.md`.
*   Validate your changes by adding an example to `examples/standalone/`.

### 5. Running Tests
Before submitting, ensure all tests pass:

```bash
# Run Maven tests
mvn test

# Run Smoke tests (validates all YAML examples)
./deploy/scripts/smoke-test.sh
```

### 6. Submit a Pull Request
*   Push your branch to your fork.
*   Open a Pull Request against the `main` branch of `talweg/flinkflow`.
*   Provide a clear description of the changes and link any relevant issues.

---

## 📜 Licensing

By contributing to Flinkflow, you agree that your contributions will be licensed under the **Apache License, Version 2.0**.

---

**Happy Streaming!** 🌊
