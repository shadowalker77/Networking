package ir.ayantech.networking.ui

import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.model.ReferrerTypeUIModel
import ir.ayantech.sample.domain.usecase.GetDonationUseCase
import ir.ayantech.sample.ui.MainUIState
import ir.ayantech.sample.ui.MainViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MainViewModelTest {

    @Test
    fun `initial state is idle`() {
        val viewModel = MainViewModel(FakeGetDonationUseCase(Result.success(emptyList())))

        assertSame(MainUIState.IDLE, viewModel.uiState.value)
    }

    @Test
    fun `getDonation sends fixed request and exposes successful data`() = runTest {
        val data = listOf(ReferrerTypeUIModel(name = "friend", showName = "Friend"))
        val useCase = FakeGetDonationUseCase(Result.success(data))
        val viewModel = MainViewModel(useCase)

        viewModel.getDonation()

        assertEquals(GetDonationRequestData(id = "1234"), useCase.receivedRequest)
        assertEquals(MainUIState.Success(data), viewModel.uiState.value)
    }

    @Test
    fun `getDonation exposes loading while the request is suspended`() = runTest {
        val started = CompletableDeferred<Unit>()
        val completion = CompletableDeferred<Result<List<ReferrerTypeUIModel>>>()
        val viewModel = MainViewModel(object : GetDonationUseCase {
            override suspend fun invoke(requestData: GetDonationRequestData): Result<List<ReferrerTypeUIModel>> {
                started.complete(Unit)
                return completion.await()
            }
        })

        viewModel.getDonation()

        completion.complete(Result.success(emptyList()))
        assertEquals(MainUIState.Success(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `getDonation exposes networking failure`() = runTest {
        val failure = testFailure()
        val useCase = FakeGetDonationUseCase(Result.failure(failure))
        val viewModel = MainViewModel(useCase)

        viewModel.getDonation()

        assertEquals(MainUIState.Error(failure), viewModel.uiState.value)
    }

    @Test
    fun `getDonation ignores errors that are not networking failures`() = runTest {
        val useCase = FakeGetDonationUseCase(
            Result.failure(IllegalStateException("unexpected"))
        )
        val viewModel = MainViewModel(useCase)

        viewModel.getDonation()

        assertSame(MainUIState.IDLE, viewModel.uiState.value)
    }

    private class FakeGetDonationUseCase(
        private val result: Result<List<ReferrerTypeUIModel>>
    ) : GetDonationUseCase {
        var receivedRequest: GetDonationRequestData? = null

        override suspend fun invoke(
            requestData: GetDonationRequestData
        ): Result<List<ReferrerTypeUIModel>> {
            receivedRequest = requestData
            return result
        }
    }

    private fun testFailure() = Failure(
        failureRepository = FailureRepository.REMOTE,
        failureType = FailureType.UNKNOWN,
        failureCode = "TEST",
        language = Language.ENGLISH,
        failureStatus = null
    )
}
