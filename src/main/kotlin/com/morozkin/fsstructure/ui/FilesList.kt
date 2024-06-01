package com.morozkin.fsstructure.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.morozkin.fsstructure.viewModel.FilesListViewModel
import org.jetbrains.jewel.ui.component.Text

@Composable
fun FilesList(viewModel: FilesListViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text("Hello world!")
  }
}