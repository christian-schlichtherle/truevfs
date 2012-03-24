/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.sample;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.fs.archive.zip.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriverEntry;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.sl.IOPoolLocator;
import de.schlichtherle.truezip.zip.WinZipAesParameters;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import de.schlichtherle.truezip.zip.ZipKeyException;
import java.nio.charset.Charset;

/**
 * Provides static utility methods to set passwords for WinZip AES encrypted
 * ZIP entries programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @author  Christian Schlichtherle
 */
public final class KeyManagement {

    /** You cannot instantiate this class. */
    private KeyManagement() {
    }

    static void install(TArchiveDetector detector) {
// START SNIPPET: install
        TConfig.get().setArchiveDetector(detector);
// END SNIPPET: install
    }

// START SNIPPET: newArchiveDetector1
    /**
     * Returns a new archive detector which uses the given password for all
     * WinZip AES encrypted ZIP entries with the given list of suffixes.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  delegate the file system driver provider to decorate.
     * @param  suffixes A list of file name suffixes which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password byte array to be copied for internal use.
     *         The bytes should be limited to seven bits only, see
     *         {@link WinZipAesParameters}.
     * @return A new archive detector which uses the given password for all
     *         WinZip AES encrypted ZIP entries with the given list of suffixes.
     */
    public static TArchiveDetector newArchiveDetector1(
            FsDriverProvider delegate,
            String suffixes,
            byte[] password) {
        return new TArchiveDetector(delegate,
                suffixes, new CustomJarDriver1(password));
    }
    
    private static final class CustomJarDriver1 extends JarDriver {
        final ZipCryptoParameters param;
        
        CustomJarDriver1(byte[] password) {
            super(IOPoolLocator.SINGLETON);
            param = new CustomWinZipAesParameters(password);
        }
        
        @Override
        protected ZipCryptoParameters zipCryptoParameters(
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
        protected FsController<?> decorate(FsController<?> controller) {
            // This is a minor improvement: The default implementation decorates
            // the default file system controller chain with a package private
            // file system controller which keeps track of the encryption keys.
            // Because we are not using the key manager, we don't need this
            // special purpose file system controller and can use the default
            // file system controller chain instead.
            return controller;
        }
        
        @Override
        protected boolean process(
                ZipDriverEntry input,
                ZipDriverEntry output) {
            // Because we are using the same encryption key for all entries
            // of our custom archive file format we do NOT need to process the
            // entries according to the following pipeline when copying them:
            // decrypt(inputKey) -> inflate() -> deflate() -> encrypt(outputKey)
            
            // This reduces the processing pipeline to a simple copy operation
            // and is a DRASTIC performance improvement, e.g. when compacting
            // an archive file.
            return false;
            
            // This is the default implementation - try to see the difference.
            //return input.isEncrypted() || output.isEncrypted();
        }
    } // CustomJarDriver1
    
    private static final class CustomWinZipAesParameters
    implements WinZipAesParameters {
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
     * WinZip AES encrypted ZIP entries with the given list of suffixes.
     * <p>
     * When used for encryption, the AES key strength will be set to 128 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param  delegate the file system driver provider to decorate.
     * @param  suffixes A list of file name suffixes which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  password the password char array to be copied for internal use.
     *         The characters should be limited to US-ASCII, see
     *         {@link WinZipAesParameters}.
     * @return A new archive detector which uses the given password for all
     *         WinZip AES encrypted ZIP entries with the given list of suffixes.
     */
    public static TArchiveDetector newArchiveDetector2(
            FsDriverProvider delegate,
            String suffixes,
            char[] password) {
        return new TArchiveDetector(delegate,
                    suffixes, new CustomJarDriver2(password));
    }
    
    private static final class CustomJarDriver2 extends JarDriver {
        final KeyManagerProvider provider;
        
        CustomJarDriver2(char[] password) {
            super(IOPoolLocator.SINGLETON);
            this.provider = new PromptingKeyManagerService(
                    new CustomView(password));
        }
        
        @Override
        protected KeyManagerProvider getKeyManagerProvider() {
            return provider;
        }
    } // CustomJarDriver2
    
    private static final class CustomView
    implements PromptingKeyProvider.View<AesPbeParameters> {
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
        public void promptWriteKey(Controller<AesPbeParameters> controller)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(newKey());
        }
        
        @Override
        public void promptReadKey(  Controller<AesPbeParameters> controller,
                                    boolean invalid)
        throws UnknownKeyException {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            // Note that this would typically return the hierarchical URI of
            // the archive file unless ZipDriver.mountPointUri(FsModel) would
            // have been overridden.
            controller.setKey(newKey());
        }
    } // CustomView
// END SNIPPET: newArchiveDetector2
}