/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.kernel.spec.FsDriver;
import global.namespace.truevfs.kernel.spec.FsScheme;
import global.namespace.truevfs.kernel.spec.spi.FsDriverMapModifier;

import java.util.Map;

/**
 * Maps a file system driver for accessing the platform file system.
 * The modified map will contain the following entry:
 * <p>
 * <table border=1 cellpadding=5 summary="">
 * <thead>
 * <tr>
 * <th>URI Schemes</th>
 * <th>File System Driver Class</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@code file}</td>
 * <td>{@link FileDriver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class FileDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        map.put(FsScheme.create("file"), new FileDriver());
        return map;
    }
}
