/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.jar;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.comp.zipdriver.JarDriver;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Maps a file system driver for accessing the JAR file format.
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
 * <td>{@code ear}, {@code jar}, {@code war}</td>
 * <td>{@link JarDriver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 * @see <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 */
@Immutable
@ServiceImplementation(priority = -100)
public final class JarDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new JarDriver();
        new ExtensionSet("ear|jar|war").forEach(e -> map.put(FsScheme.create(e), driver));
        return map;
    }
}
