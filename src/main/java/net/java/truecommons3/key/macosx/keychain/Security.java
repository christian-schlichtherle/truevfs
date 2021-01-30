/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.macosx.keychain;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.java.truecommons3.key.macosx.keychain.CoreFoundation.CFStringRef;
import net.java.truecommons3.key.macosx.keychain.CoreFoundation.CFTypeRef;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static net.java.truecommons3.key.macosx.keychain.CoreFoundation.CFRelease;
import static net.java.truecommons3.key.macosx.keychain.CoreFoundation.decode;

/**
 * Exposes some parts of Apple's native Keychain Services API.
 *
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/security/Reference/keychainservices/Reference/reference.html">Mac Developer Library: Keychain Services Reference</a>
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings({ "PackageVisibleInnerClass", "PackageVisibleField" })
final class Security {

    static { Native.register(Security.class.getSimpleName()); }

    private Security() { }

    //
    // Some constants.
    //

    static final int
            errSecSuccess           = 0,
            errSecUnimplemented     = -4,
            errSecInvalidKeychain   = -25295,
            errSecDuplicateKeychain = -25296,
            errSecDuplicateItem     = -25299,
            errSecItemNotFound      = -25300;

    static final int
            CSSM_DL_DB_RECORD_ANY           = 0xA,
            CSSM_DL_DB_RECORD_CERT          = 0xA + 1,
            CSSM_DL_DB_RECORD_CRL           = 0xA + 2,
            CSSM_DL_DB_RECORD_POLICY        = 0xA + 3,
            CSSM_DL_DB_RECORD_GENERIC       = 0xA + 4,
            kSecPublicKeyItemClass          = 0xA + 5,
            kSecPrivateKeyItemClass         = 0xA + 6,
            kSecSymmetricKeyItemClass       = 0xA + 7,
            CSSM_DL_DB_RECORD_ALL_KEYS      = 0xA + 8,
            kSecInternetPasswordItemClass   = (('i' << 8 | 'n') << 8 | 'e') << 8 | 't',
            kSecGenericPasswordItemClass    = (('g' << 8 | 'e') << 8 | 'n') << 8 | 'p',
            kSecAppleSharePasswordItemClass = (('a' << 8 | 's') << 8 | 'h') << 8 | 'p',
            kSecCertificateItemClass        = 0x8000_1000;

    static final int
            kSecKeyKeyClass         = 0,
            kSecKeyPrintName        = 1,
            kSecKeyAlias            = 2,
            kSecKeyPermanent        = 3,
            kSecKeyPrivate          = 4,
            kSecKeyModifiable       = 5,
            kSecKeyLabel            = 6,
            kSecKeyApplicationTag   = 7,
            kSecKeyKeyCreator       = 8,
            kSecKeyKeyType          = 9,
            kSecKeyKeySizeInBits    = 10,
            kSecKeyEffectiveKeySize = 11,
            kSecKeyStartDate        = 12,
            kSecKeyEndDate          = 13,
            kSecKeySensitive        = 14,
            kSecKeyAlwaysSensitive  = 15,
            kSecKeyExtractable      = 16,
            kSecKeyNeverExtractable = 17,
            kSecKeyEncrypt          = 18,
            kSecKeyDecrypt          = 19,
            kSecKeyDerive           = 20,
            kSecKeySign             = 21,
            kSecKeyVerify           = 22,
            kSecKeySignRecover      = 23,
            kSecKeyVerifyRecover    = 24,
            kSecKeyWrap             = 25,
            kSecKeyUnwrap           = 26;

    static final int
            kSecCreationDateItemAttr       = (('c' << 8 | 'd') << 8 | 'a') << 8 | 't',
            kSecModDateItemAttr            = (('m' << 8 | 'd') << 8 | 'a') << 8 | 't',
            kSecDescriptionItemAttr        = (('d' << 8 | 'e') << 8 | 's') << 8 | 'c',
            kSecCommentItemAttr            = (('i' << 8 | 'c') << 8 | 'm') << 8 | 't',
            kSecCreatorItemAttr            = (('c' << 8 | 'r') << 8 | 't') << 8 | 'r',
            kSecTypeItemAttr               = (('t' << 8 | 'y') << 8 | 'p') << 8 | 'e',
            kSecScriptCodeItemAttr         = (('s' << 8 | 'c') << 8 | 'r') << 8 | 'p',
            kSecLabelItemAttr              = (('l' << 8 | 'a') << 8 | 'b') << 8 | 'l',
            kSecInvisibleItemAttr          = (('i' << 8 | 'n') << 8 | 'v') << 8 | 'i',
            kSecNegativeItemAttr           = (('n' << 8 | 'e') << 8 | 'g') << 8 | 'a',
            kSecCustomIconItemAttr         = (('c' << 8 | 'u') << 8 | 's') << 8 | 'i',
            kSecAccountItemAttr            = (('a' << 8 | 'c') << 8 | 'c') << 8 | 't',
            kSecServiceItemAttr            = (('s' << 8 | 'v') << 8 | 'c') << 8 | 'e',
            kSecGenericItemAttr            = (('g' << 8 | 'e') << 8 | 'n') << 8 | 'a',
            kSecSecurityDomainItemAttr     = (('s' << 8 | 'd') << 8 | 'm') << 8 | 'n',
            kSecServerItemAttr             = (('s' << 8 | 'r') << 8 | 'v') << 8 | 'r',
            kSecAuthenticationTypeItemAttr = (('a' << 8 | 't') << 8 | 'y') << 8 | 'p',
            kSecPortItemAttr               = (('p' << 8 | 'o') << 8 | 'r') << 8 | 't',
            kSecPathItemAttr               = (('p' << 8 | 'a') << 8 | 't') << 8 | 'h',
            kSecVolumeItemAttr             = (('v' << 8 | 'l') << 8 | 'm') << 8 | 'e',
            kSecAddressItemAttr            = (('a' << 8 | 'd') << 8 | 'd') << 8 | 'r',
            kSecSignatureItemAttr          = (('s' << 8 | 's') << 8 | 'i') << 8 | 'g',
            kSecProtocolItemAttr           = (('p' << 8 | 't') << 8 | 'c') << 8 | 'l',
            kSecCertificateType            = (('c' << 8 | 't') << 8 | 'y') << 8 | 'p',
            kSecCertificateEncoding        = (('c' << 8 | 'e') << 8 | 'n') << 8 | 'c',
            kSecCrlType                    = (('c' << 8 | 'r') << 8 | 't') << 8 | 'p',
            kSecCrlEncoding                = (('c' << 8 | 'r') << 8 | 'n') << 8 | 'c',
            kSecAlias                      = (('a' << 8 | 'l') << 8 | 'i') << 8 | 's';

    static final int kSecFormatUnknown = 0;

    //
    // Some functions.
    //

    static native int // OSStatus
    SecKeychainCopyDefault(
        PointerByReference keychain // SecKeychainRef*
    );

    static native int // OSStatus
    SecKeychainCreate(
            String pathName, // const char*
            int passwordLength, // Uint32
            @Nullable ByteBuffer password, // const void*
            boolean promptUser, // Boolean
            @Nullable SecAccessRef initialAccess,
            @Nullable PointerByReference keychain // SecKeychainRef*
    );

    static native int // OSStatus
    SecKeychainDelete(
            SecKeychainRef keychainOrArray
    );

    static native int // OSStatus
    SecKeychainItemCopyAttributesAndData(
            SecKeychainItemRef itemRef, // SecKeychainItemRef
            @Nullable SecKeychainAttributeInfo info, // SecKeychainAttributeInfo*
            @Nullable IntByReference itemClass, // SecItemClass*
            @Nullable PointerByReference attrList, // SecKeychainAttributeList**
            @Nullable IntByReference length, // UInt32*
            @Nullable PointerByReference outData // void**
    );

    static native int // OSStatus
    SecKeychainItemCreateFromContent(
            int itemClass, // SecItemClass
            SecKeychainAttributeList attrList, // SecKeychainAttributeList*
            int length, // UInt32
            ByteBuffer data, // const void*
            @Nullable SecKeychainRef keychainRef,
            @Nullable SecAccessRef initialAccess,
            @Nullable PointerByReference itemRef // SecKeychainItemRef*
    );

    static native int // OSStatus
    SecKeychainItemDelete(
            SecKeychainItemRef itemRef
    );

    static native int // OSStatus
    SecKeychainItemFreeAttributesAndData(
            @Nullable SecKeychainAttributeList attrList, // SecKeychainAttributeList*
            @Nullable Pointer data // void*
    );

    static native int // OSStatus
    SecKeychainItemModifyAttributesAndData (
            SecKeychainItemRef itemRef,
            @Nullable SecKeychainAttributeList attrList, // const SecKeychainAttributeList*
            int length, // UInt32
            @Nullable ByteBuffer data // const void*
    );

    static native int // OSStatus
    SecKeychainOpen(
            String pathName, // const char*
            PointerByReference keychain // SecKeychainRef*
    );

    static native int // OSStatus
    SecKeychainSearchCopyNext(
            SecKeychainSearchRef searchRef,
            PointerByReference itemRef // SecKeychainItemRef*
    );

    static native int // OSStatus
    SecKeychainSearchCreateFromAttributes(
            @Nullable CFTypeRef keychainOrArray,
            int itemClass, // SecItemClass
            @Nullable SecKeychainAttributeList attrList, // const SecKeychainAttributeList*
            PointerByReference searchRef // SecKeychainSearchRef*
    );

    static native CFStringRef
    SecCopyErrorMessageString(
            int status, // OSStatus
            @Nullable Pointer reserved // void*
    );

    //
    // Some utilities.
    //

    static String message(final int status) {
        final CFStringRef stringRef = SecCopyErrorMessageString(status, null);
        try { return decode(stringRef); }
        finally { CFRelease(stringRef); }
    }

    //
    // Some types.
    //

    public static class SecAccessRef extends CFTypeRef {
        public SecAccessRef() { }
        SecAccessRef(Pointer p) { super(p); }
    }

    public static class SecKeychainAttribute extends Structure {
        public int tag; // SecKeychainAttrType
        public int length; // UInt32
        public Pointer data; // void*

        public SecKeychainAttribute() { }
        SecKeychainAttribute(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("tag", "length", "data");
        }
    }

    public static class SecKeychainAttributeInfo extends Structure {
        public int count; // UInt32
        public Pointer tag; // UInt32*
        public Pointer format; // UInt32*

        public SecKeychainAttributeInfo() { }
        SecKeychainAttributeInfo(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "tag", "format");
        }
    }

    public static class SecKeychainAttributeList extends Structure {
        public int count; // UInt32
        public Pointer attr; // SecKeychainAttribute*

        public SecKeychainAttributeList() { }
        SecKeychainAttributeList(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "attr");
        }
    }

    public static class SecKeychainItemRef extends CFTypeRef {
        public SecKeychainItemRef() { }
        SecKeychainItemRef(Pointer p) { super(p); }
    }

    public static class SecKeychainRef extends CFTypeRef {
        public SecKeychainRef() { }
        SecKeychainRef(Pointer p) { super(p); }
    }

    public static class SecKeychainSearchRef extends CFTypeRef {
        public SecKeychainSearchRef() { }
        SecKeychainSearchRef(Pointer p) { super(p); }
    }
}
