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

    private var wrappedPackage: WrappedPackage<*, GetEndUserInquiryHistoryDetail.Output>? = null

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
            ApiCache.create<GetEndUserInquiryHistoryDetail.Output>(
                ayanApi,
                "GetEndUserInquiryHistoryDetail"
            ).also { it.input = GetEndUserInquiryHistoryDetail.Input("WaterBillInquiry") }

        ggg.getFullApiResult {
            success {
                Log.d("AyanLog", it.toString())
            }
        }
    }
}