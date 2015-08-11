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

    public int getX(View mHighlightedView) {
        int [] pos = new int[2];
        mHighlightedView.getLocationOnScreen(pos);
        return pos[0];
    }

    public int getY(View mHighlightedView) {
        int [] pos = new int[2];
        mHighlightedView.getLocationInWindow(pos);
        return pos[1];
    }
}
