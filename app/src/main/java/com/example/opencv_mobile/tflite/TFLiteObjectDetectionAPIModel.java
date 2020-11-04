/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.opencv_mobile.tflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import com.example.opencv_mobile.utils.Constants;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TFLiteObjectDetectionAPIModel implements Classifier {
    private static final String TAG = "TFLiteObjectDetectionAP";
    private static final Logger LOGGER = new Logger();

    // Only return this many results.
    private static final int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;

    private Interpreter tfLite;

    private final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
    private final RectF detection = new RectF();
    private final Map<Integer, Object> outputMap = new HashMap<>();
    private final float extend_ratio_h = 0.1f;
    private final float extend_ratio_w = 0.1f;

    private TFLiteObjectDetectionAPIModel() {
    }

    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param context       The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize     The size of image input
     * @param isQuantized   Boolean representing model is quantized or not
     */
    public static Classifier create(
            final Context context,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            boolean isQuantized)
            throws IOException {
        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();

        InputStream labelsInput = null;
        AssetManager assetManager = context.getAssets();
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.labels.add(line);
        }
        br.close();
        labelsInput.close();

        d.inputSize = inputSize;
        try {
            //      hexagonDelegate=new HexagonDelegate(context);
            //       Interpreter.Options options = (new Interpreter.Options()).addDelegate(hexagonDelegate);
            Interpreter.Options options = (new Interpreter.Options());
            options.setNumThreads(NUM_THREADS);
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), options);
            //  d.tfLite = new Interpreter();
        } catch (Exception e) {
            Log.e(TAG, "create: Got exception while creating the Model==" + e.getMessage());
            throw new RuntimeException(e);
        }

        //isQuantized = true;
        d.isModelQuantized = isQuantized;

        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {

        Log.e(TAG, "recognizeImage: Widht--"+Constants.IMG_WD+" Ht--"+Constants.IMG_HT);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false);
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};

        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        // Run the inference call.
        long old = System.currentTimeMillis();
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.e(TAG, "Time taken for face detect==" + (System.currentTimeMillis() - old) + " millis");

        recognitions.clear();
        int finalPos = 0;
        float value = Float.MIN_VALUE;
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection =
                    new RectF(

                            outputLocations[0][i][1] * Constants.IMG_WD,//inputSize ,
                            outputLocations[0][i][0] * Constants.IMG_HT,//inputSize,
                            outputLocations[0][i][3] * Constants.IMG_WD,//inputSize ,
                            outputLocations[0][i][2] * Constants.IMG_HT);//inputSize);
            if (outputScores[0][i] > 0.5) {
                float width = detection.right - detection.left;
                if (width > value) {
                    finalPos = i;
                    value = width;
                }
            }
        }
        return getFinalResults(finalPos);
    }

    private ArrayList<Recognition> getFinalResults(int pos) {
        final RectF detection =
                new RectF(

                        outputLocations[0][pos][1] * Constants.IMG_WD,//inputSize ,
                        outputLocations[0][pos][0] * Constants.IMG_HT,//inputSize,
                        outputLocations[0][pos][3] * Constants.IMG_WD,//inputSize ,
                        outputLocations[0][pos][2] * Constants.IMG_HT);//inputSize);

        int labelOffset = 1;
        recognitions.add(
                new Recognition(
                        "" + pos,
                        labels.get((int) outputClasses[0][pos] + labelOffset),
                        outputScores[0][pos],
                        getNewValues(detection)));
        return recognitions;
    }

    private RectF getNewValues(RectF detection) {
        int new_top = (int) (detection.top - ((detection.bottom - detection.top) * extend_ratio_h));
        int new_bottom = (int) (detection.bottom + ((detection.bottom - detection.top) * extend_ratio_h));
        int new_left = (int) (detection.left - ((detection.right - detection.left) * extend_ratio_w));
        int new_right = (int) (detection.right + ((detection.right - detection.left) * extend_ratio_w));
        if (new_top < 0) {
            new_top = 0;
        }
        if (new_bottom > Constants.IMG_HT) {
            new_bottom = Constants.IMG_HT;
        }
        if (new_left < 0) {
            new_left = 0;
        }
        if (new_right > Constants.IMG_WD) {
            new_right = Constants.IMG_WD;
        }
        float diffHTWd = (new_bottom - new_top) - (new_right - new_left);
        int offsetY = (int) Math.abs(diffHTWd / 2);
        new_top = new_top + offsetY;
        new_bottom = new_bottom + offsetY;

        int boxWidth = new_right - new_left;
        int boxHeight = new_bottom - new_top;
        int diff = boxHeight - boxWidth;
        int delta = Math.abs(diff / 2);

        if (diff == 0) {
            detection.top = new_top;
            detection.left = new_left;
            detection.right = new_right;
            detection.bottom = new_bottom;
            return detection;
        } else if (diff > 0) {
            new_left -= delta;
            new_right += delta;
            if (diff % 2 == 1) {
                new_right += 1;
            }
        } else {
            new_top -= delta;
            new_bottom += delta;
        }
        detection.top = new_top;
        if (new_left < 0) {
            detection.left = 0;
        } else {
            detection.left = new_left;
        }

        if (new_right > Constants.IMG_WD) {
            detection.right = Constants.IMG_WD;
        } else {
            detection.right = new_right;
        }
        if (new_bottom > Constants.IMG_HT) {
            detection.bottom = Constants.IMG_HT;
        } else {
            detection.bottom = new_bottom;
        }
        return detection;
    }

    @Override
    public void enableStatLogging(final boolean logStats) {
    }

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {
        //     hexagonDelegate.close();
    }

    public void setNumThreads(int num_threads) {
        if (tfLite != null) tfLite.setNumThreads(num_threads);
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) tfLite.setUseNNAPI(isChecked);
    }
}
