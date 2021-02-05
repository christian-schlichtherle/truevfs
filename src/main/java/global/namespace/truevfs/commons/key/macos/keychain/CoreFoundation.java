/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.macos.keychain;

import com.sun.jna.*;
import lombok.val;

import java.util.Arrays;
import java.util.List;

/**
 * Exposes some parts of Apple's native Core Foundation API.
 *
 * @author Christian Schlichtherle
 * @see <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFBaseUtils/Reference/reference.html">Mac Developer Library: Base Utilities Reference</a>
 * @see <a href="https://developer.apple.com/library/mac/#documentation/CoreFoundation/Reference/CFTypeRef/Reference/reference.html">Mac Developer Library: CFType Reference</a>
 */
@SuppressWarnings({"PackageVisibleInnerClass", "PackageVisibleField"})
final class CoreFoundation {

    static {
        Native.register(CoreFoundation.class.getSimpleName());
    }

    private CoreFoundation() {
    }

    //
    // Functions:
    //

    /**
     * @see <a href="https://developer.apple.com/documentation/corefoundation/1521153-cfrelease?language=objc">Apple Developer Documentation</a>
     */
    static native void CFRelease(CFTypeRef cf);

    /**
     * @see <a href="https://developer.apple.com/documentation/corefoundation/1542656-cfstringgetcharacters?language=objc">Apple Developer Documentation</a>
     */
    static native void CFStringGetCharacters(CFStringRef theString, CFRange.ByValue range, char[] buffer);

    /**
     * @see <a href="https://developer.apple.com/documentation/corefoundation/1542853-cfstringgetlength?language=objc">Apple Developer Documentation</a>
     */
    static native CFIndex CFStringGetLength(CFStringRef theString);

    //
    // Utilities:
    //

    static String decode(final CFStringRef stringRef) {
        val length = CFStringGetLength(stringRef).intValue();
        val buffer = new char[length];
        CFStringGetCharacters(stringRef, new CFRange.ByValue(new CFIndex(0), new CFIndex(length)), buffer);
        return new String(buffer);
    }

    //
    // Types:
    //

    public static final class CFIndex extends NativeLong {

        private static final long serialVersionUID = 0;

        public CFIndex() {
        }

        CFIndex(long value) {
            super(value);
        }
    }

    public static class CFRange extends Structure {

        public CFIndex location, length;

        public CFRange() {
        }

        CFRange(final CFIndex location, final CFIndex length) {
            this.location = location;
            this.length = length;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("location", "length");
        }

        public static final class ByValue extends CFRange implements Structure.ByValue {

            public ByValue() {
            }

            ByValue(CFIndex location, CFIndex length) {
                super(location, length);
            }
        }
    }

    public static class CFTypeRef extends PointerType {

        public CFTypeRef() {
        }

        CFTypeRef(Pointer p) {
            super(p);
        }
    }

    public static final class CFStringRef extends CFTypeRef {

        public CFStringRef() {
        }

        CFStringRef(Pointer p) {
            super(p);
        }
    }
}
