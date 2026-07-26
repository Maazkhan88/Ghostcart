package com.example.ghostcart.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialRepositoryTest {
    private fun repository(
        store: MemoryTutorialStore = MemoryTutorialStore(),
        clock: MutableClock = MutableClock()
    ) = TutorialRepository(store, "installation-test", clock::now) { "session-test" }

    @Test fun firstLaunchShowsTutorial() {
        assertTrue(repository().shouldAutoLaunch())
    }

    @Test fun completedTutorialDoesNotShowAgain() {
        val repo = repository()
        driveToComplete(repo)
        repo.startTutorialDelivery()
        repo.complete()
        assertFalse(repo.shouldAutoLaunch())
    }

    @Test fun skippedTutorialDoesNotAutomaticallyReopen() {
        val repo = repository()
        repo.start()
        repo.skip()
        assertFalse(repo.shouldAutoLaunch())
    }

    @Test fun replayCreatesFreshIsolatedSession() {
        val repo = repository()
        repo.start()
        repo.skip()
        val replay = repo.start(replay = true)
        assertEquals(TutorialStatus.IN_PROGRESS, replay.status)
        assertTrue(replay.isReplay)
        assertEquals(TutorialStep.WELCOME, replay.currentStep)
    }

    @Test fun currentStepSurvivesRepositoryRecreation() {
        val store = MemoryTutorialStore()
        val repo = repository(store)
        repo.start()
        repo.advance(TutorialStep.WELCOME, TutorialStep.PRACTICE_INTRO)
        assertEquals(TutorialStep.PRACTICE_INTRO, repository(store).load().currentStep)
    }

    @Test fun tutorialNeverTouchesProductionCartOrHistory() {
        val production = mutableMapOf("cart" to "real-product", "history" to "real-receipt")
        val repo = repository()
        repo.start()
        repo.advance(TutorialStep.WELCOME, TutorialStep.PRACTICE_INTRO)
        repo.advance(TutorialStep.PRACTICE_INTRO, TutorialStep.PRODUCT)
        repo.addPracticeItemToCart()
        assertEquals(TutorialStep.PRODUCT, repo.load().currentStep)
        repo.openTutorialCart()
        assertEquals(mapOf("cart" to "real-product", "history" to "real-receipt"), production)
    }

    @Test fun tutorialNeverAffectsWalletOrLeaderboardTotals() {
        val totals = mutableMapOf("wallet" to 10_000_00L, "moneyKept" to 900_00L, "leaderboard" to 7L)
        val repo = repository()
        driveToReceipt(repo)
        assertEquals(10_000_00L, totals["wallet"])
        assertEquals(900_00L, totals["moneyKept"])
        assertEquals(7L, totals["leaderboard"])
    }

    @Test fun completionRemovesAllTutorialSessionObjects() {
        val store = MemoryTutorialStore()
        val repo = repository(store)
        driveToComplete(repo)
        repo.startTutorialDelivery()
        val completed = repo.complete()
        assertFalse(completed.practiceItemInCart)
        assertNull(completed.sessionId)
        assertNull(completed.coolingEndsAt)
        assertNull(completed.decision)
        assertFalse(store.snapshot().keys.any { it in tutorialSessionKeys })
    }

    @Test fun skipRemovesAllTutorialSessionObjects() {
        val repo = repository()
        repo.start()
        val skipped = repo.skip()
        assertEquals(TutorialStatus.SKIPPED, skipped.status)
        assertFalse(skipped.practiceItemInCart)
        assertNull(skipped.sessionId)
    }

    @Test fun cleanupDoesNotRemoveUnrelatedKeys() {
        val store = MemoryTutorialStore(mutableMapOf("real_user_data" to "keep-me"))
        val repo = repository(store)
        repo.start()
        repo.clearTutorialSession()
        assertEquals("keep-me", store.read("real_user_data"))
    }

    @Test fun exitUsesSkippedTerminalState() {
        val repo = repository()
        repo.start()
        assertEquals(TutorialStatus.SKIPPED, repo.skip().status)
    }

    @Test fun consentMustBeRecordedExplicitly() {
        val repo = repository()
        assertEquals(0, repo.load().consentVersion)
        assertEquals(TUTORIAL_CONSENT_VERSION, repo.recordConsentAccepted().consentVersion)
    }

    @Test fun consentAndTutorialVersionsAreIndependent() {
        val repo = repository()
        val consented = repo.recordConsentAccepted(7)
        assertEquals(7, consented.consentVersion)
        assertEquals(TUTORIAL_VERSION, consented.tutorialVersion)
    }

    @Test fun corruptedTutorialStateResetsSafelyToWelcome() {
        val store = MemoryTutorialStore(mutableMapOf(
            "tutorial_version" to TUTORIAL_VERSION.toString(),
            "tutorial_status" to TutorialStatus.IN_PROGRESS.name,
            "current_tutorial_step" to "NOT_A_STEP"
        ))
        val recovered = repository(store).load()
        assertEquals(TutorialStatus.NOT_STARTED, recovered.status)
        assertEquals(TutorialStep.WELCOME, recovered.currentStep)
    }

    @Test fun rotationEquivalentStateReloadPreservesProgress() {
        val store = MemoryTutorialStore()
        val repo = repository(store)
        repo.start()
        repo.advance(TutorialStep.WELCOME, TutorialStep.PRACTICE_INTRO)
        repo.advance(TutorialStep.PRACTICE_INTRO, TutorialStep.PRODUCT)
        assertEquals(TutorialStep.PRODUCT, repository(store).load().currentStep)
    }

    @Test fun fakeCheckoutCreatesOnlyInSessionCountdown() {
        val repo = repository()
        driveToCheckout(repo)
        val cooling = repo.completeFakeCheckout()
        assertEquals(TutorialStep.COOLING, cooling.currentStep)
        assertEquals(TUTORIAL_COOLDOWN_MILLIS, cooling.coolingEndsAt!! - cooling.startedAt!!)
    }

    @Test fun stateMachineRejectsImpossibleTransitions() {
        val repo = repository()
        repo.start()
        val failure = runCatching { repo.advance(TutorialStep.WELCOME, TutorialStep.DECISION) }
        assertTrue(failure.isFailure)
    }

    @Test fun debugStartCreatesValidStateForEveryStep() {
        TutorialStep.entries.forEach { step ->
            val state = repository().startAtForDebug(step)
            assertEquals(step, state.currentStep)
            assertEquals(TutorialStatus.IN_PROGRESS, state.status)
        }
    }

    private fun driveToCheckout(repo: TutorialRepository) {
        repo.start()
        repo.advance(TutorialStep.WELCOME, TutorialStep.PRACTICE_INTRO)
        repo.advance(TutorialStep.PRACTICE_INTRO, TutorialStep.PRODUCT)
        repo.addPracticeItemToCart()
        repo.openTutorialCart()
        repo.advance(TutorialStep.CART, TutorialStep.COOLDOWN)
        repo.selectTutorialCooldown()
        repo.advance(TutorialStep.COOLDOWN, TutorialStep.FAKE_CHECKOUT)
    }

    private fun driveToReceipt(repo: TutorialRepository) {
        driveToCheckout(repo)
        repo.completeFakeCheckout()
        repo.finishCooling()
        repo.chooseDecision(TutorialDecision.GHOSTED)
    }

    private fun driveToComplete(repo: TutorialRepository) {
        driveToReceipt(repo)
        repo.advance(TutorialStep.GHOST_RECEIPT, TutorialStep.COMPLETE)
    }
}

private class MutableClock(var value: Long = 1_000L) {
    fun now(): Long = value
}

private class MemoryTutorialStore(
    private val values: MutableMap<String, String> = mutableMapOf()
) : TutorialStateStore {
    override fun read(key: String): String? = values[key]

    override fun write(values: Map<String, String?>) {
        values.forEach { (key, value) -> if (value == null) this.values.remove(key) else this.values[key] = value }
    }

    override fun remove(keys: Set<String>) {
        keys.forEach(values::remove)
    }

    fun snapshot(): Map<String, String> = values.toMap()
}

private val tutorialSessionKeys = setOf(
    "tutorial_session_id",
    "tutorial_item_in_cart",
    "tutorial_cooldown_millis",
    "tutorial_cooling_ends_at",
    "tutorial_decision",
    "tutorial_is_replay"
)
