/*
 * Android-VideoTrackView
 * https://github.com/crust87/Android-VideoTrackView
 *
 * Mabi
 * crust87@gmail.com
 * last modify 2015-12-15
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crust87.videocropper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.crust87.videotrackview.VideoTrackOverlay;
import com.crust87.videotrackview.VideoTrackView;

public class AnchorOverlay extends VideoTrackOverlay {
    private enum ACTION_TYPE {anchor, normal, idle}	// touch event action type

    // Overlay Components
    private Anchor mAnchor;
    private Paint mDisablePaint;
    private Rect mDisableRect;

    // Event Listener
    private OnUpdateAnchorListener mOnUpdateAnchorListener;

    // Attributes
    private int mDefaultAnchorPosition;
    private int mAnchorWidth;
    private int mAnchorRound;
    private int mAnchorArea;

    // Working Variables
    protected int currentPosition;			// current start position
    private int currentDuration;			// current duration position
    private ACTION_TYPE actionType;			// current touche event type
    protected float pastX;					// past position x of touch event

    // Constructors
    public AnchorOverlay(Context context) {
        super(context);

        mAnchor = new Anchor();
        mDefaultAnchorPosition = context.getResources().getDimensionPixelOffset(R.dimen.default_anchor_position);
        mAnchorWidth = context.getResources().getDimensionPixelOffset(R.dimen.anchor_width);
        mAnchorRound = context.getResources().getDimensionPixelOffset(R.dimen.anchor_round);
        mAnchorArea = context.getResources().getDimensionPixelOffset(R.dimen.anchor_area);

        mDisablePaint = new Paint(Color.parseColor("#000000"));
        mDisablePaint.setAlpha(128);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);

        mDisableRect = new Rect((int) mAnchor.position, 0, width, height);
    }

    @Override
    public void onSetVideo(int videoDuration, float millisecondsPerWidth) {
        super.onSetVideo(videoDuration, millisecondsPerWidth);

        currentPosition = 0;
        currentDuration = (int) (mDefaultAnchorPosition / mMillisecondsPerWidth);
        mAnchor.position = mDefaultAnchorPosition;
        mDisableRect.left = mDefaultAnchorPosition;
    }

    @Override
    public boolean onTrackTouchEvent(VideoTrackView.Track track, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(mOnUpdateAnchorListener != null) {
                    mOnUpdateAnchorListener.onUpdatePositionStart();
                }
                pastX = event.getX();

                // check event type
                if(mAnchor.contains(event.getX())) {
                    actionType = ACTION_TYPE.anchor;
                } else {
                    actionType = ACTION_TYPE.normal;
                }
            case MotionEvent.ACTION_MOVE:
                // do event process
                switch(actionType) {
                    case anchor:
                        updateAnchorPosition(track, event.getX() - pastX);
                        pastX = event.getX();
                        break;
                    case normal:
                        updateTrackPosition(track, (int) (event.getX() - pastX));
                        pastX = event.getX();
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mOnUpdateAnchorListener != null) {
                    mOnUpdateAnchorListener.onUpdatePositionEnd(currentPosition, currentDuration);
                }
                // action type to idle
                actionType = ACTION_TYPE.idle;
        }

        return true;
    }

    // update track position
    // int x: it's actually delta x
    private void updateTrackPosition(VideoTrackView.Track track, float x) {
        // check next position in boundary
        if(track.left + x > 0) {
            x = -track.left;
        }

        if(track.right + x < 0) {
            x = 0 - track.right;
        }

        track.left += x;
        track.right += x;

        currentPosition = (int) -(track.left / mMillisecondsPerWidth);
        if(x < 0) {
            int nextDuration = mVideoDuration - currentPosition;
            currentDuration = nextDuration > currentDuration ? currentDuration : nextDuration;
            mAnchor.position = (int) (currentDuration * mMillisecondsPerWidth);
            mDisableRect.left = (int) mAnchor.position;
        }

        if(mOnUpdateAnchorListener != null) {
            mOnUpdateAnchorListener.onUpdatePosition(currentPosition, currentDuration);
        }
    }

    private void updateAnchorPosition(VideoTrackView.Track track, float x) {
        // check next position in boundary
        if(mAnchor.position + x < 0) {
            x = 0 - mAnchor.position;
        }

        if(mAnchor.position + x > track.right) {
            x = track.right - mAnchor.position;
        }

        mAnchor.position += x;
        mDisableRect.left = (int) mAnchor.position;

        currentDuration = (int) (mAnchor.position / mMillisecondsPerWidth);
        if(mOnUpdateAnchorListener != null) {
            mOnUpdateAnchorListener.onUpdatePosition(currentPosition, currentDuration);
        }
    }

    @Override
    public void drawOverlay(Canvas canvas) {
        canvas.drawRect(mDisableRect, mDisablePaint);
        mAnchor.draw(canvas);
    }

    // Track anchor class
    private class Anchor {
        private Paint mAnchorPaint;
        public float position;

        public Anchor() {
            mAnchorPaint = new Paint();
            mAnchorPaint.setColor(Color.parseColor("#ffffff"));
        }

        public boolean contains(float x) {
            if(x > (position - mAnchorArea) && x < (position + mAnchorWidth + mAnchorArea)) {
                return true;
            } else {
                return false;
            }
        }

        public void draw(Canvas canvas) {
            canvas.drawRoundRect(new RectF(position, 0, position + mAnchorWidth, mHeight), mAnchorRound, mAnchorRound, mAnchorPaint);
        }
    }

    public void setOnUpdateAnchorListener(OnUpdateAnchorListener onUpdateAnchorListener) {
        mOnUpdateAnchorListener = onUpdateAnchorListener;
    }

    // video time position change listener
    public interface OnUpdateAnchorListener {
        void onUpdatePositionStart();
        void onUpdatePosition(int seek, int duration);
        void onUpdatePositionEnd(int seek, int duration);
    }
}
