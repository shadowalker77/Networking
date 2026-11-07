package ir.ayantech.sample.ui

import androidx.lifecycle.ViewModel
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.usecase.GetDonationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val donationUseCase: GetDonationUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUIState>(MainUIState.IDLE)
    val uiState = _uiState.asStateFlow()

    suspend fun getDonation() {
        val requestData = GetDonationRequestData(id = "1234")
            _uiState.value = donationUseCase(requestData).fold(
                onSuccess = { MainUIState.Success(it) },
                onFailure = { failure ->
                    if (failure is Failure) MainUIState.Error(failure) else MainUIState.IDLE
                }
            )
    }
}
