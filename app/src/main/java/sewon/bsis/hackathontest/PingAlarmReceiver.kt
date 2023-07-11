package sewon.bsis.hackathontest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.DataOutputStream
import java.net.Socket

class PingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val host_ip = intent.getStringExtra("host_ip")
        val uid = intent.getStringExtra("uid")
        Log.d("Sewon", "$uid:onReceive")

        Thread {
            val socket = Socket(host_ip, 59876)
            val stream = DataOutputStream(socket.getOutputStream())
            stream.writeUTF("$uid:Still connected!")
            stream.close()
            socket.close()
        }.start()
    }
}