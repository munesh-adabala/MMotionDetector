package com.example.opencv_mobile.motion_detection;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.opencv_mobile.BoundingBoxView;
import com.example.opencv_mobile.R;
import com.example.opencv_mobile.pojo.MotionDetectData;
import com.example.opencv_mobile.tflite.Classifier;
import com.example.opencv_mobile.tflite.FaceDetection;
import com.example.opencv_mobile.utils.Constants;
import com.example.opencv_mobile.utils.MusicPlayerUtil;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.util.List;
import java.util.Objects;

/**
 * Motion Detector where Camera frames are obtained and used to detect the motion. When motion gets detected based on the
 * user selected option music tone gets started playing.
 */
public class MotionDetector extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {
    private static final String TAG = "MotionDetector";
    Mat mat1;
    private final float extend_ratio_h = 0.1f;
    private final float extend_ratio_w = 0.1f;
    private int selectedOption = 0;
    private volatile boolean isImageSent = false;
    private CameraBridgeViewBase mCameraView;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    ;

    private FaceDetection faceDet;
    private MusicPlayerUtil musicPlayer;
    private boolean playing = false;
    private TextView mTextView;
    private Animation anim = new ScaleAnimation(0, 5, 0, 5, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    private View view;
    private boolean isSoundEnabled = true;
    private boolean isCloudEnabled = false;
    private MotionDetectionViewModel viewModel;
    private Handler handler;
    private static final int COUNT_DOWN_INTERVAL = 1000;
    private static final int COUNT_DOWN_TIME = 10 * 1000;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;

    private CountDownTimer timer = new CountDownTimer(COUNT_DOWN_TIME, COUNT_DOWN_INTERVAL) {
        @Override
        public void onTick(long millisUntilFinished) {
            if (millisUntilFinished != 0) {
                mTextView.setText(String.valueOf(millisUntilFinished / 1000));
                mTextView.startAnimation(anim);
            }
        }

        @Override
        public void onFinish() {
            mTextView.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            startDetecting = true;
        }
    };

    private Observer<MotionDetectData> motionObserver = new Observer<MotionDetectData>() {
        @Override
        public void onChanged(MotionDetectData motionDetectData) {
            if (startDetecting) {
                startDetectedSignal();
                if (!isImageSent) {
                    try {
                        //      sendPictureToCloud(motionDetectData.getMotionDetectedFrame());
                    } catch (Exception e) {
                        Log.e(TAG, "onCameraFrame: Exception while sending pic to the cloud");
                    }
                }
            }
        }
    };

    private boolean startDetecting = false;
    private BoundingBoxView boundingBoxView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.motin_detection_layout);
        handler = new Handler();
        init();
        initViews();
        anim.setDuration(1000);
        anim.setFillAfter(false);
        timer.start();
        viewModel = ViewModelProviders.of(this).get(MotionDetectionViewModel.class);
        viewModel.getLiveData().observe(this, motionObserver);
        handlerThread = new HandlerThread("FaceDetHandler");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        boundingBoxView = findViewById(R.id.bounding_box);
    }

    private void initViews() {
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        mCameraView = (JavaCameraView) findViewById(R.id.mCameraView);
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        mCameraView.setCameraIndex(selectedOption);
        mCameraView.setCvCameraViewListener(MotionDetector.this);
        mTextView = findViewById(R.id.text_view);
        view = findViewById(R.id.gray_layer);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Constants.IMG_WD = 1440;
            Constants.IMG_HT = 720;
        } else {
            Constants.IMG_WD = 864;
            Constants.IMG_HT = 480;
        }
    }

    private void init() {
        SharedPreferences sharedPreferences = getSharedPreferences("Cam_Pref", MODE_PRIVATE);
        selectedOption = sharedPreferences.getInt(Constants.CAM_PREF_KEY, 0);
        isSoundEnabled = sharedPreferences.getBoolean(Constants.SOUND_PREF_KEY, true);
        isCloudEnabled = sharedPreferences.getBoolean(Constants.CLOUD_PREF_KEY, false);
        if (isCloudEnabled) {
            Log.e(TAG, "init: Intializing the Fireabase");
          //  FirebaseApp app = FirebaseApp.initializeApp(this);
        }
        musicPlayer = MusicPlayerUtil.getInstance(this);
        try {
            faceDet = new FaceDetection(this);
        } catch (Exception e) {
            Log.e(TAG, "init: Exception while initilizing the facedetection model" + e.getMessage());
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        /*mat1 = new Mat(width, height, CvType.CV_8UC4);
        absoluteFaceSize = (int) (height * 0.3);*/
    }

    @Override
    public void onCameraViewStopped() {
        Log.e(TAG, "onCameraViewStopped: ");
        if (mat1 != null)
            mat1.release();
    }

    private void initializeOpenCVDependencies() {

/*        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }*/

        // And we are ready to go
        mCameraView.enableView();
    }


    /**
     * WE get the frames here and give to MotionDetectionViewModel to detect the motion.
     *
     * @param inputFrame
     * @return
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        viewModel.detectMotion(inputFrame);
        return inputFrame;
    }


    /**
     * This is mainly for face recognition purpose.
     * @param inputFrame
     */
    private void detectFace(Mat inputFrame) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = Bitmap.createBitmap(inputFrame.width(), inputFrame.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(inputFrame, bitmap);
                    List<Classifier.Recognition> list = faceDet.recognizeImage(bitmap);
                    boundingBoxView.setResults(list.get(0).getLocation());
                } catch (Exception e) {

                }
            }
        });

    }


    /**
     * When the motion gets detected following function will start playing the sound.
     */
    private void startDetectedSignal() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(stopSoundRunnable, 3000);
        if (isSoundEnabled && !playing) {
            playing = true;
            musicPlayer.play();
        }
    }

    /**
     * Stops the music after 3 secs.
     */
    private Runnable stopSoundRunnable = new Runnable() {
        @Override
        public void run() {
            playing = false;
            musicPlayer.stop();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.e(TAG, "onResume: Failed to load the Opencv");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MusicPlayerUtil.getInstance(this).stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        MusicPlayerUtil.getInstance(this).release();
        viewModel.getLiveData().removeObservers(this);
        backgroundHandler.removeCallbacksAndMessages(null);
        handlerThread.quitSafely();
    }


    /**
     * If we want to send to send the motion detected picture to the cloud following function will help us.
     * Note: As of now It is commented it can be used in case any usage
     * Please follow the below steps inorder to use:
     * 1)Uncomment the dependencies which is present in the build.gradle(app) at line(2,49,50,51).
     * 2)Add your google-service.json file to the app folder.
     *
     * @param inputFrame
     */

    /*public void sendPictureToCloud(Mat inputFrame) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "sendPictureToCloud: Sending image to the cloud");
                if (!isCloudEnabled) {
                    Log.e(TAG, "sendPictureToCloud: Cloud not enabled so not sending to server");
                    return;
                }
                isImageSent = true;
                Bitmap bitmap = Bitmap.createBitmap(inputFrame.width(), inputFrame.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(inputFrame, bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] data = baos.toByteArray();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
                String imgName = "Person-" + format.format(Calendar.getInstance().getTime());
                Log.e(TAG, "sendPictureToCloud: Img name==" + imgName);
                StorageReference storageReference = storage.getReference().child("Persons_Detected/" + imgName + ".jpg");
                UploadTask uploadTask = storageReference.putBytes(data);
                uploadTask.addOnSuccessListener(MotionDetector.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.e(TAG, "onSuccess: " + taskSnapshot.getMetadata().getPath());
                    }
                });
                bitmap.recycle();
                inputFrame.release();
            }
        });
    }*/
}
