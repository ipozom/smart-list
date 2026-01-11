package com.example.smartlist.ui

sealed class UiEvent {
    data class UndoInfo(val kind: String, val id: Long)

    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val undoInfo: UndoInfo? = null
    ) : UiEvent()

    data class ShowConfirm(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val cancelLabel: String,
        val kind: String,
        val id: Long
    ) : UiEvent()

    object ScrollToTop : UiEvent()
}
