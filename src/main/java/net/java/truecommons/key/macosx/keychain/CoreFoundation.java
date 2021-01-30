/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx.keychain;

import com.sun.jna.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.List;

/**
 * Exposes some parts of Apple's native Core Foundation API.
 *
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFBaseUtils/Reference/reference.html">Mac Developer Library: Base Utilities Reference</a>
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFTypeRef/Reference/reference.html">Mac Developer Library: CFType Reference</a>
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings({ "PackageVisibleInnerClass", "PackageVisibleField" })
final class CoreFoundation {

    static { Native.register(CoreFoundation.class.getSimpleName()); }

    private CoreFoundation() { }

    //
    // Some functions.
    //

    static native void CFRelease(CFTypeRef cf);
    static native void CFStringGetCharacters(CFStringRef theString, CFRange.ByValue range, char[] buffer);
    static native CFIndex CFStringGetLength(CFStringRef theString);

    //
    // Some utilities.
    //

    static String decode(final CFStringRef stringRef) {
        final int length = CFStringGetLength(stringRef).intValue();
        final char[] buffer = new char[length];
        CFStringGetCharacters(
                stringRef,
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
        CFIndex(long value) { super(value); }
    } // CFindex

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static class CFRange extends Structure {
        public CFIndex location, length;

        public CFRange() { }
        CFRange(final CFIndex location, final CFIndex length) {
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
            ByValue(CFIndex location, CFIndex length) {
                super(location, length);
            }
        } // ByValue
    } // CFRange

    public static class CFTypeRef extends PointerType {
        public CFTypeRef() { }
        CFTypeRef(Pointer p) { super(p); }
    } // CFTypeRef

    public static class CFStringRef extends CFTypeRef {
        public CFStringRef() { }
        CFStringRef(Pointer p) { super(p); }
    } // CFStringRef
}
