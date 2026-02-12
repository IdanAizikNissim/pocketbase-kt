# PocketBase Kotlin SDK

Kotlin SDK for interacting with the [PocketBase Web API](https://pocketbase.io/docs).

- [PocketBase Kotlin SDK](#pocketbase-kotlin-sdk)
  - [Installation](#installation)
  - [Compatibility](#compatibility)
  - [CI sanity checks](#ci-sanity-checks)
  - [Caveats](#caveats)
    - [File upload](#file-upload)
    - [RecordModel](#recordmodel)
    - [Error handling](#error-handling)
    - [AuthStore](#authstore)
    - [Binding filter parameters](#binding-filter-parameters)
  - [Services](#services)
    - [RecordService](#recordservice)
      - [_Crud handlers_](#crud-handlers)
      - [_Realtime handlers_](#realtime-handlers)
      - [_Auth handlers_](#auth-handlers)
    - [CollectionService](#collectionservice)
    - [LogService](#logservice)
    - [BackupService](#backupservice)
    - [CronService](#cronservice)
    - [FileService](#fileservice)
    - [BatchService](#batchservice)
    - [SettingsService](#settingsservice)
    - [HealthService](#healthservice)

## Installation

To add this SDK to your Kotlin Multiplatform project:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.pocketbase:pocketbase:0.2.0")
            }
        }
    }
}
```

```kotlin
val pb = PocketBase("http://127.0.0.1:8090")

// authenticate as regular user
val userData = pb.collection<User>("users").authWithPassword(
  usernameOrEmail = "test@example.com",
  password = "123456",
)

// list and filter "example" collection records
val result = pb.collection<Example>("example").getList(
  page = 1,
  perPage = 20,
  filter = "status = true && created >= \"2022-08-01\"",
  sort = "-created",
  expand = "someRelField",
)

// subscribe to realtime "example" collection changes
pb.collection<Example>("example").subscribe(
  topic = "*",
  filter = "someField > 10",
  callback = { e ->
    print(e.action) // create, update, delete
    print(e.record) // the changed record
  },
)

// and much more...
```

> More detailed API docs and copy-paste examples could be found in the [API documentation for each service](https://pocketbase.io/docs/api-authentication)
> or in the [Services section](#services) below.

## Compatibility

- PocketBase server: `v0.36.2`
- Kotlin SDK: `io.pocketbase:pocketbase:0.2.0`

## CI sanity checks

The repository workflow `.github/workflows/sanity.yml` runs on pull requests and:

- downloads and starts PocketBase `v0.36.2`
- seeds required test collections
- runs format checks
- runs service coverage tests:
  - `TestHealthService`
  - `TestSettingsService`
  - `TestFileService`
  - `TestServiceParity`
  - `TestBatchRealtimeCoverage`
  - `TestRecordServiceCoverage`
  - `TestAdminServicesCoverage`

### Swift Package Manager (iOS binary)

This repository publishes an iOS `XCFramework` binary for SwiftPM via GitHub releases.

Add the dependency to your `Package.swift`:

```swift
dependencies: [
    .package(
        url: "https://github.com/IdanAizikNissim/pocketbase-kt.git",
        from: "0.2.0"
    )
]
```

Then add the product dependency:

```swift
.product(name: "PocketBase", package: "pocketbase-kt")
```

#### Local SwiftPM testing (`swiftApp`)

From the repository root, build the local XCFramework:

```bash
./scripts/build-local-spm.sh
```

Then in Xcode:

1. Open `swiftApp/swiftApp.xcodeproj`.
2. Go to `File` -> `Add Package Dependencies...`.
3. Click `Add Local...` and choose this repository root (`pocketbase-kt`).
4. Add product `PocketBase` to the `swiftApp` target.

When `pocketbase/build/XCFrameworks/release/PocketBase.xcframework` exists, `Package.swift` automatically uses it as a local `binaryTarget`.

## Caveats

#### File upload

PocketBase Kotlin SDK handles file upload seamlessly by using ktor [submitFormWithBinaryData](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.request.forms/submit-form-with-binary-data.html)

Here is a simple example of uploading a single text file together with some other regular fields:

```kotlin
val pb = PocketBase("http://127.0.0.1:8090")

val record = pb.collection<Example>("example").create(
  body = Example(
    title = "Hello world!",
    // ... any other regular field
  ),
  files = listOf(
    File(
      field = "document", // the name of the file field
      fileName = "example_document.txt",
      data = "example content...".toByteArray(),
    ),
  ),
)

print(record.document) // "example_document_$id.txt"
```

#### RecordModel

The SDK comes with several helpers to make it easier working with the `RecordService` and `RecordModel` DTO.
Leverage Kotlin's type system for safer API interactions.
below is an example how to access and cast record data values:

```kotlin
@Serializable
data class Example(
  val options: List<String>,
  val email: String,
  val status: Boolean,
  val total: Int,
  val price: Double,
  val nested: String = "missing",
)

val record = pb.collection<Example>("example").getOne("RECORD_ID")
```

#### Error handling

All service methods are suspend functions, so use Kotlin coroutines for async operations:

```kotlin
// Using try/catch with coroutines
try {
    val result = pb.collection<Example>("example").getList(page = 1, perPage = 50)
    println("Result: $result")
} catch (e: ClientException) {
    println("Error: ${e.message}")
}
```

All response errors are normalized and wrapped as `ClientException` with the following public members:

```kotlin
data class ClientException(
    val statusCode: Int = 0,       // The status code of the failed request
    val url: String = "",          // The address of the failed request
    val data: JsonObject? = null,  // The JSON API error response
    val originError: String? = null // The original response error
)
```

#### AuthStore

The SDK keeps track of the authenticated token and auth model for you via the `pb.authStore` service.
The default `AuthStore` class has the following public members:

```kotlin
open class AuthStore {
    var model: Model?                   // The stored auth RecordModel
    var token: String                   // The stored auth token
    val isValid: Boolean                // Checks if store has an unexpired JWT token
    val onChange: Flow<AuthStoreEvent>  // Flow triggered on each auth store change

    open fun save(newToken: String, newModel: Model)  // Update the store with new auth data
    open fun clear()                                  // Clear the current auth store state
}
```

To _"logout"_ an authenticated record or admin, you can just call `pb.authStore.clear()`.

To _"listen"_ for changes in the auth store, you can _listen_ to the `onChange` collect flow:

```kotlin
pb.authStore.onChange.collect { e ->
  print(e.token)
  print(e.model)
}
```

**The default `AuthStore` is not persistent!**

If you want to persist the `AuthStore` state (eg. in case the app get closed), you can extend the default store and pass a new custom instance as constructor argument to the client.
To make it slightly easier, the SDK has a builtin `AsyncAuthStore` that you can combine with any async persistent layer (`multiplatform-settings`, `DataStore`, local file, etc.).
Here is an example using [`multiplatform-settings`](https://github.com/russhwolf/multiplatform-settings):

```kotlin
@Serializable
data class User(
    val username: String,
    val name: String,
) : RecordModel()

class MultiplatformSettingsAuthStore(
    private val settings: Settings,
) {
    val token: String?
        get() = settings.getStringOrNull(TOKEN_KEY)

    fun save(token: String?) {
        settings[TOKEN_KEY] = token ?: ""
    }

    private companion object {
        const val TOKEN_KEY = "token"
    }
}

val mpSettingsAuthStore = MultiplatformSettingsAuthStore(
    settings = get(), // expect val settings: Settings
)

val store = RecordAsyncAuthStore(
    cls = User::class,
    save = mpSettingsAuthStore::save,
    clear = { mpSettingsAuthStore.save(null) },
    initial = mpSettingsAuthStore.token,
)

val pb = PocketBase("http://example.com", authStore = store)
```

#### Binding filter parameters

The SDK comes with a helper `pb.filter(expr, params)` method to generate a filter string with placeholder parameters (`{:paramName}`) populated from a `Map`.

```kotlin
val records = pb.collection<Example>("example").getList(filter = pb.filter(
  // the same as: "title ~ 'exa\\'mple' && created = '2023-10-18 18:20:00.123Z'"
  expr = "title ~ {:title} && created >= {:created}",
  query = mapOf("title" to "exa'mple", "created" to Clock.System.now()),
))
```

The supported placeholder parameter values are:

- `String` (_single quotes are autoescaped_)
- `Instant` (kotlinx.datetime)
- `Boolean`
- `Number`
- `null`
- everything else is converted to a string using `json.encodeToString()`

## Services

`PocketBase` now exposes:

- `pb.collection<T>()` (RecordService)
- `pb.files`
- `pb.createBatch()`
- `pb.settings`
- `pb.collections`
- `pb.logs`
- `pb.backups`
- `pb.crons`
- `pb.health` (alias: `pb.healthCheck`)

#### RecordService

###### _Crud handlers_

```kotlin
pb.collection<T>("example").getList(...)
pb.collection<T>("example").getFullList(...)
pb.collection<T>("example").getFirstListItem(...)
pb.collection<T>("example").getOne(id = "RECORD_ID", ...)
pb.collection<T>("example").create(body = model, files = listOf(...), ...)
pb.collection<T>("example").update(id = "RECORD_ID", body = model, files = listOf(...), ...)
pb.collection<T>("example").delete(id = "RECORD_ID", ...)
```

###### _Realtime handlers_

```kotlin
pb.collection<T>("example").subscribe(
    topic = "*", // "*" or recordId
    callback = { event -> println(event.action) },
    expand = null,
    filter = null,
    fields = null,
    query = emptyMap(),
    headers = emptyMap(),
)

pb.collection<T>("example").unsubscribe(topic = "")
```

###### _Auth handlers_ (auth collections)

```kotlin
pb.collection<User>("users").listAuthMethods(...)
pb.collection<User>("users").authWithPassword(...)
pb.collection<User>("users").authWithOAuth2Code(...)
pb.collection<User>("users").authRefresh(...)
pb.collection<User>("users").requestPasswordReset(email = "user@example.com", ...)
pb.collection<User>("users").confirmPasswordReset(...)
pb.collection<User>("users").requestEmailChange(...)
pb.collection<User>("users").confirmEmailChange(...)
pb.collection<User>("users").requestVerification(...)
pb.collection<User>("users").confirmVerification(...)
pb.collection<User>("users").requestOTP(...)
pb.collection<User>("users").authWithOTP(...)
pb.collection<User>("users").listExternalAuths(recordId = "RECORD_ID", ...)
pb.collection<User>("users").unlinkExternalAuth(recordId = "RECORD_ID", provider = "google", ...)
pb.collection<User>("users").impersonate(id = "RECORD_ID", duration = 120L, ...)
```

---

#### CollectionService

```kotlin
pb.collections.getList(...)
pb.collections.getFullList(...)
pb.collections.getFirstListItem(filter = "name = 'users'", ...)
pb.collections.getOne(id = "COLLECTION_ID", ...)
pb.collections.create(body = collectionModel, ...)
pb.collections.update(id = "COLLECTION_ID", body = collectionModel, ...)
pb.collections.delete(id = "COLLECTION_ID", ...)
pb.collections.import(collections = listOf(...), deleteMissing = false, ...)
pb.collections.getScaffolds(...)
pb.collections.truncate(collectionIdOrName = "example", ...)
```

---

#### LogService

```kotlin
pb.logs.getList(...)
pb.logs.getOne(id = "LOG_ID", ...)
pb.logs.getStats(...)
```

---

#### BackupService

```kotlin
pb.backups.getFullList(...)
pb.backups.create(basename = "daily-backup", ...)
pb.backups.upload(files = listOf(File(field = "file", fileName = "backup.zip", data = bytes)), ...)
pb.backups.delete(key = "backup.zip", ...)
pb.backups.restore(key = "backup.zip", ...)
pb.backups.getDownloadURL(token = "TOKEN", key = "backup.zip")
```

---

#### CronService

```kotlin
pb.crons.getFullList(...)
pb.crons.run(jobId = "JOB_ID", ...)
```

---

#### FileService

```kotlin
pb.files.getURL(
    record = recordModel,
    fileName = "image.jpg",
    thumb = null,
    token = null,
    download = null,
    query = emptyMap(),
)

pb.files.getToken(
    body = null,
    query = emptyMap(),
    headers = emptyMap(),
)
```

---

#### BatchService

> Execute multiple operations in a single request.

```kotlin
val batch = pb.createBatch()

batch.collection<Example>("example").create(body = Example(title = "Test"))
batch.collection<Example>("example").update(recordId = "RECORD_ID", body = Example(title = "Updated"))
batch.collection<Example>("example").delete(recordId = "RECORD_ID")

val results: List<BatchResult> = batch.send()
```

---

#### SettingsService

```kotlin
pb.settings.getAll()
pb.settings.update(settings = settingsModel)
pb.settings.testS3(filesystem = "storage")
pb.settings.testEmail(email = "user@example.com", template = "verification")
pb.settings.testEmail(email = "user@example.com", template = "verification", collection = "users")
pb.settings.generateAppleClientSecret(body = request)
```

#### HealthService

```kotlin
pb.health.check(query = emptyMap(), headers = emptyMap())
// alias:
pb.healthCheck.check()
```

---

<img src="pocketbase_kt.jpg" width="300">
