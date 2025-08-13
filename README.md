# Engage Android SDK

[Engage](https://engage.so/) helps businesses deliver personalized customer messaging and marketing automation through email, SMS and in-app messaging. This Android SDK makes it easy to identify customers, sync customer data (attributes, events and device tokens) to the Engage dashboard and send in-app messages to customers.

## Features

- Track device token
- Identify users
- Update user attributes
- Track user events

## Getting started

- [Create an Engage account](https://engage.so/) and set up an account to get your public API key.
- Learn about [connecting customer data](https://engage.so/docs/guides/connecting-user-data) to Engage.

## Installation

The SDK is published through JitPack. Add it to your root `build.gradle`.

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

Next, add the dependency.

```gradle
dependencies {
  implementation 'com.github.engage-so:engage-android:v1.0.0'
}
```

## Permissions

Ensure your app includes INTERNET permission as it is needed to sync data.

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

## Initialization

Import `so.engage.android.sdk.Engage` and initialize the SDK.

```kotlin
// ...
import so.engage.android.sdk.engage.Engage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Engage.instance.initialise(this, "public-api-key")
    }
    // ...
}
```

## Identify users

Engage uses your user's unique identifier (this is mostly the ID field of the users' table) for data tracking. **Identify** lets you link this ID to the user. With identify, you are able to supply more details about the user.

```kotlin
val properties = mapOf(
    "first_name" to "Jane",
    "last_name" to "Doe",
    "last_login" to Date()
)
Engage.instance.identify("user-id", properties)
```

Engage supports the following standard attributes: `first_name`, `last_name`, `email`, `number` (customer's phone number) but you can use identify to add any customer attribute you want. `last_login` in the example above is an example.

When new users are identified, Engage assumes their signup date to be the current timestamp. You can change this by adding a `created_at` attribute.

```kotlin
val properties = mapOf(
    "first_name" to "Jane",
    "last_name" to "Doe",
    "created_at" to "2021-01-04"
)
Engage.instance.identify("user-id", properties)
```

## Add attributes

To add more attributes to the user's profile, use the `addAttributes` method.

```kotlin
val attributes = mapOf(
    "plan" to "Pro",
    "age" to 14
)
Engage.instance.addAttributes(attributes, "optional-user-id")
```

## Set device token

Engage integrates with [FCM](https://firebase.google.com/docs/cloud-messaging) to let you send push notifications to your users, either through broadcast or automation. However, to do this, you need to send the user's FCM registration token to Engage. The device registration token is a unique identifier that allows the device receive messages.

```kotlin
fun onNewToken(token: String) {
    Engage.instance.setDeviceToken(token, "optional-user-id")
}
```

## Track events

Track an event:

```kotlin
Engage.instance.track("Login", uid = "optional-user-id")
```

Track an event with a value:

```kotlin
Engage.instance.track("Clicked", value = "Login button", uid = "optional")
```

Track an event with properties:

```kotlin
val properties = mapOf(
    "type" to "button",
    "counter" to counter,
)
Engage.instance.track("Clicked", value = properties, uid = "optional")
```

Engage sets the event date to the current timestamp but if you would like to set a different date, you can add a date as an argument in the `track` method.

```kotlin
Engage.instance.track("Clicked", value = "Login button", date = Date(), uid = "optional")
```