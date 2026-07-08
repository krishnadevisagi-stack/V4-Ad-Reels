# AI Agent Guidelines

## POLICY-FIRST IMPLEMENTATION LOCK

Before implementing any advertisement-related feature, determine the active provider and ad format. Do not assume that a behavior allowed for one format is allowed for another. Never transfer dummy prototype reward logic into a production advertisement provider automatically. If a requested feature conflicts with the current official provider policy, SDK lifecycle, or supported format behavior, do not implement the feature as requested. Implement the closest policy-compliant alternative and document the change.

## 🚫 "Never Do This" Master List

1. **Coins for Clicking Ads**: Clicks must never equal coin rewards. Never display "Click to Earn", "Tap Ad for Bonus", or "Install to Unlock Coins".
2. **Forced Rewarded Experience**: Never force users into rewarded experiences. No "Open Reels -> Immediately Force Ad -> Block User" flows.
3. **Automatic Feed Reward**: Native feed impressions must never grant rewards unless explicitly reviewed and verified through the FeedRewardPolicyGate.
4. **Production Ads for Testing**: Development and testing must strictly use Google Test Ad Units. Never test with live production ad units.
5. **Fabricating Advertiser Profiles**: Never scrape, predict, or reverse-engineer fake advertiser details or follower counts to make ads look like organic profiles.
6. **CTA Interception**: Never rewrite, cover, or intercept official provider-controlled Call-To-Action behaviors.

## 🏁 Mandatory QA Checklists

### 📸 Native Feed Checklist
- [ ] Advertisement is clearly identified (e.g., "Sponsored").
- [ ] Official provider assets are used for rendering.
- [ ] No fake profile metadata, follower counts, or badges.
- [ ] Card header is completely non-clickable.
- [ ] CTA button behaviour is preserved without modification or interception.
- [ ] No click reward exists.
- [ ] No wallet animation overlays or points at the ad CTA.
- [ ] Accidental click areas are eliminated; layout is separated.
- [ ] Test ad units are used during development.

### 🎬 Rewarded Reels Checklist
- [ ] Only official rewarded ad format is used (no custom overlays on video players).
- [ ] Entry into the rewarded experience is voluntary (explicit opt-in).
- [ ] Playback timers and completion states are controlled entirely by the provider SDK.
- [ ] No custom 5-second reward timer overrides real callbacks.
- [ ] Rewards are credited strictly after verified SDK callbacks.
- [ ] No click rewards exist.
- [ ] One validated callback yields exactly one reward.
- [ ] Failure states handle gracefully without blocking user navigation.

## 🌐 Chapter 9: Multi-Provider & Hybrid Expansion Lock

### 🔒 Permanent Expansion Architecture Rules
1. **Never Assumption Transfer**: Do not copy or transfer assumptions between different providers or formats. Each provider must declare its unique capability set and go through independent policy verification.
2. **Abstract Multi-Provider Core**: The core UI layers must never depend on any specific ad provider. They must only use the abstract interfaces and map results into the unified application models.
3. **Capability-Aware Registry**: All adapters must register their capabilities and policy statuses dynamically.
4. **Governed Decision-Order Selection**: Selecting an active provider must dynamically evaluate: Compliance -> Consent -> Format Compatibility -> Circuit Breaker Status -> Performance.
5. **No Silent Format Substitutions**: If a rewarded video is requested but fails, do not silently substitute it with a native image format without updating the UI state and reward eligibility accordingly.
6. **Append-Only Server Ledger**: Real redemption requires complete provider-blind validated transaction ledgers managed and signed on the server side.

### 🏁 Chapter 9 Release Gates
- **New Provider Gate**:
  - [ ] Implementation of full adapter pattern interface.
  - [ ] Verification of capabilities in the global registry.
  - [ ] Consent management integration checked.
  - [ ] Isolation of provider-specific SDK callbacks from UI code.
  - [ ] Technical health circuit breaker verified.
- **Direct Advertiser Gate**:
  - [ ] Sponsoring identification strictly verified.
  - [ ] Full campaign metadata captured without guessing.
  - [ ] Destination URL whitelist matching active.
  - [ ] Ledger verification checks complete before reward distribution.

