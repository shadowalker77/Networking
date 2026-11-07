package ir.ayantech.sample.domain.repository

import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData

interface DonationRepository {
    suspend fun fetchDonation(data: GetDonationRequestData): Result<DonationServiceDTO.Output>
}
