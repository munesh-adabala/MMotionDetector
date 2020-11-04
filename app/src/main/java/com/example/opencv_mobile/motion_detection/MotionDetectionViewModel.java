package com.example.opencv_mobile.motion_detection;

import android.os.AsyncTask;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.opencv_mobile.pojo.MotionDetectData;
import com.example.opencv_mobile.utils.Constants;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * View Model which detects the motion in the frame.
 */
public class MotionDetectionViewModel extends ViewModel {
    private MutableLiveData<MotionDetectData> liveData;
    private BackgroundSubtractorMOG2 subtractor;
    private MotionDetectTask motionDetectTask;

    public LiveData<MotionDetectData> getLiveData() {
        if (liveData == null) {
            liveData = new MutableLiveData<>();
            subtractor = Video.createBackgroundSubtractorMOG2(10, 25, false);
        }
        return liveData;
    }

    public void detectMotion(Mat frame) {
        motionDetectTask = new MotionDetectTask(new WeakReference<MutableLiveData<MotionDetectData>>(liveData));
        motionDetectTask.execute(frame, subtractor);
    }

    static class MotionDetectTask extends AsyncTask<Object, Object, Object> {
        private Mat fgMask = new Mat();
        private Mat heirarchy = new Mat();
        private Mat canny = new Mat();
        private int KERNEL_SIZE = 0;
        private List<MatOfPoint> contours = new ArrayList<>();
        private int counter = 0;
        private MutableLiveData<MotionDetectData> liveData;

        MotionDetectTask(WeakReference<MutableLiveData<MotionDetectData>> liveDataWeakReference) {
            if (liveDataWeakReference != null) {
                this.liveData = liveDataWeakReference.get();
            }
        }

        @Override
        protected Object doInBackground(Object... objects) {
            Mat frame = (Mat) objects[0];
            BackgroundSubtractorMOG2 subtractorMOG2 = (BackgroundSubtractorMOG2) objects[1];
            doMotionDetection(frame, subtractorMOG2);
            return null;
        }

        private void doMotionDetection(Mat inputFrame, BackgroundSubtractorMOG2 subtractor) {
            Mat bgr = new Mat();
            Imgproc.cvtColor(inputFrame, bgr, Imgproc.COLOR_BGR2GRAY);
            subtractor.apply(bgr, fgMask);
            Imgproc.threshold(fgMask, fgMask, 25, 255, Imgproc.THRESH_BINARY);
            Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2 * KERNEL_SIZE + 1, 2 * KERNEL_SIZE + 1),
                    new Point(KERNEL_SIZE, KERNEL_SIZE));
            Imgproc.dilate(fgMask, fgMask, kernel, new Point(-1, -1), 2);
            fgMask.copyTo(canny);
            Imgproc.findContours(canny, contours, heirarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // DEBUG
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > Constants.AREA_THRESHOLD) {
                    if (liveData != null) {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            liveData.setValue(new MotionDetectData(true,inputFrame));
                        } else {
                            liveData.postValue(new MotionDetectData(true,inputFrame));
                        }
                    }
//                Rect rect = Imgproc.boundingRect(contour);
//                Imgproc.rectangle(inputFrame, rect.tl(), rect.br(), color, 4);
                    counter = 0;
                }
            }
            contours.clear();
        }
    }

    @Override
    protected void onCleared() {
        subtractor.clear();
        liveData = null;
        super.onCleared();
    }
}
