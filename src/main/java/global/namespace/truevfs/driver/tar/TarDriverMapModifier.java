/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.tardriver.TarDriver;
import global.namespace.truevfs.kernel.spec.FsDriver;
import global.namespace.truevfs.kernel.spec.FsScheme;
import global.namespace.truevfs.kernel.spec.spi.FsDriverMapModifier;

import java.util.Map;

/**
 * Maps a file system driver for accessing the TAR file format.
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
 * <td>{@code tar}</td>
 * <td>{@link TarDriver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class TarDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        map.put(FsScheme.create("tar"), new TarDriver());
        return map;
    }
}
