package ir.ayantech.sample.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ir.ayantech.networking.R
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.btn_fetch)
        val txtStatus = findViewById<TextView>(R.id.txt_status)
        val txtResponse = findViewById<TextView>(R.id.txt_response)

        btn.setOnClickListener {
            lifecycleScope.launch {
                mainViewModel.getDonation()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is MainUIState.Success -> {
                            txtResponse.text = buildString {
                                append("Success list count: ")
                                append(uiState.data.count())
                            }
                        }

                        is MainUIState.Error ->{
                            Log.d("TAG_AG", "onCreate: ${uiState.failure}")
                            txtStatus.text = buildString {
                                append("Error: ")
                                append(uiState.failure.failureMessage)
                            }
                        }
                        MainUIState.IDLE -> {

                        }
                    }
                }
            }
        }
    }

}
