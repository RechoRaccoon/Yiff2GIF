# Yiff2GIF

Android app: log into e621 with your username + API key, browse your favorites or
search tags, tap a post to instantly convert it (image/gif/video) into a GIF and
save it to your **Pictures/Yiff2GIF** album.

No Android Studio needed — GitHub Actions builds the APK for you.

## 1. Get your e621 API key
1. On e621.net, go to **Account → Settings → API Access**.
2. Generate a key if you don't have one, and note your username.
3. (There is no OAuth "connected apps" login for third-party apps on e621 —
   every existing e621 client authenticates with username + API key, which is
   what this app uses.)

## 2. Push this project to GitHub (from your phone)
1. Install the **GitHub** app (or use github.com in your browser) and log in.
2. Create a new empty repository, e.g. `Yiff2GIF`.
3. Easiest phone-only method: use the GitHub web uploader.
   - On the repo page, tap **Add file → Upload files**.
   - Upload this whole project folder's contents (keep the folder structure —
     `app/`, `.github/`, `build.gradle.kts`, `settings.gradle.kts`, etc.)
   - Commit directly to `main`.
   - (If your GitHub app/browser only lets you upload files one at a time,
     a file manager app that can extract zips and a "GitHub" client with
     folder-upload support, like Working Copy-style apps, makes this easier —
     but the web uploader does support drag-and-drop of a whole folder on
     desktop-mode browsers too.)

## 3. Build the APK
1. In your repo, go to the **Actions** tab.
2. You should see the **Build Yiff2GIF APK** workflow. Tap **Run workflow**
   (or just push a commit — it also runs automatically on push to `main`).
3. Wait for the green checkmark (a few minutes).
4. Open the finished run, scroll to **Artifacts**, and download
   **Yiff2GIF-apk**. It's a zip containing your `.apk`.

## 4. Install it on your phone
1. Unzip the artifact (any file manager app can do this) to get the `.apk`.
2. Tap the `.apk` file to install it. Android will ask you to allow installs
   from this source (Files app / browser) the first time — allow it.
3. Open Yiff2GIF, enter your e621 username + API key, and you're in.

## About the app's signature — "RechoRaccoon.Yiff2GIF"
- The app's package/application ID is `rechoraccoon.yiff2gif` (Android
  requires lowercase package identifiers — uppercase isn't valid there).
- The workflow builds an **unsigned debug APK** by default, which is fine for
  installing on your own phone.
- If you'd like the APK cryptographically signed with your own key (so you can
  cleanly upgrade the app later without uninstalling), you can make the key's
  **alias** literally `RechoRaccoon.Yiff2GIF`:
  1. On a computer (or Termux on your phone) run:
     ```
     keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias RechoRaccoon.Yiff2GIF
     ```
  2. Base64-encode it: `base64 keystore.jks > keystore_b64.txt`
  3. In your GitHub repo, go to **Settings → Secrets and variables → Actions**
     and add these repository secrets:
     - `KEYSTORE_BASE64` — contents of `keystore_b64.txt`
     - `KEYSTORE_PASSWORD` — the password you set
     - `KEY_ALIAS` — `RechoRaccoon.Yiff2GIF`
     - `KEY_PASSWORD` — the key password you set
  4. Re-run the Actions workflow — it will now build a **signed release APK**
     instead of a debug one.

## Notes on how it works
- **Auth**: username + API key sent as HTTP Basic Auth on every e621 request.
- **Favorites/search**: `favorites.json` and `posts.json` endpoints, paginated,
  loading more automatically as you scroll to the bottom.
- **Grid resize**: pinch in/out changes the column count by exactly ±1 per
  full pinch gesture (not continuous), from 1 to 10 columns, and the size is
  remembered between app launches.
- **Conversion**:
  - Already a `.gif` → downloaded as-is.
  - A static image (jpg/png/webp) → wrapped into a single-frame GIF.
  - A video (webm/mp4) → up to 24 frames are sampled across its duration and
    encoded into an animated GIF.
- **Download feedback**: tapping a post blurs it and shows a spinner; once
  saved, it gets a `#00ff07` outline and checkmark (still blurred) so you can
  keep tapping other posts while downloads finish in the background.
- Files are saved to the `Pictures/Yiff2GIF` album via MediaStore.
