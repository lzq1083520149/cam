package com.sctek.smartglasses.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaData implements Parcelable {

    public String url;
    public String name;
    public String mdownload = "null";//是否已经下载
   // public int progress = 0;//下载进度
   // public String isStartDownload = "N";//是否开始下载
   // public String isSucceed = "N";//是否下载成功

    public MediaData() {
    }

    ;

    public MediaData(Parcel in) {

        this.url = in.readString();
        this.name = in.readString();
        this.mdownload = in.readString();
//        this.progress = in.readInt();
//        this.isStartDownload = in.readString();
//        this.isSucceed = in.readString();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getMdownload() {
        return mdownload;
    }

    public void setMdownload(String mdownload) {
        this.mdownload = mdownload;
    }

//    public int getProgress() {
//        return progress;
//    }
//
//    public void setProgress(int progress) {
//        this.progress = progress;
//    }
//
//    public String getIsStartDownload() {
//        return isStartDownload;
//    }
//
//    public void setIsStartDownload(String isStartDownload) {
//        this.isStartDownload = isStartDownload;
//    }
//
//    public String getIsSucceed() {
//        return isSucceed;
//    }
//
//    public void setIsSucceed(String isSucceed) {
//        this.isSucceed = isSucceed;
//    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeString(url);
        dest.writeString(name);
        dest.writeString(mdownload);
//        dest.writeInt(progress);
//        dest.writeString(isStartDownload);
//        dest.writeString(isSucceed);

    }

    public static final Parcelable.Creator<MediaData> CREATOR = new Creator<MediaData>() {

        @Override
        public MediaData[] newArray(int size) {
            // TODO Auto-generated method stub
            return new MediaData[size];
        }

        @Override
        public MediaData createFromParcel(Parcel source) {
            // TODO Auto-generated method stub
            return new MediaData(source);
        }
    };

}
