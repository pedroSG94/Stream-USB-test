package com.pedro.usbtest

import android.app.Activity
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.pedro.usbtest.streamlib.RtmpUSB
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import kotlinx.android.synthetic.main.activity_main.*
import net.ossrs.rtmp.ConnectCheckerRtmp


class MainActivity : Activity(), SurfaceHolder.Callback, ConnectCheckerRtmp {

  override fun onAuthSuccessRtmp() {
    Log.e("Pedro", "auth success")
  }

  override fun onConnectionSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtmp(reason: String?) {
    runOnUiThread {
      Toast.makeText(this, "Failed $reason", Toast.LENGTH_SHORT).show()
      rtmpUSB.stopStream(uvcCamera)
    }
  }

  override fun onAuthErrorRtmp() {
    Log.e("Pedro", "auth error")
  }

  override fun onDisconnectRtmp() {
    runOnUiThread {
      Toast.makeText(this, "Disconnect", Toast.LENGTH_SHORT).show()
    }
  }

  override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
  }

  override fun surfaceDestroyed(p0: SurfaceHolder?) {

  }

  override fun surfaceCreated(p0: SurfaceHolder?) {

  }

  private lateinit var usbMonitor: USBMonitor
  private var uvcCamera: UVCCamera? = null
  private var isUsbOpen = true
  private val width = 1280
  private val height = 720
  private var defished = false
  private lateinit var rtmpUSB: RtmpUSB

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    rtmpUSB = RtmpUSB(openglview, this)
    usbMonitor = USBMonitor(this, onDeviceConnectListener)
    isUsbOpen = false
    usbMonitor.register()
    start_stop.setOnClickListener {
      if (uvcCamera != null) {
        if (!rtmpUSB.isStreaming) {
          startStream(et_url.text.toString())
          start_stop.text = "Stop stream"
        } else {
          rtmpUSB.stopStream(uvcCamera)
          start_stop.text = "Start stream"
        }
      }
    }
  }

  private fun startStream(url: String) {
    if (rtmpUSB.prepareVideo(width, height, 30, 4000 * 1024, false, 0,
        uvcCamera) && rtmpUSB.prepareAudio()) {
      rtmpUSB.startStream(uvcCamera, url)
    }
  }

  private val onDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
    override fun onAttach(device: UsbDevice?) {
      usbMonitor.requestPermission(device)
    }

    override fun onConnect(
      device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?,
      createNew: Boolean
    ) {
      val camera = UVCCamera()
      camera.open(ctrlBlock)
      try {
        camera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG)
      } catch (e: IllegalArgumentException) {
        camera.destroy()
        try {
          camera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE)
        } catch (e1: IllegalArgumentException) {
          return
        }
      }
      uvcCamera = camera
      rtmpUSB.startPreview(uvcCamera, width, height)
    }

    override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
      if (uvcCamera != null) {
        uvcCamera?.close()
        uvcCamera = null
        isUsbOpen = false
      }
    }

    override fun onDettach(device: UsbDevice?) {
      if (uvcCamera != null) {
        uvcCamera?.close()
        uvcCamera = null
        isUsbOpen = false
      }
    }

    override fun onCancel(device: UsbDevice?) {

    }
  }

  override fun onDestroy() {
    super.onDestroy()
      if (rtmpUSB.isStreaming && uvcCamera != null) rtmpUSB.stopStream(uvcCamera)
    if (rtmpUSB.isOnPreview && uvcCamera != null) rtmpUSB.stopPreview(uvcCamera)
    if (isUsbOpen) {
      uvcCamera?.close()
      usbMonitor.unregister()
    }
  }
}
