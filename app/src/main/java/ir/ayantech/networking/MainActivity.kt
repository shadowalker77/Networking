package ir.ayantech.networking

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCallStatus
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.WrappedPackage
import ir.ayantech.ayannetworking.networking.RetrofitClient
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

    private var wrappedPackage: WrappedPackage<*, GetEndUserInquiryHistoryDetailOutputModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ayanApi = AyanApi(
            { "C1901C8ED01143038C13A0B8D29EF3F5" },
            "https://mytehranapp.tehran.ir/WebServices/Core.svc/",
            ayanCommonCallingStatus
        )

//        wrappedPackage = ayanApi.ayanCall(
//            AyanCallStatus {
//                success {
//                    Log.d("AyanLog", it.response.toString())
//                    val retry = it.reCallApi
//                    retryBtn.setOnClickListener { retry() }
//                }
//            },
//            "GetEndUserInquiryHistoryDetail",
//            GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry")
//        )
//        RetrofitClient.cancelCalls()

        ayanApi.simpleCall<String>(
            "LastBillingDate"
//            , GetEndUserInquiryHistoryDetailInputModel("WaterBillInquiry")
        ) {
            Log.d("SimpleCall", it.toString())
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