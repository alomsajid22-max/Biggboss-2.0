# Glass House (BIGGBOSS REALISTIC) — Firebase Setup Guide

This Android + Three.js multiplayer project (`com.aistudio.glasshouse.mrxkqp`) uses Firebase project **`gen-lang-client-0294875711`** and Firestore database **`ai-studio-android-glasshou-6f63f3be-6956-4275-928c-87e38913e873`**.

Player registration, login, and password resets work **directly via the official Firebase Authentication SDK** — **no custom `authRegister` or `authLogin` Cloud Functions deployment is required**.

---

## 1. Required Firebase Console Configuration (Email/Password Auth)

1. Open [Firebase Console](https://console.firebase.google.com/project/gen-lang-client-0294875711) and select **`gen-lang-client-0294875711`**.
2. Go to **Build → Authentication → Sign-in method**.
3. Click **Add new provider** (or edit **Email/Password**), **Enable** **Email/Password**, and click **Save**.
   > Until Email/Password is enabled in Firebase Console, registration and sign-in return `ERROR_OPERATION_NOT_ALLOWED` with a clear prompt to enable this provider.
4. **Google Sign-In (Already Provisioned, Optional)**: Configured via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).

---

## 2. How Registration, Roles & Security Work

- **Create Account**:
  - Players enter their **Username / Display Name** (3–24 chars), **Email Address**, **Password** (8+ chars), and choose 1 of the **15 3D Housemate Characters**.
  - `Firebase.auth.createUserWithEmailAndPassword` creates the Firebase Auth account securely (passwords are never stored in Firestore).
  - A Firestore transaction reserves `/displayNames/{normalizedName}` for display-name uniqueness and creates `/users/{uid}` with **`role = "PLAYER"`**.
- **Sign In & Persistent Sessions**:
  - Uses `Firebase.auth.signInWithEmailAndPassword` and persists the Firebase session automatically across app launches until the player taps **Sign Out**.
- **Forgot Password**:
  - Uses `Firebase.auth.sendPasswordResetEmail`.
- **Zero Client-Side Admin Elevation**:
  - All new registrations receive `PLAYER` role, enforced by `firestore.rules` (`incoming().role == 'PLAYER'` on create, immutable `role` on player update).

---

## 3. Phase 10: Deploy `BIGGBOSS` Admin Cloud Functions & Secret (Fixes HTTP 404)

Admin authentication (`adminVerifyLogin`), password rotation (`adminChangePassword`), session validation (`adminCheckSession`), session revocation (`adminLogout`), and authoritative voting tally (`calculateAndCloseVoting`) run server-side on **Firebase Cloud Functions (2nd Gen, `us-central1`)** for project **`gen-lang-client-0294875711`**.

If the app reports:
> `Admin authentication backend function 'adminVerifyLogin' is not deployed yet (HTTP 404)`

complete the following steps from the project root using Firebase CLI (requires Firebase Blaze plan for Cloud Functions v2 & Secret Manager):

1. **Authenticate and select the project**:
   ```bash
   firebase login
   firebase use gen-lang-client-0294875711
   ```
2. **Install Cloud Functions dependencies**:
   ```bash
   npm --prefix functions ci
   ```
3. **Configure the secure server secret (`BIGGBOSS_ADMIN_INITIAL_PASSWORD`)**:
   ```bash
   firebase functions:secrets:set BIGGBOSS_ADMIN_INITIAL_PASSWORD
   ```
   When prompted, enter the initial Admin password (`6+` characters). `adminVerifyLogin` binds this secret via `defineSecret('BIGGBOSS_ADMIN_INITIAL_PASSWORD')` — never put it in Android/client code.
4. **Ensure Custom Token signing IAM permission (Cloud Functions 2nd Gen)**:
   `adminVerifyLogin` mints a custom Firebase Admin token (`getAuth().createCustomToken`). If token minting fails with `signBlob` / `PERMISSION_DENIED`, grant `roles/iam.serviceAccountTokenCreator` to the compute runtime service account:
   ```bash
   gcloud projects add-iam-policy-binding gen-lang-client-0294875711 \
     --member="serviceAccount:697921706997-compute@developer.gserviceaccount.com" \
     --role="roles/iam.serviceAccountTokenCreator"
   ```
5. **Deploy the Admin Cloud Functions (and rules)**:
   ```bash
   firebase deploy --only functions:adminVerifyLogin,functions:adminChangePassword,functions:adminLogout,functions:adminCheckSession,functions:calculateAndCloseVoting,firestore:rules
   ```
   *(Or deploy everything with `firebase deploy --only functions,firestore:rules,database`.)*
6. **Verify the deployed endpoint resolves without HTTP 404**:
   ```bash
   curl -i -X POST \
     "https://us-central1-gen-lang-client-0294875711.cloudfunctions.net/adminVerifyLogin" \
     -H "Content-Type: application/json" \
     -d '{"username":"BIGGBOSS","password":"<YOUR_ADMIN_PASSWORD>","seasonId":"season_01"}'
   ```
   A healthy deployment returns **HTTP 200** with `"ok": true` and a short-lived `customToken` + `sessionId` (and **HTTP 401** for wrong passwords instead of HTTP 404).

---

## 4. Optional: Enable Live Realtime Database Sync

1. In Firebase Console → **Build → Realtime Database → Create Database** (Locked mode).
2. Download the updated `google-services.json` (containing `"firebase_url"`) to replace `app/google-services.json`.
3. Deploy rules if needed:
   ```bash
   firebase login
   firebase use gen-lang-client-0294875711
   firebase deploy --only firestore:rules,database,storage
   ```
   *(Without `firebase_url`, the app still runs cleanly in Firestore Lobby + Solo/Local House mode.)*

