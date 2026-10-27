# Security and release operations

Shredr is a portfolio preview. Security rules enforce access boundaries; they do not provide moderation, spam prevention, rate limiting, or an age-verification service.

## Access model

- Signed-in users can discover `public-profiles`. Allowed fields are name, age, city, bio, photo URL, ID, and an optional demo group. Only the owner can edit or delete a profile. Email is obtained from Firebase Authentication for the current user, never from discovery.
- Legacy `users` documents can only be fetched or deleted by their owner. Collection listing and client writes are denied. Existing private fields were retained in these records, not copied into public profiles.
- An owner can list their individual likes. Only the owner and the liked person can fetch a particular like. Only the owner can create or delete it. Legacy aggregate maps are owner-only and read-only.
- Either participant can create both match records after reciprocal likes exist. Match snapshots must match canonical public profiles and the opposite record must exist after the atomic write.
- A sender can create an identical pair of message copies only with reciprocal likes and match records. Sender identity, recipient, ID, text length, and timestamp are validated. Sent messages cannot be edited. Each user can read or delete only their own conversation copy.
- Photo uploads are restricted to the owner's `users/{uid}/profile.jpg`, JPEG/PNG/WebP MIME types, and a maximum of 5 MiB. Only the owner can delete the photo. MIME metadata is not content scanning. Storage SDK reads require authentication, but Firebase download-token URLs remain shareable bearer links; profile pictures should not contain private material.
- Unmatched paths are denied. Demo groups filter the interface, not access permissions. App backup is disabled for the release.

## Rule tests

With Node.js, npm, and a supported Java runtime installed:

```sh
npm ci
npm run test:rules
```

The suite uses only the `demo-shredr` local Firestore and Storage emulators. It checks allowed user flows and rejects cross-account writes, private reads, forged/one-sided matches and messages, message editing, invalid uploads, and unknown paths.

`FirebaseFlowTest` and `FirebasePhotoTest` are explicit opt-in live tests. They create randomly named disposable accounts and clean up their fixtures. The flow test also checks deployed permission-denied responses. Do not enable them against an unrelated project.

## Deployment and migration

`firebase.json` references the checked-in rules. The one-time migration in `tools/deploy-rules.py` targets the existing Jamr project. Run `inspect` first from an authorized Google Cloud Shell, then `deploy` only when intentionally administering that project. It saves the prior rules and source records in a private timestamped backup outside the checkout, compiles both rule files, copies only public fields and individual likes without deleting originals, and activates the new rules. Subsequent deployments should use the Firebase CLI with an explicit project.

The schema change requires version 1.1.0 or later; older clients cannot write the legacy profile/likes paths. Existing matches and conversation history are preserved. Message sending still requires reciprocal likes.

## Signing and release

Release builds use a private signing key supplied through `SHREDR_KEYSTORE`, `SHREDR_STORE_PASSWORD`, and `SHREDR_KEY_PASSWORD` (alias `shredr`). Without these environment variables, Gradle produces an unsigned release artifact. Never commit keys or passwords. Keep a secure backup of the signing key: future APK updates must use the same key.

```sh
./gradlew assembleRelease
```

Attach the signed APK and a SHA-256 checksum to a GitHub Release. Do not commit APK binaries into source control. Android rejects an in-place update from a development APK signed with another key.

Release certificate SHA-256: `75108da3dae4e9a782feb9b6563ffbb7ad831be51b1e0ab523ca0b413ffd444c`.
