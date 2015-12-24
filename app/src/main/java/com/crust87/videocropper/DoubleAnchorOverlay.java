package com.crust87.videocropper;

import android.content.Context;
import android.view.MotionEvent;

import com.crust87.videotrackview.VideoTrackOverlay;
import com.crust87.videotrackview.VideoTrackView;

/**
 * Created by mabi on 2015. 12. 24..
 */
public class DoubleAnchorOverlay extends VideoTrackOverlay {

    public DoubleAnchorOverlay(Context context) {
        super(context);
    }

    @Override
    public boolean onTrackTouchEvent(VideoTrackView.Track track, MotionEvent event) {
        return false;
    }
}
