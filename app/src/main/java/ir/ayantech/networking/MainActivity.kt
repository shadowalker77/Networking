package ir.ayantech.networking

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayannetworking.api.ApiCache
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.WrappedPackage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var ayanApi: AyanApi
    private var ayanCommonCallingStatus = AyanCommonCallStatus {
        changeStatus { Log.d("AyanLog", it.name) }
        failure { failure ->
            Log.d("AyanLog", failure.failureMessage)
            retryBtn.setOnClickListener { failure.reCallApi() }
        }
    }

    private var wrappedPackage: WrappedPackage<*, GetEndUserInquiryHistoryDetailOutputModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ayanApi = AyanApi(
            this,
            { "FBA3A180FE94425BB11D4FA1D5466527" },
            "https://application.billingsystem.ayantech.ir/WebServices/Core.svc/",
            ayanCommonCallingStatus
        )

        /*ayanApi.simpleCall<String>(
            "LastBillingDate", GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry")
        ) {
            Log.d("SimpleCall", it.toString())
        }*/

//        ayanApi.call<GetEndUserInquiryHistoryDetailOutputModel>(
//            "GetEndUserInquiryHistoryDetail",
//            GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry")
//        ) {
//            useCommonChangeStatusCallback = false
//            success {
//                Log.d("AyanLog", it.toString())
//            }
//            failure {
//                Log.d("AyanLog", it.failureMessage)
//            }
//        }

        val ggg =
            ApiCache.create<GetEndUserInquiryHistoryDetailOutputModel>(
                ayanApi,
                "GetEndUserInquiryHistoryDetail"
            ).also { it.input = GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry") }

        ggg.getApiResult {
            Log.d("AyanLog", it.toString())
        }
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