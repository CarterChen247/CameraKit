package com.carterchen247.camerakit.core

interface OnPictureCapturedCallback {
    fun onPictureCaptured(data: ByteArray?, cameraPosition: Int)
}