/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsDriverMapProviders;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;

/**
 * Creates maps with drivers for the TAR file format.
 * The maps created by this factory consist of the following entries:
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
<td>{@code tar}</td>
<td>{@link net.truevfs.driver.tar.TarDriver}</td>
</tr>
<tr>
<td>{@code tar.bz2}, {@code tb2}, {@code tbz}</td>
<td>{@link net.truevfs.driver.tar.TarBZip2Driver}</td>
</tr>
<tr>
<td>{@code tar.gz}, {@code tgz}</td>
<td>{@link net.truevfs.driver.tar.TarGZipDriver}</td>
</tr>
<tr>
<td>{@code tar.xz}, {@code txz}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.tar.TarXZDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TarDriverMapFactory extends FsDriverMapFactory {
    @Override
    public Map<FsScheme, FsDriver> drivers() {
        return FsDriverMapProviders.newMap(new Object[][] {
                { "tar", new TarDriver() },
                { "tar.bz2|tb2|tbz|tbz2", new TarBZip2Driver() },
                { "tar.gz|tgz", new TarGZipDriver() },
                { "tar.xz|txz", new TarXZDriver() },
            });
    }
}
