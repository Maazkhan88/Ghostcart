package com.example.ghostcart.ui.main

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyUsesEmptyGhostCartState() = runTest {
    val viewModel = MainScreenViewModel()

    assertEquals(GhostCartUiState(), viewModel.uiState.first())
  }

  @Test
  fun addToCart_thenCoolItem_movesItOutOfCart() = runTest {
    val viewModel = MainScreenViewModel()

    viewModel.addToCart("sample-product")
    assertEquals(setOf("sample-product"), viewModel.uiState.value.cartIds)

    viewModel.startCooling("sample-product")
    assertEquals(emptySet<String>(), viewModel.uiState.value.cartIds)
    assertEquals(setOf("sample-product"), viewModel.uiState.value.cooledIds)
  }
}
