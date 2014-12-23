package com.pluscubed.mishuzhushou;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main Fragment
 */
public class MainFragment extends ListFragment {
    public static final int CHINA_UNICOM = 0;
    public static final int CHINA_TELECOM = 1;
    public static final int CHINA_MOBILE = 2;
    private List<MissedCall> mMissedCalls;
    private QueryHandler mQueryHandler;


    private String getDate(long milliSeconds) {
        DateFormat f = android.text.format.DateFormat.getDateFormat(getActivity());
        return f.format(new Date(milliSeconds));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMissedCalls = new ArrayList<>();
        mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        final LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new CursorLoaderCallbacks();
        getLoaderManager().initLoader(CHINA_UNICOM, null, loaderCallbacks);
        setListAdapter(new ContactsMissedCallAdapter());
        setRetainInstance(true);
    }


    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText("加载中。。。");
        getListView().setDividerHeight(0);
    }

    public void updateList() {
        ((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((QuickContactBadge) v.findViewById(R.id.badge)).onClick(v);
    }

    private void addMissedCalls(Cursor cursor, final int carrier) {
        if (cursor.moveToFirst()) {
            if (carrier == CHINA_UNICOM) {
                //starting up
                mMissedCalls.clear();
            }
            for (int i = 0; i < cursor.getCount(); i++) {
                final Sms sms = new Sms();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sms.address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    sms.body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    sms.dateSent = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT));
                    if (sms.dateSent.equals("0")) {
                        sms.dateSent = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    }
                } else {
                    sms.address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    sms.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    sms.dateSent = cursor.getString(cursor.getColumnIndexOrThrow("date_sent"));
                    if (sms.dateSent.equals("0")) {
                        sms.dateSent = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    }
                }

                List<String> integers = new ArrayList<>();
                Matcher matcher = Pattern.compile("\\d+").matcher(sms.body);
                while (matcher.find()) {
                    integers.add(matcher.group());
                }
                final MissedCall missedCall = new MissedCall();
                if (carrier == CHINA_UNICOM) {
                    missedCall.missedCallTime = getDate(Long.parseLong(sms.dateSent)) + " " + integers.get(0) + ":" + integers.get(1);
                    missedCall.missedCallNumber = integers.get(2);
                } else {
                    missedCall.missedCallNumber = integers.get(0);
                    Calendar calendar = Calendar.getInstance();
                    //noinspection ResourceType
                    calendar.set(calendar.get(Calendar.YEAR), Integer.parseInt(integers.get(1)) - 1, Integer.parseInt(integers.get(2)), Integer.parseInt(integers.get(3)), Integer.parseInt(integers.get(4)));
                    DateFormat f = android.text.format.DateFormat.getLongDateFormat(getActivity());
                    missedCall.missedCallTime = f.format(calendar.getTime());
                }
                missedCall.carrier = carrier;
                mMissedCalls.add(missedCall);


                cursor.moveToNext();
            }
        }
    }

    private void fillListItem(List<Object> objects) {
        QuickContactBadge badge = (QuickContactBadge) objects.get(0);
        ImageView imageView = (ImageView) objects.get(1);
        TextView primary = (TextView) objects.get(2);
        TextView secondary1 = (TextView) objects.get(3);
        TextView secondary2 = (TextView) objects.get(4);
        MissedCall missedCall = (MissedCall) objects.get(5);
        TextView topRight = (TextView) objects.get(6);
        View view = (View) objects.get(7);
        RelativeLayout progressRelative = (RelativeLayout) objects.get(8);
        RelativeLayout listItemRelative = (RelativeLayout) objects.get(9);

        progressRelative.setVisibility(View.GONE);
        listItemRelative.setVisibility(View.VISIBLE);

        secondary2.setText(missedCall.missedCallTime);
        switch (missedCall.carrier) {
            case CHINA_UNICOM:
                topRight.setText(getString(R.string.unicom));
                break;
            case CHINA_TELECOM:
                topRight.setText(getString(R.string.telecom));
                break;
            case CHINA_MOBILE:
                topRight.setText(getString(R.string.mobile));
                break;
        }
        secondary1.setText(missedCall.missedCallNumber);
        primary.setText(missedCall.displayName);

        if (missedCall.displayName != null) {
            //Found contact, change layout to 3 lines
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88, getResources().getDisplayMetrics());
            view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));

            secondary1.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams secondary2Params = (RelativeLayout.LayoutParams) secondary2.getLayoutParams();
            //Remove below whatever rule
            secondary2Params.addRule(RelativeLayout.BELOW, secondary1.getId());
            secondary2.setLayoutParams(secondary2Params);

            //SET TEXT
            badge.assignContactUri(missedCall.lookupUri);
        } else {
            //Didn't find contact, 2 line layout
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));

            secondary1.setVisibility(View.GONE);

            RelativeLayout.LayoutParams secondary2Params = (RelativeLayout.LayoutParams) secondary2.getLayoutParams();
            secondary2Params.addRule(RelativeLayout.BELOW, primary.getId());
            secondary2.setLayoutParams(secondary2Params);

            //SET TEXT
            primary.setText(missedCall.missedCallNumber);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Bundle extra = new Bundle();
                extra.putString(ContactsContract.Intents.Insert.PHONE, missedCall.missedCallNumber);
                badge.assignContactFromPhone(missedCall.missedCallNumber, true, extra);
            } else {
                badge.assignContactFromPhone(missedCall.missedCallNumber, true);
            }
        }

        if (missedCall.thumbnail != null) {
            imageView.setImageBitmap(missedCall.thumbnail);
        } else {
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_picture));
        }
    }

    public class Sms {
        public String address;
        public String body;
        public String dateSent;
    }

    public class MissedCall {

        public boolean contactInitialized;

        public Uri lookupUri;
        public String displayName;
        public Bitmap thumbnail;

        public String missedCallNumber;
        public String missedCallTime;
        public int carrier;
    }

    public class ContactsMissedCallAdapter extends ArrayAdapter<MissedCall> {

        public ContactsMissedCallAdapter() {
            super(getActivity(), 0, mMissedCalls);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_message_3lines, parent, false);
            }

            MissedCall missedCall = getItem(position);

            QuickContactBadge badge = (QuickContactBadge) convertView.findViewById(R.id.badge);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.avatar);
            TextView primary = (TextView) convertView.findViewById(R.id.list_item_message_primary);
            TextView secondary1 = (TextView) convertView.findViewById(R.id.list_item_message_secondary1);
            TextView secondary2 = (TextView) convertView.findViewById(R.id.list_item_message_secondary2);
            TextView topRight = (TextView) convertView.findViewById(R.id.list_item_message_topright);
            RelativeLayout progressRelative = (RelativeLayout) convertView.findViewById(R.id.progress_bar_relative);
            RelativeLayout listItemRelative = (RelativeLayout) convertView.findViewById(R.id.list_item_relative);

            List<Object> objects = new ArrayList<>();
            objects.add(badge);
            objects.add(imageView);
            objects.add(primary);
            objects.add(secondary1);
            objects.add(secondary2);
            objects.add(missedCall);
            objects.add(topRight);
            objects.add(convertView);
            objects.add(progressRelative);
            objects.add(listItemRelative);

            if (!missedCall.contactInitialized) {
                progressRelative.setVisibility(View.VISIBLE);
                listItemRelative.setVisibility(View.GONE);
                mQueryHandler.startQuery(
                        0,
                        objects,
                        Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(missedCall.missedCallNumber)),
                        new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY, ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI},
                        null,
                        null,
                        null
                );
            } else {
                fillListItem(objects);
            }


            return convertView;
        }
    }

    private class CursorLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String filter;
            switch (id) {
                case CHINA_UNICOM:
                    filter = "联通秘书在%为您接待了一位访客%";
                    break;
                case CHINA_TELECOM:
                    filter = "%在%呼叫过您的手机%";
                    break;
                case CHINA_MOBILE:
                    filter = "中国移动%公司来电提醒%于%呼叫过您";
                    break;
                default:
                    return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Returns a new CursorLoader
                return new CursorLoader(
                        getActivity(),   // Parent activity context
                        Telephony.Sms.Inbox.CONTENT_URI,        // Table to query
                        null,     // Projection to return
                        Telephony.Sms.Inbox.BODY + " LIKE ?",
                        new String[]{filter},
                        "date_sent DESC"        // Default sort order
                );
            } else {
                // Returns a new CursorLoader
                return new CursorLoader(
                        getActivity(),   // Parent activity context
                        Uri.parse("content://sms/inbox"),        // Table to query
                        null,     // Projection to return
                        "body LIKE ?",
                        new String[]{filter},
                        "date_sent DESC"           // Default sort order
                );
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch (loader.getId()) {
                case CHINA_UNICOM:
                    addMissedCalls(data, loader.getId());
                    getLoaderManager().initLoader(CHINA_TELECOM, null, this);
                    break;
                case CHINA_TELECOM:
                    addMissedCalls(data, loader.getId());
                    getLoaderManager().initLoader(CHINA_MOBILE, null, this);
                    break;
                case CHINA_MOBILE:
                    addMissedCalls(data, loader.getId());
                    if (mMissedCalls.size() == 0) {
                        setEmptyText("无漏电短信");
                    }
                    break;
                default:
            }
            updateList();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    public class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cookie != null) {
                List<Object> extras = (ArrayList<Object>) cookie;
                MissedCall missedCall = (MissedCall) extras.get(5);

                String id = null;
                String lookupKey = null;
                String displayName = null;
                String thumbnailUri = null;
                if (cursor.moveToFirst()) {
                    id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
                    lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY));
                    displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    thumbnailUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
                }

                boolean exists = id != null;
                if (exists) {
                    missedCall.displayName = displayName;
                    if (thumbnailUri != null) {
                        missedCall.thumbnail = Utils.loadContactPhotoThumbnail(thumbnailUri, getActivity());
                    }
                    missedCall.lookupUri = ContactsContract.Contacts.getLookupUri(Long.parseLong(id), lookupKey);
                }

                missedCall.contactInitialized = true;

                cursor.close();

                fillListItem(extras);
            }
        }
    }
}
