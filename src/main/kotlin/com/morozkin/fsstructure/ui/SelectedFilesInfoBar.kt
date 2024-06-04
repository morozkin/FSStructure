package com.morozkin.fsstructure.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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

  Box {
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
    contentAlignment = Alignment.BottomStart
  ) {
    LazyRow(
      state = lazyListState,
      contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
      pathComponents.forEachIndexed { index, pathComponent ->
        item(key = pathComponent) {
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
    modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
  ) {
    Text(text = text, style = Typography.labelTextStyle())
  }
}