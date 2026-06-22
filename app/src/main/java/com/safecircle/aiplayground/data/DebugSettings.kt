package com.safecircle.aiplayground.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "debug_settings"
private const val KEY_SHOW_RAW_OUTPUT = "show_raw_output"

object DebugSettings {
  private val _showRawOutput = MutableStateFlow(false)
  val showRawOutput: StateFlow<Boolean> = _showRawOutput.asStateFlow()

  fun init(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    _showRawOutput.value = prefs.getBoolean(KEY_SHOW_RAW_OUTPUT, false)
  }

  fun setShowRawOutput(context: Context, enabled: Boolean) {
    _showRawOutput.value = enabled
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit { putBoolean(KEY_SHOW_RAW_OUTPUT, enabled) }
  }
}
