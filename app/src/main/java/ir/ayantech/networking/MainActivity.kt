package ir.ayantech.networking

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayannetworking.api.ApiCache
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.api.AyanCommonCallStatus
import ir.ayantech.ayannetworking.api.WrappedPackage
import ir.ayantech.ayannetworking.helper.dePent
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
            ayanCommonCallingStatus,
            hostName = "application.billingsystem.ayantech.ir",
            logItems = listOf(
                5850,
                2862,
                3996,
                4536,
                8526,
                4264,
                3479,
                3840,
                2548,
                3692,
                6566,
                5850,
                5194,
                4070,
                8181,
                4959,
                8282,
                3976,
                4320,
                4949,
                7171,
                3752,
                11700,
                2915,
                7252,
                4617,
                8700,
                4428,
                3905,
                7840,
                4851,
                3976,
                6767,
                6084,
                5194,
                3996,
                7938,
                4698,
                8282,
                3621,
                4240,
                2793,
                3976,
                6633,
                5967,
                2862,
                3848,
                4212,
                8700,
                4264,
                7242,
                7840,
                4949,
                3834,
                3350,
                11466,
                5247,
                3774,
                4050,
                4872,
                4264,
                3479,
                7760,
                2352
            ),
            feed = arrayOf(
                117,
                53,
                74,
                81,
                87,
                82,
                71,
                80,
                49,
                71,
                67
            )
        )



        Log.d(
            "depent", listOf(
                13455,
                5512,
                7178,
                4050,
                4611,
                4428,
                3337,
                9040,
                5145,
                6319,
                7973,
                13104,
                2915,
                6586,
                7128,
                10005,
                5658,
                3408,
                6000,
                3675,
                6035,
                7839,
                13338,
                5353,
                8214,
                9801,
                9831,
                9184,
                5751,
                5600,
                5733,
                6958,
                6566,
                6201,
                5459,
                6142,
                5508,
                8787,
                9102,
                5609,
                8880,
                4214,
                8520,
                7370,
                6318,
                6148,
                8066,
                8262,
                9918,
                6970,
                4331
            ).dePent(
                arrayOf(
                    117,
                    53,
                    74,
                    81,
                    87,
                    82,
                    71,
                    80,
                    49,
                    71,
                    67
                )
            )
        )

        Log.d(
            "depent", listOf(
                5850,
                2862,
                3996,
                4536,
                8526,
                4264,
                3479,
                3840,
                2548,
                3692,
                6566,
                5850,
                5194,
                4070,
                8181,
                4959,
                8282,
                3976,
                4320,
                4949,
                7171,
                3752,
                11700,
                2915,
                7252,
                4617,
                8700,
                4428,
                3905,
                7840,
                4851,
                3976,
                6767,
                6084,
                5194,
                3996,
                7938,
                4698,
                8282,
                3621,
                4240,
                2793,
                3976,
                6633,
                5967,
                2862,
                3848,
                4212,
                8700,
                4264,
                7242,
                7840,
                4949,
                3834,
                3350,
                11466,
                5247,
                3774,
                4050,
                4872,
                4264,
                3479,
                7760,
                2352
            ).dePent(
                arrayOf(
                    117,
                    53,
                    74,
                    81,
                    87,
                    82,
                    71,
                    80,
                    49,
                    71,
                    67
                )
            ))

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