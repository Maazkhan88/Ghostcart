package com.example.ghostcart.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

const val TUTORIAL_VERSION = 1
const val TUTORIAL_CONSENT_VERSION = 1
const val TUTORIAL_PRODUCT_ID = "tutorial_coffee_donut_v1"
const val TUTORIAL_COOLDOWN_MILLIS = 10_000L

enum class TutorialStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, SKIPPED }

enum class TutorialStep {
    WELCOME,
    PRACTICE_INTRO,
    PRODUCT,
    CART,
    COOLDOWN,
    FAKE_CHECKOUT,
    COOLING,
    DECISION,
    GHOST_RECEIPT,
    COMPLETE,
    DELIVERY
}

enum class TutorialDecision { GHOSTED, COOL_LONGER, STILL_BUY }

data class TutorialState(
    val tutorialVersion: Int = TUTORIAL_VERSION,
    val consentVersion: Int = 0,
    val status: TutorialStatus = TutorialStatus.NOT_STARTED,
    val currentStep: TutorialStep = TutorialStep.WELCOME,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val installationId: String = "",
    val sessionId: String? = null,
    val practiceItemInCart: Boolean = false,
    val selectedCooldownMillis: Long? = null,
    val coolingEndsAt: Long? = null,
    val decision: TutorialDecision? = null,
    val isReplay: Boolean = false
)

interface TutorialStateStore {
    fun read(key: String): String?
    fun write(values: Map<String, String?>)
    fun remove(keys: Set<String>)
}

class SharedPreferencesTutorialStateStore(
    private val preferences: SharedPreferences
) : TutorialStateStore {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(values: Map<String, String?>) {
        preferences.edit().apply {
            values.forEach { (key, value) ->
                if (value == null) remove(key) else putString(key, value)
            }
        }.apply()
    }

    override fun remove(keys: Set<String>) {
        preferences.edit().apply { keys.forEach(::remove) }.apply()
    }
}

/**
 * Durable tutorial state machine. It intentionally owns no production Product, cart,
 * AlmostBuy, order, receipt, wallet, analytics-total, or WorkManager state.
 */
class TutorialRepository(
    private val store: TutorialStateStore,
    private val installationId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val newSessionId: () -> String = { UUID.randomUUID().toString() }
) {
    fun load(): TutorialState {
        val storedVersion = store.read(KEY_VERSION)?.toIntOrNull()
        if (storedVersion != null && storedVersion != TUTORIAL_VERSION) {
            resetForNewVersion()
        }
        return readStateOrReset()
    }

    fun shouldAutoLaunch(): Boolean = load().status in setOf(
        TutorialStatus.NOT_STARTED,
        TutorialStatus.IN_PROGRESS
    )

    fun recordConsentAccepted(version: Int = TUTORIAL_CONSENT_VERSION): TutorialState =
        persist(load().copy(consentVersion = version.coerceAtLeast(1)))

    fun start(replay: Boolean = false): TutorialState {
        clearTutorialSession()
        return persist(
            TutorialState(
                consentVersion = loadConsentVersion(),
                status = TutorialStatus.IN_PROGRESS,
                currentStep = TutorialStep.WELCOME,
                startedAt = now(),
                installationId = installationId,
                sessionId = newSessionId(),
                isReplay = replay
            )
        )
    }

    fun advance(expected: TutorialStep, next: TutorialStep): TutorialState {
        val current = load()
        require(current.status == TutorialStatus.IN_PROGRESS) { "Tutorial is not active" }
        require(current.currentStep == expected) { "Expected $expected but was ${current.currentStep}" }
        require(ALLOWED_TRANSITIONS[expected]?.contains(next) == true) { "Invalid tutorial transition: $expected -> $next" }
        return persist(current.copy(currentStep = next))
    }

    fun addPracticeItemToCart(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.PRODUCT && current.status == TutorialStatus.IN_PROGRESS)
        return persist(current.copy(practiceItemInCart = true))
    }

    fun openTutorialCart(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.PRODUCT && current.status == TutorialStatus.IN_PROGRESS)
        require(current.practiceItemInCart)
        return persist(current.copy(currentStep = TutorialStep.CART))
    }

    fun selectTutorialCooldown(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.COOLDOWN && current.status == TutorialStatus.IN_PROGRESS)
        return persist(current.copy(selectedCooldownMillis = TUTORIAL_COOLDOWN_MILLIS))
    }

    fun returnToTutorialCart(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.COOLDOWN && current.status == TutorialStatus.IN_PROGRESS)
        return persist(current.copy(currentStep = TutorialStep.CART, selectedCooldownMillis = null))
    }

    fun completeFakeCheckout(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.FAKE_CHECKOUT && current.status == TutorialStatus.IN_PROGRESS)
        require(current.practiceItemInCart && current.selectedCooldownMillis == TUTORIAL_COOLDOWN_MILLIS)
        return persist(
            current.copy(
                currentStep = TutorialStep.COOLING,
                coolingEndsAt = now() + TUTORIAL_COOLDOWN_MILLIS
            )
        )
    }

    fun finishCooling(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.COOLING && current.status == TutorialStatus.IN_PROGRESS)
        return persist(current.copy(currentStep = TutorialStep.DECISION, coolingEndsAt = null))
    }

    fun chooseDecision(decision: TutorialDecision): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.DECISION && current.status == TutorialStatus.IN_PROGRESS)
        return persist(current.copy(currentStep = TutorialStep.GHOST_RECEIPT, decision = decision))
    }

    fun startTutorialDelivery(): TutorialState = advance(TutorialStep.COMPLETE, TutorialStep.DELIVERY)

    fun complete(): TutorialState {
        val current = load()
        require(current.currentStep == TutorialStep.DELIVERY && current.status == TutorialStatus.IN_PROGRESS)
        val terminal = current.copy(status = TutorialStatus.COMPLETED, completedAt = now())
        clearTutorialSession()
        return persist(terminal.withoutSessionObjects())
    }

    fun skip(): TutorialState {
        val current = load()
        clearTutorialSession()
        return persist(
            current.copy(
                status = TutorialStatus.SKIPPED,
                currentStep = TutorialStep.WELCOME
            ).withoutSessionObjects()
        )
    }

    /** Clears tutorial-only objects. It never opens or edits any production repository. */
    fun clearTutorialSession() {
        store.remove(SESSION_KEYS)
    }

    fun resetForDebug(): TutorialState {
        clearTutorialSession()
        return persist(
            TutorialState(
                consentVersion = loadConsentVersion(),
                installationId = installationId
            )
        )
    }

    fun startAtForDebug(step: TutorialStep): TutorialState {
        clearTutorialSession()
        val state = TutorialState(
            consentVersion = loadConsentVersion().coerceAtLeast(TUTORIAL_CONSENT_VERSION),
            status = TutorialStatus.IN_PROGRESS,
            currentStep = step,
            startedAt = now(),
            installationId = installationId,
            sessionId = newSessionId(),
            practiceItemInCart = step.ordinal >= TutorialStep.CART.ordinal,
            selectedCooldownMillis = TUTORIAL_COOLDOWN_MILLIS.takeIf { step.ordinal >= TutorialStep.FAKE_CHECKOUT.ordinal },
            coolingEndsAt = (now() + TUTORIAL_COOLDOWN_MILLIS).takeIf { step == TutorialStep.COOLING },
            decision = TutorialDecision.GHOSTED.takeIf { step.ordinal >= TutorialStep.GHOST_RECEIPT.ordinal },
            isReplay = true
        )
        return persist(state)
    }

    private fun readStateOrReset(): TutorialState = runCatching {
        val status = enumValueOf<TutorialStatus>(store.read(KEY_STATUS) ?: TutorialStatus.NOT_STARTED.name)
        val step = enumValueOf<TutorialStep>(store.read(KEY_STEP) ?: TutorialStep.WELCOME.name)
        TutorialState(
            tutorialVersion = store.read(KEY_VERSION)?.toIntOrNull() ?: TUTORIAL_VERSION,
            consentVersion = loadConsentVersion(),
            status = status,
            currentStep = step,
            startedAt = store.read(KEY_STARTED_AT)?.toLongOrNull(),
            completedAt = store.read(KEY_COMPLETED_AT)?.toLongOrNull(),
            installationId = store.read(KEY_INSTALLATION_ID).orEmpty().ifBlank { installationId },
            sessionId = store.read(KEY_SESSION_ID),
            practiceItemInCart = store.read(KEY_IN_CART)?.toBooleanStrictOrNull() ?: false,
            selectedCooldownMillis = store.read(KEY_COOLDOWN_MILLIS)?.toLongOrNull(),
            coolingEndsAt = store.read(KEY_COOLING_ENDS_AT)?.toLongOrNull(),
            decision = store.read(KEY_DECISION)?.let { enumValueOf<TutorialDecision>(it) },
            isReplay = store.read(KEY_IS_REPLAY)?.toBooleanStrictOrNull() ?: false
        ).also(::validate)
    }.getOrElse {
        clearTutorialSession()
        persist(
            TutorialState(
                consentVersion = loadConsentVersion(),
                installationId = installationId
            )
        )
    }

    private fun validate(state: TutorialState) {
        require(state.tutorialVersion == TUTORIAL_VERSION)
        if (state.status == TutorialStatus.IN_PROGRESS) {
            require(!state.sessionId.isNullOrBlank())
            require(state.startedAt != null)
            if (state.currentStep.ordinal >= TutorialStep.CART.ordinal) require(state.practiceItemInCart)
            if (state.currentStep.ordinal >= TutorialStep.FAKE_CHECKOUT.ordinal) {
                require(state.selectedCooldownMillis == TUTORIAL_COOLDOWN_MILLIS)
            }
        }
    }

    private fun persist(state: TutorialState): TutorialState {
        store.write(
            mapOf(
                KEY_VERSION to state.tutorialVersion.toString(),
                KEY_CONSENT_VERSION to state.consentVersion.toString(),
                KEY_STATUS to state.status.name,
                KEY_STEP to state.currentStep.name,
                KEY_STARTED_AT to state.startedAt?.toString(),
                KEY_COMPLETED_AT to state.completedAt?.toString(),
                KEY_INSTALLATION_ID to state.installationId.ifBlank { installationId },
                KEY_SESSION_ID to state.sessionId,
                KEY_IN_CART to state.practiceItemInCart.toString()
                    .takeIf { state.status == TutorialStatus.IN_PROGRESS },
                KEY_COOLDOWN_MILLIS to state.selectedCooldownMillis?.toString(),
                KEY_COOLING_ENDS_AT to state.coolingEndsAt?.toString(),
                KEY_DECISION to state.decision?.name,
                KEY_IS_REPLAY to state.isReplay.toString()
                    .takeIf { state.status == TutorialStatus.IN_PROGRESS }
            )
        )
        return state
    }

    private fun resetForNewVersion() {
        clearTutorialSession()
        persist(
            TutorialState(
                consentVersion = loadConsentVersion(),
                installationId = installationId
            )
        )
    }

    private fun loadConsentVersion(): Int = store.read(KEY_CONSENT_VERSION)?.toIntOrNull() ?: 0

    private fun TutorialState.withoutSessionObjects() = copy(
        sessionId = null,
        practiceItemInCart = false,
        selectedCooldownMillis = null,
        coolingEndsAt = null,
        decision = null,
        isReplay = false
    )

    companion object {
        private const val PREFS_NAME = "ghost_cart_tutorial"
        private const val KEY_VERSION = "tutorial_version"
        private const val KEY_CONSENT_VERSION = "tutorial_consent_version"
        private const val KEY_STATUS = "tutorial_status"
        private const val KEY_STEP = "current_tutorial_step"
        private const val KEY_STARTED_AT = "tutorial_started_at"
        private const val KEY_COMPLETED_AT = "tutorial_completed_at"
        private const val KEY_INSTALLATION_ID = "tutorial_installation_id"
        private const val KEY_SESSION_ID = "tutorial_session_id"
        private const val KEY_IN_CART = "tutorial_item_in_cart"
        private const val KEY_COOLDOWN_MILLIS = "tutorial_cooldown_millis"
        private const val KEY_COOLING_ENDS_AT = "tutorial_cooling_ends_at"
        private const val KEY_DECISION = "tutorial_decision"
        private const val KEY_IS_REPLAY = "tutorial_is_replay"

        private val SESSION_KEYS = setOf(
            KEY_SESSION_ID,
            KEY_IN_CART,
            KEY_COOLDOWN_MILLIS,
            KEY_COOLING_ENDS_AT,
            KEY_DECISION,
            KEY_IS_REPLAY
        )

        private val ALLOWED_TRANSITIONS = mapOf(
            TutorialStep.WELCOME to setOf(TutorialStep.PRACTICE_INTRO),
            TutorialStep.PRACTICE_INTRO to setOf(TutorialStep.PRODUCT),
            TutorialStep.PRODUCT to setOf(TutorialStep.CART),
            TutorialStep.CART to setOf(TutorialStep.COOLDOWN),
            TutorialStep.COOLDOWN to setOf(TutorialStep.FAKE_CHECKOUT),
            TutorialStep.FAKE_CHECKOUT to setOf(TutorialStep.COOLING),
            TutorialStep.COOLING to setOf(TutorialStep.DECISION),
            TutorialStep.DECISION to setOf(TutorialStep.GHOST_RECEIPT),
            TutorialStep.GHOST_RECEIPT to setOf(TutorialStep.COMPLETE),
            TutorialStep.COMPLETE to setOf(TutorialStep.DELIVERY)
        )

        fun create(context: Context): TutorialRepository = TutorialRepository(
            store = SharedPreferencesTutorialStateStore(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ),
            installationId = InstallationId.get(context)
        )
    }
}
