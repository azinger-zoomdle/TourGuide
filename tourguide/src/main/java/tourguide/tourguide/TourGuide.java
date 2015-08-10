package tourguide.tourguide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

/**
 * Created by tanjunrong on 2/10/15.
 *
 */
public class TourGuide {

    /**
     * This describes the allowable motion, for example if you want the users to learn about clicking, but want to stop them from swiping, then use ClickOnly
     */
    public enum MotionType {
        AllowAll, ClickOnly, SwipeOnly
    }

    private Activity mActivity;
    private FrameLayoutWithHole mFrameLayout;
    private int mHoleRadius = 0;

    private MotionType mMotionType;
    private Overlay mOverlay;
    private Pointer mPointer;

    private ToolTip mToolTip;
    private AnimationTool.Technique mTechnique;

    private View mToolTipViewGroup;
    private View mHighlightedView;
    private View mRootView;

    /* Static builder */
    public static TourGuide init(Activity activity, View rootView){
        return new TourGuide(activity, rootView);
    }

    /* Constructor */
    public TourGuide(Activity activity, View rootView){
        mActivity = activity;
        mRootView = rootView;
    }

    /**
     * Clean up the tutorial that is added to the activity
     */
     public void cleanUp(){
         mFrameLayout.cleanUp();
         if (mToolTipViewGroup!=null) {
             ((ViewGroup) mActivity.getWindow().getDecorView()).removeView(mToolTipViewGroup);
         }
    }

    private void setupView(){
//        TODO: throw exception if either mActivity, mDuration, mHighlightedView is null
        checking();
        final ViewTreeObserver viewTreeObserver = mHighlightedView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // make sure this only run once
                mHighlightedView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                /* Initialize a frame layout with a hole */
                mFrameLayout = new FrameLayoutWithHole(mActivity, mHighlightedView, mRootView, mMotionType, mOverlay, mHoleRadius);
                /* handle click disable */
                handleDisableClicking(mFrameLayout);

                /* setup floating action button */
                if (mPointer != null) {
                    FloatingActionButton fab = setupAndAddFABToFrameLayout(mFrameLayout);
                    new AnimationTool(mFrameLayout, mToolTip, mActivity, mTechnique).performAnimationOn(fab);
                }
                setupFrameLayout();
                /* setup tooltip view */
                setupToolTip(mFrameLayout);
            }
        });
    }
    private void checking(){
        // There is not check for tooltip because tooltip can be null, it means there no tooltip will be shown

    }

    private void handleDisableClicking(FrameLayoutWithHole frameLayoutWithHole){
        if (mOverlay != null && mOverlay.mDisableClick) {
            frameLayoutWithHole.setViewHole(mHighlightedView);
            frameLayoutWithHole.setSoundEffectsEnabled(false);
            frameLayoutWithHole.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("tourguide", "disable, do nothing");
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupToolTip(FrameLayoutWithHole frameLayoutWithHole){
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        if (mToolTip != null) {
            /* inflate and get views */
            ViewGroup parent = (ViewGroup) mActivity.getWindow().getDecorView();
            LayoutInflater layoutInflater = mActivity.getLayoutInflater();
            mToolTipViewGroup = layoutInflater.inflate(R.layout.tooltip, null);
            View toolTipContainer = mToolTipViewGroup.findViewById(R.id.toolTip_container);
            TextView toolTipTitleTV = (TextView) mToolTipViewGroup.findViewById(R.id.title);
            TextView toolTipDescriptionTV = (TextView) mToolTipViewGroup.findViewById(R.id.description);

            /* set tooltip attributes */
            toolTipContainer.setBackgroundColor(mToolTip.mBackgroundColor);
            toolTipTitleTV.setText(mToolTip.mTitle);
            toolTipDescriptionTV.setText(mToolTip.mDescription);

            mToolTipViewGroup.startAnimation(mToolTip.mEnterAnimation);

            /* add setShadow if it's turned on */
            if (mToolTip.mShadow) {
                mToolTipViewGroup.setBackground(mActivity.getDrawable(R.drawable.drop_shadow));
            }

            /* position and size calculation */
            int [] pos = new int[2];
            mHighlightedView.getLocationOnScreen(pos);
            int targetViewX = pos[0];
            final int targetViewY = pos[1];

            // get measured size of tooltip
            mToolTipViewGroup.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            int toolTipMeasuredWidth = mToolTipViewGroup.getMeasuredWidth();
            int toolTipMeasuredHeight = mToolTipViewGroup.getMeasuredHeight();

            Point resultPoint = new Point(); // this holds the final position of tooltip
            float density = mActivity.getResources().getDisplayMetrics().density;
            final float adjustment = 10 * density; //adjustment is that little overlapping area of tooltip and targeted button

            // calculate x position, based on gravity, tooltipMeasuredWidth, parent max width, x position of target view, adjustment
            if (toolTipMeasuredWidth > parent.getWidth()){
                resultPoint.x = getXForTooTip(mToolTip.mGravity, parent.getWidth(), targetViewX, adjustment);
            } else {
                resultPoint.x = getXForTooTip(mToolTip.mGravity, toolTipMeasuredWidth, targetViewX, adjustment);
            }

            resultPoint.y = getYForTooTip(mToolTip.mGravity, toolTipMeasuredHeight, targetViewY, adjustment);

            // add view to parent
//            ((ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content)).addView(mToolTipViewGroup, layoutParams);
            parent.addView(mToolTipViewGroup, layoutParams);

            // 1. width < screen check
            if (toolTipMeasuredWidth > parent.getWidth()){
                mToolTipViewGroup.getLayoutParams().width = parent.getWidth();
                toolTipMeasuredWidth = parent.getWidth();
            }
            // 2. x left boundary check
            if (resultPoint.x < 0){
                mToolTipViewGroup.getLayoutParams().width = toolTipMeasuredWidth + resultPoint.x; //since point.x is negative, use plus
                resultPoint.x = 0;
            }
            // 3. x right boundary check
            int tempRightX = resultPoint.x + toolTipMeasuredWidth;
            if ( tempRightX > parent.getWidth()){
                mToolTipViewGroup.getLayoutParams().width = parent.getWidth() - resultPoint.x; //since point.x is negative, use plus
            }

            // TODO: no boundary check for height yet, this is a unlikely case though
            // height boundary can be fixed by user changing the gravity to the other size, since there are plenty of space vertically compared to horizontally
            // this needs an viewTreeObserver, that's because TextView measurement of it's vertical height is not accurate (didn't take into account of multiple lines yet) before it's rendered
            // re-calculate height again once it's rendered
            final ViewTreeObserver viewTreeObserver = mToolTipViewGroup.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mToolTipViewGroup.getViewTreeObserver().removeGlobalOnLayoutListener(this);// make sure this only run once

                    int fixedY;
                    int toolTipHeightAfterLayouted = mToolTipViewGroup.getHeight();
                    fixedY = getYForTooTip(mToolTip.mGravity, toolTipHeightAfterLayouted, targetViewY, adjustment);
                    layoutParams.setMargins((int)mToolTipViewGroup.getX(),fixedY,0,0);
                }
            });

            // set the position using setMargins on the left and top
            layoutParams.setMargins(resultPoint.x, resultPoint.y, 0, 0);
        }

    }

    private int getXForTooTip(int gravity, int toolTipMeasuredWidth, int targetViewX, float adjustment){
        int x;
        if ((gravity & Gravity.START) == Gravity.START){
            x = targetViewX - toolTipMeasuredWidth + (int)adjustment;
        } else if ((gravity & Gravity.END) == Gravity.END) {
            x = targetViewX + mHighlightedView.getWidth() - (int)adjustment;
        } else {
            x = targetViewX + mHighlightedView.getWidth() / 2 - toolTipMeasuredWidth / 2;
        }
        return x;
    }

    private int getYForTooTip(int gravity, int toolTipMeasuredHeight, int targetViewY, float adjustment){
        int y;
        if ((gravity & Gravity.TOP) == Gravity.TOP) {

            if (((gravity & Gravity.START) == Gravity.START) || ((gravity & Gravity.END) == Gravity.END)) {
                y =  targetViewY - toolTipMeasuredHeight + (int)adjustment;
            } else {
                y =  targetViewY - toolTipMeasuredHeight - (int)adjustment;
            }
        } else { // this is center
            if (((gravity & Gravity.START) == Gravity.START) || ((gravity & Gravity.END) == Gravity.END)) {
                y =  targetViewY + mHighlightedView.getHeight() - (int) adjustment;
            } else {
                y =  targetViewY + mHighlightedView.getHeight() + (int) adjustment;
            }
        }
        return y;
    }

    private FloatingActionButton setupAndAddFABToFrameLayout(final FrameLayoutWithHole frameLayoutWithHole){
        // invisFab is invisible, and it's only used for getting the width and height
        final FloatingActionButton invisFab = new FloatingActionButton(mActivity);
        invisFab.setSize(FloatingActionButton.SIZE_MINI);
        invisFab.setVisibility(View.INVISIBLE);
        ((ViewGroup)mActivity.getWindow().getDecorView()).addView(invisFab);

        // fab is the real fab that is going to be added
        final FloatingActionButton fab = new FloatingActionButton(mActivity);
        fab.setBackgroundColor(Color.BLUE);
        fab.setSize(FloatingActionButton.SIZE_MINI);
        fab.setColorNormal(mPointer.mColor);
        fab.setStrokeVisible(false);
        fab.setClickable(false);

        // When invisFab is layouted, it's width and height can be used to calculate the correct position of fab
        final ViewTreeObserver viewTreeObserver = invisFab.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // make sure this only run once
                invisFab.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                frameLayoutWithHole.addView(fab, params);
                // measure size of image to be placed
                int x = mPointer.getXBasedOnGravity(invisFab.getWidth(), mRootView, mHighlightedView);
                int y = mPointer.getYBasedOnGravity(invisFab.getHeight(), mRootView, mHighlightedView);
                params.setMargins(x, y, 0, 0);
            }
        });
        return fab;
    }

    private void setupFrameLayout(){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ViewGroup contentArea = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        Log.d("SetupFrameLayout", "contentArea");
        int [] pos = new int[2];
        contentArea.getLocationOnScreen(pos);
        // frameLayoutWithHole's coordinates are calculated taking full screen height into account
        // but we're adding it to the content area only, so we need to offset it to the same Y value of contentArea
        layoutParams.setMargins(0, -pos[1], 0, 0);

        ((ViewGroup) mRootView).addView(mFrameLayout, layoutParams);
    }

    /**
     * Setter for the animation to be used
     * @param technique AnimationTool to be used
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide with(AnimationTool.Technique technique){
        mTechnique = technique;
        return this;
    }

    /**
     * Sets which motion type is motionType
     * @param motionType
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide motionType(MotionType motionType){
        mMotionType = motionType;
        return this;
    }

    /**
     * Sets the duration
     * @param view the view in which the tutorial button will be placed on top of
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide playOn(View view){
        mHighlightedView = view;
        Log.d("azi", "x, y : " + view.getX() + ", " + view.getY());
        setupView();
        return this;
    }

    public TourGuide setOverlay(Overlay overlay){
        mOverlay = overlay;
        return this;
    }

    /**
     * Set the toolTip
     * @param toolTip Box with text
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setToolTip(ToolTip toolTip) {
        mToolTip = toolTip;
        return this;
    }

    /**
     * Set the radius
     * @param radius Radius of the hole
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setHoleRadius(int radius) {
        if (radius > 0) {
            mHoleRadius = radius;
        }
        return this;
    }

    /**
     * Set the Pointer
     * @param pointer
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setPointer(Pointer pointer){
        mPointer = pointer;
        return this;
    }

}
