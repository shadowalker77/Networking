package ir.ayantech.networking

import com.alirezabdn.generator.AyanAPI

@AyanAPI
class GetEndUserInquiryHistoryDetail {
    data class Input(val InquiryType: String)

    data class Output(
        val InquiryHistory: ArrayList<InquiryModel>,
        val TotalInquiryHistoryCount: Long
    )

    data class InquiryModel(
        val Description: String,
        val IsFavorite: Boolean,
        val IsElectronic: Boolean,
        val ID: Long,
        val Type: String,
        val Value: String
    )
}