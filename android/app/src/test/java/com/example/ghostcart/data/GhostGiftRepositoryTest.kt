package com.example.ghostcart.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GhostGiftRepositoryTest {
    private fun validDraft() = GhostGiftDraft(
        productId = "product-1",
        recipientName = "Aisha",
        recipientEmail = "aisha@example.com",
        recipientConsentConfirmed = true
    )

    @Test
    fun acceptsACompleteExpectedRecipientDraft() {
        assertNull(GhostGiftRepository.validationError(validDraft()))
    }

    @Test
    fun requiresAProductRecipientIdentityAndExpectedEmailConsent() {
        assertEquals(
            "Choose an item to send as a gift",
            GhostGiftRepository.validationError(validDraft().copy(productId = ""))
        )
        assertEquals(
            "Enter the recipient's name",
            GhostGiftRepository.validationError(validDraft().copy(recipientName = "   "))
        )
        assertEquals(
            "Enter a valid recipient email",
            GhostGiftRepository.validationError(validDraft().copy(recipientEmail = "not-an-email"))
        )
        assertEquals(
            "Confirm that the recipient expects this email",
            GhostGiftRepository.validationError(validDraft().copy(recipientConsentConfirmed = false))
        )
    }
}
