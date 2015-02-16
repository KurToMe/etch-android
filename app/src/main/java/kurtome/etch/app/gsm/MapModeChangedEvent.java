package kurtome.etch.app.gsm;

public class MapModeChangedEvent {

    public static enum Mode {
        MAP,
        DRAWING
    }

    public final Mode mode;

    public MapModeChangedEvent(Mode mode) {
        this.mode = mode;
    }
}
