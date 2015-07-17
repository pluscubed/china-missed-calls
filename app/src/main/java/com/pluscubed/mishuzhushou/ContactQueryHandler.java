package com.pluscubed.mishuzhushou;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.lang.ref.WeakReference;
import java.util.List;

public class ContactQueryHandler extends AsyncQueryHandler {
    private WeakReference<OnQueryCompleteCallback> mCallback;

    public ContactQueryHandler(ContentResolver cr, OnQueryCompleteCallback callback) {
        super(cr);
        mCallback = new WeakReference<>(callback);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (cookie != null) {
            List extras = (List) cookie;
            MissedCall missedCall = (MissedCall) extras.get(0);
            Context context = (Context) extras.get(2);

            String id = null;
            String lookupKey = null;
            String displayName = null;
            String thumbnailUri = null;
            if (cursor.moveToFirst()) {
                id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
                lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY));
                displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                thumbnailUri = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
            }

            boolean exists = id != null;
            if (exists) {
                missedCall.displayName = displayName;
                if (thumbnailUri != null) {
                    missedCall.thumbnail = Utils.loadContactPhotoThumbnail(thumbnailUri, context);
                }
                missedCall.lookupUri = ContactsContract.Contacts.getLookupUri(Long.parseLong(id), lookupKey);
            }

            missedCall.contactInitialized = true;

            cursor.close();

            if (mCallback != null)
                mCallback.get().onContactQueryComplete(extras);
        }
    }

    public interface OnQueryCompleteCallback {
        void onContactQueryComplete(List cookie);
    }
}
