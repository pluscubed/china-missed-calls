package com.pluscubed.mishuzhushou;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class MissedCall implements Parcelable {

    public static final Creator<MissedCall> CREATOR = new Creator<MissedCall>() {
        @Override
        public MissedCall createFromParcel(Parcel in) {
            return new MissedCall(in);
        }

        @Override
        public MissedCall[] newArray(int size) {
            return new MissedCall[size];
        }
    };
    public boolean contactInitialized;
    public Uri lookupUri;
    public String displayName;
    public Bitmap thumbnail;
    public String missedCallNumber;
    public long missedCallTime;
    public int carrier;

    public MissedCall() {
    }

    protected MissedCall(Parcel in) {
        lookupUri = in.readParcelable(Uri.class.getClassLoader());
        displayName = in.readString();
        thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        missedCallNumber = in.readString();
        missedCallTime = in.readLong();
        carrier = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(lookupUri, flags);
        dest.writeString(displayName);
        dest.writeParcelable(thumbnail, flags);
        dest.writeString(missedCallNumber);
        dest.writeLong(missedCallTime);
        dest.writeInt(carrier);
    }
}
