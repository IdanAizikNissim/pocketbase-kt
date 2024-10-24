# PocketBase Kotlin SDK

Kotlin SDK for interacting with the [PocketBase Web API](https://pocketbase.io/docs).

- [PocketBase Kotlin SDK](#pocketbase-kotlin-sdk)
  - [Installation](#installation)
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
    - [FileService](#fileservice)
    - [AdminService](#adminservice)
    - [SettingsService](#settingsservice)
    - [RealtimeService](#realtimeservice)
    - [HealthService](#healthservice)

## Installation

To add this SDK to your Kotlin Multiplatform project:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.pocketbase:pocketbase:0.1.3")
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
  expand: "someRelField",
);

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

## Caveats

#### File upload

PocketBase Kotlin SDK handles file upload seamlessly by using ktor [submitFormWithBinaryData](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.request.forms/submit-form-with-binary-data.html)

Here is a simple example of uploading a single text file together with some other regular fields:

```kotlin


val pb = PocketBase("http://127.0.0.1:8090")

val record = pb.collection<Example>("Example").create(
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

val record = pb.collection<Example>('example').getOne("RECORD_ID")
```

#### Error handling

All services return a standard Future-based response, so the error handling is straightforward:

```kotlin
pb.collection('example').getList(page: 1, perPage: 50).then((result) {
  // success...
  print('Result: $result');
}).catchError((error) {
  // error...
  print('Error: $error');
});

// OR if you are using the async/await syntax:
try {
  final result = await pb.collection('example').getList(page: 1, perPage: 50);
} catch (error) {
  print('Error: $error');
}
```

All response errors are normalized and wrapped as `ClientException` with the following public members that you could use:

```kotlin
data class ClientException(
  val url:           String      // The address of the failed request
  val statusCode:    Int         // The status code of the failed request
  val data:          JsonObject  // The JSON API error response
  val originError:   String      // The original response error
)
```

#### AuthStore

The SDK keeps track of the authenticated token and auth model for you via the `pb.authStore` service.
The default `AuthStore` class has the following public members that you could use:

```kotlin
AuthStore {
  token:    String                       // Getter for the stored auth token
  model:    RecordModel|AdminModel|null  // Getter for the stored auth RecordModel or AdminModel
  isValid   Boolean                      // Getter to loosely check if the store has an existing and unexpired token
  onChange  Flow<AuthStoreEvent>         // Flow that gets triggered on each auth store change

  // methods
  save(token, model)                     // update the store with the new auth data
  clear()                                // clears the current auth store state
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
  val verified: Boolean,
): RecordModel()

class MultiplatformSettingsAuthStore(
  private val settings: Settings,
) {
  val token: String?
    get() = settings.getStringOrNull(TOKEN_KEY)

  fun save(token: String?) {
    settings[TOKEN_KEY] = token
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
));
```

The supported placeholder parameter values are:

- `String` (_single quotes are autoescaped_)
- `Instant` (kotlinx.datetime)
- `Boolean`
- `Number`
- `null`
- everything else is converted to a string using `json.encodeToString()`

## Services

#### RecordService

###### _Crud handlers_

```kotlin
// Returns a paginated records list.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).getList(
  page: Int = 1,
  perPage: Int = 30,
  skipTotal: Boolean = false,
  expand: String? = null,
  filter: String? = null,
  sort: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns a list with all records batch fetched at once.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).getFullList(
  batch: Int = 500,
  expand: String? = null,
  filter: String? = null,
  sort: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns the first found record matching the specified filter.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).getFirstListItem(
  filter: String,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns a single record by its id.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).getOne(
  id: String,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Creates (aka. register) a new record.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).create(
  body: T? = null,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
  files: List<File> = emptyList(),
)

// Updates an existing record by its id.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).update(
  id: String,
  body: T? = null,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Deletes a single record by its id.
🔓 pb.collection<T: RecordModel>(collectionIdOrName).delete(
  id: String,
  body: T? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)
```

###### _Realtime handlers_

```kotlin
// Subscribe to realtime changes to the specified topic ("*" or recordId).
//
// It is safe to subscribe multiple times to the same topic.
//
// You can use the returned UnsubscribeFunc to remove a single registered subscription.
// If you want to remove all subscriptions related to the topic use unsubscribe(topic).
🔓 pb.collection<T>(collectionIdOrName)subscribe(
  topic: String,
  callback: (event: RecordSubscriptionEvent<T>) -> Unit,
  expand: String? = null,
  filter: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Unsubscribe from all registered subscriptions to the specified topic ("*" or recordId).
// If topic is not set, then it will remove all registered collection subscriptions.
🔓 pb.collection<T>(collectionIdOrName).unsubscribe(topic: String = "")
```

###### _Auth handlers_

> Available only for "auth" type collections.

```kotlin
// Returns all available application auth methods.
🔓 pb.collection(collectionIdOrName).listAuthMethods(
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Authenticates a record with their username/email and password.
🔓 pb.collection(collectionIdOrName).authWithPassword(
  usernameOrEmail: String,
  password: String,
  expand: String? = null,
  fields: String? = null,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Refreshes the current authenticated record model and auth token.
🔐 pb.collection(collectionIdOrName).authRefresh(
  expand: String? = null,
  fields: String? = null,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Sends a user password reset email.
🔓 pb.collection(collectionIdOrName).requestPasswordReset(
  email: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Confirms a record password reset request.
🔓 pb.collection(collectionIdOrName).confirmPasswordReset(
  passwordResetToken: String,
  password: String,
  passwordConfirm: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Sends a record verification email request.
🔓 pb.collection(collectionIdOrName).requestVerification(
  email: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Confirms a record email verification request.
🔓 pb.collection(collectionIdOrName).confirmVerification(
  verificationToken: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)
```

---

##### FileService

```kotlin
// Builds and returns an absolute record file url for the provided filename.
🔓 pb.files.getUrlgetUrl(
  record: RecordModel,
  fileName: String,
  thumb: String? = null,
  token: String? = null,
  download: Boolean? = null,
  query: Map<String, Any?> = emptyMap(),
)

// Requests a new private file access token for the current auth model (admin or record).
🔐 pb.files.getToken(
  body: @Serializable Any? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)
```

---

#### AdminService

```kotlin
// Authenticates an admin account by its email and password.
🔓 pb.admins.authWithPassword(
  email: String,
  password: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Refreshes the current admin authenticated model and token.
🔐 pb.admins.authRefresh(
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Sends an admin password reset email.
🔓 pb.admins.requestPasswordReset(
  email: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Confirms an admin password reset request.
🔓 pb.admins.confirmPasswordReset(
  passwordResetToken: String,
  password: String,
  passwordConfirm: String,
  body: Map<String, Any?> = emptyMap(),
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns a paginated admins list.
🔐 pb.admins.getList(
  page: Int = 1,
  perPage: Int = 30,
  skipTotal: Boolean = false,
  expand: String? = null,
  filter: String? = null,
  sort: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns a list with all admins batch fetched at once.
🔐 pb.admins.getFullList(
  batch: Int = 500,
  expand: String? = null,
  filter: String? = null,
  sort: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns the first found admin matching the specified filter.
🔐 pb.admins.getFirstListItem(
  filter: String,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Returns a single admin by their id.
🔐 pb.admins.getOne(
  id: String,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Creates a new admin.
🔐 pb.admins.create(
  body: T? = null,
  expand: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
  files: List<File> = emptyList(),
)

// Updates an existing admin by their id.
🔐 pb.admins.update(
  id: String,
  body: AdminModel?,
  expand: String?,
  fields: String?,
  query: Map<String, Any?>,
  headers: Map<String, String>,
)

// Deletes a single admin by their id.
🔐 pb.admins.delete(
  id: String,
  body: AdminModel?,
  query: Map<String, Any?>,
  headers: Map<String, String>,
)
```

---

#### SettingsService

```kotlin
// Returns a map with all available app settings.
🔐 pb.settings.getAll()

// Bulk updates app settings.
🔐 pb.settings.update(settings: Settings): Settings

// Performs a S3 storage connection test.
🔐 pb.settings.testS3(filesystem: String)

// Sends a test email (verification, password-reset, email-change).
🔐 pb.settings.testEmail(
  email: String,
  template: String,
);

// Generates a new Apple OAuth2 client secret.
🔐 pb.settings.generateAppleClientSecret(body: GenerateAppleClientSecretRequest): String
```

---

#### RealtimeService

> This service is usually used with custom realtime actions.
> For records realtime subscriptions you can use the subscribe/unsubscribe
> methods available in the `pb.collection()` RecordService.

```kotlin
// Initialize the realtime connection (if not already) and register the subscription.
//
// You can subscribe to the `PB_CONNECT` event if you want to listen to the realtime connection connect/reconnect events.
🔓 pb.realtime.subscribe(
  topic: String,
  listener: SubscriptionFunc,
  expand: String? = null,
  filter: String? = null,
  fields: String? = null,
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)

// Unsubscribe from a subscription (if empty - unsubscibe from all registered subscriptions).
🔓 pb.realtime.unsubscribe(topic: String = "")

// Unsubscribe from all subscriptions starting with the provided prefix.
🔓 pb.realtime.unsubscribeByPrefix(topicPrefix: String)
```

---

##### HealthService

```kotlin
// Checks the health status of the api.
🔓 pb.healthCheck.checkcheck(
  query: Map<String, Any?> = emptyMap(),
  headers: Map<String, String> = emptyMap(),
)
```

---

<img src="pocketbase_kt.jpg" width="300">
