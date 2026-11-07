package ir.ayantech.sample.domain.usecase

import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.model.ReferrerTypeUIModel

interface GetDonationUseCase {
    suspend operator fun invoke(requestData: GetDonationRequestData): Result<List<ReferrerTypeUIModel>>
}
