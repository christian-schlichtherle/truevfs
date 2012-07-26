/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.gzip;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapModifier;
import de.schlichtherle.truecommons.shed.ExtensionSet;

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
<td>{@code tar.gz}, {@code tar.gzip}, {@code tgz}</td>
<td>{@link net.truevfs.driver.tar.gzip.TarGZipDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TarGZipDriverMapModifier extends FsDriverMapModifier {
    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarGZipDriver();
        for (final String extension : new ExtensionSet("tar.gz|tar.gzip|tgz"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }
}
