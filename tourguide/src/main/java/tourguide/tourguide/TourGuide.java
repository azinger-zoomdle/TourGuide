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

    public boolean mFrameLayoutWithHoleCreated;
    public boolean mToolTipViewGroupCreated;

    private View mToolTipViewGroup;
    private View mHighlightedView;
    private View mRootView;

    private TourGuide tourGuideContext;

    private ViewTreeObserver.OnGlobalLayoutListener mHighlightedViewGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // make sure this only run once
            mHighlightedView.getViewTreeObserver().removeOnGlobalLayoutListener(mHighlightedViewGlobalLayoutListener);
            if (!mFrameLayoutWithHoleCreated) {
                // make really sure this only run once
                mFrameLayoutWithHoleCreated = true;
                // Clean variables before
                cleanUp();

                    /* Initialize a frame layout with a hole */
                mFrameLayout = new FrameLayoutWithHole(mActivity, tourGuideContext, mMotionType, mOverlay);
                    /* handle click disable */
                handleDisableClicking(mFrameLayout);

                    /* setup floating action button */
                if (mPointer != null) {
                    FloatingActionButton fab = setupAndAddFABToFrameLayout(mFrameLayout);
                    new AnimationTool(mFrameLayout, mToolTip, mActivity, mTechnique).performAnimationOn(fab);
                }
                setupFrameLayout();
                    /* setup tooltip view */
                setupToolTip();
            }
        }
    };

    private int targetViewY;
    private float adjustment;
    private FrameLayout.LayoutParams layoutParams;
    private ViewTreeObserver.OnGlobalLayoutListener mToolTipViewGroupGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // make sure this only run once
            mToolTipViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(mToolTipViewGroupGlobalLayoutListener);
            // make really sure this only run once
            if (!mToolTipViewGroupCreated) {
                mToolTipViewGroupCreated = true;
                int fixedY;
                int toolTipHeightAfterLayouted = mToolTipViewGroup.getHeight();
                fixedY = getYForTooTip(mToolTip.mGravity, toolTipHeightAfterLayouted, targetViewY, adjustment);
                layoutParams.setMargins((int)mToolTipViewGroup.getX(),fixedY,0,0);
            }
        }
    };

    private FloatingActionButton invisFab;
    private FrameLayoutWithHole mFrameLayoutWithHoleForListener;
    private FloatingActionButton mFab;
    private ViewTreeObserver.OnGlobalLayoutListener invisFabGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // make sure this only run once
            invisFab.getViewTreeObserver().removeOnGlobalLayoutListener(invisFabGlobalLayoutListener);
            final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mFrameLayoutWithHoleForListener.addView(mFab, params);
            // measure size of image to be placed
            params.setMargins(mPointer.getX(mHighlightedView), mPointer.getY(mHighlightedView), 0, 0);
        }
    };

    private static TourGuide singleton = null;

    /* Static builder */
    public static TourGuide init(Activity activity, View rootView) {
        if (singleton != null) {
            singleton.cleanUp();
            singleton.mActivity = activity;
            singleton.mRootView = rootView;
        } else {
            singleton = new TourGuide(activity, rootView);
        }
        return singleton;
    }

    /* Constructor */
    public TourGuide(Activity activity, View rootView) {
        mActivity = activity;
        mRootView = rootView;
        tourGuideContext = this;
    }

    /**
     * Sets the duration
     * @param view the view in which the tutorial button will be placed on top of
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide playOn(View view) {
        mHighlightedView = view;
        try {
            setupView();
        } catch (NullPointerException e) {
            Log.e("NullPointerException", "SetupView can't be loaded");
        }
        return this;
    }

    private void setupView() {
        if (mActivity == null || mHighlightedView == null || mRootView == null) {
            throw new NullPointerException();
        }
        this.mFrameLayoutWithHoleCreated = false;
        this.mToolTipViewGroupCreated = false;
        mHighlightedView.getViewTreeObserver().addOnGlobalLayoutListener(mHighlightedViewGlobalLayoutListener);
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
    private void setupToolTip() {
        layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        if (mToolTip != null) {
            /* inflate and get views */

            LayoutInflater layoutInflater = mActivity.getLayoutInflater();
            mToolTipViewGroup = layoutInflater.inflate(R.layout.tooltip, null);
            View toolTipContainer = mToolTipViewGroup.findViewById(R.id.toolTip_container);
            TextView toolTipTitleTV = (TextView) mToolTipViewGroup.findViewById(R.id.title);
            TextView toolTipDescriptionTV = (TextView) mToolTipViewGroup.findViewById(R.id.description);
            Point resultPoint = this.setupToolTipSizeAndPositionCalculation();
            /* set tooltip attributes */
            toolTipContainer.setBackgroundColor(mToolTip.mBackgroundColor);
            toolTipTitleTV.setText(mToolTip.mTitle);
            toolTipDescriptionTV.setText(mToolTip.mDescription);

            mToolTipViewGroup.startAnimation(mToolTip.mEnterAnimation);

            mToolTipViewGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tourGuideContext.cleanUp();
                }
            });

            /* add setShadow if it's turned on */
            if (mToolTip.mShadow) {
                mToolTipViewGroup.setBackground(mActivity.getDrawable(R.drawable.drop_shadow));
            }

            final ViewTreeObserver viewTreeObserver = mToolTipViewGroup.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(mToolTipViewGroupGlobalLayoutListener);
            // set the position using setMargins on the left and top
            layoutParams.setMargins(resultPoint.x, resultPoint.y, 0, 0);
        }

    }

    /**
     * Setup tooltip size and return position
     * @return position of tooltip group view
     */
    private Point setupToolTipSizeAndPositionCalculation() {
        ViewGroup parent = (ViewGroup) mActivity.getWindow().getDecorView();
        int [] pos = new int[2];
        mHighlightedView.getLocationOnScreen(pos);
        int targetViewX = pos[0];
        targetViewY = pos[1];
        // get measured size of tooltip
        mToolTipViewGroup.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        int toolTipMeasuredWidth = mToolTipViewGroup.getMeasuredWidth();
        int toolTipMeasuredHeight = mToolTipViewGroup.getMeasuredHeight();
        Point resultPoint = new Point(); // this holds the final position of tooltip
        float density = mActivity.getResources().getDisplayMetrics().density;
        adjustment = 10 * density; //adjustment is that little overlapping area of tooltip and targeted button
        if (toolTipMeasuredWidth > parent.getWidth()){
            resultPoint.x = getXForTooTip(mToolTip.mGravity, parent.getWidth(), targetViewX, adjustment);
        } else {
            resultPoint.x = getXForTooTip(mToolTip.mGravity, toolTipMeasuredWidth, targetViewX, adjustment);
        }
        resultPoint.y = getYForTooTip(mToolTip.mGravity, toolTipMeasuredHeight, targetViewY, adjustment);
        parent.addView(mToolTipViewGroup, layoutParams);
        // 1. width < screen check
        if (toolTipMeasuredWidth > parent.getWidth()){
            mToolTipViewGroup.getLayoutParams().width = parent.getWidth();
            toolTipMeasuredWidth = parent.getWidth();
        }
        // 2. x left boundary check
        if (resultPoint.x < 0){
            mToolTipViewGroup.getLayoutParams().width = toolTipMeasuredWidth + resultPoint.x;
            resultPoint.x = 0;
        }
        // 3. x right boundary check
        int tempRightX = resultPoint.x + toolTipMeasuredWidth;
        if ( tempRightX > parent.getWidth()){
            mToolTipViewGroup.getLayoutParams().width = parent.getWidth() - resultPoint.x;
        }
        return resultPoint;
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
        invisFab = new FloatingActionButton(mActivity);
        invisFab.setSize(FloatingActionButton.SIZE_MINI);
        invisFab.setVisibility(View.INVISIBLE);
        ((ViewGroup)mActivity.getWindow().getDecorView()).addView(invisFab);

        // fab is the real fab that is going to be added
        mFab = new FloatingActionButton(mActivity);
        mFab.setBackgroundColor(Color.BLUE);
        mFab.setSize(FloatingActionButton.SIZE_MINI);
        mFab.setColorNormal(mPointer.mColor);
        mFab.setStrokeVisible(false);
        mFab.setClickable(true);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tourGuideContext.cleanUp();
            }
        });
        // When invisFab is layouted, it's width and height can be used to calculate the correct position of fab
        final ViewTreeObserver viewTreeObserver = invisFab.getViewTreeObserver();
        mFrameLayoutWithHoleForListener = frameLayoutWithHole;
        viewTreeObserver.addOnGlobalLayoutListener(invisFabGlobalLayoutListener);
        return mFab;
    }

    private void setupFrameLayout(){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ViewGroup contentArea = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        int [] pos = new int[2];
        contentArea.getLocationOnScreen(pos);
        // frameLayoutWithHole's coordinates are calculated taking full screen height into account
        // but we're adding it to the content area only, so we need to offset it to the same Y value of contentArea
        layoutParams.setMargins(0, -pos[1], 0, 0);

        ((ViewGroup) mActivity.getWindow().getDecorView()).addView(mFrameLayout, layoutParams);
    }

    /**
     * Clean up the tutorial that is added to the activity
     */
    public void cleanUp() {
        if (mFrameLayout != null) {
            mFrameLayout.cleanUp();
            mFrameLayout = null;
        }
        mHighlightedView.getViewTreeObserver().removeOnGlobalLayoutListener(mHighlightedViewGlobalLayoutListener);

        if (mToolTipViewGroup != null) {
            mToolTipViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(mToolTipViewGroupGlobalLayoutListener);
            ((ViewGroup) mActivity.getWindow().getDecorView()).removeView(mToolTipViewGroup);
            mToolTipViewGroup = null;
        }
        if (invisFab != null) {
            invisFab.getViewTreeObserver().removeOnGlobalLayoutListener(invisFabGlobalLayoutListener);
            ((ViewGroup) mActivity.getWindow().getDecorView()).removeView(invisFab);
            invisFab = null;
        }
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
     * Set the Pointer
     * @param pointer Pointer in the hole
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setPointer(Pointer pointer){
        mPointer = pointer;
        return this;
    }

    /**
     * Set the hole radius
     * @param radius Radius of the hole
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setHoleRadius(int radius){
        mHoleRadius = radius;
        return this;
    }

    /**
     * @return return highlighted/Hole view
     */
    public View getHighlightedView() {
        return mHighlightedView;
    }

    /**
     * @return Return root view of tour guide
     */
    public View getRootView() {
        return mRootView;
    }

    /**
     * @return Return the highlighted/hole radius
     */
    public int getRadius() {
        return mHoleRadius;
    }

}
