/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.odf;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.commons.shed.ExtensionSet;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsScheme;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;

import java.util.Map;

/**
 * Maps a file system driver for accessing the Open Document File format.
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
 * <td>{@code odt}, {@code ott}, {@code odg}, {@code otg}, {@code odp}, {@code otp}, {@code ods}, {@code ots}, {@code odc}, {@code otc}, {@code odi}, {@code oti}, {@code odf}, {@code otf}, {@code odm}, {@code oth}, {@code odb}</td>
 * <td>{@link OdfDriver}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Christian Schlichtherle
 * @see <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 */
@ServiceImplementation(priority = -100)
public final class OdfDriverMapModifier implements FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new OdfDriver();
        new ExtensionSet("odt|ott|odg|otg|odp|otp|ods|ots|odc|otc|odi|oti|odf|otf|odm|oth|odb")
                .forEach(e -> map.put(FsScheme.create(e), driver));
        return map;
    }
}
