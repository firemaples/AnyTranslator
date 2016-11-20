package tw.firemaples.onscreenocr;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import tw.firemaples.onscreenocr.screenshot.ScreenshotHandler;
import tw.firemaples.onscreenocr.utils.Callback;
import tw.firemaples.onscreenocr.utils.Tool;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM = 101;
    private final int REQUEST_CODE_REQUEST_EXTERNAL_STORAGE_READ_WRITE = 102;
    private final int REQUEST_CODE_REQUEST_MEDIA_PROJECTION_RESULT = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        findViewById(R.id.bt_serviceToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OnScreenTranslateService.isRunning(MainActivity.this)) {
                    stopService();
                } else {
                    startApp();
                }
            }
        });

        //noinspection ConstantConditions
        findViewById(R.id.bt_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.start(MainActivity.this, null);
            }
        });

        startApp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM) {
            onCheckDrawOverlayPermissionResult();
        } else if (requestCode == REQUEST_CODE_REQUEST_EXTERNAL_STORAGE_READ_WRITE) {
            onRequestExternalStorageReadWritePermissionResult();
        } else if (requestCode == REQUEST_CODE_REQUEST_MEDIA_PROJECTION_RESULT) {
            onRequestMediaProjectionResult(resultCode, data);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        if (!Settings.canDrawOverlays(MainActivity.this)) {
            Tool.logInfo("Requesting draw overlay permission");
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle("Need permission");
            ab.setMessage("This app needs [DrawOverlay] Permission for running!");
            ab.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_CHECK_DRAW_OVERLAY_PERM);
                }
            });
            ab.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            });
            ab.show();
        } else {
            Tool.logInfo("Has draw overlay permission");
            checkExternalStorageReadWritePermission();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void onCheckDrawOverlayPermissionResult() {
        if (Settings.canDrawOverlays(this)) {
            Tool.logInfo("Got draw overlay permission");
            checkExternalStorageReadWritePermission();
        } else {
            Tool.logInfo("Not get draw overlay permission, show error dialog");
            showErrorDialog("This app needs [DrawOverlay] Permission for running!", new Callback<Void>() {
                @Override
                public boolean onCallback(Void result) {
                    Tool.logInfo("Retry to get draw overlay permission");
                    checkDrawOverlayPermission();
                    return false;
                }
            });
        }
    }

    private void checkExternalStorageReadWritePermission() {
        if (!hasExternalStorageReadWritePermission()) {
            Tool.logInfo("Requesting read/write external storage permission");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_REQUEST_EXTERNAL_STORAGE_READ_WRITE
            );
        } else {
            Tool.logInfo("Has read/write external storage permission");
            requestMediaProjection();
        }
    }

    private boolean hasExternalStorageReadWritePermission() {
        int writePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void onRequestExternalStorageReadWritePermissionResult() {
        if (hasExternalStorageReadWritePermission()) {
            Tool.logInfo("Got read/write external storage permission");
            requestMediaProjection();
        } else {
            Tool.logInfo("Not get read/write external storage permission");
            showErrorDialog("This app needs [Read/Write External Storage] Permission for running!", new Callback<Void>() {
                @Override
                public boolean onCallback(Void result) {
                    Tool.logInfo("Retry to get read/write external storage permission");
                    checkExternalStorageReadWritePermission();
                    return false;
                }
            });
        }
    }

    private void requestMediaProjection() {
        Tool.logInfo("Requesting for media projection");
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_REQUEST_MEDIA_PROJECTION_RESULT);
    }

    private void onRequestMediaProjectionResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Tool.logInfo("Got media projection");
            ScreenshotHandler.init(this).setMediaProjectionIntent(data);
            startService();
        } else {
            Tool.logInfo("Not get media projection");
            showErrorDialog("Please submit Screenshot Permission for using this service!", new Callback<Void>() {
                @Override
                public boolean onCallback(Void result) {
                    Tool.logInfo("Retry to get media projection");
                    requestMediaProjection();
                    return false;
                }
            });
        }
    }


    private void showErrorDialog(String msg, final Callback<Void> onRetryCallback) {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Error");
        ab.setMessage(msg);
        ab.setPositiveButton("Request Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onRetryCallback.onCallback(null);
            }
        });
        ab.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.finish();
            }
        });
        ab.show();
    }

    private void startApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDrawOverlayPermission();
        } else {
            requestMediaProjection();
        }
    }

    private void startService() {
//        OnScreenTranslateService.start(this);
        ScreenTranslatorService.start(this);
        finish();
    }

    private void stopService() {
//        OnScreenTranslateService.stop();
        ScreenTranslatorService.stop();
    }
}
