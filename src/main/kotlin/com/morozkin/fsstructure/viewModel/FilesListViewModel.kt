package com.morozkin.fsstructure.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
class FilesListViewModel {
  @Immutable
  data class State(
    val elements: List<String> = emptyList()
  )

  val state: StateFlow<State> = MutableStateFlow(State())
}