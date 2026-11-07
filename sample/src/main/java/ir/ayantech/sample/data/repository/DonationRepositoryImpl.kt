package ir.ayantech.sample.data.repository

import ir.ayantech.sample.data.source.DonationRemoteDataSource
import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.repository.DonationRepository

class DonationRepositoryImpl(private val remoteSource: DonationRemoteDataSource) :
    DonationRepository {

    override suspend fun fetchDonation(data: GetDonationRequestData): Result<DonationServiceDTO.Output> {
        return remoteSource.fetchDonation(data)
    }
}
