/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes.sample;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.raes.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Arrays;

/**
 * Provides static utility methods to set passwords for RAES encrypted ZIP
 * files programmatically.
 * Use whatever approach fits your needs best when you want to set the password
 * programmatically instead of prompting the user for a key by means of the
 * default Swing or Console based user interfaces.
 *
 * @deprecated This sample code should <em>not</em> be considered part of the
 *             public API.
 *             If you want to use it, then copy it, don't call it!
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
@Deprecated
@DefaultAnnotation(NonNull.class)
public final class KeyManagement {

    /** You cannot instantiate this class. */
    private KeyManagement() {
    }

// START SNIPPET: setPassword
    /**
     * Sets the password for an individual RAES encrypted ZIP file a priori.
     * <p>
     * This method uses the default {@link KeyManager} for
     * {@link AesCipherParameters} and the default {@link KeyProvider} for the
     * given {@code file} in order to set the given {@code password}.
     * <p>
     * As another side effect, the key strength of the
     * {@link AesCipherParameters} is set to the maximum 256 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param file the TZP archive file to set the password for.
     * @param password the password char array to be copied for internal use.
     */
    public static void setPassword(TFile file, char[] password) {
        if (!file.isArchive())
            throw new IllegalArgumentException(file + " (not an archive file)");
        AesCipherParameters params = new AesCipherParameters();
        params.setPassword(password);
        Arrays.fill(password, (char) 0);
        KeyManagerLocator
                .SINGLETON
                .get(AesCipherParameters.class)
                .getKeyProvider(file.toFsPath().toHierarchicalUri()) // http://java.net/jira/browse/TRUEZIP-72
                .setKey(params);
    }
// END SNIPPET: setPassword

// START SNIPPET: setAllPasswords
    /**
     * Sets a common password for all RAES encrypted ZIP files ad hoc.
     * <p>
     * This method installs a custom {@link TArchiveDetector} as the
     * {@link TFile#setDefaultArchiveDetector default archive detector}.
     * This side effect will apply only to subsequently created {@link TFile}
     * objects.
     * The custom archive detector decorates {@link TArchiveDetector#ALL}
     * with a custom archive driver for the canonical archive file suffixes
     * {@code "tzp|zip.rae|zip.raes"}.
     * The custom archive driver uses a custom {@link View} implementation as
     * its {@link PromptingKeyManagerService}.
     * <p>
     * As another side effect, the key strength of all
     * {@link AesCipherParameters} is set to the maximum 256 bits.
     * <p>
     * A protective copy of the given password char array is made.
     * It's recommended to overwrite the parameter array with any non-password
     * data after calling this method.
     *
     * @param password the password char array to be copied for internal use.
     */
    public static void setAllPasswords(char[] password) {
        TFile.setDefaultArchiveDetector(
                new TArchiveDetector(
                    TArchiveDetector.ALL,
                    "tzp|zip.rae|zip.raes",
                    new SafeZipRaesDriver(
                        IOPoolLocator.SINGLETON,
                        new PromptingKeyManagerService(
                            new SimpleView(password)))));
    }
    
    private static class SimpleView
    implements PromptingKeyProvider.View<AesCipherParameters> {
        private AesCipherParameters params = new AesCipherParameters();
        
        SimpleView(char[] password) {
            if (null == password)
                throw new NullPointerException();
            params.setPassword(password);
        }
        
        @Override
        public void promptWriteKey(Controller<AesCipherParameters> controller) {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            controller.setKey(params);
        }
        
        @Override
        public void promptReadKey(  Controller<AesCipherParameters> controller,
                                    boolean invalid) {
            // You might as well call controller.getResource() here in order to
            // programmatically set the parameters for individual resource URIs.
            controller.setKey(params);
        }
    } // SimpleView
// END SNIPPET: setAllPasswords
}
