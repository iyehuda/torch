package com.magshimim.torch;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by yehud on 2/13/2017.
 */

@RunWith(AndroidJUnit4.class)
public class FrameRecorderTest {
    private static MainActivity mainActivity;

    public static void setMainActivity(MainActivity mainActivity) {
        FrameRecorderTest.mainActivity = mainActivity;
    }

    @Test
    public void useFrameRecorder() throws Exception {
        
    }
}
