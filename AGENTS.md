# Kharcha: project rules

Kharcha is a native Android, local-first expense tracker for India. It parses bank SMS on-device.

## Package & App Config
- **Package Name:** `com.mukeshprajapat.kharcha`
- **Application Name:** Kharcha
- **Min SDK:** 26
- **Target / Compile SDK:** 34 or 35

## Stack
Kotlin, Jetpack Compose, Material 3, Room, Hilt, WorkManager, DataStore, Navigation Compose, Vico. Min SDK 26. Versions live only in `gradle/libs.versions.toml`.

## Hard rules
- Money is a `Long` in paise. Never `Double` or `Float`. Parse amounts with `BigDecimal` and convert to paise.
- No `INTERNET` permission, no analytics, no ads, no network calls.
- Never log SMS bodies. Store a hash and parsed fields only.
- The parser package is pure Kotlin with no Android imports and has unit tests for every template.
- Match SMS senders on the header only (ignore the 2-letter prefix and the suffix). The allowlist is user-editable in DataStore.
- Dedupe on the UPI ref or UTR with a unique index.
- All colours, type and spacing come from the theme tokens. No hard-coded values in screens.
- Amounts use Indian grouping (lakh, crore). Use tabular numerals.
- Every screen has loading, empty and error states.
- Every ViewModel exposes one UiState. Repositories return Flow.

## Process
- Work on one phase from the plan at a time. Stop and report when its acceptance check passes.
- Write the test before the parser change. Run unit tests before saying a phase is done.
- Do not add libraries that aren't in the plan without asking.
- Test fixtures use fake names, refs and links only.
