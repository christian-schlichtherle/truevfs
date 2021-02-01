/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.bzip2;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.shed.ExtensionSet;
import global.namespace.truevfs.kernel.spec.FsDriver;
import global.namespace.truevfs.kernel.spec.FsScheme;
import global.namespace.truevfs.kernel.spec.spi.FsDriverMapModifier;

import java.util.Map;

/**
 * Maps a file system driver for accessing the BZIP2 compressed TAR file format.
 * The modified map will contain the following entries:
 * <p>
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 * <th>URI Schemes / Archive File Extensions</th>
 * <th>File System Driver Class</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@code tar.bz2}, {@code tar.bzip2}, {@code tb2}, {@code tbz}, {@code tbz2}</td>
 * <td>{@link TarBZip2Driver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class TarBZip2DriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarBZip2Driver();
        new ExtensionSet("tar.bz2|tar.bzip2|tb2|tbz|tbz2").forEach(e -> map.put(FsScheme.create(e), driver));
        return map;
    }
}
