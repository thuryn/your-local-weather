package org.thosp.yourlocalweather;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

public class ActivityTransitionTouchListener implements View.OnTouchListener {

    private static String TAG = "ActivityTransitionTouchListener";

    Class<?> destinationActivityOnLeftSide;
    Class<?> destinationActivityOnRightSide;
    Context context;

    private boolean start;
    private float downX;
    private float upX;
    private float downY;
    private float upY;

    public ActivityTransitionTouchListener(Class<?> destinationActivityOnLeftSide,
                                          Class<?> destinationActivityOnRightSide,
                                          Context context) {
        super();
        this.context = context;
        this.destinationActivityOnLeftSide = destinationActivityOnLeftSide;
        this.destinationActivityOnRightSide = destinationActivityOnRightSide;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                if (start) {
                    return false;
                }
                downX = event.getX();
                downY = event.getY();
                start = true;
                break;
            case MotionEvent.ACTION_UP:
                upX = event.getX();
                upY = event.getY();
                start = false;

                float deltaX = downX - upX;
                float deltaY = downY - upY;
                float absDeltaX = Math.abs(deltaX);

                if ((absDeltaX > Math.abs(deltaY)) && absDeltaX > 200) {
                    if (deltaX > 0){
                        swipeToRight();
                        return true;
                    } else {
                        swipeToLeft();
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    private void swipeToLeft() {
        if (destinationActivityOnLeftSide == null) {
            return;
        }
        Intent intentToMoveToActivity = new Intent(context, destinationActivityOnLeftSide);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(context, R.anim.anim_slide_in_right,
                    R.anim.anim_slide_out_right);
            context.startActivity(intentToMoveToActivity, options.toBundle());
        } else {
            context.startActivity(intentToMoveToActivity);
        }
    }

    private void swipeToRight() {
        if (destinationActivityOnRightSide == null) {
            return;
        }
        Intent intentToMoveToActivity = new Intent(context, destinationActivityOnRightSide);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(context, R.anim.anim_slide_in_left,
                    R.anim.anim_slide_out_left);
            context.startActivity(intentToMoveToActivity, options.toBundle());
        } else {
            context.startActivity(intentToMoveToActivity);
        }
    }
}
