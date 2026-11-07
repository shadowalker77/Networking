package ir.ayantech.sample.domain.usecase

import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.sample.domain.model.GetDonationRequestData
import ir.ayantech.sample.domain.model.ReferrerTypeUIModel
import ir.ayantech.sample.domain.repository.DonationRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class GetDonationUseCaseImpl(
    private val repository: DonationRepository,
    private val coroutineContext: CoroutineContext
) : GetDonationUseCase {

    override suspend fun invoke(requestData: GetDonationRequestData): Result<List<ReferrerTypeUIModel>> =
        withContext(coroutineContext) {
            repository.fetchDonation(requestData).fold(
                onSuccess = { donationServiceDTO ->
                    val referrers = donationServiceDTO.referrerTypeList
                        ?: return@withContext Result.failure(
                            Failure(
                                failureRepository = FailureRepository.LOCAL,
                                failureType = FailureType.NOT_200,
                                failureStatus = null,
                                failureCode = Failure.NO_CODE_SERVER_ERROR_CODE,
                                language = Language.PERSIAN,
                            )
                        )
                    Result.success(referrers.map { ReferrerTypeUIModel(it.name, it.showName) })
                },
                onFailure = { failure ->
                    Result.failure(failure)
                },
            )
        }
}
