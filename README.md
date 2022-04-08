# Engage Androd SDK

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

Import `so.engage.android.sdk.Engage;` and initialize the SDK.

```java
// ...
import so.engage.android.sdk.Engage;

public class MainActivity extends AppCompatActivity {
  // ...
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Engage.init("public-api-key");
```

## Identify users

Engage uses your user's unique identifier (this is mostly the ID field of the users' table) for data tracking. **Identify** lets you link this ID to the user. With identify, you are able to supply more details about the user. 

```java
private void updateUiWithUser(LoggedInUserView model) {
  // ...
  HashMap<String, Object> attributes = new HashMap<String, Object>();
  attributes.put("first_name", model.getDisplayName());
  attributes.put("last_login", new Date());
  Engage.identify(model.getDisplayId(), attributes);
```

Engage supports the following standard attributes: `first_name`, `last_name`, `email`, `number` (customer's phone number) but you can use identify to add any customer attribute you want. `last_login` in the example above is an example.

When new users are identified, Engage assumes their signup date to be the current timestamp. You can change this by adding a `created_at` attribute.

```java
HashMap<String, Object> attributes = new HashMap<String, Object>();
attributes.put("first_name", model.getDisplayName());
attributes.put("last_login", new Date());
attributes.put("created_at", "2021-01-04");
Engage.identify(model.getDisplayId(), attributes);
```

## Add attributes

To add more attributes to the user's profile, use the `addAttributes` method.

```java
HashMap<String, Object> attributes = new HashMap<String, Object>();
attributes.put("plan", "Pro");
attributes.put("age", 14);
Engage.addAttributes(model.getDisplayId(), attributes);
```

## Track events

Track an event:

```java
Engage.trackEvents(model.getDisplayId(), "Login");
```

Track an event with a value:

```java
Engage.trackEvents(model.getDisplayId(), "Clicked", "Login button");
```

Track an event with properties:

```java
HashMap<String, Object> properties = new HashMap<String, Object>();
properties.put("type", "button");
properties.put("counter", counter);
Engage.trackEvents(model.getDisplayId(), "Clicked", properties);
```

Engage sets the event date to the current timestamp but if you would like to set a different date, you can add a date as the last argument of the `trackEvents` method.

```java
Date lastWk = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
Engage.trackEvents(model.getDisplayId(), "Clicked", "Login button", lastWk);
```