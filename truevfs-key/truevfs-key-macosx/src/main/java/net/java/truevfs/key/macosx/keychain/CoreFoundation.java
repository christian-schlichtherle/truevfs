/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx.keychain;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Exposes some parts of Apple's native Core Foundation Framework API.
 *
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFBaseUtils/Reference/reference.html">Mac Developer Library: Base Utilities Reference</a>
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFTypeRef/Reference/reference.html">Mac Developer Library: CFType Reference</a>
 * @author Christian Schlichtherle
 */
@SuppressWarnings({ "PackageVisibleInnerClass", "PackageVisibleField" })
final class CoreFoundation implements Library {

    static { Native.register(CoreFoundation.class.getSimpleName()); }

    private CoreFoundation() { }

    //
    // Some functions.
    //

    public static native void CFRelease(CFTypeRef cf);
    public static native void CFStringGetCharacters(CFStringRef theString, CFRange.ByValue range, char[] buffer);
    public static native CFIndex CFStringGetLength(CFStringRef theString);

    //
    // Some utilities.
    //

    static String decode(final CFStringRef theString) {
        final int length = CFStringGetLength(theString).intValue();
        final char[] buffer = new char[length];
        CFStringGetCharacters(
                theString,
                new CFRange.ByValue(new CFIndex(0), new CFIndex(length)),
                buffer);
        return new String(buffer);
    }

    //
    // Some types.
    //

    public static class CFIndex extends NativeLong {
        private static final long serialVersionUID = 0;

        public CFIndex() { }
        public CFIndex(long value) { super(value); }
    } // CFindex

    public static class CFRange extends Structure {
        public CFIndex location, length;

        public CFRange() { }
        public CFRange(final CFIndex location, final CFIndex length) {
            this.location = location;
            this.length = length;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("location", "length");
        }

        public static class ByValue
        extends CFRange implements Structure.ByValue {
            public ByValue() { }
            public ByValue(CFIndex location, CFIndex length) {
                super(location, length);
            }
        } // ByValue
    } // CFRange

    public static class CFTypeRef extends PointerType {
        public CFTypeRef() { }
        public CFTypeRef(Pointer p) { super(p); }
    } // CFTypeRef

    public static class CFStringRef extends CFTypeRef {
        public CFStringRef() { }
        public CFStringRef(Pointer p) { super(p); }
    } // CFStringRef
}
