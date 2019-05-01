package ir.ayantech.networking

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCallStatus
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var ayanApi: AyanApi
    private var ayanCommonCallingStatus = AyanCommonCallStatus {
        loading { Log.d("AyanLog", "loading") }
        failure { failure ->
            Log.d("AyanLog", failure.failureMessage)
            retryBtn.setOnClickListener { failure.reCallApi() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ayanApi = AyanApi(
            { "4A1F899B6516E91180DE8AEEEED4CA9C" },
            "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/"
        )

        ayanApi.ayanCall<GetEndUserInquiryHistoryDetailOutputModel>(
            AyanCallStatus(ayanCommonCallingStatus) {
                success {
                    Log.d("AyanLog", it.response.toString())
                    val retry = it.reCallApi
                    retryBtn.setOnClickListener { retry() }
                }
            },
            "GetEndUserInquiryHistoryDetail",
            GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry")
        )
    }
}

data class GetEndUserInquiryHistoryDetailInputModel(val InquiryType: String)

data class GetEndUserInquiryHistoryDetailOutputModel(
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