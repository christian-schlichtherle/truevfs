/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.gzip;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.shed.ExtensionSet;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsScheme;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;

import java.util.Map;

/**
 * Maps a file system driver for accessing the GZIP compressed TAR file format.
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
 * <td>{@code tar.gz}, {@code tar.gzip}, {@code tgz}</td>
 * <td>{@link TarGZipDriver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class TarGZipDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarGZipDriver();
        new ExtensionSet("tar.gz|tar.gzip|tgz").forEach(e -> map.put(FsScheme.create(e), driver));
        return map;
    }
}
