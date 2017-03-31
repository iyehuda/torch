package com.magshimim.torch;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class FrameShower extends AppCompatActivity {
    private boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "FrameShower";
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_shower);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ImageView imageView =(ImageView) findViewById(R.id.imageView2);
        byte[] b = getIntent().getByteArrayExtra("frame");
        if (DEBUG)
        {
            Log.d(TAG,"Bitmap Size: " + b.length);
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        imageView.setImageBitmap(bitmap);

        if (DEBUG)
        {
            Log.d(TAG,"canvas has been created");
        }

    }

}
