# Android Cooling Off Live Update — reconciliation plan

Status: planning only. No code changed to produce this document. Read alongside `android/ANDROID_LIVE_UPDATES_IMPLEMENTATION_PLAN.md` (the canonical spec, authored on `main` at commit `0c43050`, written for "Codex or any engineer" to implement Cooling Off parity with iOS's shipped `ios/GhostCart/LiveActivity/`) - this document exists because that spec was written without visibility into work already sitting on `agent/ghost-delivery-v1`, and the two need reconciling before anyone starts implementing.

## Decision: keep the existing Ghost Delivery live update as-is

`GhostDeliveryLiveUpdate.kt` (already built and pushed on this branch, commit `2192dbe`) stays exactly as it is - not rewritten, not replaced. It maps directly onto what the canonical spec itself calls out as a legitimate, deliberately-deferred "v2" feature:

> "Ghost Delivery stage tracker (placed → preparing → out for delivery → nearby → delivered) ... Yes — good second candidate, not in this plan's v1 scope ... Worth a v2 once the countdown ships and is validated."

So this branch has effectively already built the thing the spec names as v2, just without v1 (the plain Cooling Off countdown) existing first. That's fine - the ordering doesn't matter as much as both eventually existing and being reconciled correctly, which is what this document is for.

## The finding that changes the plan: cooldown-start and order-placed are now the same event

The canonical spec's hook points (§4.2) assume `AppViewModel.startCoolingPeriod(...)` is where a cooldown begins, mirroring iOS's `GhostCartStore.startCooling`. That's accurate **on `main`**, where the spec was written. It is **not accurate on `agent/ghost-delivery-v1`**: `startCoolingPeriod` doesn't exist on this branch at all. Confirmed by grep - the only places `AlmostBuyStatus.COOLING` gets set and `GhostDeliveryScheduler.schedule(...)` gets called are the same call sites (`AppViewModel.kt:1295`, `:1357`), because this branch's cart-first redesign (`Ghost it` → adds to cart only; delivery time chosen at checkout) collapsed "cooldown starts" and "Ghost Order placed" into a single event. There is no longer a moment where an item is cooling *without* a Ghost Order already existing for it, the way there still is on `main`/iOS today.

**Consequence:** if a separate "Cooling Off" live update were built to literally mirror the spec's hook points on this branch, it would start at the exact same instant as the existing `GhostDeliveryLiveUpdate`, for the exact same item, showing two ongoing/promoted notifications for one countdown. That's not feature parity, it's a redundant, confusing duplicate - two system-level ongoing notifications competing for the same status-bar/Now-Bar slot for the same thing.

## What "same features as iOS" should actually mean here, given that

Not "add a second notification alongside the existing one." The two real options:

1. **Upgrade `GhostDeliveryLiveUpdate` to meet the spec's technical bar, keep its stage-narrative framing.** iOS doesn't have a six-stage delivery narrative in its Live Activity today - Android's existing implementation is already ahead of iOS on richness. What Android is *behind* on is the technical correctness items the spec is explicit about, which this branch's implementation currently lacks (confirmed by reading `GhostDeliveryLiveUpdate.kt` against the spec, not assumed):
   - **`POST_PROMOTED_NOTIFICATIONS` permission** - not declared in the manifest, not checked before calling `setRequestPromotedOngoing(true)`. Real gap: promotion may silently never engage on a real Android 16 device without it.
   - **SDK-version gating** - the spec calls for explicit `Build.VERSION.SDK_INT >= 36` guards around `ProgressStyle`/`setRequestPromotedOngoing`, with a legacy `setProgress(max, progress, false)` fallback below that (`minSdk = 24`). The current implementation calls these unconditionally.
   - **Dedicated channel** - the spec wants a new channel separate from one-shot push channels, for the reasons in its §3 (users muting the ongoing countdown independently of the one-shot "resolved" push). Current implementation reuses the existing delivery channel.
   - **"Almost done" threshold copy** - the spec's `WorkManager`-scheduled single checkpoint at ~10 minutes remaining (not a poll) isn't present in the current implementation.
   - This path keeps Android's actual product decision (a richer, stage-narrative live update) while closing the gaps that would stop it from actually promoting/appearing correctly - which is very likely the real explanation for "I don't see Ghost Cart live updates anywhere" once this ships as an APK, on top of the branch simply being unmerged.

2. **Build a literal, separate plain "Cooling Off" notification per the spec, and suppress/replace the delivery one for the same window.** True 1:1 parity with iOS's current copy/status model, but throws away the richer stage narrative Android already has for no product benefit, and is more total work (two full implementations to maintain instead of one upgraded).

**Recommendation: option 1.** It's less work, doesn't regress anything already built, and closes real functional gaps rather than adding a redundant surface. It also matches the spirit of the canonical spec's own §9 ("don't retrofit... speculatively - extract shared code only once there are two real call sites") - there's only one real call site here (Ghost Order placement), not two.

## What this plan does NOT decide

- Whether to literally rename `GhostDeliveryLiveUpdate` → something matching the spec's `CoolingLiveUpdateManager` naming for cross-platform consistency, vs. keeping its current name since it's now doing something iOS's `LiveActivityManager` doesn't (stage narrative). Naming call, not a functional one - flag for whoever implements.
- The exact wording for the "almost done" threshold state layered onto the existing stage copy (the spec's "👻 Almost free" doesn't map cleanly onto "Ghost Rider picking up" - needs actual product/copy decision, not just an engineering one).
- Whether `main`'s iOS-authored spec doc should be edited in place to note this reconciliation, or left as-is with this document as the pointer. Leaning toward leaving it as-is (it's still accurate for iOS and for `main`'s current architecture) and treating this document as the Android-branch-specific addendum.

## Next step

Waiting on explicit go-ahead before implementing anything from either option - this document is the plan, not the start of the work.
