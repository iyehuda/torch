package magshimim.torchmobile;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import magshimim.torchmobile.services.BackgroundService;
import magshimim.torchmobile.utils.Callback;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_MEDIA_PROJECTION = 1;
    private final static String TAG = MainActivity.class.getSimpleName();

    private boolean on = false;
    private BackgroundService backgroundService;
    private BackgroundService.BackgroundServiceBinder binder;
    private BackgroundServiceConnection serviceConnection;
    private Button btnConnect;
    private Callback<Void> startedCallback = new Callback<Void>() {
        @Override
        public void invoke(Void arg) {
            on = true;
            updateUI();
        }
    };
    private Callback<Void> stoppedCallback = new Callback<Void>() {
        @Override
        public void invoke(Void arg) {
            unbindService(serviceConnection);
            on = false;
            updateUI();
        }
    };
    private Callback<String> toastCallback = new Callback<String>() {
        @Override
        public void invoke(String arg) {
            Toast.makeText(getApplicationContext(), arg, Toast.LENGTH_LONG).show();
        }
    };
    private EditText txtAddress;
    private Intent resultData;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != REQUEST_MEDIA_PROJECTION)
            return;

        if(resultCode == RESULT_OK) {
            resultData = data;
            Intent intent = new Intent(this, BackgroundService.class);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceConnection = new BackgroundServiceConnection();
        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
        btnConnect = (Button)findViewById(R.id.btnConnect);
        txtAddress = (EditText)findViewById(R.id.txtAddress);
        refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(backgroundService != null)
            unregisterCallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(backgroundService != null)
            registerCallbacks();
    }

    private void refresh() {
        if(on) {
            btnConnect.setText("Disconnect");
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopBackgroundService();
                }
            });
            txtAddress.setEnabled(false);
        } else {
            btnConnect.setText("Connect");
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestRecordingPermission();
                }
            });
            txtAddress.setEnabled(true);
        }
    }

    private void registerCallbacks() {
        backgroundService.onStarted.registerCallback(startedCallback);
        backgroundService.onStopped.registerCallback(stoppedCallback);
        backgroundService.onToast.registerCallback(toastCallback);
    }

    private void requestRecordingPermission() {
        if(!on)
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void stopBackgroundService() {
        if(backgroundService != null && backgroundService.isWorking())
            try {
                backgroundService.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error while stopping background service", e);
            }
    }

    private void unregisterCallbacks() {
        backgroundService.onStarted.unregisterEvent(startedCallback);
        backgroundService.onStopped.unregisterEvent(stoppedCallback);
        backgroundService.onToast.unregisterEvent(toastCallback);
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
    }

    private class BackgroundServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (BackgroundService.BackgroundServiceBinder)service;
            backgroundService = binder.getService();
            registerCallbacks();
            backgroundService.start(txtAddress.getText().toString(), resultData);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unregisterCallbacks();
            backgroundService = null;
            binder = null;
        }
    }
}
