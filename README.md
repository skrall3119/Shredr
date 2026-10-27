# Shredr

### An Android social-matching prototype built with Kotlin and Firebase

Shredr brings account creation, profile discovery, mutual matching, and messaging into one native Android application. Users browse profile cards, express interest with a swipe, and connect through a match-based messaging flow.

The project demonstrates integration across mobile UI, asynchronous data access, authentication, cloud storage, and event-driven updates.

## Screenshots

Captured from the running Android app on an API 30 emulator. Profiles, artwork, and conversations use fictional demo accounts; no real user content is shown.

| Discover musicians | Edit your profile | Your connections |
| --- | --- | --- |
| <img src="docs/screenshots/discover.png" width="240" alt="Swipeable musician discovery cards"> | <img src="docs/screenshots/profile.png" width="240" alt="Editable profile with uploaded photo and bio"> | <img src="docs/screenshots/connections.png" width="240" alt="Mutual matches ready for conversation"> |

| Conversation | Sign in | Create an account |
| --- | --- | --- |
| <img src="docs/screenshots/conversation.png" width="240" alt="Firebase-backed demo conversation"> | <img src="docs/screenshots/sign-in.png" width="240" alt="Shredr sign-in screen"> | <img src="docs/screenshots/create-account.png" width="240" alt="Account registration form"> |

<details>
<summary>Gallery selection and profile editing</summary>

| Choose a photo | Save a bio |
| --- | --- |
| <img src="docs/screenshots/gallery.png" width="240" alt="Android gallery picker with generated demo artwork"> | <img src="docs/screenshots/edit-profile.png" width="240" alt="Profile bio editor and save action"> |

</details>

## Features

- **Account registration and sign-in** using Firebase Authentication, with form validation.
- **User profiles** with editable bios and gallery-selected profile photos stored in Firebase Storage.
- **Swipe-based discovery** with like, skip, and rewind controls using CardStackView.
- **Mutual matching** that checks for reciprocal likes and writes match records for both users.
- **Messaging** backed by Cloud Firestore snapshot listeners, with timestamp-ordered messages and separate incoming/outgoing layouts.
- **Fragment-based navigation** between discovery, profiles, and messages.

## Technical overview

| Layer | Implementation |
| --- | --- |
| Language and UI | Kotlin, Android SDK, XML layouts, AndroidX fragments |
| Authentication | Firebase Authentication |
| Application data | Cloud Firestore |
| Profile images | Firebase Storage and Picasso |
| Async operations | Kotlin coroutines and Firebase task callbacks |
| Lists and cards | RecyclerView, Groupie, CardStackView |

### How matching works

A right swipe records a like for the selected profile. The app then checks whether that person has already liked the current user. A reciprocal like creates a match record under each user's account.

Discoverable profile fields live in `public-profiles`; email stays in Firebase Authentication. Legacy `users` records are owner-only. Individual likes live at `user-likes/{uid}/likes/{otherUid}`. Matches and messages use `user-matches` and `user-messages`. Profile images are stored at `users/{uid}/profile.jpg`.

Versioned [Firestore rules](firestore.rules) and [Storage rules](storage.rules) enforce profile ownership, private likes, mutual matching, sender identity, and photo upload limits. See [security and release notes](SECURITY.md) for the access model and tests.

### Code worth exploring

- [Discovery and mutual matching](app/src/main/java/com/alexjanci/jamr/MainFragment.kt) — card interactions, coroutine-based profile loading, and reciprocal-like checks.
- [Chat](app/src/main/java/com/alexjanci/jamr/ChatLogFragment.kt) — Firestore listeners, message ordering, and listener removal when the screen stops.
- [Profiles](app/src/main/java/com/alexjanci/jamr/Profile.kt) — image uploads, biography updates, and live profile data.
- [Registration](app/src/main/java/com/alexjanci/jamr/SignUp.kt) — input checks, Firebase account creation, and initial profile records.
- [Card rendering](app/src/main/java/com/alexjanci/jamr/CardStackAdapter.kt) — profile presentation in the discovery stack.

## Getting started

### Get the Android app

[Download Shredr 1.1.0 for Android](https://github.com/skrall3119/Shredr/releases/download/v1.1.0/Shredr-1.1.0.apk), or open the [preview release](https://github.com/skrall3119/Shredr/releases/tag/v1.1.0) for release notes and its SHA-256 checksum. This is a portfolio preview, not a production service.

1. Open the release on your Android phone and expand **Assets**.
2. Download the file ending in `.apk`. The **Source code** ZIP and TAR files are for developers and cannot be installed as an app.
3. Open the downloaded APK and follow Android's installation prompts. If prompted, allow your browser or file manager to install this app; you can turn that permission off afterward.
4. Open Shredr, choose **Create an account**, and register with your email and password.
5. Add a profile photo and bio, then start discovering other musicians.

Shredr requires Android 8.0 (API 26) or later and an internet connection for its online features. It has been tested on an Android 11 (API 30) emulator; other Android versions have not been verified. There is no iOS build.

The app already connects to its configured Firebase backend. You do not need to create a Firebase project, connect a Firebase account, or replace any configuration files to try it.

If you previously installed a development build, its signing key may differ. Android will require you to uninstall that build before installing this release, which removes its local app data. Your Firebase account remains available to sign in again.

### Build from source

1. Clone this repository and open it in Android Studio.
2. Configure a compatible Gradle/JDK environment. The project uses Kotlin 1.3.72, Android Gradle Plugin 4.1.0, and compile/target SDK 30; the debug build has been verified with JDK 11.
3. Select an Android device or emulator supporting API 26 or later and run the `app` configuration.
4. Register directly in the app using **Create an account**.

## Suggested demo

With two test accounts, complete the profile data, browse discovery cards, like each other, and open the resulting match to exchange messages. Change a profile photo and bio to explore the storage and profile-update paths.

## Project status and next steps

Shredr is a portfolio prototype. The debug build and isolated live Firebase tests have passed for authentication, profile persistence, matching, mirrored messages, photo uploads, and downloads. See [verification notes](FIREBASE-VERIFICATION.md) for scope and remaining limitations.

Priorities for continued development:

- Migrate synthetic view access and older build dependencies.
- Improve loading, empty, and failure states, and remove unsafe null assumptions.
- Add offline recovery and retry coverage beyond the current atomic match/message writes.
- Add abuse prevention, reporting/blocking, and account deletion before a wider production launch. Live Firebase tests remain opt-in and use disposable fixtures.

## Author

[Alex Janci](https://github.com/skrall3119)
