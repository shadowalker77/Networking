package ir.ayantech.networking.domain.usecase

import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.sample.domain.model.DonationServiceDTO
import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.model.ReferrerTypeUIModel
import ir.ayantech.sample.domain.repository.DonationRepository
import ir.ayantech.sample.domain.usecase.GetDonationUseCaseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetDonationUseCaseImplTest {

    private val request = GetDonationRequestData(id = "42")

    @Test
    fun `maps repository DTOs to UI models`() = runTest {
        val output = DonationServiceDTO.Output(
            referrerTypeList = listOf(
                DonationServiceDTO.ReferrerType("friend", "Friend"),
                DonationServiceDTO.ReferrerType("web", "Website")
            )
        )
        val repository = FakeDonationRepository(Result.success(output))
        val useCase = GetDonationUseCaseImpl(repository, Dispatchers.Unconfined)

        val result = useCase(request)

        assertEquals(request, repository.receivedRequest)
        assertEquals(
            listOf(
                ReferrerTypeUIModel("friend", "Friend"),
                ReferrerTypeUIModel("web", "Website")
            ),
            result.getOrThrow()
        )
    }

    @Test
    fun `turns a null DTO list into a local failure`() = runTest {
        val repository = FakeDonationRepository(
            Result.success(DonationServiceDTO.Output(null))
        )
        val useCase = GetDonationUseCaseImpl(repository, Dispatchers.Unconfined)

        val failure = useCase(request).exceptionOrNull() as Failure

        assertEquals(FailureRepository.LOCAL, failure.failureRepository)
        assertEquals(FailureType.NOT_200, failure.failureType)
        assertEquals(Failure.NO_CODE_SERVER_ERROR_CODE, failure.failureCode)
    }

    @Test
    fun `forwards repository failures unchanged`() = runTest {
        val failure = testFailure()
        val repository = FakeDonationRepository(Result.failure(failure))
        val useCase = GetDonationUseCaseImpl(repository, Dispatchers.Unconfined)

        val result = useCase(request)

        assertSame(failure, result.exceptionOrNull())
    }

    private class FakeDonationRepository(
        private val result: Result<DonationServiceDTO.Output>
    ) : DonationRepository {
        var receivedRequest: GetDonationRequestData? = null

        override suspend fun fetchDonation(
            data: GetDonationRequestData
        ): Result<DonationServiceDTO.Output> {
            receivedRequest = data
            return result
        }
    }

    private fun testFailure() = Failure(
        failureRepository = FailureRepository.REMOTE,
        failureType = FailureType.TIMEOUT,
        failureCode = "TIMEOUT",
        language = Language.ENGLISH,
        failureStatus = null
    )
}
