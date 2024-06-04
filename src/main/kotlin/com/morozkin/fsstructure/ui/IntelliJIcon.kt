package com.morozkin.fsstructure.ui

import androidx.compose.runtime.Composable
import com.intellij.icons.AllIcons
import org.jetbrains.jewel.ui.component.Icon

enum class IntelliJIcons(val value: String) {
  File("fileTypes/text.svg"),
  Directory("nodes/folder.svg"),
  Symlink("nodes/symlink.svg"),
  Breadcrumb("actions/arrowExpand.svg")
}

@Composable
fun IntelliJIcon(icon: IntelliJIcons) {
  Icon(
    resource = icon.value,
    iconClass = AllIcons::class.java,
    contentDescription = icon.name
  )

//  Alternative approach:
//  val painterProvider = bridgePainterProvider(icon.value)
//  val painter by painterProvider.getPainter()
//  Icon(painter = painter, contentDescription = null)
}