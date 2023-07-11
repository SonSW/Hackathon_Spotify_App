@file:Suppress("DEPRECATION")

package sewon.bsis.hackathontest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import java.io.DataOutputStream
import java.net.Socket


class MainActivity : AppCompatActivity() {

    private val clientId = "a0c1fdefc1184a0ca550ac5162bb9c70"
    private val redirectUri = "http://localhost:8888/callback"

    private val REQUEST_CODE_AUTH = 777
    private val REQUEST_CODE_QR = 888

    private lateinit var connect_button: Button

    private lateinit var wifi_name: String
    private lateinit var host_ip: String
    private var token: String? = null

    private val qrLauncher = registerForActivityResult(ScanContract()) {
        if (it.contents == null)
            Toast.makeText(this, "QR Null", Toast.LENGTH_SHORT).show()
        else {
            wifi_name = it.contents.split('\n')[0]
            host_ip = it.contents.split('\n')[1]
            Log.d("MainActivity", "wifi name: $wifi_name")
            Log.d("MainActivity", "host_ip: $host_ip")

            try {
                Thread {
                    val socket = Socket(host_ip, 59876)
                    val stream = DataOutputStream(socket.getOutputStream())
                    stream.writeUTF(token)
                }.start()
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Toast.makeText(this, "Spotify와 연결 중입니다. 기다려주세요.", Toast.LENGTH_LONG).show()
        val builder =
            AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
        builder.setScopes(arrayOf("user-top-read", "user-follow-read", "user-read-private"))
        val request = builder.build()
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE_AUTH, request)

        connect_button = findViewById(R.id.connect_btn)
        connect_button.setOnClickListener {
            if (token == null) {
                Toast.makeText(this, "token is null", Toast.LENGTH_SHORT).show()
            } else {
                qrLauncher.launch(ScanOptions())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        if (requestCode == REQUEST_CODE_AUTH) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            val type = response.type
            when (type) {
                AuthorizationResponse.Type.TOKEN -> {
                    token = response.accessToken
                }

                AuthorizationResponse.Type.ERROR -> Toast.makeText(
                    this,
                    "Error while getting Spotify token.",
                    Toast.LENGTH_SHORT
                ).show()

                else -> {}
            }
        }
    }
}