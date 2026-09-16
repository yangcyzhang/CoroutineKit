# Contributing to CoroutineKit

First off, thank you for considering contributing! 🎉

---

## 🛠 Getting Started

1. **Fork** the repository and clone it locally.
2. Create a new branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-description
   ```
3. Make your changes and write tests.
4. Run tests to ensure everything passes:
   ```bash
   ./gradlew :coroutinekit:test
   ```
5. Submit a **Pull Request** against the `main` branch.

---

## 📐 Branch Naming Convention

| Type | Pattern | Example |
|---|---|---|
| New feature | `feature/xxx` | `feature/add-debounce-operator` |
| Bug fix | `fix/xxx` | `fix/cancel-exception-swallowed` |
| Docs | `docs/xxx` | `docs/update-readme-interop` |
| Refactor | `refactor/xxx` | `refactor/retry-policy-sealed` |

---

## 🎨 Code Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Run `./gradlew ktlintCheck` (if configured) before committing.
- All **public APIs must have KDoc** comments.
- Use meaningful names — avoid abbreviations.

---

## 🧪 Writing Tests

- All new features **must** include unit tests.
- Use `runTest { }` from `kotlinx-coroutines-test` for suspend functions and Flow.
- Use `advanceTimeBy()` / `TestCoroutineScheduler` for time-sensitive tests — avoid `Thread.sleep`.
- Test method names should use **backtick syntax** and be descriptive:
  ```kotlin
  @Test
  fun `suspendRunCatching does not swallow CancellationException`() = runTest { ... }
  ```

---

## 🚫 What NOT to do

- **Do not** catch `CancellationException` and swallow it silently.
- **Do not** use `GlobalScope` in library code unless explicitly providing Java-interop bridges.
- **Do not** add heavy dependencies — keep the library lightweight.

---

## 📬 Pull Request Checklist

- [ ] Code follows the Kotlin style guide
- [ ] Tests are included and all pass
- [ ] KDoc added to all public APIs
- [ ] README updated if API surface changed
- [ ] No unnecessary dependencies added

---

## Code of Conduct

Be respectful, inclusive, and constructive. We follow the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).
