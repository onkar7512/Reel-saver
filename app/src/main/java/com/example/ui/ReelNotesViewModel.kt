package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ReelNote
import com.example.data.ReelNoteRepository
import com.example.util.InstagramUrlParser
import com.example.util.ParsedReelData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReelNotesFilterState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val activeShareData: ParsedReelData? = null,
    val isCreateSheetOpen: Boolean = false,
    val editingNote: ReelNote? = null,
    val viewingDetailNote: ReelNote? = null,
    val userMessage: String? = null
)

data class ReelNotesUiState(
    val notes: List<ReelNote> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val activeShareData: ParsedReelData? = null,
    val isCreateSheetOpen: Boolean = false,
    val editingNote: ReelNote? = null,
    val viewingDetailNote: ReelNote? = null,
    val userMessage: String? = null
)

class ReelNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReelNoteRepository
    private val filterState = MutableStateFlow(ReelNotesFilterState())

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ReelNoteRepository(database.reelNoteDao())
    }

    val uiState: StateFlow<ReelNotesUiState> = combine(
        repository.allNotes,
        filterState
    ) { allNotes, filter ->
        val filtered = allNotes.filter { note ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    note.title.contains(filter.searchQuery, ignoreCase = true) ||
                    note.useCase.contains(filter.searchQuery, ignoreCase = true) ||
                    note.notes.contains(filter.searchQuery, ignoreCase = true) ||
                    note.category.contains(filter.searchQuery, ignoreCase = true)

            val matchesCategory = filter.selectedCategory == null ||
                    note.category.equals(filter.selectedCategory, ignoreCase = true)

            val matchesStatus = when (filter.selectedStatus) {
                "TO_TRY" -> note.actionStatus == "TO_TRY"
                "TRIED" -> note.actionStatus == "TRIED"
                "SAVED" -> note.actionStatus == "SAVED"
                "FAVORITE" -> note.isFavorite
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }

        ReelNotesUiState(
            notes = filtered,
            searchQuery = filter.searchQuery,
            selectedCategory = filter.selectedCategory,
            selectedStatus = filter.selectedStatus,
            activeShareData = filter.activeShareData,
            isCreateSheetOpen = filter.isCreateSheetOpen,
            editingNote = filter.editingNote,
            viewingDetailNote = filter.viewingDetailNote,
            userMessage = filter.userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReelNotesUiState()
    )

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null && type.startsWith("text/")) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: intent.clipData?.getItemAt(0)?.text?.toString()
                ?: intent.dataString

            if (!sharedText.isNullOrBlank()) {
                val parsed = InstagramUrlParser.parse(sharedText)
                filterState.update {
                    it.copy(
                        activeShareData = parsed,
                        isCreateSheetOpen = true,
                        editingNote = null,
                        userMessage = "Received Instagram link to note!"
                    )
                }
            }
        }
    }

    fun openManualAdd(prefilledUrl: String = "", prefilledText: String = "") {
        val parsed = if (prefilledUrl.isNotBlank()) {
            InstagramUrlParser.parse(prefilledUrl)
        } else {
            ParsedReelData(
                cleanUrl = "",
                shortCode = "",
                suggestedTitle = "",
                extractedCaption = prefilledText,
                isInstagramUrl = false,
                suggestedCategory = "Ideas"
            )
        }
        filterState.update {
            it.copy(
                editingNote = null,
                activeShareData = parsed,
                isCreateSheetOpen = true
            )
        }
    }

    fun openEditNote(note: ReelNote) {
        val parsed = ParsedReelData(
            cleanUrl = note.reelUrl,
            shortCode = note.shortCode,
            suggestedTitle = note.title,
            extractedCaption = note.notes,
            isInstagramUrl = note.reelUrl.contains("instagram.com"),
            suggestedCategory = note.category
        )
        filterState.update {
            it.copy(
                editingNote = note,
                activeShareData = parsed,
                isCreateSheetOpen = true
            )
        }
    }

    fun dismissSheet() {
        filterState.update {
            it.copy(
                isCreateSheetOpen = false,
                activeShareData = null,
                editingNote = null
            )
        }
    }

    fun saveNote(
        reelUrl: String,
        title: String,
        useCase: String,
        notes: String,
        category: String,
        actionStatus: String,
        isFavorite: Boolean
    ) {
        viewModelScope.launch {
            val currentEditing = filterState.value.editingNote
            val parsed = InstagramUrlParser.parse(reelUrl)

            if (currentEditing != null) {
                val updated = currentEditing.copy(
                    reelUrl = reelUrl.trim().ifEmpty { currentEditing.reelUrl },
                    shortCode = parsed.shortCode.ifEmpty { currentEditing.shortCode },
                    title = title.trim().ifEmpty { "Instagram Reel Note" },
                    useCase = useCase.trim().ifEmpty { "General reference" },
                    notes = notes.trim(),
                    category = category,
                    actionStatus = actionStatus,
                    isFavorite = isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(updated)
                filterState.update { it.copy(userMessage = "Note updated! ✨") }
            } else {
                val newNote = ReelNote(
                    reelUrl = reelUrl.trim(),
                    shortCode = parsed.shortCode,
                    title = title.trim().ifEmpty { parsed.suggestedTitle.ifEmpty { "Instagram Reel Note" } },
                    useCase = useCase.trim().ifEmpty { "To remember and use later" },
                    notes = notes.trim(),
                    category = category,
                    actionStatus = actionStatus,
                    isFavorite = isFavorite,
                    rawSharedText = filterState.value.activeShareData?.extractedCaption ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.insert(newNote)
                filterState.update { it.copy(userMessage = "Saved to Reel Notes! 📌") }
            }
            dismissSheet()
        }
    }

    fun toggleTriedStatus(note: ReelNote) {
        viewModelScope.launch {
            val nextStatus = if (note.actionStatus == "TRIED") "TO_TRY" else "TRIED"
            repository.update(note.copy(actionStatus = nextStatus, updatedAt = System.currentTimeMillis()))
            filterState.update {
                it.copy(userMessage = if (nextStatus == "TRIED") "Marked as Tried! ✅" else "Moved to To Try 📌")
            }
        }
    }

    fun toggleFavorite(note: ReelNote) {
        viewModelScope.launch {
            val nextFav = !note.isFavorite
            repository.update(note.copy(isFavorite = nextFav, updatedAt = System.currentTimeMillis()))
            filterState.update {
                it.copy(userMessage = if (nextFav) "Added to Favorites ❤️" else "Removed from Favorites")
            }
        }
    }

    fun deleteNote(note: ReelNote) {
        viewModelScope.launch {
            repository.delete(note)
            filterState.update {
                it.copy(
                    viewingDetailNote = if (it.viewingDetailNote?.id == note.id) null else it.viewingDetailNote,
                    userMessage = "Reel note deleted"
                )
            }
        }
    }

    fun setViewingDetailNote(note: ReelNote?) {
        filterState.update { it.copy(viewingDetailNote = note) }
    }

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun setCategory(category: String?) {
        filterState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun setStatusFilter(status: String?) {
        filterState.update {
            it.copy(selectedStatus = if (it.selectedStatus == status) null else status)
        }
    }

    fun clearUserMessage() {
        filterState.update { it.copy(userMessage = null) }
    }
}
