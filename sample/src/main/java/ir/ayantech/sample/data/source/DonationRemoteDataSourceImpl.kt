package ir.ayantech.sample.data.source

import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.sample.data.APIs
import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData

class DonationRemoteDataSourceImpl(private val ayanApi: AyanApi) : DonationRemoteDataSource {
    override suspend fun fetchDonation(data: GetDonationRequestData): Result<DonationServiceDTO.Output> {
        return ayanApi.post<GetDonationRequestData, DonationServiceDTO.Output>(
            body = data,
            endPoint = APIs.DONATION_SERVICE,
        )
    }
}
