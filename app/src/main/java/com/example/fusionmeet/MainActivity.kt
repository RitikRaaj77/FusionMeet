package com.example.fusionmeet

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fusionmeet.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoCompositingLayout.Canvas

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val appId = "73040bcfd9f240d7a58eda24cbe20528"

    private val channelName = "whisperbox"

    private val token = "007eJxTYLjKtTEha3dPYvTPn26vDio/UrgUZGnSp+vV+cZBlHvCn4MKDObGBiYGSclpKZZpRiYGKeaJphapKYlGJslJqUYGpkYWr+zepDUEMjLsj41mZmSAQBCfi6E8I7O4ILUoKb+CgQEAZ9wjNQ=="
    private var uid = 0

    private var isJoined = false

    private var agoraEngine : RtcEngine? = null

    private var localSurfaceView : SurfaceView? = null
    private var remoteSurfaceView : SurfaceView? = null




    private val PERMISSION_ID  = 12
    private val REWUESTED_PERMISSION = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean{
        return !(ContextCompat.checkSelfPermission(
            this,
            REWUESTED_PERMISSION[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, REWUESTED_PERMISSION[1]
                ) != PackageManager.PERMISSION_GRANTED )
    }

    private fun showMessage(message : String){
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpVideoSdKEngine(){
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        }catch (e : Exception){
            showMessage(e.message!!)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!checkSelfPermission()){
            ActivityCompat
                .requestPermissions(
                    this, REWUESTED_PERMISSION, PERMISSION_ID
                )
        }

        setUpVideoSdKEngine()

        binding.joinButton.setOnClickListener {
            joinCall()
        }
        binding.leaveButton.setOnClickListener {
            leaveCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread{
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    private fun leaveCall() {
        if(!isJoined){
            showMessage("Join a channel first")
        }else{
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if(remoteSurfaceView !=null){
                remoteSurfaceView!!.visibility = GONE
            }
            if(localSurfaceView !=null){
                localSurfaceView!!.visibility = GONE
            }
            isJoined = false
        }
    }

    private fun joinCall() {
        if(checkSelfPermission()){
            val option = ChannelMediaOptions()
            option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setUpLocalVideo()
            localSurfaceView!!.visibility = VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, option)
        }else{
            showMessage("Permission Not Granted")
        }
    }
    private val mRtcEventHandler : IRtcEngineEventHandler = object : IRtcEngineEventHandler(){

        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined with uid : $uid")

            runOnUiThread { setUpRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel : $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("User Offline")

            runOnUiThread {
                remoteSurfaceView!!.visibility = GONE
            }
        }
    }

    private fun setUpRemoteVideo(uid : Int){
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteUser.addView(remoteSurfaceView)

        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
    }
    private fun setUpLocalVideo(){
        localSurfaceView = SurfaceView(baseContext)
        binding.localUser.addView(localSurfaceView)

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                0
            )
        )
    }
}

