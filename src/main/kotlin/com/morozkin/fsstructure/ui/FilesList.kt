package com.morozkin.fsstructure.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ui.JBColor
import com.morozkin.fsstructure.viewModel.FileSystemElement
import com.morozkin.fsstructure.viewModel.FilesListViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import org.jetbrains.jewel.bridge.medium
import org.jetbrains.jewel.bridge.toComposeColor
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.tree.rememberTreeState
import org.jetbrains.jewel.ui.component.LazyTree
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Typography
import org.jetbrains.jewel.ui.component.VerticalScrollbar

@OptIn(ExperimentalJewelApi::class)
@Composable
fun FilesList(viewModel: FilesListViewModel) {
  val tree = viewModel.state.collectAsState()

  val bgColor by remember(JBColor.PanelBackground.rgb) { mutableStateOf(JBColor.PanelBackground.toComposeColor()) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxSize(),
      contentAlignment = Alignment.TopEnd
    ) {
      val lazyListState = rememberLazyListState()
      val treeState = rememberTreeState(lazyListState)

      LaunchedEffect(treeState) {
        snapshotFlow { treeState.openNodes }
          .map { openedNodeIds ->
            openedNodeIds
              .mapNotNull { it as? String }
              .toSet()
          }
          .runningFold(Pair<Set<String>, Set<String>>(emptySet(), emptySet())) { acc, new ->
            acc.second to new
          }
          .collect { (old, new) ->
            // Children of the closed parent are not removed from `treeState.openNodes`
            // that's why only opening is handled.
            val newlyOpened = new - old
            if (newlyOpened.isEmpty()) return@collect
            viewModel.handleOpenedElements(newlyOpened.toList())
          }
      }

      LazyTree(
        tree = tree.value,
        treeState = treeState,
        nodeContent = {
          val element = it.data

          ContextMenuArea(
            enabled = element.isReadable,
            items = {
              listOf(
                ContextMenuItem(label = if (!element.displayAttributes) "Show attributes" else "Hide attributes") {
                  viewModel.toggleAttributesVisibility(element)
                }
              )
            }
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FileTypeIcon(element)

              Spacer(modifier = Modifier.width(4.dp))

              if (element.displayAttributes && element.attributes != null) {
                Row(
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = element.name, style = Typography.labelTextStyle(), softWrap = false)
                  Spacer(Modifier.width(8.dp))
                  Text(text = element.attributes!!, style = Typography.medium(), softWrap = false)
                }
              } else {
                Text(text = element.name, style = Typography.labelTextStyle(), softWrap = false)
              }
            }
          }
        },
        onSelectionChange = { selectedElements ->
          viewModel.handleSelectedElements(
            selectedElements.map { it.data }
          )
        }
      )

      VerticalScrollbar(rememberScrollbarAdapter(lazyListState))
    }

    SelectedFilesInfoBar(viewModel)
  }
}

@Composable
private fun FileTypeIcon(element: FileSystemElement) {
  val icon = if (element is FileSystemElement.File) IntelliJIcons.File else IntelliJIcons.Directory
  if (element.isSymLink) {
    Box {
      IntelliJIcon(icon)
      IntelliJIcon(IntelliJIcons.Symlink)
    }
  } else {
    IntelliJIcon(icon)
  }
}