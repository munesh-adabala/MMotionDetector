package com.example.opencv_mobile.pojo;

public class OptionsData {
    int imgID;
    String optionsName;
    public OptionsData(int imgID, String optionsName){
        this.imgID=imgID;
        this.optionsName=optionsName;
    }

    public int getImgID() {
        return imgID;
    }

    public void setImgID(int imgID) {
        this.imgID = imgID;
    }

    public String getOptionsName() {
        return optionsName;
    }

    public void setOptionsName(String optionsName) {
        this.optionsName = optionsName;
    }
}
