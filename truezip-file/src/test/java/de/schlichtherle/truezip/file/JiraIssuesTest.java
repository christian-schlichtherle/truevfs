/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import java.net.URI;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import java.net.URISyntaxException;
import java.util.ServiceConfigurationError;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests various JIRA issues.
 * 
 * @see     <a href="http://java.net/jira/browse/TRUEZIP">JIRA issue Tracker</a>
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class JiraIssuesTest extends TestBase<MockArchiveDriver> {

    @Override
    protected String getSuffixList() {
        return "mok";
    }

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }

    /**
     * Tests issue #TRUEZIP-154.
     * 
     * @see     <a href="http://java.net/jira/browse/TRUEZIP-154">ServiceConfigurationError: Unknown file system scheme for path without a suffix</a>
     * @author  hierynomus (Reporter)
     * @author  Christian Schlichtherle
     */
    @Test
    public void testTrueZIP154() throws URISyntaxException {
        for (String param : new String[] {
            "mok:file:/foo!/",
            "mok:mok:file:/foo!/bar!/",
        }) {
            FsPath path = new FsPath(new URI(param));
            try {
                assertTrueZIP154(new TFile(path));
                assertTrueZIP154(new TFile(path.toUri()));
            } catch (ServiceConfigurationError error) {
                throw new AssertionError(param, error);
            }
        }
    }

    private void assertTrueZIP154(TFile file) {
        for (; null != file; file = file.getEnclArchive()) {
            assertTrue(file.isArchive());
            try {
                file.exists();
                fail("The mock archive driver should not support I/O.");
            } catch (UnsupportedOperationException expected) {
            }
        }
    }
}
