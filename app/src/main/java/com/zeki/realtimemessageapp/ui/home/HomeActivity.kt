package com.zeki.realtimemessageapp.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.MediaRecorder
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.nkzawa.socketio.client.IO
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zeki.realtimemessageapp.databinding.ActivityMainBinding
import com.zeki.realtimemessageapp.model.RTCUser
import com.zeki.realtimemessageapp.ui.callvideopage.CallActivity
import com.zeki.realtimemessageapp.utils.jsonToList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

var mediaProjectionPermissionResultData: Intent? = null

class HomeActivity : Activity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initView()
        initSocket()
        initScreenService()
    }

    private fun initView() {
        binding.rfIds.setOnRefreshListener {
            socket?.refreshIdList()
        }
        //配置在线用户列表
        binding.rvRtcUser.adapter = RTCRecyclerAdapter {
            //点击Call 开始视频通话
            RxPermissions(this).request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).doOnNext { granted ->
                if (granted) {
                    CallActivity.jumpHere(
                        context = this,
                        isCallComing = false,
                        toClientId = it.id
                    )
                }
            }.subscribe()
        }
    }

    private fun initSocket() {
        // 初始化 Socket 通信
        try {
            /*socket?.disconnect()
            socket?.close()*/
            socket = IO.socket("http://192.168.50.80:3000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        socket?.connect()
    }

    val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun initScreenService() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, 888)
    }

    //接受对面的电话
    private fun receiveCall(fromClientId: String) {
        CallActivity.jumpHere(context = this, isCallComing = true, fromClientId = fromClientId)
    }

    override fun onResume() {
        super.onResume()
        //连接成功后，服务端返回id
        socket?.on("id") { args ->
            val id = args[0] as String
            Log.d("id", id)
            runOnUiThread { Toast.makeText(this, "已连上服务器", Toast.LENGTH_SHORT).show() }
            //startLocalCamera( this@MainActivity)
            socket?.readyToStream("${Build.MODEL}")
        }

        //发送readyToStream后，服务器返回所有在线id列表
        socket?.on("ids") { args ->
            if (args.isNotEmpty()) {
                val jsonArray = args[0] as? JSONArray
                Log.d("list", jsonArray.toString())
                runOnUiThread {
                    Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
                    val list: List<RTCUser> = jsonArray.toString().jsonToList() ?: listOf()
                    (binding.rvRtcUser.adapter as RTCRecyclerAdapter).replaceData(list.filterNot { it.name == Build.MODEL })
                    binding.rfIds.isRefreshing = false
                }
            }
        }

        //来自对端的消息
        socket?.on("message") { args ->
            val data = args[0] as JSONObject
            try {
                val from = data.getString("from")
                val type = data.getString("type")

                //根据不同的指令类型和数据响应相应步骤的方法
                when (type) {
                    "call" -> receiveCall(from) //对面来电，写死直接接听电话
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 888 && resultCode == RESULT_OK) {
            mediaProjectionPermissionResultData = data
        }
    }

}