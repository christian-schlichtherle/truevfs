/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import java.util.Formatter;

import static global.namespace.truevfs.comp.cio.Entry.*;

/**
 * Static utility methods for {@link FsArchiveEntry} objects.
 * 
 * @author Christian Schlichtherle
 */
public final class FsArchiveEntries {

    private FsArchiveEntries() { }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     * 
     * @param  e the archive entry
     * @return A string representation of this object for debugging and logging
     *         purposes.
     */
    public static String toString(final FsArchiveEntry e) {
        final StringBuilder s = new StringBuilder(256);
        final Formatter f = new Formatter(s)
                .format("%s@%x[name=%s, type=%s",
                    e.getClass().getName(), e.hashCode(), e.getName(), e.getType());
        for (Size type : ALL_SIZES) {
            final long size = e.getSize(type);
            if (UNKNOWN != size)
                f.format(", size(%s)=%d", type, size);
        }
        for (Access type : ALL_ACCESS) {
            final long time = e.getTime(type);
            if (UNKNOWN != time)
                f.format(", time(%s)=%tc", type, time);
        }
        return s.append("]").toString();
    }
}
