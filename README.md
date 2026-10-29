# CoroutineKit

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.yangcyzhang/coroutinekit.svg)](https://search.maven.org/artifact/com.yangcyzhang/coroutinekit)

> **Production-grade Kotlin coroutine utilities for Android & JVM.**
> Write safer, cleaner, and more testable asynchronous code — without reinventing the wheel.

---

## ✨ Features

| Module | What it gives you |
|---|---|
| `FlowExt` | `throttleFirst`, `retryWithDelay`, `onEachCatching` and more Flow operators |
| `SuspendExt` | `suspendRunCatching` (CancellationException-safe), `withTimeoutOrDefault`, `launchCatching` |
| `ManagedScope` | Lifecycle-aware `CoroutineScope` with `SupervisorJob` and `Closeable` |
| `RetryPolicy` | Fixed, Exponential Backoff, and Immediate retry strategies |
| `JavaInterop` | `CompletableFuture` bridge + universal Callback → Flow adapter |
| `DispatcherUtils` | Centralized, test-mockable dispatcher provider |

---

## 📦 Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.yangcyzhang:coroutinekit:0.1.0")
}
```

Make sure `mavenCentral()` is in your repository list.

---

## 🚀 Quick Start

### 1. Safe suspend calls — `suspendRunCatching`

```kotlin
// Never accidentally swallows CancellationException
val result: Result<User> = suspendRunCatching {
    userRepository.fetchUser(id)
}
result.onSuccess { user -> render(user) }
      .onFailure { e -> showError(e) }
```

### 2. Timeout with fallback — `withTimeoutOrDefault`

```kotlin
val data = withTimeoutOrDefault(timeMillis = 3_000, defaultValue = emptyList()) {
    networkService.fetchFeed()
}
```

### 3. Flow — `throttleFirst` (great for UI click events)

```kotlin
buttonClickFlow
    .throttleFirst(windowDuration = 500)
    .onEach { handleClick() }
    .launchIn(viewModelScope)
```

### 4. Flow — `retryWithDelay` (exponential backoff built-in)

```kotlin
apiFlow
    .retryWithDelay(times = 3, initialDelay = 200, factor = 2.0)
    .collect { result -> process(result) }
```

### 5. `ManagedScope` — lifecycle-safe background scope

```kotlin
class MyRepository : Closeable {
    private val scope = ManagedScope(Dispatchers.IO)

    fun startSync() = scope.launch {
        while (isActive) {
            sync()
            delay(30_000)
        }
    }

    override fun close() = scope.close() // cancels everything safely
}
```

### 6. Retry Policies

```kotlin
// Exponential backoff: 100ms → 200ms → 400ms → ...
val result = withRetry(
    policy = RetryPolicy.ExponentialBackoff(times = 4, initialDelayMs = 100)
) { attempt ->
    println("Attempt $attempt")
    apiCall()
}
```

### 7. Java Interop — `CompletableFuture` bridge

```kotlin
// Expose a suspend function to Java callers
val future: CompletableFuture<User> = suspendToFuture {
    userRepository.fetchUser(id)
}
```

### 8. Java Interop — Callback → Flow

```kotlin
val sensorFlow: Flow<SensorData> = callbackToFlow(
    register = { onResult, onError ->
        sensorManager.register(object : SensorListener {
            override fun onData(d: SensorData) = onResult(d)
            override fun onError(e: Throwable) = onError(e)
        })
    },
    unregister = { sensorManager.unregister() }
)
```

---

## 🧪 Running Tests

```bash
./gradlew :coroutinekit:test
```

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

---

## 📄 License

```
MIT License — Copyright (c) 2026 yangcyzhang
```

See [LICENSE](LICENSE) for full text.
