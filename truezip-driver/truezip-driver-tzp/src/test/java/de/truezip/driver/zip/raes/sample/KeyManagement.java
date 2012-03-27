/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.sample;

import de.truezip.driver.zip.raes.SafeZipRaesDriver;
import de.truezip.driver.zip.raes.crypto.RaesKeyException;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.Type0RaesParameters;
import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDriverProvider;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.sl.IOPoolLocator;
import de.truezip.key.param.AesKeyStrength;

/**
 * Provides static utility methods to set passwords for RAES encrypted ZIP
 * files programmatically.
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
     * RAES encrypted ZIP files with the given list of suffixes.
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
     * @return A new archive detector which uses the given password for all
     *         RAES encrypted ZIP files with the given list of suffixes.
     */
    public static TArchiveDetector newArchiveDetector(
            FsDriverProvider delegate,
            String suffixes,
            char[] password) {
        return new TArchiveDetector(delegate,
                suffixes, new CustomZipRaesDriver(password));
    }
    
    private static final class CustomZipRaesDriver extends SafeZipRaesDriver {
        final RaesParameters param;
        
        CustomZipRaesDriver(char[] password) {
            super(IOPoolLocator.SINGLETON);
            param = new CustomRaesParameters(password);
        }
        
        @Override
        protected RaesParameters raesParameters(FsModel model) {
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
    } // CustomZipRaesDriver
    
    private static final class CustomRaesParameters
    implements Type0RaesParameters {
        final char[] password;
        
        CustomRaesParameters(final char[] password) {
            this.password = password.clone();
        }
        
        @Override
        public char[] getWritePassword()
        throws RaesKeyException {
            return password.clone();
        }
        
        @Override
        public char[] getReadPassword(boolean invalid)
        throws RaesKeyException {
            if (invalid)
                throw new RaesKeyException("Invalid password!");
            return password.clone();
        }
        
        @Override
        public AesKeyStrength getKeyStrength()
        throws RaesKeyException {
            return AesKeyStrength.BITS_128;
        }
        
        @Override
        public void setKeyStrength(AesKeyStrength keyStrength)
        throws RaesKeyException {
            // We have been using only 128 bits to create archive entries.
            assert AesKeyStrength.BITS_128 == keyStrength;
        }
    } // CustomRaesParameters
// END SNIPPET: newArchiveDetector
}