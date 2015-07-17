package com.pluscubed.mishuzhushou;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Fragment
 */
public class MainFragment extends ListFragment implements ContactQueryHandler.OnQueryCompleteCallback {

    private List<MissedCall> mMissedCalls;
    private ContactQueryHandler mContactQueryHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMissedCalls = new ArrayList<>();
        mContactQueryHandler = new ContactQueryHandler(getActivity().getContentResolver(), this);
        final LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new SmsLoaderCallbacks();
        getLoaderManager().initLoader(Utils.CHINA_UNICOM, null, loaderCallbacks);
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
        ((QuickContactBadge) v.findViewById(R.id.badge)).onClick(null);
    }

    private void addMissedCalls(Cursor cursor, final int carrier) {
        if (cursor.moveToFirst()) {
            if (carrier == Utils.CHINA_UNICOM) {
                //starting up
                mMissedCalls.clear();
            }
            for (int i = 0; i < cursor.getCount(); i++) {
                final Sms sms = new Sms();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sms.body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    int dateSentColumn = cursor.getColumnIndex(Telephony.Sms.DATE_SENT);
                    if (dateSentColumn != -1) {
                        sms.dateSent = Long.parseLong(cursor.getString(dateSentColumn));
                    } else {
                        sms.dateSent = Long.parseLong(cursor.getString(cursor.getColumnIndex(Telephony.Sms.DATE)));
                    }
                } else {
                    sms.body = cursor.getString(cursor.getColumnIndex("body"));
                    int dateSentColumn = cursor.getColumnIndex("date_sent");
                    if (dateSentColumn != -1) {
                        sms.dateSent = Long.parseLong(cursor.getString(dateSentColumn));
                    } else {
                        sms.dateSent = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                    }
                }

                final MissedCall missedCall = Utils.getMissedCall(carrier, sms);
                mMissedCalls.add(missedCall);

                cursor.moveToNext();
            }
        }
    }

    public void fillListItem(List objects) {
        MissedCall missedCall = (MissedCall) objects.get(0);
        View convertView = (View) objects.get(1);

        QuickContactBadge badge = (QuickContactBadge) convertView.findViewById(R.id.badge);
        RoundedImageView imageView = (RoundedImageView) convertView.findViewById(R.id.avatar);
        TextView primary = (TextView) convertView.findViewById(R.id.list_item_message_primary);
        TextView secondary1 = (TextView) convertView.findViewById(R.id.list_item_message_secondary1);
        TextView secondary2 = (TextView) convertView.findViewById(R.id.list_item_message_secondary2);
        TextView topRight = (TextView) convertView.findViewById(R.id.list_item_message_topright);
        RelativeLayout progressRelative = (RelativeLayout) convertView.findViewById(R.id.progress_bar_relative);
        RelativeLayout listItemRelative = (RelativeLayout) convertView.findViewById(R.id.list_item_relative);

        progressRelative.setVisibility(View.GONE);
        listItemRelative.setVisibility(View.VISIBLE);

        secondary2.setText(Utils.getDateString(missedCall.missedCallTime));
        switch (missedCall.carrier) {
            case Utils.CHINA_UNICOM:
                topRight.setText(getString(R.string.unicom));
                break;
            case Utils.CHINA_TELECOM:
                topRight.setText(getString(R.string.telecom));
                break;
            case Utils.CHINA_MOBILE:
                topRight.setText(getString(R.string.mobile));
                break;
        }
        secondary1.setText(Utils.formatPhoneNumber(missedCall.missedCallNumber));
        primary.setText(missedCall.displayName);

        if (missedCall.displayName != null) {
            //Found contact, change layout to 3 lines
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88, getResources().getDisplayMetrics());
            convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));

            secondary1.setVisibility(View.VISIBLE);

            //SET TEXT
            badge.assignContactUri(missedCall.lookupUri);
        } else {
            //Didn't find contact, 2 line layout
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height));

            secondary1.setVisibility(View.GONE);

            //SET TEXT
            primary.setText(Utils.formatPhoneNumber(missedCall.missedCallNumber));
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
            imageView.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_person_white_24dp));

            int color = Utils.getBackgroundColor(missedCall);
            imageView.mutateBackground(true);
            imageView.setBackground(new ColorDrawable(color));
        }
    }

    @Override
    public void onContactQueryComplete(List cookie) {
        fillListItem(cookie);
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

            RelativeLayout progressRelative = (RelativeLayout) convertView.findViewById(R.id.progress_bar_relative);
            RelativeLayout listItemRelative = (RelativeLayout) convertView.findViewById(R.id.list_item_relative);

            List<Object> objects = new ArrayList<>();
            objects.add(missedCall);
            objects.add(convertView);
            objects.add(getActivity());

            if (!missedCall.contactInitialized) {
                progressRelative.setVisibility(View.VISIBLE);
                listItemRelative.setVisibility(View.GONE);
                mContactQueryHandler.startQuery(
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

    private class SmsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String filter;
            switch (id) {
                case Utils.CHINA_UNICOM:
                    filter = "联通秘书在%为您接待了一位访客%";
                    break;
                case Utils.CHINA_TELECOM:
                    filter = "%在%呼叫过您的手机%";
                    break;
                case Utils.CHINA_MOBILE:
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
                case Utils.CHINA_UNICOM:
                    addMissedCalls(data, loader.getId());
                    getLoaderManager().initLoader(Utils.CHINA_TELECOM, null, this);
                    break;
                case Utils.CHINA_TELECOM:
                    addMissedCalls(data, loader.getId());
                    getLoaderManager().initLoader(Utils.CHINA_MOBILE, null, this);
                    break;
                case Utils.CHINA_MOBILE:
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

}
