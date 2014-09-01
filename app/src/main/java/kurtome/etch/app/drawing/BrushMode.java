package kurtome.etch.app.drawing;

import android.graphics.PorterDuff;

enum BrushMode {
    NORMAL("Normal", 0, PorterDuff.Mode.SRC_OVER),
    REPLACE("Replace", 1, PorterDuff.Mode.SRC),
    UNDER("Under", 2, PorterDuff.Mode.DST_OVER);

    public String display;
    public int displayPosition;
    public PorterDuff.Mode porterDuff;

    BrushMode(String display, int displayPosition, PorterDuff.Mode porterDuff) {
        this.display = display;
        this.displayPosition = displayPosition;
        this.porterDuff = porterDuff;
    }

    public static BrushMode fromPorterDuff(PorterDuff.Mode porterDuff) {
        for (BrushMode enumVal : BrushMode.values()) {
            if (enumVal.porterDuff == porterDuff) {
                return enumVal;
            }
        }

        throw new IllegalArgumentException("Unknown PorterDuff mode");
    }

    public static BrushMode fromDisplayPosition(int position) {
        for (BrushMode enumVal : BrushMode.values()) {
            if (enumVal.displayPosition == position) {
                return enumVal;
            }
        }

        throw new IllegalArgumentException("Unknown display position");
    }
}
