package com.yourapp.obd.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.domain.usecase.RecordVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoSegment(
    val file: File,
    val isProtected: Boolean,
    val sizeBytes: Long,
    val durationMs: Long
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordVideoUseCase: RecordVideoUseCase
) : ViewModel() {

    private val _segments = MutableStateFlow<List<VideoSegment>>(emptyList())
    val segments: StateFlow<List<VideoSegment>> = _segments.asStateFlow()

    private val _selectedSegment = MutableStateFlow<VideoSegment?>(null)
    val selectedSegment: StateFlow<VideoSegment?> = _selectedSegment.asStateFlow()

    init {
        loadSegments()
    }

    fun loadSegments() {
        viewModelScope.launch {
            val files = recordVideoUseCase.getAllSegments()
            _segments.value = files.map { file ->
                VideoSegment(
                    file = file,
                    isProtected = file.name.startsWith("protected_"),
                    sizeBytes = file.length(),
                    durationMs = 0L
                )
            }
        }
    }

    fun selectSegment(segment: VideoSegment) {
        _selectedSegment.value = segment
    }

    fun deleteSegment(segment: VideoSegment) {
        if (segment.isProtected) return
        recordVideoUseCase.delete(segment.file)
        loadSegments()
        if (_selectedSegment.value?.file == segment.file) {
            _selectedSegment.value = null
        }
    }

    fun deleteAllUnprotected() {
        val toDelete = _segments.value.filter { !it.isProtected }
        toDelete.forEach { recordVideoUseCase.delete(it.file) }
        loadSegments()
        _selectedSegment.value = null
    }
}
