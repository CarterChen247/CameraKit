package com.carterchen247.camerademo

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.carterchen247.camerakit.util.ImageUtil
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {

    companion object {
        const val FILE_PATH = "FILE_PATH"
        const val IS_FRONT_CAMERA = "IS_FRONT_CAMERA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val path = intent.getStringExtra(FILE_PATH)
        val isFrontCamera = intent.getBooleanExtra(IS_FRONT_CAMERA, false)

        capturedImg.doOnPreDraw {
            loadCapturedImage(path, isFrontCamera)
        }
    }

    private fun loadCapturedImage(path: String?, isFrontCamera: Boolean) {
        capturedImg.run {
            val bitmap = ImageUtil.getRotatedBitmap(Uri.parse(path).path, width, height, isFrontCamera)
            setImageBitmap(bitmap)
        }
    }
}