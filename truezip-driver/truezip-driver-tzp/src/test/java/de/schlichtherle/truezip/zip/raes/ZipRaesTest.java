/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
