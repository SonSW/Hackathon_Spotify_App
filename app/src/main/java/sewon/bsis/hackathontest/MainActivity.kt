package sewon.bsis.hackathontest

//import com.spotify.protocol.client.Subscription
//import com.spotify.protocol.types.PlayerState

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.system.Os.socket
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import java.io.DataOutputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.Socket


class MainActivity : AppCompatActivity() {

    private val clientId = "a0c1fdefc1184a0ca550ac5162bb9c70"
    private val redirectUri = "http://localhost:8888/callback"
//    private var spotifyAppRemote: SpotifyAppRemote? = null

    private val REQUEST_CODE = 777

    private lateinit var textView: TextView
//    private lateinit var token_button: Button
    private lateinit var connect_button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        connect_button = findViewById(R.id.connect_btn)

        connect_button.setOnClickListener {
            val builder =
                AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, redirectUri)
            builder.setScopes(
                arrayOf("user-top-read", "user-follow-read", "user-read-private")
//                arrayOf("streaming,user-read-private,user-read-email,playlist-read-private,playlist-read-collaborative,user-follow-read,user-read-playback-position,user-top-read,user-read-recently-played")
            )
            val request = builder.build()
            AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)

            val handler = it.handler
            Thread {
                try {
                    val socket = Socket("192.168.26.159",59876)
                    val stream = DataOutputStream(socket.getOutputStream())
                    stream.writeUTF("Passione rocks!")
                } catch (e: Exception) {
                    handler.post {
                        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    override fun onStart() {
        super.onStart()
        ////////////////////////////////////////////////////////////
//        val connectionParams = ConnectionParams.Builder(clientId)
//            .setRedirectUri(redirectUri)
//            .showAuthView(true)
//            .build()
//
//        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
//            override fun onConnected(appRemote: SpotifyAppRemote) {
//                spotifyAppRemote = appRemote
//                Log.d("MainActivity", "Connected! Yay!")
//                // Now you can start interacting with App Remote
//                connected()
//            }
//
//            override fun onFailure(throwable: Throwable) {
//                Log.e("MainActivity", throwable.message, throwable)
//                // Something went wrong when attempting to connect! Handle errors here
//            }
//        })
    }
//
//    private fun connected() {
//        spotifyAppRemote?.let {
//            // Play a playlist
//            val playlistURI = "spotify:playlist:37i9dQZF1DX2sUQwD7tbmL"
//            it.playerApi.play(playlistURI)
//            // Subscribe to PlayerState
//            it.playerApi.subscribeToPlayerState().setEventCallback {
//                val track: Track = it.track
//                Log.d("MainActivity", track.name + " by " + track.artist.name)
//            }
//        }
//
//    }
//
//    override fun onStop() {
//        super.onStop()
//        spotifyAppRemote?.let {
//            SpotifyAppRemote.disconnect(it)
//        }
//    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            when (val type = response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    textView.text = response.accessToken
                }

                AuthorizationResponse.Type.ERROR -> {
                    textView.text = "Error!"
                }

                else -> {
                    textView.text = "??? Something's wrong"
                }
            }
        }
    }
}