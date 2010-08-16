/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.util.zip;

import de.schlichtherle.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.crypto.io.raes.RaesParameters;
import de.schlichtherle.crypto.io.raes.Type0RaesParameters;
import de.schlichtherle.crypto.io.raes.RaesReadOnlyFile;
import de.schlichtherle.io.rof.ReadOnlyFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Tests compression and encryption of data.
 * Subclasses must override {@link #setUp}.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class RaesZipTestCase extends ZipTestCase {

    private static final Logger logger
            = Logger.getLogger(RaesZipTestCase.class.getName());

    /** Cipher text shorter than this gets authenticated. */
    private static int AUTHENTICATION_TRIGGER = 512 * 1024;
    
    private static final String PASSWD = "passwd";

    private static final int[] keyStrengths = {
        Type0RaesParameters.KEY_STRENGTH_128,
        Type0RaesParameters.KEY_STRENGTH_192,
        Type0RaesParameters.KEY_STRENGTH_256,
    };

    private static int createKeyStrength() {
        final int keyStrength = keyStrengths[rnd.nextInt(keyStrengths.length)];
        //final int keyStrength = KEY_STRENGTH_ULTRA;
        logger.fine("Using " + (128 + keyStrength * 64) + " bits cipher key.");
        return keyStrength;
    }

    private static final RaesParameters raesParameters = new Type0RaesParameters() {
        public char[] getOpenPasswd() {
            return PASSWD.toCharArray();
        }

        public void invalidOpenPasswd() {
        }

        public char[] getCreatePasswd() {
            return PASSWD.toCharArray();
        }

        public int getKeyStrength() {
            return createKeyStrength();
        }
        
        public void setKeyStrength(int keyStrength) {
        }
    };
    
    /** Creates a new instance of RandomMessageZipTest */
    public RaesZipTestCase(String testName) {
        super(testName);
    }

    protected ZipOutputStream createZipOutputStream(final OutputStream out)
    throws IOException {
        final RaesOutputStream ros = RaesOutputStream.getInstance(
                out, raesParameters);
        try {
            return new ZipOutputStream(ros);
        } catch (NullPointerException exc) {
            ros.close();
            throw exc;
        }
    }

    protected ZipOutputStream createZipOutputStream(
            final OutputStream out, final String encoding)
    throws IOException, UnsupportedEncodingException {
        final RaesOutputStream ros = RaesOutputStream.getInstance(
                out, raesParameters);
        try {
            return new ZipOutputStream(ros, encoding);
        } catch (NullPointerException exc) {
            ros.close();
            throw exc;
        } catch (UnsupportedEncodingException exc) {
            ros.close();
            throw exc;
        }
    }

    protected ZipFile createZipFile(final String name)
    throws IOException {
        return new ZipFile(name) {
            protected ReadOnlyFile createReadOnlyFile(final File file)
            throws IOException {
                final RaesReadOnlyFile rof = RaesReadOnlyFile.getInstance(
                        file, raesParameters);
                if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                    rof.authenticate();
                return rof;
            }
        };
    }

    protected ZipFile createZipFile(
            final String name, final String encoding)
    throws IOException, UnsupportedEncodingException {
        return new ZipFile(name, encoding) {
            protected ReadOnlyFile createReadOnlyFile(final File file)
            throws IOException {
                final RaesReadOnlyFile rof = RaesReadOnlyFile.getInstance(
                        file, raesParameters);
                if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                    rof.authenticate();
                return rof;
            }
        };
    }

    protected ZipFile createZipFile(final File file)
    throws IOException {
        return new ZipFile(file) {
            protected ReadOnlyFile createReadOnlyFile(final File file)
            throws IOException {
                final RaesReadOnlyFile rof = RaesReadOnlyFile.getInstance(
                        file, raesParameters);
                if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                    rof.authenticate();
                return rof;
            }
        };
    }

    protected ZipFile createZipFile(
            final File file, final String encoding)
    throws IOException, UnsupportedEncodingException {
        return new ZipFile(file, encoding) {
            protected ReadOnlyFile createReadOnlyFile(final File file)
            throws IOException {
                final RaesReadOnlyFile rof = RaesReadOnlyFile.getInstance(
                        file, raesParameters);
                if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                    rof.authenticate();
                return rof;
            }
        };
    }

    protected ZipFile createZipFile(final ReadOnlyFile file)
    throws IOException {
        final RaesReadOnlyFile rof;
        rof = RaesReadOnlyFile.getInstance(file, raesParameters);
        if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
            rof.authenticate();
        try {
            return new ZipFile(rof);
        } catch (NullPointerException exc) {
            rof.close();
            throw exc;
        } catch (IOException exc) {
            rof.close();
            throw exc;
        }
    }

    protected ZipFile createZipFile(
            final ReadOnlyFile file, final String encoding)
    throws NullPointerException, IOException, UnsupportedEncodingException {
        // Check parameters (fail fast).
        if (encoding == null)
            throw new NullPointerException("encoding");
        new String(new byte[0], encoding); // may throw UnsupportedEncodingException!

        final RaesReadOnlyFile rof;
        rof = RaesReadOnlyFile.getInstance(file, raesParameters);
        if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
            rof.authenticate();
        try {
            return new ZipFile(rof, encoding);
        } catch (NullPointerException exc) {
            rof.close();
            throw exc;
        } catch (IOException exc) {
            rof.close();
            throw exc;
        }
    }
}
