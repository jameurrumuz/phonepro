package com.example.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class CallNoteDebouncer(
    private val scope: CoroutineScope,
    private val onSaveNote: suspend (phoneNumber: String, note: String) -> Unit
) {
    private val _noteFlow = MutableStateFlow<Pair<String, String>?>(null)

    init {
        scope.launch {
            _noteFlow
                .filterNotNull()
                .debounce(600L)
                .distinctUntilChanged()
                .collectLatest { (number, note) ->
                    if (number.isNotBlank()) {
                        onSaveNote(number, note)
                    }
                }
        }
    }

    fun onTextChanged(phoneNumber: String, note: String) {
        _noteFlow.value = phoneNumber to note
    }
}
