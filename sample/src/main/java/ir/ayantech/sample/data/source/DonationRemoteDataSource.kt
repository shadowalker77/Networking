package ir.ayantech.sample.data.source

import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData

interface DonationRemoteDataSource {
    suspend fun fetchDonation(data: GetDonationRequestData): Result<DonationServiceDTO.Output>
}
