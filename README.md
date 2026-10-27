# Shredr

### An Android social-matching prototype built with Kotlin and Firebase

Shredr brings account creation, profile discovery, mutual matching, and messaging into one native Android application. Users browse profile cards, express interest with a swipe, and connect through a match-based messaging flow.

The project demonstrates integration across mobile UI, asynchronous data access, authentication, cloud storage, and event-driven updates.

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

The data model uses Firestore collections for `users`, `user-likes`, `user-matches`, and `user-messages`. Profile images are stored separately at `users/{uid}/profile.jpg`.

### Code worth exploring

- [Discovery and mutual matching](app/src/main/java/com/alexjanci/jamr/MainFragment.kt) — card interactions, coroutine-based profile loading, and reciprocal-like checks.
- [Chat](app/src/main/java/com/alexjanci/jamr/ChatLogFragment.kt) — Firestore listeners, message ordering, and listener removal when the screen stops.
- [Profiles](app/src/main/java/com/alexjanci/jamr/Profile.kt) — image uploads, biography updates, and live profile data.
- [Registration](app/src/main/java/com/alexjanci/jamr/SignUp.kt) — input checks, Firebase account creation, and initial profile records.
- [Card rendering](app/src/main/java/com/alexjanci/jamr/CardStackAdapter.kt) — profile presentation in the discovery stack.

## Getting started

This repository preserves an earlier Android implementation. Its build configuration uses Kotlin 1.3.72, Android Gradle Plugin 4.1.0, compile/target SDK 30, and minimum SDK 26. Compatibility with current tooling has not been verified.

1. Clone the repository:
   ```bash
   git clone https://github.com/skrall3119/Shredr.git
   ```
2. Open the project in Android Studio and configure a compatible Gradle/JDK environment. The existing build references JCenter and older dependencies; dependency or tooling migration may be necessary.
3. Create your own Firebase project and register the Android application ID `com.alexjanci.jamr`.
4. Enable email/password authentication, Cloud Firestore, and Firebase Storage. Replace `app/google-services.json` with your project's configuration.
5. Configure Firestore and Storage access rules for the application's authenticated-user flows. Backend rules are not included in this repository.
6. Add a fallback profile image at `users/default/default.png` in Storage. Populate the `city` field on test-user documents; discovery expects it, but registration does not currently collect it.
7. Build and run on an Android device or emulator supporting API 26 or later.

The checked-in Firebase configuration does not guarantee access to a working backend. Use a development project and test accounts.

## Suggested demo

With two test accounts, complete the profile data, browse discovery cards, like each other, and open the resulting match to exchange messages. Change a profile photo and bio to explore the storage and profile-update paths.

## Project status and next steps

Shredr is a portfolio prototype. The source contains the features described above; a current end-to-end build and backend run have not been verified for this documentation.

Priorities for continued development:

- Migrate synthetic view access and older build dependencies.
- Replace global coroutine scopes with lifecycle-aware work.
- Improve loading, empty, and failure states, and remove unsafe null assumptions.
- Make matching and message writes more resilient to partial failures.
- Add feature-specific tests and version-controlled backend rules; existing test files are starter examples.

## Author

[Alex Janci](https://github.com/skrall3119)
