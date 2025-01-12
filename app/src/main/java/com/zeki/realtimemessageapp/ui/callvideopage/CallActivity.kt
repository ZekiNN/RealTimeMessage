package com.zeki.realtimemessageapp.ui.callvideopage

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zeki.realtimemessageapp.databinding.ActivityCallVideoBinding
import com.zeki.realtimemessageapp.webrtc.WebRtcClient
import com.zeki.realtimemessageapp.ui.home.HomeActivity
import com.zeki.realtimemessageapp.webrtc.serviceIntent
import org.webrtc.EglBase
import org.webrtc.MediaStream

class CallActivity : Activity() {

    private val eglBase by lazy { EglBase.create() }
    private val binding by lazy { ActivityCallVideoBinding.inflate(layoutInflater) }
    private var webRtcClient: WebRtcClient? = null
    private val isCallComing by lazy { intent.extras?.getBoolean(KEY_IS_CALL_COMING) }
    private val toId by lazy { intent.extras?.getString(KEY_TO_USER_ID) }
    private val fromId by lazy { intent.extras?.getString(KEY_FROM_USER_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initData()
    }

    private fun initView() {

        binding.localRenderer.apply {
            setEnableHardwareScaler(true)
            setMirror(false)
            init(eglBase.eglBaseContext, null)
        }

        binding.remoteRenderer.apply {
            setEnableHardwareScaler(true)
            setMirror(false)
            init(eglBase.eglBaseContext, null)
        }

        //断开
        binding.btnCancel.setOnClickListener {
            leave(true)
        }
    }

    private fun initData() {
        RxPermissions(this).request(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).doOnNext { granted ->
            if (granted) {

                initWebRtc()

                //如果是接受对面电话的情况，直接向对面发送一个receive
                if (isCallComing == true) {
                    webRtcClient?.startLocalCamera(context = applicationContext)
                    webRtcClient?.initSocketMessage()
                    webRtcClient?.receiveByClientId(fromId!!)
                } else {
                    webRtcClient?.startLocalCamera(context = applicationContext)
                    webRtcClient?.initSocketMessage()
                    webRtcClient?.callByClientId(toId!!)
                }
            }
        }.subscribe()
    }

    private fun initWebRtc() {
        webRtcClient = WebRtcClient(
            application,
            eglBase.eglBaseContext,
            object : WebRtcClient.RtcListener {
                override fun onLocalStream(localStream: MediaStream) {
                    localStream.videoTracks[0].addSink(binding.localRenderer)
                }

                override fun onAddRemoteStream(remoteStream: MediaStream, endPoint: Int) {
                    remoteStream.videoTracks[0].addSink(binding.remoteRenderer)
                }

                override fun onRemoveRemoteStream(endPoint: Int) {
                    leave(false)
                }
            }
        )
    }

    private fun leave(isSender: Boolean) {

        webRtcClient?.onDestroy(isSender)
        /*binding.localRenderer.release()
        binding.remoteRenderer.release()*/

        finish()

    }

    override fun onPause() {
        super.onPause()
        webRtcClient?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webRtcClient?.onResume()
    }

    companion object {
        private val TAG = HomeActivity::class.java.canonicalName

        fun jumpHere(
            context: Context,
            isCallComing: Boolean = false,
            toClientId: String? = null, //当isCallComing为false时必填
            fromClientId: String? = null //当isCallComing为true时必填
        ) {
            val intent = Intent(context, CallActivity::class.java).apply {
                putExtra(KEY_IS_CALL_COMING, isCallComing)
                putExtra(KEY_FROM_USER_ID, fromClientId)
                putExtra(KEY_TO_USER_ID, toClientId)
            }
            context.startActivity(intent)
        }
    }
}