# Fold Calendar Widget

A native Android home-screen widget (Kotlin + Jetpack Glance) for the Samsung
Galaxy Fold that shows your upcoming Microsoft 365 / Outlook (OneDrive
account) calendar appointments, and adapts its layout between the Fold's
cover screen and its unfolded inner screen.

- **Cover screen (narrow)** — the time of your next appointment.
- **Unfolded (wide)** — a list of upcoming appointments with time, subject
  and location.
- Tapping the widget opens your Outlook web calendar (or, if you're not
  signed in yet, opens the app to sign in).

Reading someone's Outlook/OneDrive calendar requires signing into their
Microsoft account with OAuth — there's no way around that, so this project
includes a small host app (`MainActivity`) with a **"Sign in with
Microsoft"** button, built on the Microsoft Authentication Library (MSAL).
**You must register your own Azure AD app before this will authenticate** —
see below.

## One-time setup: register an Azure AD app

1. Go to [entra.microsoft.com](https://entra.microsoft.com) (or the Azure
   Portal → **Microsoft Entra ID**) → **App registrations** → **New
   registration**.
   - Name: anything, e.g. "Fold Calendar Widget".
   - Supported account types: **"Accounts in any organizational directory
     and personal Microsoft accounts"** (so it works with a personal
     OneDrive/Outlook.com account as well as a work/school one) — narrow
     this if you only ever want one account type.
   - Leave Redirect URI blank for now; add it in step 3.
   - Click **Register**, then copy the **Application (client) ID** from the
     Overview page.

2. **API permissions** → **Add a permission** → **Microsoft Graph** →
   **Delegated permissions** → search for and add **Calendars.Read**.
   (`offline_access` and `openid`/`profile` are included automatically by
   MSAL.) Personal Microsoft accounts don't need admin consent; a work/school
   tenant might, depending on your org's policy — click **Grant admin
   consent** if your tenant requires it.

3. Get your app's signature hash (Android needs this to lock down the OAuth
   redirect to your APK). For the debug keystore Android Studio uses by
   default:

   ```
   keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore \
     -storepass android | openssl sha1 -binary | openssl base64
   ```

   That prints something like `xIQvaTMPhh6C1ZQjIhTBnbz4WA0=`. URL-encode it
   for the manifest (`+` → `%2B`, `/` → `%2F`, `=` → `%3D` if present) — for
   the JSON config below you can use it as-is.

4. Back in the Azure app registration → **Authentication** → **Add a
   platform** → **Android**:
   - Package name: `com.foldcalendar.widget`
   - Signature hash: paste the value from step 3.
   - Save. Azure will show you the exact `msauth://...` redirect URI it
     registered — copy it.

5. In this repo, replace the placeholders:
   - `app/src/main/res/raw/auth_config_single_account.json`:
     `client_id` → your Application (client) ID; `redirect_uri` → the
     `msauth://com.foldcalendar.widget/<signature-hash>` URI from step 4.
   - `app/src/main/AndroidManifest.xml`: in the `BrowserTabActivity`
     intent-filter, replace `PLACEHOLDER_SIGNATURE_HASH` in the
     `android:path` with the **URL-encoded** signature hash (same value,
     just percent-encoded — this one is matched against the incoming
     redirect, not free text).

If you ever switch to a release keystore, repeat steps 3–5 for that
keystore's signature hash (you can register multiple Android platforms/redirect
URIs on the same Azure app, one per keystore).

## Project layout

```
samsung-fold-widget/app/src/main/java/com/foldcalendar/widget/
  MainActivity.kt              # sign-in / sign-out screen (MSAL)
  MsalAuthManager.kt           # suspend wrapper over MSAL's single-account API
  GraphCalendarClient.kt       # GET /me/calendarView against Microsoft Graph
  Appointment.kt               # appointment model + JSON (de)serialization
  FoldCalendarWidget.kt        # Glance composable, responsive cover/unfolded layouts
  FoldCalendarWidgetReceiver.kt# AppWidgetProvider entry point, schedules refresh
  CalendarRefreshWorker.kt     # WorkManager job: silent token refresh + Graph fetch
```

## How it stays up to date

- `FoldCalendarWidgetReceiver` enqueues a `WorkManager` periodic job (15
  minutes — the shortest interval WorkManager allows) plus one immediate
  one-time job whenever the widget is added.
- `MainActivity` also kicks off an immediate refresh right after a
  successful sign-in, so the widget doesn't wait up to 15 minutes for its
  first data.
- The worker never prompts interactively — it only calls
  `acquireTokenSilent()`. If that fails (no signed-in account, or the
  refresh token was revoked), the widget falls back to "Tap to sign in"
  and the next tap opens `MainActivity` to sign in properly.

## Building

Authored in a sandboxed environment without access to `dl.google.com`
(Google's Maven repo, needed for the Android Gradle Plugin and AndroidX
artifacts), so it hasn't been compiled here — a GitHub Actions workflow
(`.github/workflows/build-fold-widget.yml`) builds it on push instead. To
build locally: open `samsung-fold-widget/` in Android Studio (Iguana or
newer), let it sync, fill in the Azure placeholders above, then run on a
Fold device or the Fold emulator skin.

**The CI-built APK will install and the UI will render fine even before you
fill in the Azure placeholders — sign-in just won't succeed until you do.**

## Known limitations

- MSAL version pinned to `5.6.0`; bump it if Android Studio flags it as
  outdated.
- Only reads the primary calendar's next 7 days, top 8 events
  (`GraphCalendarClient.LOOKAHEAD_DAYS` / `MAX_RESULTS`) — adjust to taste.
- Not yet compiled/tested against a real SDK or a live Azure app — see
  "Building" above.
