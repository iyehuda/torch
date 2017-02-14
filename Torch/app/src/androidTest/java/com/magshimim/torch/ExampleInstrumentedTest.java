package com.magshimim.torch;

import android.content.Context;
import android.media.projection.MediaProjection;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.magshimim.torch.recording.FrameRecorder;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.magshimim.torch", appContext.getPackageName());
    }

    @Test
    public void startRecording() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        // MainActivity mainActivity = (MainActivity) InstrumentationRegistry.getTargetContext().

        // assertEquals(false, mainActivity.setup);
        // MediaProjection mediaProjection =
        // FrameRecorder fr = new FrameRecorder()
    }
}
