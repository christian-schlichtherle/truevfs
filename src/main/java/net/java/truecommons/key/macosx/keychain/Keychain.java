/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx.keychain;

import net.java.truecommons.shed.Option;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.util.Map;

import static net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass.*;
import static net.java.truecommons.key.macosx.keychain.Security.*;

/**
 * A simple abstraction over Apple's Keychain Services API.
 * The methods in this class generally throw {@link KeychainException} to
 * report any errors when accessing this keychain.
 * <p>
 * Implementations need to safe for multi-threading.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("PackageVisibleInnerClass")
@ThreadSafe
public abstract class Keychain implements AutoCloseable {

    /**
     * Opens the default keychain.
     */
    public static Keychain open() throws KeychainException {
        return new KeychainImpl();
    }

    /**
     * Opens the specified keychain, creating it first if necessary.
     *
     * @param path the path name of the keychain.
     * @param password the password to use for creating the keychain.
     *        If {@code null}, then the user gets prompted for a password.
     */
    public static Keychain open(String path, @Nullable char[] password)
    throws KeychainException {
        return new KeychainImpl(path, Option.apply(password));
    }

    /**
     * Creates a new item in this keychain.
     *
     * @param id the class of the item to create.
     * @param attributes the attributes of the item to create.
     * @param secret the secret data of the item to create.
     */
    public abstract void createItem(
            ItemClass id,
            Map<AttributeClass, ByteBuffer> attributes,
            ByteBuffer secret)
    throws KeychainException;

    /**
     * Visits items in this keychain.
     * The {@code id} and {@code attributes} parameters filter the set of
     * items to visit.
     *
     * @param id the class of the items to visit.
     *        Use {@code null} or {@link ItemClass#ANY_ITEM} to visit items of
     *        any class.
     * @param attributes the attributes to visit.
     *        Use {@code null} or an empty map to visit items with any
     *        attributes.
     * @param visitor the visitor to apply to all matching items.
     */
    public abstract void visitItems(
            @Nullable ItemClass id,
            @Nullable Map<AttributeClass, ByteBuffer> attributes,
            Visitor visitor)
    throws KeychainException;

    /**
     * Deletes and closes this keychain.
     */
    public abstract void delete() throws KeychainException;

    /**
     * Closes this keychain.
     */
    @Override public abstract void close();

    /** A visitor for items in a keychain. */
    public interface Visitor { void visit(Item item) throws KeychainException; }

    /** An item in a keychain. */
    public interface Item {

        /**
         * Returns the class of this item.
         */
        ItemClass getItemClass() throws KeychainException;

        /**
         * Returns the value of the attribute with the given class.
         */
        @Nullable ByteBuffer getAttribute(AttributeClass id)
        throws KeychainException;

        /**
         * Sets the value of the attribute with the given class.
         */
        void setAttribute(AttributeClass id, @Nullable ByteBuffer value)
        throws KeychainException;

        /**
         * Returns all attributes of this item in a map.
         */
        Map<AttributeClass, ByteBuffer> getAttributeMap()
        throws KeychainException;

        /**
         * Puts the given attributes into this item.
         *
         * @param attributes the map of attributes to put into this item.
         */
        void putAttributeMap(Map<AttributeClass, ByteBuffer> attributes)
        throws KeychainException;

        /**
         * Returns the secret data of this item.
         */
        ByteBuffer getSecret() throws KeychainException;

        /**
         * Sets the secret data of this item.
         *
         * @param secret the secret data to set.
         */
        void setSecret(ByteBuffer secret) throws KeychainException;

        /**
         * Deletes this item.
         */
        void delete() throws KeychainException;
    }

    /** Enumerates classes of items in a keychain. */
    public enum ItemClass {

        /**
         * Use this to find any item class.
         */
        ANY_ITEM(CSSM_DL_DB_RECORD_ANY),

        CERT_ITEM(CSSM_DL_DB_RECORD_CRL,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ), // TODO: This is untested!

        CRL_ITEM(CSSM_DL_DB_RECORD_CRL,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ), // TODO: This is untested!

        POLICY_ITEM(CSSM_DL_DB_RECORD_POLICY,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ), // TODO: This is untested!

        GENERIC_ITEM(CSSM_DL_DB_RECORD_GENERIC,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ), // TODO: This is untested!

        PUBLIC_KEY(kSecPublicKeyItemClass,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ),

        PRIVATE_KEY(kSecPrivateKeyItemClass,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ),

        SYMMETRIC_KEY(kSecSymmetricKeyItemClass,
                KEY_CLASS, KEY_PRINT_NAME, KEY_ALIAS, KEY_PERMANENT,
                KEY_PRIVATE, KEY_MODIFIABLE, KEY_LABEL, KEY_APPLICATION_TAG,
                KEY_KEY_CREATOR, KEY_KEY_KYPE, KEY_KEY_SIZE_IN_BITS,
                KEY_EFFECTIVE_KEY_SIZE, KEY_START_DATE, KEY_END_DATE,
                KEY_SENSITIVE, KEY_ALWAYS_SENSITIVE, KEY_EXTRACTABLE,
                KEY_NEVER_EXTRACTABLE, KEY_ENCRYPT, DEY_DECRYPT, KEY_DERIVE,
                DEY_SIGN, KEY_VERIFY, KEY_SIGN_RECOVER, KEY_VERIFY_RECOVER,
                KEY_WRAP, KEY_UNWRAP
        ), // TODO: This is untested!

        ALL_KEY_ITEMS(CSSM_DL_DB_RECORD_ALL_KEYS),

        INTERNET_PASSWORD(kSecInternetPasswordItemClass,
                LABEL, DESCRIPTION, CREATION_DATE, MOD_DATE,
                COMMENT, CREATOR, TYPE, SCRIPT_CODE, INVISIBLE, NEGATIVE,
                CUSTOM_ICON,
                ACCOUNT, SECURITY_DOMAIN, SERVER, AUTHENTICATION_TYPE, PORT,
                PATH, PROTOCOL
        ),

        GENERIC_PASSWORD(kSecGenericPasswordItemClass,
                LABEL, DESCRIPTION, CREATION_DATE, MOD_DATE,
                COMMENT, CREATOR, TYPE, SCRIPT_CODE, INVISIBLE, NEGATIVE,
                CUSTOM_ICON,
                ACCOUNT, SERVICE, GENERIC
        ),

        APPLE_SHARE_PASSWORD(kSecAppleSharePasswordItemClass,
                LABEL, DESCRIPTION, CREATION_DATE, MOD_DATE,
                COMMENT, CREATOR, TYPE, SCRIPT_CODE, INVISIBLE, NEGATIVE,
                CUSTOM_ICON,
                ACCOUNT, VOLUME, ADDRESS, SIGNATURE, PROTOCOL
        ), // TODO: This is untested!

        CERTIFICATE(kSecCertificateItemClass,
                LABEL,
                CERTIFICATE_TYPE, CERTIFICATE_ENCODING, ALIAS
        );

        private final int tag;
        private final AttributeClass[] ids;

        static Option<ItemClass> lookup(final int tag) {
            switch (tag) {
                case CSSM_DL_DB_RECORD_ANY: assert false; return Option.some(ANY_ITEM);
                case CSSM_DL_DB_RECORD_CERT: assert false; return Option.some(CERT_ITEM);
                case CSSM_DL_DB_RECORD_CRL: assert false; return Option.some(CRL_ITEM);
                case CSSM_DL_DB_RECORD_POLICY: assert false; return Option.some(POLICY_ITEM);
                case CSSM_DL_DB_RECORD_GENERIC: assert false; return Option.some(GENERIC_ITEM);
                case kSecPublicKeyItemClass: return Option.some(PUBLIC_KEY);
                case kSecPrivateKeyItemClass: return Option.some(PRIVATE_KEY);
                case kSecSymmetricKeyItemClass: return Option.some(SYMMETRIC_KEY);
                case CSSM_DL_DB_RECORD_ALL_KEYS: assert false; return Option.some(ALL_KEY_ITEMS);
                case kSecInternetPasswordItemClass: return Option.some(INTERNET_PASSWORD);
                case kSecGenericPasswordItemClass: return Option.some(GENERIC_PASSWORD);
                case kSecAppleSharePasswordItemClass: return Option.some(APPLE_SHARE_PASSWORD);
                case kSecCertificateItemClass: return Option.some(CERTIFICATE);
                default: return Option.none();
            }
        }

        ItemClass(final int tag, final AttributeClass... ids) {
            this.tag = tag;
            this.ids = ids;
        }

        int getTag() { return tag; }

        /**
         * Returns a clone of the attribute classes supported by this item
         * class.
         */
        public AttributeClass[] getAttributeClasses() { return ids.clone(); }
    }

    /** Enumerates classes of attributes of items in a ketchain. */
    public enum AttributeClass {

        KEY_CLASS(kSecKeyKeyClass),
        KEY_PRINT_NAME(kSecKeyPrintName),
        KEY_ALIAS(kSecKeyAlias),
        KEY_PERMANENT(kSecKeyPermanent),
        KEY_PRIVATE(kSecKeyPrivate),
        KEY_MODIFIABLE(kSecKeyModifiable),
        KEY_LABEL(kSecKeyLabel),
        KEY_APPLICATION_TAG(kSecKeyApplicationTag),
        KEY_KEY_CREATOR(kSecKeyKeyCreator),
        KEY_KEY_KYPE(kSecKeyKeyType),
        KEY_KEY_SIZE_IN_BITS(kSecKeyKeySizeInBits),
        KEY_EFFECTIVE_KEY_SIZE(kSecKeyEffectiveKeySize),
        KEY_START_DATE(kSecKeyStartDate),
        KEY_END_DATE(kSecKeyEndDate),
        KEY_SENSITIVE(kSecKeySensitive),
        KEY_ALWAYS_SENSITIVE(kSecKeyAlwaysSensitive),
        KEY_EXTRACTABLE(kSecKeyExtractable),
        KEY_NEVER_EXTRACTABLE(kSecKeyNeverExtractable),
        KEY_ENCRYPT(kSecKeyEncrypt),
        DEY_DECRYPT(kSecKeyDecrypt),
        KEY_DERIVE(kSecKeyDerive),
        DEY_SIGN(kSecKeySign),
        KEY_VERIFY(kSecKeyVerify),
        KEY_SIGN_RECOVER(kSecKeySignRecover),
        KEY_VERIFY_RECOVER(kSecKeyVerifyRecover),
        KEY_WRAP(kSecKeyWrap),
        KEY_UNWRAP(kSecKeyUnwrap),

        CREATION_DATE(kSecCreationDateItemAttr),
        MOD_DATE(kSecModDateItemAttr),
        DESCRIPTION(kSecDescriptionItemAttr),
        COMMENT(kSecCommentItemAttr),
        CREATOR(kSecCreatorItemAttr),
        TYPE(kSecTypeItemAttr),
        SCRIPT_CODE(kSecScriptCodeItemAttr),
        LABEL(kSecLabelItemAttr),
        INVISIBLE(kSecInvisibleItemAttr),
        NEGATIVE(kSecNegativeItemAttr),
        CUSTOM_ICON(kSecCustomIconItemAttr),
        ACCOUNT(kSecAccountItemAttr),
        SERVICE(kSecServiceItemAttr),
        GENERIC(kSecGenericItemAttr),
        SECURITY_DOMAIN(kSecSecurityDomainItemAttr),
        SERVER(kSecServerItemAttr),
        AUTHENTICATION_TYPE(kSecAuthenticationTypeItemAttr),
        PORT(kSecPortItemAttr),
        PATH(kSecPathItemAttr),
        VOLUME(kSecVolumeItemAttr),
        ADDRESS(kSecAddressItemAttr),
        SIGNATURE(kSecSignatureItemAttr),
        PROTOCOL(kSecProtocolItemAttr),
        CERTIFICATE_TYPE(kSecCertificateType),
        CERTIFICATE_ENCODING(kSecCertificateEncoding),
        CRL_TYPE(kSecCrlType),
        CRL_ENCODING(kSecCrlEncoding),
        ALIAS(kSecAlias);

        private final int tag;

        static @Nullable AttributeClass lookup(final int tag) {
            switch (tag) {
                case kSecKeyKeyClass: return KEY_CLASS;
                case kSecKeyPrintName: return KEY_PRINT_NAME;
                case kSecKeyAlias: return KEY_ALIAS;
                case kSecKeyPermanent: return KEY_PERMANENT;
                case kSecKeyPrivate: return KEY_PRIVATE;
                case kSecKeyModifiable: return KEY_MODIFIABLE;
                case kSecKeyLabel: return KEY_LABEL;
                case kSecKeyApplicationTag: return KEY_APPLICATION_TAG;
                case kSecKeyKeyCreator: return KEY_KEY_CREATOR;
                case kSecKeyKeyType: return KEY_KEY_KYPE;
                case kSecKeyKeySizeInBits: return KEY_KEY_SIZE_IN_BITS;
                case kSecKeyEffectiveKeySize: return KEY_EFFECTIVE_KEY_SIZE;
                case kSecKeyStartDate: return KEY_START_DATE;
                case kSecKeyEndDate: return KEY_END_DATE;
                case kSecKeySensitive: return KEY_SENSITIVE;
                case kSecKeyAlwaysSensitive: return KEY_ALWAYS_SENSITIVE;
                case kSecKeyExtractable: return KEY_EXTRACTABLE;
                case kSecKeyNeverExtractable: return KEY_NEVER_EXTRACTABLE;
                case kSecKeyEncrypt: return KEY_ENCRYPT;
                case kSecKeyDecrypt: return DEY_DECRYPT;
                case kSecKeyDerive: return KEY_DERIVE;
                case kSecKeySign: return DEY_SIGN;
                case kSecKeyVerify: return KEY_VERIFY;
                case kSecKeySignRecover: return KEY_SIGN_RECOVER;
                case kSecKeyVerifyRecover: return KEY_VERIFY_RECOVER;
                case kSecKeyWrap: return KEY_WRAP;
                case kSecKeyUnwrap: return KEY_UNWRAP;

                case kSecCreationDateItemAttr: return CREATION_DATE;
                case kSecModDateItemAttr: return MOD_DATE;
                case kSecDescriptionItemAttr: return DESCRIPTION;
                case kSecCommentItemAttr: return COMMENT;
                case kSecCreatorItemAttr: return CREATOR;
                case kSecTypeItemAttr: return TYPE;
                case kSecScriptCodeItemAttr: return SCRIPT_CODE;
                case kSecLabelItemAttr: return LABEL;
                case kSecInvisibleItemAttr: return INVISIBLE;
                case kSecNegativeItemAttr: return NEGATIVE;
                case kSecCustomIconItemAttr: return CUSTOM_ICON;
                case kSecAccountItemAttr: return ACCOUNT;
                case kSecServiceItemAttr: return SERVICE;
                case kSecGenericItemAttr: return GENERIC;
                case kSecSecurityDomainItemAttr: return SECURITY_DOMAIN;
                case kSecServerItemAttr: return SERVER;
                case kSecAuthenticationTypeItemAttr: return AUTHENTICATION_TYPE;
                case kSecPortItemAttr: return PORT;
                case kSecPathItemAttr: return PATH;
                case kSecVolumeItemAttr: return VOLUME;
                case kSecAddressItemAttr: return ADDRESS;
                case kSecSignatureItemAttr: return SIGNATURE;
                case kSecProtocolItemAttr: return PROTOCOL;
                case kSecCertificateType: return CERTIFICATE_TYPE;
                case kSecCertificateEncoding: return CERTIFICATE_ENCODING;
                case kSecCrlType: return CRL_TYPE;
                case kSecCrlEncoding: return CRL_ENCODING;
                case kSecAlias: return ALIAS;
                default: return null;
            }
        }

        AttributeClass(final int tag) { this.tag = tag; }

        int getTag() { return tag; }
    }
}
