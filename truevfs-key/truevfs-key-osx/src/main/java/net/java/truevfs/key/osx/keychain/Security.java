/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx.keychain;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import static net.java.truevfs.key.osx.keychain.CoreFoundation.*;
import net.java.truevfs.key.osx.keychain.CoreFoundation.CFStringRef;
import net.java.truevfs.key.osx.keychain.CoreFoundation.CFTypeRef;

/**
 * Exposes some parts of Apple's native Keychain Services Framework API.
 *
 * @see    <a href="https://developer.apple.com/library/mac/#documentation/security/Reference/keychainservices/Reference/reference.html">Mac Developer Library: Keychain Services Reference</a>
 * @author Christian Schlichtherle
 */
@SuppressWarnings({ "PackageVisibleInnerClass", "PackageVisibleField" })
final class Security implements Library {

    static { Native.register(Security.class.getSimpleName()); }

    private Security() { }

    //
    // Some constants.
    //

    public static final int
            errSecSuccess           = 0,
            errSecUnimplemented     = -4,
            errSecInvalidKeychain   = -25295,
            errSecDuplicateKeychain = -25296,
            errSecDuplicateItem     = -25299,
            errSecItemNotFound      = -25300;

    public static final int
            CSSM_DL_DB_RECORD_ANY           = 0xA + 0,
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

    public static final int
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

    public static final int
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

    public static final int kSecFormatUnknown = 0;

    //
    // Some functions.
    //

    public static native int // OSStatus
    SecKeychainCopyDefault(
        PointerByReference keychain // SecKeychainRef*
    );

    public static native int // OSStatus
    SecKeychainCreate(
            String pathName, // const char*
            int passwordLength, // Uint32
            @CheckForNull ByteBuffer password, // const void*
            boolean promptUser, // Boolean
            @CheckForNull SecAccessRef initialAccess,
            @CheckForNull PointerByReference keychain // SecKeychainRef*
    );

    public static native int // OSStatus
    SecKeychainDelete(
            SecKeychainRef keychainOrArray
    );

    public static native int // OSStatus
    SecKeychainItemCopyAttributesAndData(
            SecKeychainItemRef itemRef, // SecKeychainItemRef
            @CheckForNull SecKeychainAttributeInfo info, // SecKeychainAttributeInfo*
            @CheckForNull IntByReference itemClass, // SecItemClass*
            @CheckForNull PointerByReference attrList, // SecKeychainAttributeList**
            @CheckForNull IntByReference length, // UInt32*
            @CheckForNull PointerByReference outData // void**
    );

    public static native int // OSStatus
    SecKeychainItemCreateFromContent(
            int itemClass, // SecItemClass
            SecKeychainAttributeList attrList, // SecKeychainAttributeList*
            int length, // UInt32
            ByteBuffer data, // const void*
            @CheckForNull SecKeychainRef keychainRef,
            @CheckForNull SecAccessRef initialAccess,
            @CheckForNull PointerByReference itemRef // SecKeychainItemRef*
    );

    public static native int // OSStatus
    SecKeychainItemDelete(
            SecKeychainItemRef itemRef
    );

    public static native int // OSStatus
    SecKeychainItemFreeAttributesAndData(
            @CheckForNull SecKeychainAttributeList attrList, // SecKeychainAttributeList*
            @CheckForNull Pointer data // void*
    );

    public static native int // OSStatus
    SecKeychainItemModifyAttributesAndData (
            SecKeychainItemRef itemRef,
            @CheckForNull SecKeychainAttributeList attrList, // const SecKeychainAttributeList*
            int length, // UInt32
            @CheckForNull ByteBuffer data // const void*
    );

    public static native int // OSStatus
    SecKeychainOpen(
            String pathName, // const char*
            PointerByReference keychain // SecKeychainRef*
    );

    public static native int // OSStatus
    SecKeychainSearchCopyNext(
            SecKeychainSearchRef searchRef,
            PointerByReference itemRef // SecKeychainItemRef*
    );

    public static native int // OSStatus
    SecKeychainSearchCreateFromAttributes(
            @CheckForNull CFTypeRef keychainOrArray,
            int itemClass, // SecItemClass
            @CheckForNull SecKeychainAttributeList attrList, // const SecKeychainAttributeList*
            PointerByReference searchRef // SecKeychainSearchRef*
    );

    public static native CFStringRef
    SecCopyErrorMessageString(
            int status, // OSStatus
            @CheckForNull Pointer reserved // void*
    );

    //
    // Some utilities.
    //

    static String message(final int status) {
        final CFStringRef theString = SecCopyErrorMessageString(status, null);
        try { return decode(theString); }
        finally { CFRelease(theString); }
    }

    //
    // Some types.
    //

    public static class SecAccessRef extends CFTypeRef {
        public SecAccessRef() { }
        public SecAccessRef(Pointer p) { super(p); }
    } // SecAccessRef

    public static class SecKeychainAttribute extends Structure {
        public int tag; // SecKeychainAttrType
        public int length; // UInt32
        public Pointer data; // void*

        public SecKeychainAttribute() { }
        public SecKeychainAttribute(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("tag", "length", "data");
        }
    } // SecKeyChainAttribute

    public static class SecKeychainAttributeInfo extends Structure {
        public int count; // UInt32
        public Pointer tag; // UInt32*
        public Pointer format; // UInt32*

        public SecKeychainAttributeInfo() { }
        public SecKeychainAttributeInfo(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "tag", "format");
        }
    } // SecKeyChainAttribute

    public static class SecKeychainAttributeList extends Structure {
        public int count; // UInt32
        public Pointer attr; // SecKeychainAttribute*

        public SecKeychainAttributeList() { }
        public SecKeychainAttributeList(Pointer p) { super(p); }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "attr");
        }
    } // SecKeychainAttributeList

    public static class SecKeychainItemRef extends CFTypeRef {
        public SecKeychainItemRef() { }
        public SecKeychainItemRef(Pointer p) { super(p); }
    } // SecKeychainItemRef

    public static class SecKeychainRef extends CFTypeRef {
        public SecKeychainRef() { }
        public SecKeychainRef(Pointer p) { super(p); }
    } // SecKeychainRef

    public static class SecKeychainSearchRef extends CFTypeRef {
        public SecKeychainSearchRef() { }
        public SecKeychainSearchRef(Pointer p) { super(p); }
    } // SecKeychainSearchRef
} // Security
