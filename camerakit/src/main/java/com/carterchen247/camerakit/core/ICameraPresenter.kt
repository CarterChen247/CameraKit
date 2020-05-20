package com.carterchen247.camerakit.core

interface ICameraPresenter {
    fun setUserActionCallback(view: UserActionCallback)
    fun setOnPictureCapturedCallback(pictureCapturedCallback: OnPictureCapturedCallback)
    fun startPreview()
    fun stopPreview()
    fun releaseCamera()
}