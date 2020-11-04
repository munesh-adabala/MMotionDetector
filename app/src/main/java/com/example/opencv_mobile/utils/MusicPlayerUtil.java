package com.example.opencv_mobile.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import com.example.opencv_mobile.R;


/**
 *  Sample Music Player to play simple music.
 */
public class MusicPlayerUtil {
    private static MusicPlayerUtil instance;
    private SoundPool mPlayer;
    private int poolId;
    private int streamId;
    private MusicPlayerUtil(Context context){
        AudioAttributes audioAttributes= new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        mPlayer=new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        poolId=mPlayer.load(context, R.raw.alert,0);
    }

    public static synchronized MusicPlayerUtil getInstance(Context context){
        if(instance==null){
            instance=new MusicPlayerUtil(context);
        }
        return instance;
    }

    public void play(){
       streamId= mPlayer.play(poolId,0.99f,0.99f,0,-1,1);
    }

    public void stop(){
        mPlayer.stop(streamId);
    }

    public void release(){
        mPlayer.release();
        instance=null;
    }
}
