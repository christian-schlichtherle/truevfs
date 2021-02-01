/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.sample;

import global.namespace.truevfs.access.TArchiveDetector;
import global.namespace.truevfs.access.TConfig;
import global.namespace.truevfs.comp.key.spec.KeyManagerMap;
import global.namespace.truevfs.comp.key.spec.UnknownKeyException;
import global.namespace.truevfs.comp.key.spec.common.AesKeyStrength;
import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;
import global.namespace.truevfs.comp.key.spec.prompting.PromptingKey.Controller;
import global.namespace.truevfs.comp.key.spec.prompting.PromptingKey.View;
import global.namespace.truevfs.comp.key.spec.prompting.PromptingKeyManagerMap;
import global.namespace.truevfs.comp.zip.WinZipAesParameters;
import global.namespace.truevfs.comp.zip.ZipKeyException;
import global.namespace.truevfs.comp.zipdriver.AbstractZipDriverEntry;
import global.namespace.truevfs.comp.zipdriver.ZipDriver;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsModel;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Provides static utility methods to set passwords for WinZip AES encrypted
 * ZIP entries programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @author Christian Schlichtherle
 */
public final class KeyManagement {

    private KeyManagement() {
    }

    static void install(TArchiveDetector detector) {
// START SNIPPET: install
        TConfig.current().setArchiveDetector(detector);
// END SNIPPET: install
    }

// START SNIPPET: newArchiveDetector1

    /**
     * Returns a new archive detector which uses the given password for all
     * WinZip AES encrypted ZIP entries with the given list of extensions.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param extensions A list of file name extensions which shall identify
     *                   prospective archive files.
     *                   This must not be {@code null} and must not be empty.
     * @param password   the password byte array to be copied for internal use.
     *                   The bytes should be limited to seven bits only, see
     *                   {@link WinZipAesParameters}.
     * @param detector   the archive detector to decorate.
     * @return A new archive detector which uses the given password for all
     * WinZip AES encrypted ZIP entries with the given list of
     * extensions.
     */
    public static TArchiveDetector newArchiveDetector1(String extensions, byte[] password, TArchiveDetector detector) {
        return new TArchiveDetector(extensions, Optional.of(new CustomZipDriver1(password)), detector);
    }

    private static final class CustomZipDriver1 extends ZipDriver {

        final WinZipAesParameters param;

        CustomZipDriver1(byte[] password) {
            param = new CustomWinZipAesParameters(password);
        }

        @Override
        protected WinZipAesParameters zipCryptoParameters(
                FsModel model,
                Charset charset) {
            // If you need the URI of the particular archive file, then call
            // model.getMountPoint().toUri().
            // If you need a more user friendly form of this URI, then call
            // model.getMountPoint().toHierarchicalUri().

            // Let's not use the key manager but instead our custom parameters.
            return param;
        }

        @Override
        public FsController decorate(FsController controller) {
            // This is a minor improvement: The default implementation decorates
            // the default file system controller chain with a package private
            // file system controller which uses the key manager to keep track
            // of the encryption parameters.
            // Because we are not using the key manager, we don't need this
            // special purpose file system controller and can simply return the
            // given file system controller chain instead.
            return controller;
        }

        @Override
        protected boolean rdc(
                AbstractZipDriverEntry input,
                AbstractZipDriverEntry output) {
            // Because we are using the same encryption key for all entries
            // of our custom archive file format we do NOT need to process the
            // entries according to the following pipeline when copying them:
            // decrypt(inputKey) -> inflate() -> deflate() -> encrypt(outputKey)

            // This reduces the processing pipeline to a simple copy operation
            // and is a DRASTIC performance improvement, e.g. when compacting
            // an archive file.
            return true;

            // This is the default implementation - try to see the difference.
            //return input.isEncrypted() || output.isEncrypted();
        }
    } // CustomZipDriver1

    private static final class CustomWinZipAesParameters implements WinZipAesParameters {

        final byte[] password;

        CustomWinZipAesParameters(final byte[] password) {
            this.password = password.clone();
        }

        @Override
        public byte[] getWritePassword(String name)
                throws ZipKeyException {
            return password.clone();
        }

        @Override
        public byte[] getReadPassword(String name, boolean invalid)
                throws ZipKeyException {
            if (invalid)
                throw new ZipKeyException(name + " (invalid password)");
            return password.clone();
        }

        @Override
        public AesKeyStrength getKeyStrength(String arg0)
                throws ZipKeyException {
            return AesKeyStrength.BITS_128;
        }

        @Override
        public void setKeyStrength(String name, AesKeyStrength keyStrength)
                throws ZipKeyException {
            // We have been using only 128 bits to create archive entries.
            assert AesKeyStrength.BITS_128 == keyStrength;
        }
    } // CustomWinZipAesParameters
// END SNIPPET: newArchiveDetector1

// START SNIPPET: newArchiveDetector2

    /**
     * Returns a new archive detector which uses the given password for all
     * WinZip AES encrypted ZIP entries with the given list of extensions.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param extensions A list of file name extensions which shall identify
     *                   prospective archive files.
     *                   This must not be {@code null} and must not be empty.
     * @param password   the password char array to be copied for internal use.
     *                   The characters should be limited to US-ASCII, see
     *                   {@link WinZipAesParameters}.
     * @param detector   the archive detector to decorate.
     * @return A new archive detector which uses the given password for all
     * WinZip AES encrypted ZIP entries with the given list of
     * extensions.
     */
    public static TArchiveDetector newArchiveDetector2(String extensions, char[] password, TArchiveDetector detector) {
        return new TArchiveDetector(extensions, Optional.of(new CustomZipDriver2(password)), detector);
    }

    private static final class CustomZipDriver2 extends ZipDriver {

        final KeyManagerMap map;

        CustomZipDriver2(char[] password) {
            this.map = new PromptingKeyManagerMap(
                    AesPbeParameters.class,
                    new CustomView(password));
        }

        @Override
        public KeyManagerMap getKeyManagerMap() {
            return map;
        }
    } // CustomZipDriver2

    private static final class CustomView
            implements View<AesPbeParameters> {
        final char[] password;

        CustomView(char[] password) {
            this.password = password.clone();
        }

        /**
         * You need to create a new key because the key manager may eventually
         * reset it when the archive file gets moved or deleted.
         */
        private AesPbeParameters newKey() {
            AesPbeParameters param = new AesPbeParameters();
            param.setPassword(password);
            param.setKeyStrength(AesKeyStrength.BITS_128);
            return param;
        }

        @Override
        public void promptKeyForWriting(Controller<AesPbeParameters> controller)
                throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKeyClone(newKey());
        }

        @Override
        public void promptKeyForReading(
                Controller<AesPbeParameters> controller,
                boolean invalid)
                throws UnknownKeyException {
            if (invalid) throw new UnknownKeyException();
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKeyClone(newKey());
        }
    } // CustomView
// END SNIPPET: newArchiveDetector2
}