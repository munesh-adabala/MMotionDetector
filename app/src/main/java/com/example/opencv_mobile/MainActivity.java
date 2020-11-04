package com.example.opencv_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv_mobile.adapters.OptionsAdapter;
import com.example.opencv_mobile.motion_detection.MotionDetector;
import com.example.opencv_mobile.pojo.OptionsData;
import com.example.opencv_mobile.utils.Constants;
import com.example.opencv_mobile.utils.MEventsManager;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;

import static com.example.opencv_mobile.utils.Constants.BACK_CAM_ID;
import static com.example.opencv_mobile.utils.Constants.FRONT_CAM_ID;

public class MainActivity extends AppCompatActivity implements MEventsManager.EventNotifier {
    private static final String TAG = "MainActivity";
    private OptionsAdapter adapter;
    private static final int STATUS_CODE = 1;
    private SharedPreferences sharedPreferences;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.e(TAG, "static initializer: Opencv successfully initialized");
        } else {
            Log.e(TAG, "static initializer: Opencv failed ");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (sharedPreferences.getInt(Constants.CAM_PREF_KEY, 0) == BACK_CAM_ID) {
            menu.add(0, FRONT_CAM_ID, Menu.NONE, R.string.front_cam);
        } else {
            menu.add(0, BACK_CAM_ID, Menu.NONE, R.string.back_cam);
        }

        if (sharedPreferences.getBoolean(Constants.SOUND_PREF_KEY, true)) {
            menu.add(0, R.string.disable_sound, Menu.NONE, R.string.disable_sound);
        } else {
            menu.add(0, R.string.enable_sound, Menu.NONE, R.string.enable_sound);
        }
        if (sharedPreferences.getBoolean(Constants.CLOUD_PREF_KEY, false)) {
            menu.add(0, R.string.disable_cloud, Menu.NONE, R.string.disable_cloud);
        } else {
            menu.add(0, R.string.enable_cloud, Menu.NONE, R.string.enable_cloud);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (item.getItemId()) {
            case BACK_CAM_ID:
                editor.putInt(Constants.CAM_PREF_KEY, BACK_CAM_ID);
                editor.apply();
                break;
            case FRONT_CAM_ID:
                editor.putInt(Constants.CAM_PREF_KEY, FRONT_CAM_ID);
                editor.apply();
                break;
            case R.string.disable_sound:
                editor.putBoolean(Constants.SOUND_PREF_KEY, false);
                editor.apply();
                break;
            case R.string.enable_sound:
                editor.putBoolean(Constants.SOUND_PREF_KEY, true);
                editor.apply();
                break;
            case R.string.disable_cloud:
                editor.putBoolean(Constants.CLOUD_PREF_KEY, false);
                editor.apply();
                break;
            case R.string.enable_cloud:
                editor.putBoolean(Constants.CLOUD_PREF_KEY, true);
                editor.apply();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("Cam_Pref", MODE_PRIVATE);
        initViews();
        checkPermission();
        adapter.setData(prepareData());
    }

    /**
     * Requesting the Camera access permission
     */
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    STATUS_CODE);
        }
    }

    private void initViews() {
        RecyclerView optionsList = findViewById(R.id.options_list_view);
        GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 2, RecyclerView.VERTICAL, false);
        optionsList.setLayoutManager(layoutManager);
        adapter = new OptionsAdapter(getApplicationContext());
        optionsList.setAdapter(adapter);
    }

    private ArrayList<OptionsData> prepareData() {
        ArrayList<OptionsData> list = new ArrayList<>();
        list.add(new OptionsData(R.drawable.motion_detect_icon, "Motion Detection"));
  //      list.add(new OptionsData(R.drawable.persons_count_icon, "Person Counter"));
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        MEventsManager.getInstance().addListener(MEventsManager.SELECTED_OPTION_TYPE, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MEventsManager.getInstance().removeListener(MEventsManager.SELECTED_OPTION_TYPE, this);
    }

    @Override
    public void onReceiveEvent(int type, Object object) {
        Log.e(TAG, "onReceiveEvent: Type==" + type);
        switch (type) {
            case MEventsManager.SELECTED_OPTION_TYPE:
                String selected = (String) object;
                switch (selected) {
                    case Constants.MOTION_DETECTOR:
                        Intent intent = new Intent(this, MotionDetector.class);
                        startActivity(intent);
                        break;
                   /* case Constants.PERSONS_COUNT:
                        Intent face_detect = new Intent(this, FaceDetectionSample.class);
                        startActivity(face_detect);
                        break;*/
                }
        }
    }
}
