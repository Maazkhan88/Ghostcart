package com.example.ghostcart.ui.tutorial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ghostcart.data.Analytics
import com.example.ghostcart.data.TUTORIAL_CONSENT_VERSION
import com.example.ghostcart.data.TUTORIAL_VERSION
import com.example.ghostcart.data.TutorialDecision
import com.example.ghostcart.data.TutorialRepository
import com.example.ghostcart.data.TutorialState
import com.example.ghostcart.data.TutorialStatus
import com.example.ghostcart.data.TutorialStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TutorialViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TutorialRepository.create(application)
    private val _state = MutableStateFlow(repository.load())
    val state: StateFlow<TutorialState> = _state.asStateFlow()

    fun shouldAutoLaunch(): Boolean = repository.shouldAutoLaunch()

    fun recordConsentAccepted() {
        _state.value = repository.recordConsentAccepted(TUTORIAL_CONSENT_VERSION)
    }

    fun startIfNeeded() {
        val current = repository.load()
        _state.value = if (current.status == TutorialStatus.IN_PROGRESS) {
            current
        } else {
            repository.start().also {
                Analytics.logTutorialStarted(getApplication(), TUTORIAL_VERSION, replayed = false)
            }
        }
        Analytics.logTutorialWelcomeViewed(getApplication(), TUTORIAL_VERSION)
    }

    fun replay() {
        _state.value = repository.start(replay = true)
        Analytics.logTutorialReplayed(getApplication(), TUTORIAL_VERSION)
    }

    fun continueFromWelcome() = advance(TutorialStep.WELCOME, TutorialStep.PRACTICE_INTRO)
    fun openPracticeProduct() = advance(TutorialStep.PRACTICE_INTRO, TutorialStep.PRODUCT)

    fun addPracticeItemToCart() {
        if (_state.value.currentStep != TutorialStep.PRODUCT) return
        if (_state.value.practiceItemInCart) return
        _state.value = repository.addPracticeItemToCart()
        Analytics.logTutorialStepCompleted(getApplication(), TUTORIAL_VERSION, TutorialStep.PRODUCT.name)
    }

    fun advanceMarketplaceSpotlight() {
        if (_state.value.currentStep != TutorialStep.PRODUCT) return
        if (_state.value.marketplaceSpotlightStep >= 4) return
        _state.value = repository.advanceMarketplaceSpotlight()
        Analytics.logTutorialStepViewed(
            getApplication(),
            TUTORIAL_VERSION,
            "marketplace_${_state.value.marketplaceSpotlightStep}"
        )
    }

    fun openTutorialCart() {
        if (_state.value.currentStep != TutorialStep.PRODUCT || !_state.value.practiceItemInCart) return
        _state.value = repository.openTutorialCart()
    }

    fun openCooldownPicker() = advance(TutorialStep.CART, TutorialStep.COOLDOWN)

    fun selectTutorialCooldown() {
        if (_state.value.currentStep != TutorialStep.COOLDOWN) return
        _state.value = repository.selectTutorialCooldown()
    }

    fun returnToTutorialCart() {
        if (_state.value.currentStep != TutorialStep.COOLDOWN) return
        _state.value = repository.returnToTutorialCart()
    }

    fun continueToFakeCheckout() = advance(TutorialStep.COOLDOWN, TutorialStep.FAKE_CHECKOUT)

    fun completeFakeCheckout() {
        if (_state.value.currentStep != TutorialStep.FAKE_CHECKOUT) return
        _state.value = repository.completeFakeCheckout()
        Analytics.logTutorialStepCompleted(getApplication(), TUTORIAL_VERSION, TutorialStep.FAKE_CHECKOUT.name)
    }

    fun finishCooling() {
        if (_state.value.currentStep != TutorialStep.COOLING) return
        _state.value = repository.finishCooling()
        Analytics.logTutorialStepCompleted(getApplication(), TUTORIAL_VERSION, TutorialStep.COOLING.name)
    }

    fun chooseDecision(decision: TutorialDecision) {
        _state.value = when (_state.value.currentStep) {
            TutorialStep.DELIVERY -> repository.resolveTutorialDelivery(decision)
            TutorialStep.DECISION -> repository.chooseDecision(decision)
            else -> return
        }
        Analytics.logTutorialStepCompleted(
            getApplication(),
            TUTORIAL_VERSION,
            TutorialStep.DECISION.name,
            selectedDecision = decision.name
        )
    }

    fun continueFromReceipt() = advance(TutorialStep.GHOST_RECEIPT, TutorialStep.COMPLETE)

    fun complete() {
        if (_state.value.currentStep != TutorialStep.COMPLETE) return
        _state.value = repository.complete()
        Analytics.logTutorialCompleted(getApplication(), TUTORIAL_VERSION)
    }

    fun skip(exitDuringTutorial: Boolean = false) {
        val exitedAtStep = _state.value.currentStep.name
        _state.value = repository.skip()
        if (exitDuringTutorial) {
            Analytics.logTutorialExited(getApplication(), TUTORIAL_VERSION, exitedAtStep)
        } else {
            Analytics.logTutorialSkipped(getApplication(), TUTORIAL_VERSION, exitedAtStep)
        }
    }

    fun clearTutorialSession() {
        repository.clearTutorialSession()
        _state.value = repository.load()
    }

    fun onUserSignedOut() {
        if (_state.value.status == TutorialStatus.IN_PROGRESS) {
            skip(exitDuringTutorial = true)
        } else {
            clearTutorialSession()
        }
    }

    fun resetForDebug() {
        _state.value = repository.resetForDebug()
    }

    fun startAtForDebug(step: TutorialStep) {
        _state.value = repository.startAtForDebug(step)
    }

    private fun advance(expected: TutorialStep, next: TutorialStep) {
        if (_state.value.currentStep != expected) return
        _state.value = repository.advance(expected, next)
        Analytics.logTutorialStepCompleted(getApplication(), TUTORIAL_VERSION, expected.name)
    }
}
