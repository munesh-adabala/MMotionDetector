package com.example.opencv_mobile.tflite;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import static com.example.opencv_mobile.utils.Constants.TF_OD_API_INPUT_SIZE;

/**
 * Class for FaceDetection
 */
public class FaceDetection {

    private static final String TAG = "face_detection";
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "face_detect.tflite";// "face_detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/face_labelmap.txt";
    private Classifier detector;

    public FaceDetection(Context context) {
        loadModel(context);
    }

    /**
     * Function to Load Face detection model
     */
    private void loadModel(Context context) {
        try {

            detector = TFLiteObjectDetectionAPIModel.create(
                    context,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);

        } catch (final IOException e) {
            Log.e(TAG, "loadModel: EXcpetion while face recog model==" + e.getMessage());
        }

    }

    /**
     * Function to get List of Face Recognition
     *
     * @param previewBitmap : Bitmap
     * @return
     */
    public List<Classifier.Recognition> recognizeImage(Bitmap previewBitmap) {

        return detector.recognizeImage(previewBitmap);
    }

    /**
     * Function to close the object
     */
    public void clean() {
        detector.close();
    }
}
















