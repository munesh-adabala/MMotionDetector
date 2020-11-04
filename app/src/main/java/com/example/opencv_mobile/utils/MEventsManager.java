package com.example.opencv_mobile.utils;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class MEventsManager {
    private static final String TAG = "MEventsManager";
    public static final int SELECTED_OPTION_TYPE = 101;

    public interface EventNotifier{
        public void onReceiveEvent(int type, Object object);
    }

    private static MEventsManager mgr;
    private ConcurrentHashMap<Integer, Vector<EventNotifier>> mapOfListeners=new ConcurrentHashMap<>();

    private MEventsManager(){

    }

    public static synchronized MEventsManager getInstance(){
        if(mgr==null){
            mgr=new MEventsManager();
        }
        return mgr;
    }

    public void addListener(int type,EventNotifier listener){
        if(listener==null){
            return;
        }
        Vector<EventNotifier> listenersList=mapOfListeners.get(type);
        if(listenersList==null){
            listenersList=new Vector<>();
            mapOfListeners.put(type,listenersList);
        }
        listenersList.add(listener);
    }

    public void removeListener(int type,EventNotifier listener){
        if(listener==null){
            return;
        }
        Vector<EventNotifier> listenersList = mapOfListeners.get(type);
        if(listenersList==null){
            return;
        }
        listenersList.remove(listener);
    }

    public void inject(int type,Object object){
        Vector<EventNotifier> listenersList = mapOfListeners.get(type);
        if(listenersList!=null){
            for(EventNotifier listener:listenersList){
                listener.onReceiveEvent(type,object);
            }
        }
    }
}
