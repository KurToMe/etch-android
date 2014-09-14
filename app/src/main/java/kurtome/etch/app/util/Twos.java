package kurtome.etch.app.util;

public class Twos {
    private Twos() {
        // don't instantiate me
    }

    public static int firstLargerPowerOfTwo(int num) {
        for (int exponent = 1; exponent < 31; exponent++) {
            int power = 1 << exponent;
            if (power > num) {
                return power;
            }
        }

        throw new IllegalArgumentException("Error finding next power of two for " + num);
    }

}
