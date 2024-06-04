package com.morozkin.fsstructure.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.ScrollbarStyle
import org.jetbrains.jewel.ui.theme.scrollbarStyle

/**
 * jewel implementation leads to `LinkageError` because of `toInt(unit: kotlin.time.DurationUnit)` fun usage.
 *
 * loader constraint violation: when resolving method 'int kotlin.time.Duration.toInt-impl(long, kotlin.time.DurationUnit)'
 * the class loader com.intellij.ide.plugins.cl.PluginClassLoader @293d468a of the current class,
 * org/jetbrains/jewel/ui/component/ScrollbarsKt, and the class loader com.intellij.util.lang.PathClassLoader @4563e9ab
 * for the method's defining class, kotlin/time/Duration,have different Class objects for the type kotlin/time/DurationUnit
 * used in the signature (org.jetbrains.jewel.ui.component.ScrollbarsKt is in unnamed module of
 * loader com.intellij.ide.plugins.cl.PluginClassLoader @293d468a,parent loader 'bootstrap';
 * kotlin.time.Duration is in unnamed module of loader com.intellij.util.lang.PathClassLoader @4563e9ab)
 */
@Composable
fun HorizontalScrollbar(
  adapter: ScrollbarAdapter,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  style: ScrollbarStyle = JewelTheme.scrollbarStyle,
) {
  val shape by remember { mutableStateOf(RoundedCornerShape(style.metrics.thumbCornerSize)) }
  val hoverDurationMillis by remember { mutableStateOf(style.hoverDuration.inWholeMilliseconds) }

  val composeScrollbarStyle = androidx.compose.foundation.ScrollbarStyle(
    minimalHeight = style.metrics.minThumbLength,
    thickness = style.metrics.thumbThickness,
    shape = shape,
    hoverDurationMillis = hoverDurationMillis.toInt(),
    unhoverColor = style.colors.thumbBackground,
    hoverColor = style.colors.thumbBackgroundHovered,
  )
  CompositionLocalProvider(LocalScrollbarStyle provides composeScrollbarStyle) {
    androidx.compose.foundation.HorizontalScrollbar(
      adapter = adapter,
      modifier = modifier.padding(style.metrics.trackPadding),
      style = LocalScrollbarStyle.current,
      interactionSource = interactionSource,
    )
  }
}