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

/**
 * Created by mabi on 2015. 12. 24..
 */
public class DoubleAnchorOverlay extends VideoTrackOverlay {
    private enum ACTION_TYPE {startAnchor, endAnchor, normal, idle}	// touch event action type

    // Overlay Components
    private Anchor mStartAnchor;
    private Anchor mEndAnchor;
    private Paint mDisablePaint;
    private Rect mDisableRectRight;
    private Rect mDisableRectLeft;

    // Event Listener
    private OnUpdateAnchorListener mOnUpdateAnchorListener;

    // Attributes
    private int mDefaultAnchorPosition;
    private boolean isVideoOpen;

    // Working Variables
    protected int currentPosition;			// current start position
    private int currentDuration;			// current duration position
    private ACTION_TYPE actionType;			// current touche event type
    protected float pastX;					// past position x of touch event

    // Constructors
    public DoubleAnchorOverlay(Context context) {
        super(context);

        mStartAnchor = new Anchor(context);
        mEndAnchor = new Anchor(context);
        mDefaultAnchorPosition = context.getResources().getDimensionPixelOffset(R.dimen.default_anchor_position);
        isVideoOpen = false;

        mDisablePaint = new Paint(Color.parseColor("#000000"));
        mDisablePaint.setAlpha(128);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);

        mDisableRectRight = new Rect((int) mEndAnchor.position, 0, width, height);
        mDisableRectLeft = new Rect(0, 0, (int) mStartAnchor.position, height);
    }

    @Override
    public void onSetVideo(int videoDuration, float millisecondsPerWidth) {
        super.onSetVideo(videoDuration, millisecondsPerWidth);

        currentPosition = 0;
        currentDuration = (int) (mDefaultAnchorPosition / mMillisecondsPerWidth);
        mStartAnchor.position = mDefaultAnchorPosition;
        mEndAnchor.position = mWidth - mDefaultAnchorPosition;
        mDisableRectRight.left = mDefaultAnchorPosition;
        mDisableRectLeft.right = mDefaultAnchorPosition;
        isVideoOpen = true;
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
                if(mStartAnchor.contains(event.getX())) {
                    actionType = ACTION_TYPE.startAnchor;
                } else if(mEndAnchor.contains(event.getX())) {
                    actionType = ACTION_TYPE.endAnchor;
                } else {
                    actionType = ACTION_TYPE.normal;
                }
            case MotionEvent.ACTION_MOVE:
                // do event process
                switch(actionType) {
                    case startAnchor:
                    case endAnchor:
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
    }

    private void updateAnchorPosition(VideoTrackView.Track track, float x) {
        if(actionType ==  ACTION_TYPE.startAnchor) {
            // check next position in boundary
            if(mStartAnchor.position + x < 0) {
                x = 0 - mStartAnchor.position;
            }

            if(mStartAnchor.position + x > track.right) {
                x = track.right - mStartAnchor.position;
            }

            mStartAnchor.position += x;
            mDisableRectLeft.right = (int) mStartAnchor.position;
        } else if(actionType == ACTION_TYPE.endAnchor) {
            // check next position in boundary
            if(mEndAnchor.position + x < 0) {
                x = 0 - mEndAnchor.position;
            }

            if(mEndAnchor.position + x > track.right) {
                x = track.right - mEndAnchor.position;
            }

            mEndAnchor.position += x;
            mDisableRectRight.left = (int) mEndAnchor.position;
        }
    }

    @Override
    public void drawOverlay(Canvas canvas) {
        if(isVideoOpen) {
            canvas.drawRect(mDisableRectLeft, mDisablePaint);
            canvas.drawRect(mDisableRectRight, mDisablePaint);
            mStartAnchor.draw(canvas);
            mEndAnchor.draw(canvas);
        }
    }

    // Track anchor class
    private class Anchor {

        // Components
        private Paint mAnchorPaint;

        // Attributes
        public float position;
        private int mAnchorWidth;
        private int mAnchorRound;
        private int mAnchorArea;

        public Anchor(Context context) {
            mAnchorPaint = new Paint();
            mAnchorPaint.setColor(Color.parseColor("#ffffff"));

            mAnchorWidth = context.getResources().getDimensionPixelOffset(R.dimen.anchor_width);
            mAnchorRound = context.getResources().getDimensionPixelOffset(R.dimen.anchor_round);
            mAnchorArea = context.getResources().getDimensionPixelOffset(R.dimen.anchor_area);
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
