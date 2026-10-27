# Firebase verification

Verified October 27, 2026 on the API 30 Android emulator.

## Results

- Debug build and existing unit tests passed.
- Isolated live Firebase test passed (1 test, 0 failures/errors): email/password account creation, sign-out/sign-in, profile write/read, reciprocal likes/matches, and mirrored message write/read.
- The test removed its own documents and both disposable Auth accounts. It does not send messages to existing users.
- These checks verify Firebase operations, not a complete manual UI walkthrough.
- Cloud Storage is blocked: the Firebase console requires a pricing-plan upgrade for this Spark project. App photos now fall back to the bundled image; a successful real photo upload remains unverified.
- Deployed Firestore rules permit every authenticated account to read/write every document. Do not treat this as production-ready authorization. Before public release, separate private profile fields and enforce ownership, mutual-match requirements, and conversation participation in tested rules.

## Opt-in live test

This test uses the Firebase project configured in `google-services.json`. It is skipped unless explicitly enabled. Use a dedicated development project whenever possible.

```powershell
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.alexjanci.jamr.FirebaseFlowTest' '-Pandroid.testInstrumentationRunnerArguments.liveFirebase=true'
```

## Before README screenshots

Sign in with a demonstration account and manually verify profile save feedback, gallery cancellation, navigation away/back, and an actual two-account conversation. Resolve the Storage plan requirement and verify photo upload/download if screenshots should contain real uploaded photos. Use only consenting demo profiles and conversations in public screenshots.

## Photo test after Blaze upgrade

The opt-in FirebasePhotoTest ran on October 27, 2026. Upload failed with HTTP 412: the Storage service account lacks required permissions. Cleanup of the disposable account and its profile document completed. Download and photo-URL checks were not reached.

The console confirms Blaze, but Storage displays Get started and proposes a new default bucket (`jamr-7f77c.firebasestorage.app`). No bucket or permissions were changed during testing. The existing app bucket needs repair/relinking, or the new bucket must be provisioned and the app configuration updated before rerunning.

## Repair completed and photo test passed

On October 27, 2026, the existing `jamr-7f77c.appspot.com` bucket was re-imported using the official `projects.buckets.addFirebase` endpoint. IAM verification confirmed the Firebase Storage service agent has `roles/firebasestorage.serviceAgent`. No new bucket was created.

After permission propagation, FirebasePhotoTest passed (1 test, 0 failures/errors). It verified a JPEG upload using putFile, download URL retrieval, Firestore profile URL persistence with a server read, and byte-identical downloads through both the Storage SDK and the HTTPS URL. Its disposable Auth account, profile document, local fixture, and uploaded image were cleaned up. The earlier Storage blocker is resolved; manual gallery/profile UI verification remains separate.

## Manual UI walkthrough and README captures

Verified October 27, 2026 in the API 30 emulator with three fictional demo accounts:

- Gallery opens from the profile photo. Cancelling returns to the unchanged profile.
- Selecting an alternate JPEG uploads successfully and displays Photo saved. The replacement survives navigating away and back.
- Restoring the original artwork also succeeds; a server-side download matches the selected local JPEG byte-for-byte.
- An edit made through the bio field and Save profile persists on the server and after restarting the app.
- A like through the discovery UI creates the reciprocal match for the prepared demo accounts. The connection appears with its photo in Messages.
- A reply composed and sent through the chat UI appears in the recipient's Firestore conversation; the sender draft clears after success.
- The chat now shows the recipient's name and uses the sender's actual profile image instead of an unconditional placeholder.

Eight PNG captures in docs/screenshots show sign-in, registration, discovery, profile, connections, conversation, gallery, and bio editing. They are direct emulator captures, not layout previews. Demo art and the initial conversation were prepared as fixtures; the final reply was sent through the app. These are fictional demo data, not real user activity.

Demo accounts remain available for review. Their generated passwords are stored outside this repository in the parent workspace's ignored .android-tools/demo/ACCOUNTS.txt file. No credentials are included in README captures or source. Demo profiles discover only profiles with the same demoGroup value; this is demo isolation, not an authorization boundary.

## Security rules and Android preview release

On October 27, 2026, Firestore and Storage rules were tightened and version-controlled. This supersedes the earlier broad authenticated-user rules described above.

- Migrated 15 legacy profiles to public-profiles without email or other private fields, and 60 legacy likes to individual owner/target records. Original records and existing chats were retained. A private Cloud Shell backup preserves the previous rules and migration inputs.
- Deployed Firestore ruleset 9bb79432-b618-495d-81c7-f7139d612ab3 and Storage ruleset 112ecfcb-76e7-4acb-b64c-0a32d1fddee3.
- All eight local emulator security-test groups passed, including cross-account denials, mutual matching, mirrored messages, immutable sent messages, and upload ownership/type/size checks.
- Both updated live Android tests passed (2 tests, no failures): authentication, profile persistence, matching, mirrored messages, photo upload/download, and server rejection of unauthorized access. Disposable fixtures were cleaned up.
- Debug/release builds, release lint, and unit tests passed. The signed 1.1.0 APK has version code 2, minimum API 26, target API 30, and a verified APK v2 signature. Release builds are not debuggable.

See SECURITY.md for the access model and remaining preview limitations, including shareable profile-photo URLs and absent abuse-prevention features.
- The exact signed release APK was installed and launched successfully in a separate clean API 30 emulator. The sign-in screen rendered correctly and the crash log was empty.
