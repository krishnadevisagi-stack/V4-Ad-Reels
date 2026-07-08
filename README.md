<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

**PROJECT CONSTITUTION**

This application is a transparent advertisement discovery and eligible reward platform. The application must never generate artificial advertisement activity, incentivize unsupported interactions, fabricate advertiser identities, manipulate provider-controlled advertisement behaviour, or bypass provider policies. Home, Reels, Profile, Advertisement, Reward, Wallet, Analytics and Compliance must remain architecturally separated. Dummy prototype behaviour must never silently enter production. Every provider and ad format must be evaluated independently. Policy compliance and official SDK behaviour take priority over old prototype requirements, business logic and visual design.

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/ff0499df-f0c0-4011-b45e-60f7bfe321d8

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
