# Kharcha: Build Plan

Nov 11, 2026 · @mukesh prajapat

Kharcha is a native Android expense tracker for India that reads bank SMS on-device, logs UPI and NEFT transactions automatically, and asks you for a short description through a notification. Nothing leaves the phone.

## Locked decisions

| Decision | Choice |
| --- | --- |
| Name | Kharcha |
| Platform | Native Android (Kotlin, Jetpack Compose, Material 3) |
| Data | Local-first, Room database on device, no backend |
| Capture | Reads bank SMS, parses on-device, asks for a note via heads-up notification |
| Money | Integer paise only, never Double or Float |
| Distribution (v1) | Sideload for personal use. Play Store restricts SMS permissions, so publishing needs a different capture path |
| First bank | Kotak (senders KOTAKD and KOTAKB), then add HDFC, ICICI, SBI, Axis |

## Setup before you start coding

Finish these ten items first. They take about an afternoon and save days of rework later.

- [ ] **Android Studio (stable channel)** installed with the Android SDK. Use it for the emulator, Logcat and the Compose preview even if Antigravity writes the code.
- [ ] **A physical Android phone** with Developer options and USB debugging on. SMS receiving, notification behaviour and battery restrictions only behave truthfully on a real device.
- [ ] **A private GitHub repo** named kharcha. Your GitHub profile is public, so keep this one private until the test fixtures are fully anonymised.
- [ ] **Package name** decided once, for example com.yourhandle.kharcha. It cannot change after install without losing data. Don't use a domain you don't own.
- [ ] **Signing keystore** generated and backed up in two places. Losing it means you can't update the installed app and must reinstall, wiping local data.
- [ ] **Antigravity project rules file** at the repo root (for example AGENTS.md) containing the block in the last section of this plan. Check which rules filename your Antigravity version reads and use that.
- [ ] **Version catalog** (gradle/libs.versions.toml) so dependency versions live in one file and the agent doesn't pin random versions.
- [ ] **Fixtures folder** with 20 to 30 anonymised SMS in a plain text or JSON file. Fake names, fake UPI refs, fake short links. Keep real ones out of the repo.
- [ ] **Reference screens** from other apps, each with one line on what you like (entry speed, chart, empty state, etc.). Put them in /design/refs.
- [ ] **Brand tokens** chosen from the next section: colours, fonts, app icon, so the first screens are built on the real theme and not rebuilt later.

## Brand and theme

Pick the palette and fonts once, export them as Compose theme files, and let every screen read from those tokens. Never hard-code a colour or size in a screen.

### Tools to use

| Job | Tool | Why |
| --- | --- | --- |
| Colour scheme | Material Theme Builder (m3.material.io/theme-builder) | Give it one seed colour and it generates light and dark schemes and exports Color.kt and Theme.kt for Compose |
| Palette ideas | Coolors, Realtime Colors | Try seed colours and preview them on a sample UI |
| Contrast check | WebAIM contrast checker | Amounts and labels must pass AA contrast in both themes |
| Fonts | Google Fonts | Free and licensed for apps. Check the family has tabular numerals and Devanagari support |
| App icon | Icon Kitchen (icon.kitchen) or Android Studio Image Asset | Produces the adaptive icon and the monochrome themed-icon layer |
| Screen design (optional) | Figma with the Material 3 Design Kit | Only if you want to design before building. Skipping it is fine since you have reference screens |
|  |  |  |

### Starting palette (proposal, replace after Theme Builder)

Use these as the seed and semantic colours, then let Theme Builder derive the rest.

| Role | Hex | Use |
| --- | --- | --- |
| Primary seed | #0F6B63 | Deep teal for buttons, active states, charts |
| Accent | #F2A900 | Turmeric for highlights and the add button |
| Expense | #C8372D | Debits |
| Income | #1E8E5A | Credits |
| Background (light) | #FBF7F0 | Warm off-white, softer than pure white |
| Surface (dark) | #121413 | Near-black, not pure black |

Never rely on red and green alone to mark expense and income. Add a sign, an arrow or a label, because red-green colour blindness is common.

### Typography

- **UI and headings:** Plus Jakarta Sans or Manrope.
- **Amounts:** the same family with tabular numerals turned on so digits align in lists. If your pick lacks them, use Inter for numbers.
- **Devanagari fallback:** Noto Sans Devanagari, so Hindi and Marathi payee names don't render as boxes.
- **Bundle the font files in the app** instead of using downloadable fonts, because the app is offline-first.
- **Scale:** 5 sizes (display for the monthly total, title, body, label, caption). Don't add more.

### Brand rules

- Format amounts the Indian way: ₹1,23,456.50 with lakh and crore grouping.
- Keep the logo mark simple: the letter K or a rupee-style stroke, one colour, legible at 48px.
- Generate the themed-icon (monochrome) layer so the icon follows the user's wallpaper colours on Android 13 and later.
- Support dynamic colour as an option, but ship the Kharcha palette as the default so the app looks like itself.

## Tech stack

Use current stable versions of everything and pin them in the version catalog.

| Layer | Choice | Note |
| --- | --- | --- |
| Language | Kotlin | Coroutines and Flow for async and reactive data |
| UI | Jetpack Compose + Material 3 | Single Activity, Navigation Compose |
| Database | Room (SQLite) | Optional SQLCipher later for an encrypted database |
| DI | Hilt | Keeps parser, repositories and workers testable |
| Background | WorkManager | Parses SMS off the receiver thread, survives process death |
| Settings | DataStore (Preferences) | Sender allowlist, theme, budget defaults |
| Charts | Vico (Compose charts) | Category split and monthly trend |
| Dates | java.time with core library desugaring | Safe on older Android versions |
| Tests | JUnit, Turbine, Room in-memory, Compose UI tests | The parser gets the heaviest test coverage |
| Min SDK | 26 | Covers nearly all active devices and simplifies notification channels |

No server, no Firebase, no analytics SDK, no ads SDK. The manifest should hold no INTERNET permission in v1, which makes the privacy claim checkable.

## Architecture

One Gradle module with strict package layers: UI depends on domain, domain depends on nothing Android, data implements domain. Split into modules later only if build times demand it.

SMS arrives at a receiver, which hands off to a worker, which runs the pure-Kotlin parser, writes to Room, and posts the notification. The UI only reads from Room through Flow.

```text
app/src/main/java/.../kharcha/
  ui/           screens, components, theme, navigation
  domain/       models (Money, Txn), use cases, parser interfaces
  parser/       sender rules, templates, categoriser (pure Kotlin, no Android)
  data/         Room entities, DAOs, repositories, DataStore
  capture/      SmsReceiver, ParseWorker, NotificationHelper, NoteReplyReceiver
  di/           Hilt modules
```

Rules: the parser package must compile without the Android SDK so it runs as a fast unit test. ViewModels expose a single UiState per screen. Every repository returns Flow.

## Data model

All money is a Long in paise. Format to rupees only at the UI edge.

| Entity | Key fields |
| --- | --- |
| Account | id, name, bankCode (KOTAK), last4 (0693), type (savings, credit card, cash), openingBalancePaise |
| Category | id, name, icon, colorToken, kind (expense, income), isDefault |
| Transaction | id, accountId, amountPaise, direction (DEBIT, CREDIT), occurredAt (epoch millis), payee, note, categoryId, source (SMS, MANUAL), dedupeKey, status (CONFIRMED, NEEDS\_REVIEW), rawSmsHash |
| Budget | id, month (YYYY-MM), categoryId (null for overall), limitPaise |
| Rule | id, matchType (payee contains, sender), pattern, categoryId |
| SenderAllow | header (KOTAKD), enabled, addedByUser |

- **Dedupe:** unique index on dedupeKey. For UPI it is the 12-digit UPI ref, for NEFT it is the UTR. If a message has neither, use a hash of account, amount, date and payee.
- **Raw SMS:** store only a hash and the parsed fields, not the full message body. Bodies can hold account details and links you don't need.
- **Time:** the SMS text has a date but no time, so take the date from the text and the time of day from the SMS received timestamp.
- **Auto-created accounts:** the first time XX0693 appears from a KOTAK header, create the account with bankCode KOTAK and last4 0693.
- **Indexes:** occurredAt, categoryId and accountId, since the dashboard and charts filter on all three.

## SMS parser spec

The parser is a pure Kotlin function: it takes the sender address, body and received time, and returns either a parsed transaction or null. A message becomes a transaction only if it matches a known template, so OTPs and offers fall through and are ignored.

### Sender rules

Sender IDs look like JM-KOTAKD-S: a two-letter prefix, a header, and a one-letter suffix. The prefix changes by carrier and circle, so ignore it. Match on the header.

```kotlin
val SENDER = Regex("^[A-Z]{2}-([A-Z0-9]{3,8})-([A-Z])$")
// group 1 = header (KOTAKD, KOTAKB), group 2 = suffix (S, T, P, G)
```

- Default allowlist: KOTAKD and KOTAKB, editable in settings so a new sender needs no app update.
- Suffix P is commonly the promotional category, so skip P by default with a user override. Treat the S, T and G meanings as unconfirmed until you open a thread and check.
- KOTAKB-S is your largest sender (800+ messages) and is unverified. Don't assume what it carries. The template match decides.
- Some senders arrive without a prefix or suffix. Fall back to the full uppercase address as the header.

### The three Kotak templates

| Type | Shape |
| --- | --- |
| UPI sent | Sent Rs.X from XX0693 to PAYEE on DD-Mon-YY. UPI ref no. 12 digits. |
| UPI received | Received Rs.X from NAME in your Kotak811 a/c XX0693 on DD-Mon-YY. UPI ref no. 12 digits. |
| NEFT credit | Rs.X credited to your Kotak Bank a/c XX0693 via NEFT from beneficiary NAME. UTR Ref. ID |

```kotlin
val SENT = Regex("""^Sent\s+Rs\.?\s?([\d,]+(?:\.\d{1,2})?)\s+from\s+[Xx*]*(\d{4})\s+to\s+(.+?)\s+on\s+(\d{1,2}-[A-Za-z]{3}-\d{2,4})\.\s+UPI ref no\.\s*(\d{12})""", RegexOption.IGNORE_CASE)

val RECV = Regex("""^Received\s+Rs\.?\s?([\d,]+(?:\.\d{1,2})?)\s+from\s+(.+?)\s+in your .*?a/c\s+[Xx*]*(\d{4})\s+on\s+(\d{1,2}-[A-Za-z]{3}-\d{2,4})\.\s+UPI ref no\.\s*(\d{12})""", RegexOption.IGNORE_CASE)

val NEFT = Regex("""Rs\.?\s?([\d,]+(?:\.\d{1,2})?)\s+credited to your .*?a/c\s+[Xx*]*(\d{4})\s+via\s+(NEFT|IMPS|RTGS)\s+from(?:\s+beneficiary)?\s+(.+?)\.\s+(?:UTR|Ref)\.?\s*(?:Ref\.?)?\s*([A-Z0-9]{10,})""", RegexOption.IGNORE_CASE)
```

These were written from six real messages and are a starting point. The test suite below decides whether they hold.

### Parsing rules

- Strip URLs and the tails "Not you? Tap..." and "View balance:..." before matching. Never store them.
- Parse the amount with BigDecimal and convert to paise. Handles Rs.319.00, Rs.4.85 and Rs.37099 (no decimals).
- Date format is d-MMM-yy, for example 10-Nov-26. Take the time of day from the SMS received timestamp.
- Person versus merchant: received messages show a person's name in capitals, but don't treat that as a rule. It is a hint, and the notification asks you to confirm.
- Unknown shapes return null and are counted in a local "unparsed" counter in settings, so you can see which new templates to add.

### Starter auto-categories (all user-correctable)

| Payee or pattern | Category |
| --- | --- |
| Zepto | Groceries |
| Indian Railways UTS | Transport |
| NEFT from your employer | Salary (rule on sender text) |
| UPI received from a person | None, ask in the notification |

### Test fixtures (fake data, safe for any repo)

Each fixture has a sender, a body and the expected result. Add 3 or more per template, plus ignore cases.

```text
JM-KOTAKD-S | Sent Rs.250.00 from XX0001 to Test Mart on 10-Nov-26. UPI ref no. 100000000001. Not you? Tap https://example.com/x to report -Kotak
  -> DEBIT 25000 paise, payee "Test Mart", acct 0001, key 100000000001

AD-KOTAKD-S | Received Rs.150.00 from TEST PERSON in your Kotak811 a/c XX0001 on 15-Sep-26. UPI ref no. 100000000002. View balance: https://example.com/y -Kotak
  -> CREDIT 15000 paise, payee "TEST PERSON", acct 0001, key 100000000002

JX-KOTAKB-S | Rs.37099 credited to your Kotak Bank a/c XX0001 via NEFT from beneficiary TEST EMPLOYER LTD. UTR Ref. TEST000000000001
  -> CREDIT 3709900 paise, payee "TEST EMPLOYER LTD", key TEST000000000001

JX-KOTAKB-S | 123456 is your OTP. Do not share it.
  -> ignored (null)

JM-KOTAKB-P | Pre-approved offer for you. Tap here.
  -> ignored (suffix P)

JM-KOTAKD-S | (same UPI sent message as above, received twice)
  -> second insert rejected by the dedupe key
```

### Still needed to harden the parser

- 3 to 5 masked messages from a KOTAKB-S thread, including an OTP or login alert and any card or ATM spend.
- A failed or reversed UPI message and a credit card message, if you have them.
- A look inside a KOTAKB-P thread to confirm what P carries.

## Capture flow

Modern Android blocks background apps from opening screens, so the "popup" is a heads-up notification with an inline text reply. It appears within a second of the SMS and takes one tap and a few words.

1. **Receive.** A manifest-registered receiver for SMS\_RECEIVED does almost nothing: it reads sender, body and timestamp and enqueues a WorkManager job. Use goAsync only for trivial work.
2. **Filter.** The worker checks the sender header against the allowlist, then runs the parser.
3. **Save.** A parsed message is inserted as NEEDS\_REVIEW with an auto-category if a rule matches. The dedupe key blocks duplicates.
4. **Notify.** A high-importance notification shows "Rs.319 to Zepto" with three actions: an inline **Add note** reply, a **Category** action that opens a small picker, and **Skip**.
5. **Confirm.** The reply broadcast updates the note and flips the status to CONFIRMED. Ignoring the notification leaves it in the review queue.
6. **Review queue.** The home screen shows a count of unreviewed transactions. Clearing them is a swipe-through list, so a missed notification costs nothing.

### Reliability notes

- Ask for RECEIVE\_SMS at first run with a plain-language explanation that parsing happens on the phone. Ask for READ\_SMS only for the optional "import past messages" feature.
- Request POST\_NOTIFICATIONS on Android 13 and later.
- Some phone makers kill background apps aggressively. Add an onboarding step that explains battery optimisation and links to the right settings screen.
- Debits arrive before you remember them, so the notification text must be short enough to answer in under five seconds.
- Add a manual quick-add as the fallback for cash and for banks the parser doesn't know yet.

## Features and screens (v1)

Build the five screens below first. Each should be fast to scan and reachable in one tap from the bottom bar.

| Screen | What it holds |
| --- | --- |
| Home | Month total, budget progress bar, review-queue count, recent transactions, quick-add button |
| Transactions | Date-grouped list, search, filters (account, category, date range, debit or credit), swipe to edit or delete |
| Add or edit | Amount keypad first, then category chips, account, date, note. Defaults to today and the last used account |
| Insights | Category split, monthly trend, top payees, month-over-month change |
| Settings | Accounts, categories, sender allowlist, budgets, theme, export, backup, permissions help |

### India-specific features

- Rupee formatting with lakh and crore grouping, and a compact form (₹1.2L) in tight spaces.
- UPI payee cleanup: strip handles and noise so "Zepto" shows as Zepto, and remember the choice.
- Per-payee memory: after you categorise a payee once, the same payee is auto-categorised next time.
- Salary-cycle months: let a month run from the salary date (for example the 1st or 28th) instead of calendar month.
- Credit card due-date reminders and cash as a first-class account.
- Hindi and Marathi UI strings later, with Devanagari-safe fonts from day one.

### Features most trackers have (include)

- Recurring transactions and bill reminders
- Per-category budgets as well as an overall budget
- CSV export and a local backup file
- Biometric lock
- Home-screen widget for quick add
- Dark mode

### Defer to v2

Multi-device sync, receipt photos, split bills, investments, and other banks' parsers beyond the first two or three.

### Reference screens

For each screen you send from other apps, add one line: what to copy (entry flow, chart style, spacing, empty state) and what to avoid. Hand those to Antigravity as images with those lines beside them, so it copies intent rather than pixels. Don't clone an app's branding or icons.

## Build phases

Build the parser and capture flow before the pretty screens. They are the riskiest part, and the UI has nothing real to show without them. Give Antigravity one phase at a time and don't move on until the acceptance check passes.

1. **Foundation.** Project, version catalog, Hilt, theme tokens from the brand section, navigation shell with the five empty screens. *Done when:* the app runs on your phone in light and dark with the real palette and fonts.
2. **Data layer.** Room entities, DAOs, repositories, seed categories, Money type with paise and Indian formatting. *Done when:* in-memory Room tests pass and ₹1,23,456.50 renders correctly.
3. **Parser.** Sender rules, three templates, fixtures, dedupe. *Done when:* every fixture passes as a plain unit test with no Android dependency.
4. **Capture.** Receiver, worker, notification with inline note, review queue. *Done when:* a test SMS sent to your phone produces a notification and a saved transaction within a few seconds, and a duplicate adds nothing.
5. **Core screens.** Home, transactions, add or edit, using your reference screens. *Done when:* you can log a manual expense in under five seconds.
6. **Insights and budgets.** Monthly budget bar, category split, trend chart. *Done when:* numbers match a hand-checked month.
7. **Polish and safety.** Empty states, motion, biometric lock, CSV export, backup, battery-optimisation onboarding, accessibility pass. *Done when:* you've used it as your only tracker for two weeks and logged what annoyed you.
8. **Widen the parser.** Add the next bank's templates and the card and failed-UPI messages as you collect them.

## Privacy, permissions and distribution

The privacy promise is that nothing leaves the phone, and the build should let anyone verify it.

- **No INTERNET permission** in v1. Add it only if you later add optional sync, and then behind an explicit setting.
- **Android auto-backup off** for the database, or backups encrypted, so transactions don't land in a Google backup unasked. Offer your own encrypted export instead.
- **Store parsed fields only.** Keep a hash of the SMS, not the body.
- **No logging of SMS text** in release builds, including Logcat.
- **Biometric or PIN lock** for opening the app. Hide amounts in the recents screen.
- **Optional database encryption** with SQLCipher if you want protection against a rooted or lost phone.

### Play Store reality

Google Play restricts SMS and Call Log permissions to apps whose core function is default SMS handling, and exceptions are narrow. A tracker that reads bank SMS generally doesn't qualify, so a Play release would need a different capture path, such as notification access or manual entry. Sideloading the APK works fine for yourself and friends. Check Google's current Play policy before you plan a public launch, because the rules change.

A release signed with a stable key lets updates install over the old version without wiping data.

## Paste into Antigravity

Save this as the rules file at the repo root, then give the agent one build phase at a time.

```markdown
# Kharcha: project rules

Kharcha is a native Android, local-first expense tracker for India. It parses bank SMS on-device.

## Stack
Kotlin, Jetpack Compose, Material 3, Room, Hilt, WorkManager, DataStore, Navigation Compose, Vico. Min SDK 26. Versions live only in gradle/libs.versions.toml.

## Hard rules
- Money is a Long in paise. Never Double or Float. Parse amounts with BigDecimal.
- No INTERNET permission, no analytics, no ads, no network calls.
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
```

### Open items to resolve

- Three to five masked KOTAKB-S messages, plus a look inside a KOTAKB-P thread.
- Failed or reversed UPI and credit card message samples.
- Your reference screens, each with one line on what to copy.
- Final seed colour and font after trying them in Theme Builder.
- Package name and keystore.
- Which second bank's parser comes after Kotak.

### Sources

- [Material Theme Builder](https://m3.material.io/theme-builder)
- [Icon Kitchen](https://icon.kitchen)
- [Google Fonts](https://fonts.google.com)
