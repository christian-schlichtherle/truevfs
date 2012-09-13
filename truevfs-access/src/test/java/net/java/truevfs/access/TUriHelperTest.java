/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.net.URI;
import java.net.URISyntaxException;
import static net.java.truevfs.access.TUriHelper.*;
import net.java.truevfs.kernel.spec.FsNodePath;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class TUriHelperTest extends MockArchiveDriverTestBase {

    @Test
    public void testParent() throws URISyntaxException {
        for (String[] params : new String[][] {
            // $path, $parent
            { "", null },
            { "foo", "" },
            { "file:/", null },
            { "file:/foo", "file:/" },
            { "file:/foo/", "file:/" },
            { "file:/foo/bar", "file:/foo/" },
            { "file:/foo/bar/", "file:/foo/" },
            { "mok:file:/foo!/", "file:/" },
            { "mok:file:/foo!/bar", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/baz", "mok:mok:file:/foo!/bar!/" },
            { "mok:mok:file:/foo!/bar!/baz/boom", "mok:mok:file:/foo!/bar!/baz" },
        }) {
            final FsNodePath path = new FsNodePath(new URI(params[0]));
            final FsNodePath parent = params[1] == null
                    ? null
                    : new FsNodePath(new URI(params[1]));
            assertThat(parent(path), is(parent));
        }
    }
}
