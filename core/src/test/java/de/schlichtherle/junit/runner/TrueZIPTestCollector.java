/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

package de.schlichtherle.junit.runner;

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import junit.runner.TestCollector;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.4
 */
public class TrueZIPTestCollector implements TestCollector {

    private static final int SUFFIX_LENGTH = ".class".length();

    private final List tests = new LinkedList();

    /** Creates a new instance of TrueZIPTestCollector */
    public TrueZIPTestCollector() {
        final String[] cp = System.getProperty("java.class.path").split(
                System.getProperty("path.separator"));
        for (int i = 0; i < cp.length; i++) {
            scan(new File(cp[i], ArchiveDetector.DEFAULT), ""); // use DEFAULT in case of reconfiguration!
        }
    }

    private void scan(final File root, final String path) {
        final File file = new File(root, path);
        if (file.isDirectory()) {
            final String[] children = file.list();
            for (int i = 0; i < children.length; i++)
                scan(root, path + File.separator + children[i]);
        } else if (file.isFile()) {
            if (isTestClass(path))
                tests.add(toClassName(path));
        }
    }

    protected boolean isTestClass(String path) {
        return path.endsWith("Test.class") &&
                path.indexOf("junit" + File.separator) < 0 &&
                path.indexOf('$') < 0;
    }

    protected String toClassName(String path) {
        return path.substring(1, path.length() - SUFFIX_LENGTH)
                .replace(File.separatorChar, '.');
    }	

    public Enumeration collectTests() {
        return Collections.enumeration(tests);
    }
}
