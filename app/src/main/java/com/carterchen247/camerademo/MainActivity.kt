package com.carterchen247.camerademo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.carterchen247.camerakit.config.CameraConfig
import com.carterchen247.camerakit.core.CameraPresenter
import com.carterchen247.camerakit.core.OnPictureCapturedCallback
import com.carterchen247.camerakit.core.UserActionCallback
import com.carterchen247.camerakit.util.ImageUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPresenter: CameraPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
        initClickEvents()
        initCameraKit()
    }

    private fun initClickEvents() {
        val listener = View.OnClickListener { view ->
            when (view.id) {
                R.id.btnFacing -> cameraPresenter.toggleCameraPosition()
                R.id.btnFlash -> cameraPresenter.toggleFlashMode()
                R.id.btnCapture -> cameraPresenter.capture()
                R.id.previewContainer -> cameraPresenter.focus()
            }
        }
        btnFacing.setOnClickListener(listener)
        btnFlash.setOnClickListener(listener)
        btnCapture.setOnClickListener(listener)
        previewContainer.setOnClickListener(listener)
    }

    private fun initCameraKit() {
        val config = CameraConfig()
        cameraPresenter = CameraPresenter(this, previewContainer, config).apply {
            forceFullScreenPreview(false)
            setUserActionCallback(createUserActionCallback())
            setOnPictureCapturedCallback(createPictureCapturedCallback())
        }
    }

    private fun createUserActionCallback(): UserActionCallback {
        return object : UserActionCallback {
            override fun onUsingFrontCamera() {
                btnFacing.setImageResource(R.drawable.icon_camera_front)
            }

            override fun onUsingBackCamera() {
                btnFacing.setImageResource(R.drawable.icon_camera_back)
            }

            override fun onUsingFlashOnMode() {
                btnFlash.setImageResource(R.drawable.icon_flash_on)
            }

            override fun onUsingFlashOffMode() {
                btnFlash.setImageResource(R.drawable.icon_flash_off)
            }

            override fun onUsingFlashAutoMode() {
                btnFlash.setImageResource(R.drawable.icon_flash_auto)
            }

            override fun onCaptureButtonShouldLock() {
            }

            override fun onCaptureButtonShouldUnlock() {
            }
        }
    }

    private fun createPictureCapturedCallback(): OnPictureCapturedCallback {
        return OnPictureCapturedCallback { data, cameraPosition ->
            val outputFile = ImageUtil.makeTempFile(this@MainActivity, ImageUtil.createSaveDir("photo"), "camera", ".jpg")
            ImageUtil.saveToDiskAsync(data, outputFile) {
                val intent = Intent(this@MainActivity, ResultActivity::class.java)
                intent.putExtra(Constant.FILE, outputFile.absolutePath)
                intent.putExtra(Constant.IS_FRONT_CAMERA, cameraPosition)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cameraPresenter.startPreview()
    }

    override fun onPause() {
        super.onPause()
        cameraPresenter.stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraPresenter.releaseCamera()
    }
}