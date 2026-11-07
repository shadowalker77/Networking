[![](https://jitpack.io/v/AyanTech/Networking.svg)](https://jitpack.io/#AyanTech/Networking)

# Ayan Networking v2

`v2` is the Ktor-based API client in the `ayannetworking` Android library. It sends
serializable request models inside the Ayan request envelope and returns a Kotlin
`Result<Response>` from each suspending request.

This repository-level document covers only the implementation under
`ayannetworking/src/main/java/ir/ayantech/networking/v2`. It does not document
the legacy networking implementation.

## Requirements

- Android API 21 or newer
- Kotlin coroutines
- Kotlin serialization
- Ktor client with the OkHttp engine

When using the module from this repository, add it to the consuming Android module:

Add it in your `settings.gradle.kts` at the end of repositories:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.AyanTech:Networking:LATEST_VERSION")
}
```

Request and response DTOs must be serializable:

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequest(
    @SerialName("UserId") val userId: String,
)

@Serializable
data class ProfileResponse(
    @SerialName("DisplayName") val displayName: String,
)
```

## Create a client

Create one `AyanApi` instance and reuse it. The base URL and endpoint are concatenated
as provided, so include any required `/` separator in one of them.

```kotlin
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.v2.AyanApi
import kotlin.time.Duration.Companion.seconds

val ayanApi = AyanApi.Builder(
    context = applicationContext,
    baseUrl = "https://api.example.com/",
)
    .setTimeOutDuration(20.seconds)
    .setAcceptLanguage(Language.ENGLISH)
    .setLogLevel(LogLevel.LOG_ALL)
    .setInvokeUserToken { tokenStore.currentToken.orEmpty() }
    .build()
```

The token callback is evaluated when requests are created, allowing it to return the
latest token. The client sends it in both the Ayan `Identity.Token` field and the HTTP
`Authorization` header.

### Builder defaults

| Setting | Default |
| --- | --- |
| Timeout | 30 seconds |
| Language | `Language.PERSIAN` |
| Logging | `LogLevel.LOG_ALL` |
| No-proxy mode | `true` |
| Follow redirects | `true` |
| Token | Not set |

`LOG_ALL` logs request and response details through Ktor. Authorization, proxy
authorization, and cookie headers are sanitized. Disable full logging for production
if payloads may contain sensitive information.

## Make a POST request

Call `post` from a suspend function or coroutine.

```kotlin
val result = ayanApi.post<ProfileRequest, ProfileResponse>(
    body = ProfileRequest(userId = "1234"),
    endPoint = "GetProfile",
)
```

Use the optional `baseUrl` argument to override the configured base URL for one call:

```kotlin
val result = ayanApi.post<ProfileRequest, ProfileResponse>(
    body = ProfileRequest(userId = "1234"),
    endPoint = "GetProfile",
    baseUrl = "https://alternate.example.com/",
)
```

## Handle the result

Each call returns `Result.success(response)` or `Result.failure(failure)`. Manage
loading state around the suspend call. Coroutine cancellation propagates to the caller.

```kotlin
import ir.ayantech.networking.v2.helpers.Failure

result
    .onSuccess { profile -> showProfile(profile) }
    .onFailure { exception ->
        val failure = exception as? Failure
        showError(failure?.failureMessage ?: exception.message.orEmpty())
    }
```

Use `fold` to turn either outcome into a value.

```kotlin
val uiEvent = result.fold(
    onSuccess = { UiEvent.ProfileLoaded(it) },
    onFailure = { UiEvent.RequestFailed((it as? Failure)?.failureMessage ?: it.message.orEmpty()) },
)
```

## Wire format

v2 wraps every request body automatically:

```json
{
  "Identity": {
    "Token": "user-token"
  },
  "Parameters": {
    "UserId": "1234"
  }
}
```

The server response must use this envelope:

```json
{
  "Parameters": {
    "DisplayName": "Example User"
  },
  "Status": {
    "Code": "G00000",
    "Description": null,
    "Hint": null,
    "IsFromCache": false,
    "Retryable": false,
    "Type": null
  }
}
```

A response is successful only when all the following are true:

- The HTTP status is `200 OK`.
- `Status.Code` is `G00000`.
- `Parameters` is not `null`.

The client checks `Status.Code` before decoding `Parameters`. `G00002` is mapped to
`LOGIN_REQUIRED`. Other service codes are mapped to an unknown remote failure.
For every service failure, including `Parameters = null`, the complete status and a
nonblank `Status.Description` are retained in `Failure`. HTTP errors carrying a
service envelope preserve the same message; other HTTP errors use a transport fallback.

## Failures

A failed `Result` contains a v2 `Failure` exception with the following fields.

- `failureRepository`: whether the problem is local or remote
- `failureType`: categorized cause
- `failureCode`: server code or an internal fallback code
- `failureMessage`: localized, user-readable message
- `failureStatus`: the server's status object when available

Recognized failure types include no internet connection, timeout, login required,
non-200 response, canceled request, and unknown error. Failure messages support
Persian, English, and Arabic.

```kotlin
result.onFailure { exception ->
    when (val failure = exception as? Failure) {
        null -> logUnexpectedError(exception)
        else -> {
            logFailure(failure.failureCode, failure.failureType)
            showError(failure.failureMessage)
        }
    }
}
```

## Current limitations

- Only POST requests are exposed.
- Only HTTP `200 OK` is treated as a valid transport response.
- A successful status with `Parameters = null` is still treated as a failure,
  including for operations with no response body.
- `setCustomHeaders`, `setPathUrl`, and `setNoProxyMode` store configuration but the
  current Ktor client does not apply it.
- `setFollowRedirect` stores configuration, but redirects are currently always
  enabled by the Ktor client.
- The selected builder language is sent through `Accept-Language`; some server-side
  failures are currently created with Persian messages regardless of that setting.

## Package overview

| Package | Purpose |
| --- | --- |
| `v2.AyanApi` | Client builder and public POST API |
| `v2.api` | Request execution and safe-call mapping |
| `v2.model` | Request/response envelopes and API status models |
| `v2.helpers` | Failure model and user-agent generation |
| `v2.network` | Ktor `HttpClient` configuration |
