package com.pluscubed.mishuzhushou;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {


    public static final int CHINA_UNICOM = 0;
    public static final int CHINA_TELECOM = 1;
    public static final int CHINA_MOBILE = 2;

    public static Bitmap loadContactPhotoThumbnail(String photoData, Context context) {

        // Creates an asset file descriptor for the thumbnail file.
        AssetFileDescriptor afd = null;
        // try-catch block for file not found
        try {
            // Creates a holder for the URI.
            Uri thumbUri;
            thumbUri = Uri.parse(photoData);
        /*
         * Retrieves an AssetFileDescriptor object for the thumbnail
         * URI
         * using ContentResolver.openAssetFileDescriptor
         */
            afd = context.getContentResolver().openAssetFileDescriptor(thumbUri, "r");
        /*
         * Gets a file descriptor from the asset file descriptor.
         * This object can be used across processes.
         */
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            // Decode the photo file and return the result as a Bitmap
            // If the file descriptor is valid
            if (fileDescriptor != null) {
                // Decodes the bitmap
                return BitmapFactory.decodeFileDescriptor(
                        fileDescriptor, null, null);
            }
            // If the file isn't found
        } catch (FileNotFoundException e) {
            // In all cases, close the asset file descriptor
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    public static String getDateString(long milliSeconds) {
        DateFormat f = DateFormat.getDateTimeInstance();
        return f.format(new Date(milliSeconds));
    }

    @NonNull
    public static MissedCall getMissedCall(@Carrier int carrier, Sms sms) {
        List<String> integers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(sms.body);
        while (matcher.find()) {
            integers.add(matcher.group());
        }
        final MissedCall missedCall = new MissedCall();
        if (carrier == CHINA_UNICOM) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(sms.dateSent));
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(integers.get(0)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(integers.get(1)));
            missedCall.missedCallTime = calendar.getTimeInMillis();
            missedCall.missedCallNumber = integers.get(2);
        } else {
            missedCall.missedCallNumber = integers.get(0);
            Calendar calendar = Calendar.getInstance();
            //noinspection ResourceType
            calendar.set(calendar.get(Calendar.YEAR), Integer.parseInt(integers.get(1)) - 1, Integer.parseInt(integers.get(2)), Integer.parseInt(integers.get(3)), Integer.parseInt(integers.get(4)));
            missedCall.missedCallTime = calendar.getTimeInMillis();
        }
        missedCall.carrier = carrier;
        return missedCall;
    }

    public static int getBackgroundColor(MissedCall missedCall) {
        Random rand = new Random(Long.valueOf(missedCall.missedCallNumber));
        int r = (int) (rand.nextFloat() * 0.4F * 255);
        int g = (int) (rand.nextFloat() * 0.4F * 255);
        int b = (int) (rand.nextFloat() * 0.4F * 255);
        return Color.rgb(r, g, b);
    }

    public static String formatPhoneNumber(String number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PhoneNumberUtils.formatNumber(number, "CN");
        } else {
            return PhoneNumberUtils.formatNumber(number);
        }
    }

    @IntDef({CHINA_UNICOM, CHINA_TELECOM, CHINA_MOBILE})
    public @interface Carrier {
    }
}
