/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class KeyManagementIT extends KeyManagementTestBase {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    
    @Test
    public void testSetPasswords1() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.getBytes(US_ASCII));
        roundTripTest(new TFile(temp, detector), data);
    }

    @Test
    public void testSetPasswords2() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector2(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.toCharArray());
        roundTripTest(new TFile(temp, detector), data);
    }
}
