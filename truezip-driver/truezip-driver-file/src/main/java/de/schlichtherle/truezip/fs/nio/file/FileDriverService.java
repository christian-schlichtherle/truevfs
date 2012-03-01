/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * An immutable container of a map with a driver for accessing the {@code file}
 * scheme.
 * The map provided by this service consists of the following entries:
 * <p>
<table border=1 cellpadding=5 summary="">
<thead>
<tr>
<th>URI Schemes</th>
<th>File System Driver Class</th>
</tr>
</thead>
<tbody>
<tr>
<td>{@code file}</td>
<td>{@link de.schlichtherle.truezip.fs.nio.file.FileDriver}</td>
</tr>
</tbody>
</table>
 *
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileDriverService extends FsDriverService {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "file", new FileDriver() },
            });

    @Override
    public Map<FsScheme, FsDriver> get() {
        return DRIVERS;
    }
}
