package com.pluscubed.mishuzhushou;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity {


    public static final int REQUEST_PERMISSIONS_CODE = 42;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE: {
                // If request is cancelled, the result arrays are empty.
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        permissionsDenied();
                        return;
                    }
                }
                permissionsGranted();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!BuildConfig.DEBUG)
            Fabric.with(this, new Crashlytics());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(null, null, ContextCompat.getColor(this, R.color.primary_dark)));
        }

        final boolean contactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
        final boolean readSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED;
        final boolean receiveSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED;
        if (contactsPermission || readSmsPermission || receiveSmsPermission) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
                new MaterialDialog.Builder(this)
                        .title(R.string.permission_needed)
                        .content(R.string.permission_needed_desc)
                        .positiveText(android.R.string.ok)
                        .positiveColor(ContextCompat.getColor(this, R.color.primary))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                requestPermissions(contactsPermission, readSmsPermission, receiveSmsPermission);
                            }
                        })
                        .show();

            } else {
                requestPermissions(contactsPermission, readSmsPermission, receiveSmsPermission);
            }
        } else {
            permissionsGranted();
        }


    }

    private void requestPermissions(boolean contacts, boolean readSms, boolean receiveSms) {
        List<String> permissions = new ArrayList<>();
        if (contacts) {
            permissions.add(Manifest.permission.READ_CONTACTS);
        }
        if (readSms) {
            permissions.add(Manifest.permission.READ_SMS);
        }
        if (receiveSms) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }

        ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[permissions.size()]),
                REQUEST_PERMISSIONS_CODE);
    }

    private void permissionsDenied() {
        new MaterialDialog.Builder(this)
                .title(R.string.permission_needed)
                .content(getString(R.string.permission_needed_desc) + getString(R.string.permission_denied))
                .positiveText(android.R.string.ok)
                .positiveColor(ContextCompat.getColor(this, R.color.primary))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        finish();
                    }
                })
                .show();
    }

    private void permissionsGranted() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.activity_main_content_framelayout);
        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.activity_main_content_framelayout, new MainFragment())
                    .commit();
        }
    }


}
