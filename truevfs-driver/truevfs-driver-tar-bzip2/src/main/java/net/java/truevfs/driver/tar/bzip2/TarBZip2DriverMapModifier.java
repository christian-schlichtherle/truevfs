/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.bzip2;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Maps a file system driver for accessing the BZIP2 compressed TAR file format.
 * The modified map will contain the following entries:
 * <p>
<table border=1 cellpadding=5 summary="">
<thead>
<tr>
<th>URI Schemes / Archive File Extensions</th>
<th>File System Driver Class</th>
</tr>
</thead>
<tbody>
<tr>
<td>{@code tar.bz2}, {@code tar.bzip2}, {@code tb2}, {@code tbz}, {@code tbz2}</td>
<td>{@link TarBZip2Driver}</td>
</tr>
</tbody>
</table>
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TarBZip2DriverMapModifier extends FsDriverMapModifier {
    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarBZip2Driver();
        for (final String extension : new ExtensionSet("tar.bz2|tar.bzip2|tb2|tbz|tbz2"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }
}
