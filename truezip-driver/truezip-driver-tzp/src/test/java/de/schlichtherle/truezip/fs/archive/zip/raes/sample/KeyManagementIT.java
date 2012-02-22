/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.sample.KeyManagementTestBase;
import java.io.IOException;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class KeyManagementIT extends KeyManagementTestBase {

    @Test
    public void testSetPasswords1() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.toCharArray());
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
