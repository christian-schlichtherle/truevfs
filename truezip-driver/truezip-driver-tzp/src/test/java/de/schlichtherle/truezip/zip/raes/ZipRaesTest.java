/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip.raes;

import de.schlichtherle.truezip.crypto.raes.MockType0RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.zip.ZipFile;
import de.schlichtherle.truezip.zip.ZipOutputStream;
import de.schlichtherle.truezip.zip.ZipTestSuite;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Tests compression and encryption of data.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesTest extends ZipTestSuite {

    /** Cipher text shorter than this gets authenticated. */
    private static int AUTHENTICATION_TRIGGER = 512 * 1024;
    
    private static final RaesParameters raesParameters = new MockType0RaesParameters();

    @Override
    protected ZipOutputStream newZipOutputStream(final OutputStream out)
    throws IOException {
        final RaesOutputStream ros = RaesOutputStream.getInstance(
                out, raesParameters);
        try {
            return new ZipOutputStream(ros);
        } catch (RuntimeException ex) {
            ros.close();
            throw ex;
        }
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            final OutputStream out, final Charset cs)
    throws IOException {
        final RaesOutputStream ros = RaesOutputStream.getInstance(
                out, raesParameters);
        try {
            return new ZipOutputStream(ros, cs);
        } catch (RuntimeException ex) {
            ros.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final String name)
    throws IOException {
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(new File(name), raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof);
        } catch (RuntimeException ex) {
            rof.close();
            throw ex;
        } catch (IOException exc) {
            rof.close();
            throw exc;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(final String name, final Charset cs)
    throws IOException {
        if (null == cs)
            throw new NullPointerException();
        new String(new byte[0], cs); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(new File(name), raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof, cs);
        } catch (RuntimeException ex) {
            rof.close();
            throw ex;
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final File file)
    throws IOException {
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(file, raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof);
        } catch (RuntimeException ex) {
            rof.close();
            throw ex;
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(
            final File file, final Charset charset)
    throws IOException {
        if (null == charset)
            throw new NullPointerException();
        new String(new byte[0], charset); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(file, raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof, charset);
        } catch (RuntimeException ex) {
            rof.close();
            throw ex;
        } catch (IOException exc) {
            rof.close();
            throw exc;
        }
    }

    @Override
    protected ZipFile newZipFile(final ReadOnlyFile rof)
    throws IOException {
        final RaesReadOnlyFile rrof
                = RaesReadOnlyFile.getInstance(rof, raesParameters);
        try {
            if (rrof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rrof.authenticate();
            return new ZipFile(rrof);
        } catch (RuntimeException ex) {
            rrof.close();
            throw ex;
        } catch (IOException exc) {
            rrof.close();
            throw exc;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(final ReadOnlyFile rof, final Charset cs)
    throws IOException {
        if (null == cs)
            throw new NullPointerException();
        new String(new byte[0], cs); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rrof
                = RaesReadOnlyFile.getInstance(rof, raesParameters);
        try {
            if (rrof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rrof.authenticate();
            return new ZipFile(rrof, cs);
        } catch (RuntimeException ex) {
            rrof.close();
            throw ex;
        } catch (IOException ex) {
            rrof.close();
            throw ex;
        }
    }

    /**
     * Skipped because the test is specific to plain ZIP files.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testBadGetCheckedInputStream() {
    }

    /**
     * Skipped because appending to RAES encrypted ZIP files is not possible
     * by design.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testAppending() {
    }
}
