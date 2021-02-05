/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.macos.keychain;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import global.namespace.truevfs.commons.key.macos.keychain.CoreFoundation.CFStringRef;
import global.namespace.truevfs.commons.key.macos.keychain.CoreFoundation.CFTypeRef;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static global.namespace.truevfs.commons.key.macos.keychain.CoreFoundation.CFRelease;
import static global.namespace.truevfs.commons.key.macos.keychain.CoreFoundation.decode;

/**
 * Exposes some parts of Apple's native Keychain Services API.
 *
 * @author Christian Schlichtherle
 * @see <a href="https://developer.apple.com/library/mac/#documentation/security/Reference/keychainservices/Reference/reference.html">Mac Developer Library: Keychain Services Reference</a>
 */
@SuppressWarnings({"PackageVisibleInnerClass", "PackageVisibleField"})
final class Security {

    static {
        Native.register(Security.class.getSimpleName());
    }

    private Security() {
    }

    //
    // Constants:
    //

    static final int
            errSecSuccess = 0,
            errSecUnimplemented = -4,
            errSecInvalidKeychain = -25295,
            errSecDuplicateKeychain = -25296,
            errSecDuplicateItem = -25299,
            errSecItemNotFound = -25300;

    static final int
            CSSM_DL_DB_RECORD_ANY = 0xA,
            CSSM_DL_DB_RECORD_CERT = 0xA + 1,
            CSSM_DL_DB_RECORD_CRL = 0xA + 2,
            CSSM_DL_DB_RECORD_POLICY = 0xA + 3,
            CSSM_DL_DB_RECORD_GENERIC = 0xA + 4,
            kSecPublicKeyItemClass = 0xA + 5,
            kSecPrivateKeyItemClass = 0xA + 6,
            kSecSymmetricKeyItemClass = 0xA + 7,
            CSSM_DL_DB_RECORD_ALL_KEYS = 0xA + 8,
            kSecInternetPasswordItemClass = (('i' << 8 | 'n') << 8 | 'e') << 8 | 't',
            kSecGenericPasswordItemClass = (('g' << 8 | 'e') << 8 | 'n') << 8 | 'p',
            kSecAppleSharePasswordItemClass = (('a' << 8 | 's') << 8 | 'h') << 8 | 'p',
            kSecCertificateItemClass = 0x8000_1000;

    static final int
            kSecKeyKeyClass = 0,
            kSecKeyPrintName = 1,
            kSecKeyAlias = 2,
            kSecKeyPermanent = 3,
            kSecKeyPrivate = 4,
            kSecKeyModifiable = 5,
            kSecKeyLabel = 6,
            kSecKeyApplicationTag = 7,
            kSecKeyKeyCreator = 8,
            kSecKeyKeyType = 9,
            kSecKeyKeySizeInBits = 10,
            kSecKeyEffectiveKeySize = 11,
            kSecKeyStartDate = 12,
            kSecKeyEndDate = 13,
            kSecKeySensitive = 14,
            kSecKeyAlwaysSensitive = 15,
            kSecKeyExtractable = 16,
            kSecKeyNeverExtractable = 17,
            kSecKeyEncrypt = 18,
            kSecKeyDecrypt = 19,
            kSecKeyDerive = 20,
            kSecKeySign = 21,
            kSecKeyVerify = 22,
            kSecKeySignRecover = 23,
            kSecKeyVerifyRecover = 24,
            kSecKeyWrap = 25,
            kSecKeyUnwrap = 26;

    static final int
            kSecCreationDateItemAttr = (('c' << 8 | 'd') << 8 | 'a') << 8 | 't',
            kSecModDateItemAttr = (('m' << 8 | 'd') << 8 | 'a') << 8 | 't',
            kSecDescriptionItemAttr = (('d' << 8 | 'e') << 8 | 's') << 8 | 'c',
            kSecCommentItemAttr = (('i' << 8 | 'c') << 8 | 'm') << 8 | 't',
            kSecCreatorItemAttr = (('c' << 8 | 'r') << 8 | 't') << 8 | 'r',
            kSecTypeItemAttr = (('t' << 8 | 'y') << 8 | 'p') << 8 | 'e',
            kSecScriptCodeItemAttr = (('s' << 8 | 'c') << 8 | 'r') << 8 | 'p',
            kSecLabelItemAttr = (('l' << 8 | 'a') << 8 | 'b') << 8 | 'l',
            kSecInvisibleItemAttr = (('i' << 8 | 'n') << 8 | 'v') << 8 | 'i',
            kSecNegativeItemAttr = (('n' << 8 | 'e') << 8 | 'g') << 8 | 'a',
            kSecCustomIconItemAttr = (('c' << 8 | 'u') << 8 | 's') << 8 | 'i',
            kSecAccountItemAttr = (('a' << 8 | 'c') << 8 | 'c') << 8 | 't',
            kSecServiceItemAttr = (('s' << 8 | 'v') << 8 | 'c') << 8 | 'e',
            kSecGenericItemAttr = (('g' << 8 | 'e') << 8 | 'n') << 8 | 'a',
            kSecSecurityDomainItemAttr = (('s' << 8 | 'd') << 8 | 'm') << 8 | 'n',
            kSecServerItemAttr = (('s' << 8 | 'r') << 8 | 'v') << 8 | 'r',
            kSecAuthenticationTypeItemAttr = (('a' << 8 | 't') << 8 | 'y') << 8 | 'p',
            kSecPortItemAttr = (('p' << 8 | 'o') << 8 | 'r') << 8 | 't',
            kSecPathItemAttr = (('p' << 8 | 'a') << 8 | 't') << 8 | 'h',
            kSecVolumeItemAttr = (('v' << 8 | 'l') << 8 | 'm') << 8 | 'e',
            kSecAddressItemAttr = (('a' << 8 | 'd') << 8 | 'd') << 8 | 'r',
            kSecSignatureItemAttr = (('s' << 8 | 's') << 8 | 'i') << 8 | 'g',
            kSecProtocolItemAttr = (('p' << 8 | 't') << 8 | 'c') << 8 | 'l',
            kSecCertificateType = (('c' << 8 | 't') << 8 | 'y') << 8 | 'p',
            kSecCertificateEncoding = (('c' << 8 | 'e') << 8 | 'n') << 8 | 'c',
            kSecCrlType = (('c' << 8 | 'r') << 8 | 't') << 8 | 'p',
            kSecCrlEncoding = (('c' << 8 | 'r') << 8 | 'n') << 8 | 'c',
            kSecAlias = (('a' << 8 | 'l') << 8 | 'i') << 8 | 's';

    static final int kSecFormatUnknown = 0;

    //
    // Functions:
    //

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1400743-seckeychaincopydefault?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainCopyDefault(
            PointerByReference keychain
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1401214-seckeychaincreate?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainCreate(
            String pathName,
            int passwordLength,
            @Nullable ByteBuffer password,
            boolean promptUser,
            @Nullable SecAccessRef initialAccess,
            PointerByReference keychain
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1395206-seckeychaindelete?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainDelete(
            SecKeychainRef keychainOrArray
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1400528-seckeychainitemcopyattributesand?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainItemCopyAttributesAndData(
            SecKeychainItemRef itemRef,
            @Nullable SecKeychainAttributeInfo info,
            @Nullable IntByReference itemClass,
            @Nullable PointerByReference attrList,
            @Nullable IntByReference length,
            @Nullable PointerByReference outData
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1393225-seckeychainitemcreatefromcontent?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainItemCreateFromContent(
            int itemClass,
            SecKeychainAttributeList attrList,
            int length,
            @Nullable ByteBuffer data,
            @Nullable SecKeychainRef keychainRef,
            @Nullable SecAccessRef initialAccess,
            @Nullable PointerByReference itemRef
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1400090-seckeychainitemdelete?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainItemDelete(
            SecKeychainItemRef itemRef
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1397736-seckeychainitemfreeattributesand?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainItemFreeAttributesAndData(
            @Nullable SecKeychainAttributeList attrList,
            @Nullable Pointer data // void*
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1399963-seckeychainitemmodifyattributesa?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainItemModifyAttributesAndData(
            SecKeychainItemRef itemRef,
            @Nullable SecKeychainAttributeList attrList,
            int length,
            @Nullable ByteBuffer data
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1396431-seckeychainopen?language=objc">Apple Developer Documentation</a>
     */
    static native int SecKeychainOpen(
            String pathName,
            @Nullable PointerByReference keychain
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1515362-seckeychainsearchcopynext?language=objc">Apple Developer Documentation</a>
     * @deprecated Use SecItemCopyMatching.
     */
    @Deprecated
    static native int SecKeychainSearchCopyNext(
            SecKeychainSearchRef searchRef,
            @Nullable PointerByReference itemRef
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1515366-seckeychainsearchcreatefromattri?language=objc">Apple Developer Documentation</a>
     * @deprecated Use SecItemCopyMatching.
     */
    @Deprecated
    static native int SecKeychainSearchCreateFromAttributes(
            @Nullable CFTypeRef keychainOrArray,
            int itemClass,
            @Nullable SecKeychainAttributeList attrList,
            PointerByReference searchRef
    );

    /**
     * @see <a href="https://developer.apple.com/documentation/security/1394686-seccopyerrormessagestring?language=objc">Apple Developer Documentation</a>
     */
    static native CFStringRef SecCopyErrorMessageString(
            int status,
            @Nullable Pointer reserved
    );

    //
    // Utilities:
    //

    static String message(final int status) {
        final CFStringRef stringRef = SecCopyErrorMessageString(status, null);
        try {
            return decode(stringRef);
        } finally {
            CFRelease(stringRef);
        }
    }

    //
    // Types:
    //

    public static final class SecAccessRef extends CFTypeRef {

        public SecAccessRef() {
        }

        SecAccessRef(Pointer p) {
            super(p);
        }
    }

    public static final class SecKeychainAttribute extends Structure {

        public int tag; // SecKeychainAttrType

        public int length; // UInt32

        public Pointer data; // void*

        public SecKeychainAttribute() {
        }

        SecKeychainAttribute(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("tag", "length", "data");
        }
    }

    public static final class SecKeychainAttributeInfo extends Structure {

        public int count; // UInt32

        public Pointer tag; // UInt32*

        public Pointer format; // UInt32*

        public SecKeychainAttributeInfo() {
        }

        SecKeychainAttributeInfo(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "tag", "format");
        }
    }

    public static final class SecKeychainAttributeList extends Structure {

        public int count; // UInt32

        public Pointer attr; // SecKeychainAttribute*

        public SecKeychainAttributeList() {
        }

        SecKeychainAttributeList(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("count", "attr");
        }
    }

    public static final class SecKeychainItemRef extends CFTypeRef {

        public SecKeychainItemRef() {
        }

        SecKeychainItemRef(Pointer p) {
            super(p);
        }
    }

    public static final class SecKeychainRef extends CFTypeRef {

        public SecKeychainRef() {
        }

        SecKeychainRef(Pointer p) {
            super(p);
        }
    }

    public static final class SecKeychainSearchRef extends CFTypeRef {

        public SecKeychainSearchRef() {
        }

        SecKeychainSearchRef(Pointer p) {
            super(p);
        }
    }
}
