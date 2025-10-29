package com.example.multicast

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import org.webrtc.*
import java.net.URI
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject

class RtcClient(
    private val ctx: Context,
    private val signalUrl: String,
    private val room: String,
    private val mpm: MediaProjectionManager,
    private val resultCode: Int,
    private val resultData: Intent
) {
    private var eglBase: EglBase = EglBase.create()
    private var pc: PeerConnection? = null
    private var ws: WebSocketClient? = null
    private var clientId: String? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null

    fun start() {
        // WebSocket signaling
        ws = object : WebSocketClient(URI(signalUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                val join = JSONObject().apply {
                    put("type", "join")
                    put("roomId", room)
                }
                send(join.toString())
            }
            override fun onMessage(message: String?) {
                if (message == null) return
                val msg = JSONObject(message)
                when (msg.getString("type")) {
                    "joined" -> {
                        clientId = msg.getString("clientId")
                        // Ждем входящий offer от viewer (recvonly) и отдадим answer
                    }
                    "signal" -> {
                        val payload = msg.getJSONObject("payload")
                        ensurePeer()
                        if (payload.has("sdp")) {
                            val sdpObj = payload.getJSONObject("sdp")
                            val desc = SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(sdpObj.getString("type")),
                                sdpObj.getString("sdp")
                            )
                            pc?.setRemoteDescription(object: SdpObserverAdapter() {}, desc)
                            if (desc.type == SessionDescription.Type.OFFER) {
                                pc?.createAnswer(object: SdpObserverAdapter() {
                                    override fun onCreateSuccess(answer: SessionDescription?) {
                                        pc?.setLocalDescription(SdpObserverAdapter(), answer)
                                        val resp = JSONObject().apply {
                                            put("type", "signal")
                                            put("roomId", room)
                                            put("targetId", msg.getString("fromId"))
                                            put("payload", JSONObject().apply {
                                                put("sdp", JSONObject().apply {
                                                    put("type", answer?.type?.canonicalForm())
                                                    put("sdp", answer?.description)
                                                })
                                            })
                                        }
                                        ws?.send(resp.toString())
                                    }
                                }, MediaConstraints())
                            }
                        } else if (payload.has("candidate")) {
                            val cand = payload.getJSONObject("candidate")
                            val ice = IceCandidate(
                                cand.getString("sdpMid"),
                                cand.getInt("sdpMLineIndex"),
                                cand.getString("candidate")
                            )
                            pc?.addIceCandidate(ice)
                        }
                    }
                }
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {}
            override fun onError(ex: Exception?) { ex?.printStackTrace() }
        }
        ws?.connect()
    }

    private fun ensurePeer() {
        if (pc != null) return
        val factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        val ice = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            // Добавьте TURN
        )
        pc = factory.createPeerConnection(ice, object: PeerConnectionAdapter() {
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val out = JSONObject().apply {
                        put("type", "signal")
                        put("roomId", room)
                        // Мы не знаем заранее targetId (viewer меняется); viewer пришлет нам offer, у него есть fromId —
                        // мы отвечаем только в ответ на его сигналах в onMessage (выше). Здесь можно буферизовать.
                    }
                }
            }
        })

        // Захват экрана
        val videoCapturer = ScreenCapturerAndroid(resultData, object: MediaProjection.Callback() {})
        videoSource = factory.createVideoSource(false)
        videoCapturer.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            ctx, videoSource?.capturerObserver
        )
        videoCapturer.startCapture(1280, 720, 30)

        videoTrack = factory.createVideoTrack("video", videoSource)
        val stream = factory.createLocalMediaStream("stream")
        stream.addTrack(videoTrack)
        pc?.addStream(stream)
    }

    fun stop() {
        try {
            videoSource?.dispose()
            pc?.close()
            ws?.close()
        } catch (_: Exception) {}
    }
}

// Упрощенные адаптеры
open class SdpObserverAdapter: SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}

open class PeerConnectionAdapter: PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidate(p0: IceCandidate?) {}
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
    override fun onAddStream(p0: MediaStream?) {}
    override fun onRemoveStream(p0: MediaStream?) {}
    override fun onDataChannel(p0: DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
}