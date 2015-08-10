package tourguide.tourguide;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;

/**
 * Created by tanjunrong on 6/20/15.
 *
 */
public class Pointer {

    public int mGravity = Gravity.CENTER;
    public int mColor = Color.WHITE;

    public Pointer() {
        this(Gravity.CENTER, Color.parseColor("#FFFFFF"));
    }

    public Pointer(int gravity, int color) {
        this.mGravity = gravity;
        this.mColor = color;
    }

    /**
     * Set color
     * @param color Pointer color
     * @return return Pointer instance for chaining purpose
     */
    public Pointer setColor(int color){
        mColor = color;
        return this;
    }

    /**
     * Set gravity
     * @param gravity Pointer gravity relative to the view pointed
     * @return return Pointer instance for chaining purpose
     */
    public Pointer setGravity(int gravity){
        mGravity = gravity;
        return this;
    }

    public int getXBasedOnGravity(int width, View mRootView, View mHighlightedView) {
        int [] pos = new int[2];
        int [] rootPos = new int [2];
        mRootView.getLocationOnScreen(rootPos);
        mHighlightedView.getLocationOnScreen(pos);
        int x = pos[0] - rootPos[0];
        if ((this.mGravity & Gravity.END) == Gravity.END) {
            return x + mHighlightedView.getWidth() - width;
        } else if ((this.mGravity & Gravity.START) == Gravity.START) {
            return x;
        } else { // this is center
            return x + mHighlightedView.getWidth() / 2 - width / 2;
        }
    }

    public int getYBasedOnGravity(int height, View mRootView, View mHighlightedView) {
        int [] pos = new int[2];
        int [] rootPos = new int [2];
        mRootView.getLocationOnScreen(rootPos);
        mHighlightedView.getLocationInWindow(pos);
        int y = pos[1] - rootPos[1];
        if ((this.mGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            return y + mHighlightedView.getHeight() - height;
        } else if ((this.mGravity & Gravity.TOP) == Gravity.TOP) {
            return y;
        } else { // this is center
            return y + mHighlightedView.getHeight() / 2 - height / 2;
        }
    }
}
