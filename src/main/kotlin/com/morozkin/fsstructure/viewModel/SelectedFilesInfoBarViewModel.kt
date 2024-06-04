package com.morozkin.fsstructure.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

@Immutable
sealed class SelectedFilesInfoBarState {
  @Immutable
  data class File(val pathComponents: List<String>) : SelectedFilesInfoBarState()

  @Immutable
  data class Files(val info: String) : SelectedFilesInfoBarState()
}

@Stable
interface SelectedFilesInfoBarViewModel {
  val selectedFilesInfoBarState: StateFlow<SelectedFilesInfoBarState>
}