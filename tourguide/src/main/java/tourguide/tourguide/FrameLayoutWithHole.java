package tourguide.tourguide;

import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class FrameLayoutWithHole extends FrameLayout {
    private Activity mActivity;
    private TourGuide.MotionType mMotionType;
    private Paint mEraser;

    Bitmap mEraserBitmap;
    private Canvas mEraserCanvas;

    private View mViewHole;
    private int [] mPos;
    private int mRadius = 0;

    private float mDensity;
    private Overlay mOverlay;



    private ArrayList<AnimatorSet> mAnimatorSetArrayList;

    public void setViewHole(View viewHole) {
        this.mViewHole = viewHole;
        enforceMotionType();
    }
    public void addAnimatorSet(AnimatorSet animatorSet){
        if (mAnimatorSetArrayList==null){
            mAnimatorSetArrayList = new ArrayList<>();
        }
        mAnimatorSetArrayList.add(animatorSet);
    }

    private void enforceMotionType(){
        if (mViewHole != null) {Log.d("tourguide","enforceMotionType 2");
            if (mMotionType!=null && mMotionType == TourGuide.MotionType.ClickOnly) {
                mViewHole.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        mViewHole.getParent().requestDisallowInterceptTouchEvent(true);
                        return false;
                    }
                });
            } else if (mMotionType!=null && mMotionType == TourGuide.MotionType.SwipeOnly) {
                mViewHole.setClickable(false);
            }
        }
    }

    private void initRelativePosition(View view) {
        mPos = new int [2];
        view.getLocationOnScreen(mPos);
        Log.d("TourGuide", "Target position : " + mPos[0] + " " + mPos[1]);
    }

    private void initRadius(Activity context, int radius) {
        mDensity = context.getResources().getDisplayMetrics().density;
        int padding = (int)(20 * mDensity);
        if (radius <= 0) {
            if (mViewHole.getHeight() > mViewHole.getWidth()) {
                mRadius = mViewHole.getHeight() / 2 + padding;
            } else {
                mRadius = mViewHole.getWidth() / 2 + padding;
            }
        } else {
            mRadius = radius;
        }
    }

    public FrameLayoutWithHole(Activity context, TourGuide tourGuide, TourGuide.MotionType motionType, Overlay overlay) {
        super(context);
        mOverlay = overlay;
        mActivity = context;
        mViewHole = tourGuide.getHighlightedView();
        mMotionType = motionType;
        this.init();
        this.enforceMotionType();
        this.initRelativePosition(tourGuide.getHighlightedView());
        this.initRadius(context, tourGuide.getRadius());
    }

    private void init() {
        setWillNotDraw(false);
        // Set up a default TextPaint object
        TextPaint mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        Point size = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(size);

        mEraserBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        mEraserCanvas = new Canvas(mEraserBitmap);

        Paint mPaint = new Paint();
        mPaint.setColor(0xcc000000);
        Paint transparentPaint = new Paint();
        transparentPaint.setColor(getResources().getColor(android.R.color.transparent));
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mEraser = new Paint();
        mEraser.setColor(0xFFFFFFFF);
        mEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

    }

    private boolean mCleanUpLock = false;

    protected void cleanUp(){
        if (getParent() != null) {
            ((ViewGroup) this.getParent()).removeView(this);
        }
    }

    private void performOverlayExitAnimation(){
        if (!mCleanUpLock) {
            final FrameLayout _pointerToFrameLayout = this;
            mCleanUpLock = true;
            Log.d("tourguide","Overlay exit animation listener is overwritten...");
            mOverlay.mExitAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    ((ViewGroup) _pointerToFrameLayout.getParent()).removeView(_pointerToFrameLayout);
                }
            });
            this.startAnimation(mOverlay.mExitAnimation);
        }
    }

    /* comment this whole method to cause a memory leak */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        /* cleanup reference to prevent memory leak */
        mEraserCanvas.setBitmap(null);
        mEraserBitmap = null;

        if (mAnimatorSetArrayList != null && mAnimatorSetArrayList.size() > 0){
            for(int i=0;i<mAnimatorSetArrayList.size();i++){
                mAnimatorSetArrayList.get(i).end();
                mAnimatorSetArrayList.get(i).removeAllListeners();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mEraserBitmap.eraseColor(Color.TRANSPARENT);

        if (mOverlay!=null) {
            mEraserCanvas.drawColor(mOverlay.mBackgroundColor);
            int padding = (int) (10 * mDensity);
            if (mOverlay.mStyle == Overlay.Style.Rectangle) {
                mEraserCanvas.drawRect(mPos[0] - padding, mPos[1] - padding, mPos[0] + mViewHole.getWidth() + padding, mPos[1] + mViewHole.getHeight() + padding, mEraser);
            } else {
                mEraserCanvas.drawCircle(mPos[0] + mViewHole.getWidth() / 2, mPos[1] + mViewHole.getHeight() / 2, mRadius, mEraser);
            }
        }
        canvas.drawBitmap(mEraserBitmap, 0, 0, null);

    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mOverlay!=null && mOverlay.mEnterAnimation!=null) {
            this.startAnimation(mOverlay.mEnterAnimation);
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if(mViewHole != null) {
            int[] pos = new int[2];
            mViewHole.getLocationOnScreen(pos);
        }
        return true;
    }
}
