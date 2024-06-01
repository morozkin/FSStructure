package com.morozkin.fsstructure.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
@Service
class FilesListViewModel(
  private val coroutineScope: CoroutineScope
) {
  @Immutable
  data class State(
    val elements: List<String> = emptyList()
  )

  val state: StateFlow<State> = MutableStateFlow(State())
}