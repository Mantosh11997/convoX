# convoX — WhatsApp-style Chat App (Supabase edition)

Kotlin + Supabase (Auth + Postgres + Realtime) Android app: unique usernames,
friend requests (send / accept / reject), and real-time 1:1 chat once a
request is accepted. No Firebase, no credit card needed.

## 1. Create a Supabase project (free, ~3 minutes)

1. Go to https://supabase.com -> Sign up (GitHub login works) -> **New project**.
   Pick any name (e.g. "convox"), set a database password, choose a region.
2. When the project finishes provisioning, open **SQL Editor** (left sidebar)
   -> **New query** -> paste the ENTIRE contents of `supabase_setup.sql`
   from this folder -> **Run**. This creates all tables, security rules,
   and server functions in one shot.
3. Turn off email confirmation so signups work instantly:
   **Authentication -> Sign In / Providers -> Email** -> disable
   **"Confirm email"** -> Save.
4. Get your keys: **Project Settings (gear) -> API**. Copy:
   - **Project URL** (looks like `https://abcdefgh.supabase.co`)
   - **anon / public** key (long string)

## 2. Put your keys in the app

Open `app/src/main/java/com/convox/app/util/Supa.kt` and replace:

```kotlin
private const val SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co"
private const val SUPABASE_ANON_KEY = "YOUR_ANON_PUBLIC_KEY"
```

with your real values. (The anon key is designed to be public — Row Level
Security in the database is what protects the data.)

You can edit the file directly on GitHub's website after uploading if that's
easier: open the file -> pencil icon -> paste values -> Commit.

## 3. Build the APK — two options

### Option A: GitHub Actions (no Android Studio needed)

1. Create a new GitHub repository (public or private).
2. Push/upload this whole folder to it (make sure the hidden `.github`
   folder is included — `git push` is the reliable way; if you use the web
   uploader and Actions doesn't appear, create
   `.github/workflows/build-apk.yml` manually via Add file -> Create new file
   and paste the workflow content).
3. The "Build convoX APK" workflow runs on every push (or trigger it from
   the **Actions** tab -> Run workflow). It fails with a clear message if
   you forgot step 2 above.
4. When it finishes (~3-5 min), open the run -> **Artifacts** -> download
   **convoX-debug-apk** -> unzip -> install `app-debug.apk` on your phone
   (enable "Install unknown apps" when prompted).

### Option B: Android Studio (local build)

1. Install Android Studio: https://developer.android.com/studio
2. **File -> Open** -> select this `convoX` folder, let Gradle sync.
3. **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** -> click **locate**
   -> `app/build/outputs/apk/debug/app-debug.apk`.

## How it works

- **Sign up**: username availability is enforced by a UNIQUE constraint in
  Postgres via the `claim_username` function — two people can never take the
  same name, even simultaneously.
- **Log in**: the `get_login_email` SQL function (security definer) resolves
  a username to its account email server-side, then normal email+password
  sign-in happens. Other users' emails are never exposed.
- **Add Friend**: search profiles by username prefix, insert a row into
  `friend_requests` (RLS ensures you can only send as yourself).
- **Accept**: the `accept_friend_request` function atomically marks the
  request accepted and creates the shared chat row — only the recipient can
  accept, enforced in SQL.
- **Chat**: messages insert into the `messages` table; Supabase Realtime
  pushes changes to both phones instantly. RLS guarantees only the two
  participants can read or write a chat.

## Free tier notes (as of 2026)

- 50,000 monthly active users, 500 MB database, Realtime included — no
  credit card required.
- Free projects pause after ~1 week of inactivity; just hit "Restore" in the
  dashboard to wake them (takes a minute or two).

## Ideas to extend

- Online/typing indicators via Supabase Realtime "presence"
- Image sharing via Supabase Storage (1 GB free)
- Push notifications when the app is closed (needs FCM or a service like
  OneSignal — Supabase doesn't push to closed apps by itself)
