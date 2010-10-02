/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.schlichtherle.truezip.io.archive.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class UpdatingArchiveControllerTestCase extends TestCase {

    private static final Logger logger = Logger.getLogger(
            UpdatingArchiveControllerTestCase.class.getName());

    protected static final java.io.File tempDir = new java.io.File(
            System.getProperty("java.io.tmpdir"));

    private static final Matcher tempMatcher
            = Pattern.compile(UpdatingArchiveController.TEMP_FILE_PREFIX
            + ".*\\" + UpdatingArchiveController.TEMP_FILE_SUFFIX).matcher("");

    private static final Set<String> totalTemps = new HashSet<String>();

    protected UpdatingArchiveControllerTestCase(String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        final String[] temps = tempDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return tempMatcher.reset(name).matches();
            }
        });
        assert temps != null;
        for (final String temp : temps) {
            if (totalTemps.add(temp)) {
                // If the TrueZIP API itself (rather than this test code)
                // leaves a temporary file, then that's considered a bug!
                logger.log(Level.WARNING, "Bug in TrueZIP API: Temp file found: {0}", temp);
            }
        }
    }

}
