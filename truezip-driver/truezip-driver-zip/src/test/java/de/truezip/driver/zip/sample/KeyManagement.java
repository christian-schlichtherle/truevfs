/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.sample;

import de.truezip.driver.zip.JarDriver;
import de.truezip.driver.zip.ZipDriverEntry;
import de.truezip.driver.zip.io.WinZipAesParameters;
import de.truezip.driver.zip.io.ZipCryptoParameters;
import de.truezip.driver.zip.io.ZipKeyException;
import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDriverProvider;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.sl.IOPoolLocator;
import de.truezip.key.param.AesKeyStrength;
import java.nio.charset.Charset;

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

    /* Can't touch this - hammer time! */
    private KeyManagement() { }

    static void install(TArchiveDetector detector) {
// START SNIPPET: install
        TConfig.get().setArchiveDetector(detector);
// END SNIPPET: install
    }

// START SNIPPET: newArchiveDetector
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
    public static TArchiveDetector newArchiveDetector(
            FsDriverProvider delegate,
            String suffixes,
            byte[] password) {
        return new TArchiveDetector(delegate,
                suffixes, new CustomJarDriver(password));
    }
    
    private static final class CustomJarDriver extends JarDriver {
        final ZipCryptoParameters param;
        
        CustomJarDriver(byte[] password) {
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
            // file system controller which uses the key manager to keep track
            // of the encryption keys.
            // Because we are not using the key manager, we don't need this
            // special purpose file system controller and can simply return the
            // given file system controller chain instead.
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
    } // CustomJarDriver
    
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
// END SNIPPET: newArchiveDetector
}