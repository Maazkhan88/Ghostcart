# Social login setup

Ghost Cart uses Android Credential Manager for the Google button. The app does
not contain a shared OAuth secret and does not fake a successful social login.

## Google

1. Create a Web application OAuth client in Google Auth Platform.
2. Configure the Android application `com.ghostcart.app` with the signing
   certificate SHA-256 used for the build.
3. Pass the Web client ID when building:

   ```powershell
   $env:GHOST_CART_GOOGLE_WEB_CLIENT_ID="your-client-id.apps.googleusercontent.com"
   .\gradlew.bat assembleDebug
   ```

The value is compiled into `BuildConfig.GOOGLE_WEB_CLIENT_ID`. When it is not
configured, Ghost Cart explains the missing setup instead of presenting a fake
account result.

## Apple

Sign in with Apple on Android requires an Apple Developer App ID, Services ID,
verified domain, return URL, and server-side authorization callback. Those
account-owned values are not present in this repository yet. The Apple button
therefore explains what is missing and must not claim a successful login until
the callback and token validation are configured.
