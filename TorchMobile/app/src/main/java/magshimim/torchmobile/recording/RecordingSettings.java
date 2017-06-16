package magshimim.torchmobile.recording;

import android.media.projection.MediaProjection;
import android.view.Display;

public class RecordingSettings
{
    final int fps;
    final Display display;
    final MediaProjection mediaProjection;

    public RecordingSettings(int fps,
                             Display display,
                             MediaProjection mediaProjection) {
        if(fps <= 0 || display == null || mediaProjection == null)
            throw new IllegalArgumentException("Illegal arguments provided");

        this.display = display;
        this.fps = fps;
        this.mediaProjection = mediaProjection;
    }
}
