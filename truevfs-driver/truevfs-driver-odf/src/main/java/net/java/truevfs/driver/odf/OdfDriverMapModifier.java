/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.odf;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Maps a file system driver for accessing the Open Document File format.
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
<td>{@code odt}, {@code ott}, {@code odg}, {@code otg}, {@code odp}, {@code otp}, {@code ods}, {@code ots}, {@code odc}, {@code otc}, {@code odi}, {@code oti}, {@code odf}, {@code otf}, {@code odm}, {@code oth}, {@code odb}</td>
<td>{@link OdfDriver}</td>
</tr>
</tbody>
</table>
 *
 * @see     <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 * @author  Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class OdfDriverMapModifier extends FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new OdfDriver();
        for (final String extension : new ExtensionSet("odt|ott|odg|otg|odp|otp|ods|ots|odc|otc|odi|oti|odf|otf|odm|oth|odb"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }

    /** @return -100 */
    @Override
    public int getPriority() { return -100; }
}
