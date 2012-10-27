/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.xz;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.services.annotations.ServiceImplementation;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Maps a file system driver for accessing the XZ compressed TAR file format.
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
<td>{@code tar.xz}, {@code txz}</td>
<td>{@link TarXZDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class TarXZDriverMapModifier extends FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarXZDriver();
        for (final String extension : new ExtensionSet("tar.xz|txz"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }
}
