# BIGG BOSS – THE HOUSE (Glass House 3D Multiplayer)

Native Android (Kotlin + Jetpack Compose) and Three.js WebGL 3D multiplayer reality-house game (`com.aistudio.glasshouse.mrxkqp`).

## Implemented Phases (Phases 1–8)

1. **Phases 1–4 — 3D Mansion, 15-Player Multiplayer, Realistic Avatars & Firebase Auth**
   - 3D Glass House with Living Atrium, Spiral Vine-Wrapped Luxury Tree, Gourmet Kitchen, Dining Area, 15-Player Bedroom, Botanical Garden, Activity Room, Main Hall, Pool Deck, and Smoking Lounge.
   - Procedural articulated human-proportioned 3D characters with male/female presets, hairstyles, outfits, skin tones, smooth animation blending (idle, walk, run, sit, sleep, swim, dance, cook, eat, seated eat), and facial expressions.
   - Mobile dual-thumb touchscreen controls (`overscroll-behavior: none`, zoom prevention) and Android safe-area insets.
   - Official Firebase Email/Password & Google Sign-In (`CredentialManager`), zero-trust `/admins/{uid}` role verification, and RTDB live transforms/doors/seats/lighting.
2. **Phase 5 — Admin Control Room, CCTV Wall & Voting / Evictions**
   - Admin-only Control Suite (Overview, Player Moderation & Ban/Kick, Live 7-Camera 3D CCTV Matrix + Firebase Storage archive, Voting Phone & Nominations, Evictions, Multi-language TTS Announcements).
3. **Phase 6 — Tasks, Teams, Captaincy, House Rules & Daily Schedule**
   - Interactive 3D & Hub tasks (`MEMORY`, `QUIZ`, `PUZZLE`, `TREASURE_HUNT`, `TEAM_COORDINATION`, `OBSERVATION`, `OBSTACLE`), Teams builder, House Captain assignment, Rules & live violation warning banners, and 8-slot Daily Schedule.
4. **Phase 7 — Kitchen, Rations & Virtual Cooking**
   - Interactive Gourmet Kitchen (`kitchen_fridge`, `kitchen_stove_1`, `kitchen_stove_2`, `kitchen_sink`, `kitchen_prep`, `kitchen_dining_serve`) with live stove boiling/steam/glow visual state.
   - Shared house ration pantry with atomic Firestore transactions, stock alerts (`AVAILABLE`, `LOW_STOCK`, `OUT_OF_STOCK`, `RESTRICTED`), player pouch, cooldowns, 5 virtual in-game dishes, Energy/Hunger nutrition meters, seated dining eat interaction, and Admin ration replenish/lock & cooking session override controls.
5. **Phase 8 — Advanced Task System, Checkpoints, Leaderboard & Anti-Duplicate Rewards**
   - 8 Task Categories (`INDIVIDUAL`, `GROUP`, `TEAM`, `DAILY`, `WEEKLY`, `HOUSE`, `CAPTAIN`, `SECRET`), 4 Difficulty multipliers (`EASY` 1.0x → `EXTREME` 2.0x), and Fair Randomizer assignment (`generateFairTaskAssignment`).
   - 3D Room Checkpoints (`CHECKPOINT`), Kitchen Culinary Duty (`COOKING`), live per-player progress bars, deterministic `rewardGrants` (`TASK_{taskId}_{uid}`, `COOK_{sessionId}_{uid}`), unlocked achievements, and live Leaderboard (`Player` Hub + `Admin` Suite).

---

## Build & Run Android APK

- **Min SDK**: `24` (Android 7.0)
- **Target / Compile SDK**: `36`
- **Application ID**: `com.aistudio.glasshouse.mrxkqp`

### Build Debug APK (Command Line)
```bash
gradle assembleDebug
```
Output APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Build in Android Studio
1. Open the repository root in **Android Studio**.
2. Let Gradle sync (`gradle/libs.versions.toml`).
3. Ensure `app/google-services.json` is configured for your Firebase project (see `FIREBASE_SETUP.md`).
4. Run **Build → Build Bundle(s) / APK(s) → Build APK(s)** or run directly on a physical Android device via USB debugging.

---

## GitHub Setup Notes

- Sensitive files (`.env`, local `debug.keystore`, `local.properties`, `/build`, `.gradle`, `functions/node_modules`) are ignored via `.gitignore`.
- See `FIREBASE_SETUP.md` for enabling Firebase Auth Email/Password, designating `/admins/{uid}`, and deploying Firestore / Realtime Database / Storage security rules.
