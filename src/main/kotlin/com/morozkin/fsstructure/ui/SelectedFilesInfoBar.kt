package com.morozkin.fsstructure.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morozkin.fsstructure.viewModel.SelectedFilesInfoBarState
import com.morozkin.fsstructure.viewModel.SelectedFilesInfoBarViewModel
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Typography

@Composable
fun SelectedFilesInfoBar(viewModel: SelectedFilesInfoBarViewModel) {
  val state = viewModel.selectedFilesInfoBarState.collectAsState()

  Box(
    modifier = Modifier.height(28.dp)
  ) {
    Divider(orientation = Orientation.Horizontal)

    when (val currentState = state.value) {
      is SelectedFilesInfoBarState.File -> {
        FilePathBreadcrumbBar(currentState.pathComponents)
      }

      is SelectedFilesInfoBarState.Files -> {
        InfoBar(currentState.info)
      }
    }
  }
}

@Composable
private fun FilePathBreadcrumbBar(pathComponents: List<String>) {
  val lazyListState = rememberLazyListState()

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.BottomStart
  ) {
    LazyRow(
      modifier = Modifier.fillMaxHeight(),
      state = lazyListState,
      contentPadding = PaddingValues(start = 12.dp, end = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      pathComponents.forEachIndexed { index, pathComponent ->
        item(key = "$pathComponent$index") {
          Text(text = pathComponent, style = Typography.labelTextStyle())
          if (index != pathComponents.count() - 1) {
            IntelliJIcon(IntelliJIcons.Breadcrumb)
          }
        }
      }
    }

    HorizontalScrollbar(
      adapter = rememberScrollbarAdapter(lazyListState)
    )
  }
}

@Composable
private fun InfoBar(text: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(start = 12.dp, end = 12.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Text(text = text, style = Typography.labelTextStyle())
  }
}