package com.morozkin.fsstructure

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.morozkin.fsstructure.ui.FilesList
import com.morozkin.fsstructure.viewModel.FilesListViewModel
import org.jetbrains.jewel.bridge.addComposeTab

@Suppress("unused")
class ToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.addComposeTab("Files List") {
      FilesList(viewModel = FilesListViewModel())
    }
  }
}