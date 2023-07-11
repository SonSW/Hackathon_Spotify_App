@file:Suppress("DEPRECATION")

package sewon.bsis.hackathontest

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
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
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private val TAG = "Sewon"

    private val clientId = "a0c1fdefc1184a0ca550ac5162bb9c70"
    private val redirectUri = "http://localhost:8888/callback"

    private val REQUEST_CODE_AUTH = 777

    private lateinit var connect_button: Button
    private lateinit var send_button: Button

    private val uniqueID = UUID.randomUUID().toString()

    private lateinit var wifi_name: String
    private lateinit var host_ip: String
    private var token: String? = null

    private lateinit var alarmIntent: PendingIntent

    private val qrLauncher = registerForActivityResult(ScanContract()) {
        if (it.contents == null)
            Toast.makeText(this, "QR Null", Toast.LENGTH_SHORT).show()
        else {
            wifi_name = it.contents.split('\n')[0]
            host_ip = it.contents.split('\n')[1]
            Log.d(TAG, "wifi name: $wifi_name")
            Log.d(TAG, "host_ip: $host_ip")

            Toast.makeText(applicationContext, wifi_name+"으로 연결하십시오.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            send_button.visibility = View.VISIBLE
        }
    }

    private var pManager: PowerManager? = null
    private var alarmMgr: AlarmManager? = null

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "uid is $uniqueID")

        connect_button = findViewById(R.id.connect_btn)
        send_button = findViewById(R.id.send_btn)

        pManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if(!pManager!!.isIgnoringBatteryOptimizations(packageName)) {
            val permIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            permIntent.data = Uri.parse("package:$packageName")
            startActivity(permIntent)
        }

//        Toast.makeText(this, "Spotify와 연결 중입니다. 기다려주세요.", Toast.LENGTH_LONG).show()
        val builder =
            AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
        builder.setScopes(arrayOf("user-top-read", "user-follow-read", "user-read-private"))
        val request = builder.build()
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE_AUTH, request)

        connect_button.setOnClickListener {
            if (token == null) {
                Toast.makeText(this, "token is null", Toast.LENGTH_SHORT).show()
            } else {
                qrLauncher.launch(ScanOptions())
            }
        }

        send_button.setOnClickListener {
            alarmIntent = Intent(applicationContext, PingAlarmReceiver::class.java).apply {
                this.putExtra("host_ip", host_ip);
                this.putExtra("uid", uniqueID)
            }.let {
                PendingIntent.getBroadcast(applicationContext, 0, it, PendingIntent.FLAG_MUTABLE)
            }

            alarmMgr!!.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60*1000,
                60*1000,
                alarmIntent
            )
            Log.d(TAG, "alarmMgr!!.setInexactRepeating done.")

            try {
                Thread {
                    val socket = Socket(host_ip, 59876)
                    val stream = DataOutputStream(socket.getOutputStream())
                    stream.writeUTF("$uniqueID:$token")
                    stream.close()
                    socket.close()
                }.start()
            } catch (_: Exception) { }
            Toast.makeText(this, "Succesfully connected with Passione!", Toast.LENGTH_SHORT).show()
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