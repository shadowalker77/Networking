package ir.ayantech.sample.ui

import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.sample.domain.model.ReferrerTypeUIModel

sealed interface MainUIState {
    object IDLE : MainUIState
    data class Success(val data: List<ReferrerTypeUIModel>) : MainUIState
    data class Error(val failure: Failure) : MainUIState
}
