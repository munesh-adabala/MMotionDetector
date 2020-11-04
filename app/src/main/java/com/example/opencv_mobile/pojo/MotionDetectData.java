package com.example.opencv_mobile.pojo;

import org.opencv.core.Mat;

public class MotionDetectData {
    private boolean isMotionDetected;
    private Mat motionDetectedFrame;

    public MotionDetectData(boolean isMotionDetected, Mat motionDetectedFrame){
        this.isMotionDetected=isMotionDetected;
        this.motionDetectedFrame=motionDetectedFrame;
    }
    public boolean isMotionDetected() {
        return isMotionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        isMotionDetected = motionDetected;
    }

    public Mat getMotionDetectedFrame() {
        return motionDetectedFrame;
    }

    public void setMotionDetectedFrame(Mat motionDetectedFrame) {
        this.motionDetectedFrame = motionDetectedFrame;
    }
}
