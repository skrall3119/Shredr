# Firebase verification

Latest verification: **October 27, 2026** · Shredr **1.1.0** · Android 11 (API 30) emulator.

## Results

| Area | Verified behavior | Result |
| --- | --- | --- |
| Authentication and profiles | Registration, sign-out/sign-in, and persisted profile updates | Passed |
| Matching and messaging | Reciprocal likes, mutual matches, and message delivery to both participants | Passed |
| Photo storage | JPEG upload, saved profile URL, and byte-identical downloads through the SDK and HTTPS | Passed |
| Access control | Rejection of unauthorized profile edits, private reads, forged matches, and message edits | Passed |
| Release | Debug/release builds, unit tests, release lint, APK signature, and installation/launch on a clean emulator | Passed |

**Automated coverage:** eight local Firestore/Storage security-test groups and two opt-in live Firebase tests passed with no failures. Live tests used disposable accounts and cleaned up their fixtures.

**Manual UI checks:** gallery selection and cancellation, photo persistence after navigation, bio persistence after restart, creating a match, and sending a reply were verified with fictional demo accounts. The eight README screenshots contain demo content only. These UI checks preceded the final security changes; the live tests above were rerun against the deployed rules.

## Security and scope

Deployed, version-controlled rules enforce profile ownership, private individual likes, reciprocal matching, sender identity, immutable sent messages, and photo ownership/type/size limits. Discoverable profiles exclude email addresses; legacy records remain owner-only.

This is a portfolio preview, not a production-readiness certification. Other Android versions, offline recovery, and abuse prevention have not been fully verified. See [SECURITY.md](SECURITY.md) for the access model, test instructions, and remaining limitations.

Test sources: [rules suite](tests/rules.test.cjs) · [Firebase flows](app/src/androidTest/java/com/alexjanci/jamr/FirebaseFlowTest.kt) · [photo uploads](app/src/androidTest/java/com/alexjanci/jamr/FirebasePhotoTest.kt).
