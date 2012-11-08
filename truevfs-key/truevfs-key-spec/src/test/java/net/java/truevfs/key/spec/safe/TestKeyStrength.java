package net.java.truevfs.key.spec.safe;

/**
 * @author Christian Schlichtherle
 */
public enum TestKeyStrength implements SafeKeyStrength {
    STRONG;

    @Override
    public int getBits() {
        return 256;
    }

    @Override
    public int getBytes() {
        return 32;
    }

}
