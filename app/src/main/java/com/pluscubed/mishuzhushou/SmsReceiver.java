package com.pluscubed.mishuzhushou;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v7.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.widget.QuickContactBadge;

import java.util.ArrayList;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {

    public static final String SMS_RECEIVED_ACTION =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                    Telephony.Sms.Intents.SMS_RECEIVED_ACTION : "android.provider.Telephony.SMS_RECEIVED";

    public static final String NOTIFICATION_CLICKED_ACTION = "com.pluscubed.mishuzhushou.NOTIFICATION_CLICKED";
    public static final int NOTIFICATION_ID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                for (SmsMessage message : messages) {
                    showNotification(context, message);
                }
            }
        } else if (intent.getAction().equals(NOTIFICATION_CLICKED_ACTION)) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(NOTIFICATION_ID);

            MissedCall missedCall = intent.getParcelableExtra("missedCall");
            QuickContactBadge badge = new QuickContactBadge(context);
            if (missedCall.displayName != null) {
                badge.assignContactUri(missedCall.lookupUri);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Bundle extra = new Bundle();
                    extra.putString(ContactsContract.Intents.Insert.PHONE, missedCall.missedCallNumber);
                    badge.assignContactFromPhone(missedCall.missedCallNumber, true, extra);
                } else {
                    badge.assignContactFromPhone(missedCall.missedCallNumber, true);
                }
            }
            badge.onClick(null);
        }
    }

    private void showNotification(Context context, SmsMessage message) {

        Sms sms = new Sms();
        sms.body = message.getMessageBody();
        sms.dateSent = message.getTimestampMillis();

        int carrier = -1;
        if (message.getMessageBody().matches("联通秘书在.*为您接待了一位访客.*")) {
            carrier = Utils.CHINA_UNICOM;
        } else if (message.getMessageBody().matches(".*在.*呼叫过您的手机.*")) {
            carrier = Utils.CHINA_TELECOM;
        } else if (message.getMessageBody().matches("中国移动.*公司来电提醒.*于.*呼叫过您")) {
            carrier = Utils.CHINA_MOBILE;
        }

        if (carrier != -1) {
            MissedCall missedCall = Utils.getMissedCall(carrier, sms);


            List<Object> objects = new ArrayList<>();
            objects.add(missedCall);
            objects.add(null);
            objects.add(context);

            new ContactQueryHandler(context.getContentResolver(), new ContactQueryHandler.OnQueryCompleteCallback() {
                @Override
                public void onContactQueryComplete(List cookie) {
                    MissedCall missedCall = (MissedCall) cookie.get(0);
                    Context context = (Context) cookie.get(2);

                    Intent intent = new Intent(context, SmsReceiver.class);
                    intent.putExtra("missedCall", missedCall);
                    intent.setAction(NOTIFICATION_CLICKED_ACTION);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder notification = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_phone_missed_white_24dp)
                            .setWhen(missedCall.missedCallTime)
                            .setContentText(context.getString(R.string.missed_call) + Utils.getDateString(missedCall.missedCallTime))
                            .setContentIntent(pendingIntent);

                    if (missedCall.displayName != null) {
                        notification.setContentTitle(missedCall.displayName + "（" + Utils.formatPhoneNumber(missedCall.missedCallNumber) + "）")
                                .addPerson(missedCall.lookupUri.toString());
                        if (missedCall.thumbnail != null) {
                            int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
                            int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
                            notification.setLargeIcon(Utils.getCircleBitmap(Bitmap.createScaledBitmap(missedCall.thumbnail, width, height, false)));
                        }
                    } else {
                        notification.setContentTitle(Utils.formatPhoneNumber(missedCall.missedCallNumber));
                    }
                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(NOTIFICATION_ID, notification.build());
                }
            }).startQuery(
                    0,
                    objects,
                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(missedCall.missedCallNumber)),
                    new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY, ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI},
                    null,
                    null,
                    null);
        }
    }
}
