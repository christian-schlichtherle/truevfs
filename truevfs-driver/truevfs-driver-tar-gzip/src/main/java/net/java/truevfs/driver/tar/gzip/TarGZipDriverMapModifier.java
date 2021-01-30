/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.gzip;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

import javax.annotation.concurrent.Immutable;
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
@Immutable
@ServiceImplementation(priority = -100)
public final class TarGZipDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new TarGZipDriver();
        new ExtensionSet("tar.gz|tar.gzip|tgz").forEach(e -> map.put(FsScheme.create(e), driver));
        return map;
    }
}
