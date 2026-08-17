package com.netly.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netly.app.domain.repository.DownloadRepository
import com.netly.app.domain.repository.ExtractorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeStats(
    val totalGbSavedThisMonth: Double = 0.0,
    val completedCountThisMonth: Int = 0
)

class HomeViewModel(
    private val extractorRepository: ExtractorRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val completedDownloads: StateFlow<List<com.netly.app.data.local.entity.DownloadEntity>> =
        downloadRepository.getCompletedDownloads()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val stats: StateFlow<HomeStats> = downloadRepository.getCompletedDownloads()
        .map { downloads ->
            val currentCal = Calendar.getInstance()
            val currentMonth = currentCal.get(Calendar.MONTH)
            val currentYear = currentCal.get(Calendar.YEAR)

            val monthDownloads = downloads.filter { dl ->
                val dlCal = Calendar.getInstance().apply { timeInMillis = dl.createdAt }
                dlCal.get(Calendar.MONTH) == currentMonth && dlCal.get(Calendar.YEAR) == currentYear
            }

            val totalMB = monthDownloads.sumOf { it.totalSizeMB }
            HomeStats(
                totalGbSavedThisMonth = totalMB / 1024.0,
                completedCountThisMonth = monthDownloads.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeStats()
        )

    companion object {
        fun factory(
            extractorRepository: ExtractorRepository,
            downloadRepository: DownloadRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(extractorRepository, downloadRepository) as T
                }
            }
    }
}
