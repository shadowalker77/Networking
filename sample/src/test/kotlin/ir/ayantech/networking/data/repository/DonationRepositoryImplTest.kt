package ir.ayantech.networking.data.repository

import ir.ayantech.sample.data.repository.DonationRepositoryImpl
import ir.ayantech.sample.data.source.DonationRemoteDataSource
import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DonationRepositoryImplTest {

    @Test
    fun `fetchDonation delegates request and returns remote result`() = runTest {
        val request = GetDonationRequestData(id = "99")
        val output = DonationServiceDTO.Output(emptyList())
        val remoteSource = FakeDonationRemoteDataSource(Result.success(output))
        val repository = DonationRepositoryImpl(remoteSource)

        val result = repository.fetchDonation(request)

        assertEquals(request, remoteSource.receivedRequest)
        assertSame(output, result.getOrThrow())
    }

    private class FakeDonationRemoteDataSource(
        private val result: Result<DonationServiceDTO.Output>
    ) : DonationRemoteDataSource {
        var receivedRequest: GetDonationRequestData? = null

        override suspend fun fetchDonation(
            data: GetDonationRequestData
        ): Result<DonationServiceDTO.Output> {
            receivedRequest = data
            return result
        }
    }
}
