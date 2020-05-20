package com.carterchen247.camerakit.core

interface UserActionCallback {
    fun onUsingFrontCamera()
    fun onUsingBackCamera()
    fun onUsingFlashOnMode()
    fun onUsingFlashOffMode()
    fun onUsingFlashAutoMode()
    fun onCaptureButtonShouldLock()
    fun onCaptureButtonShouldUnlock()
}