package tourguide.tourguide;

/**
 * // Example usage:
 * Sequence seq = new Sequence.SequenceBuilder()
 *                                 .add(tg1, tg2, tg3)
 *                                 .setDefaultOverlay()
 *                                 .setDefaultToolTip()
 *                                 .setDefaultPointer()
 *                                 .setContinueMethod()
 *                                 .setDisableButton(true or false)
 *                                 .build();
 * // Usage 1:
 * // TourGuide mHandler = TourGuide.init().playSequence(seq);
 *
 * // OR
 * // Usage 2: this has to be played later
 *
 * TourGuide mHandler = TourGuide.init().setSequence(seq);
 * button.setOnClickListener(){
 *     mHandler.next();
 *     // user code
 * }
 * button2.setOnClickListener(){
 *     mHandler.next();
 *     // user code
 * }
 */

public class Sequence {
    TourGuide [] mTourGuideArray;
    Overlay mDefaultOverlay;
    ToolTip mDefaultToolTip;
    Pointer mDefaultPointer;
    int mContinueMethod;
    boolean mDisableTargetButton;

    public static int ContinueMethodOverlay = 1;
    public static int ContinueMethodToolTip = 2;

    public Sequence(TourGuide [] tourGuideArray, Overlay defaultOverlay, ToolTip defaultToolTip, Pointer defaultPointer, int continueMethod, boolean disableTargetButton){
        //TODO
    }

    public static class SequenceBuilder {
        TourGuide [] mTourGuideArray;
        Overlay mDefaultOverlay;
        ToolTip mDefaultToolTip;
        Pointer mDefaultPointer;
        int mContinueMethod;
        boolean mDisableTargetButton;

        public SequenceBuilder add(TourGuide... tourGuideArray){
            mTourGuideArray = tourGuideArray;
            return this;
        }

        // TODO: implement
        public SequenceBuilder setDefaultOverlay(){

            return this;
        }

        // This might not be useful, but who knows.. maybe someone needs it
        // TODO: implement
        public SequenceBuilder setDefaultToolTip(){

            return this;
        }

        // TODO: implement
        public SequenceBuilder setDefaultPointer(){

            return this;
        }

        // TODO: implement
        public SequenceBuilder setDisableButton(){

            return this;
        }

        /**
         * @param continueMethod should only be ContinueMethodOverlay or ContinueMethodToolTip, if both are intended to be clickable, user can use ContinueMethodOverlay|ContinueMethodToolTip
         */
        public SequenceBuilder setContinueMethod(int continueMethod){
            // TODO: check that it must be one of: ContinueMethodOverlay, ContinueMethodToolTip or both
            // TODO: implement
            return this;
        }

        public Sequence build(){
            // TODO: check that it must have mTourGuideArray with at least 2 items, and choose a ContinueMethod, else an throw error with proper help message

            Sequence seq = new Sequence(mTourGuideArray, mDefaultOverlay, mDefaultToolTip, mDefaultPointer, mContinueMethod, mDisableTargetButton);
            return seq;
        }
    }
}
