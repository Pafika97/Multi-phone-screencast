package com.example.multicast

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editSignalUrl: EditText
    private lateinit var editRoom: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var rtcClient: RtcClient? = null
    private var mpm: MediaProjectionManager? = null
    private var resultCode: Int = 0
    private var resultData: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editSignalUrl = findViewById(R.id.editSignalUrl)
        editRoom = findViewById(R.id.editRoom)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        btnStart.setOnClickListener {
            if (resultData == null) {
                startActivityForResult(mpm?.createScreenCaptureIntent(), 1001)
            } else {
                startStreaming()
            }
        }
        btnStop.setOnClickListener { stopStreaming() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            this.resultCode = resultCode
            this.resultData = data
            startStreaming()
        }
    }

    private fun startStreaming() {
        val url = editSignalUrl.text.toString().trim()
        val room = editRoom.text.toString().trim()
        if (url.isEmpty() || room.isEmpty() || resultData == null) return
        rtcClient = RtcClient(this, url, room, mpm!!, resultCode, resultData!!)
        rtcClient?.start()
    }

    private fun stopStreaming() {
        rtcClient?.stop()
        rtcClient = null
    }
}