/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.sfx;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsDriverMapProviders;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;

/**
 * Creates maps with drivers for the ZIP file format.
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
<td>{@code exe}</td>
<td>{@link net.truevfs.driver.zip.ReadOnlySfxDriver}</td>
</tr>
</tbody>
</table>
 * <p>
 * Note that the regular expression is actually decomposed into separate
 * {@link FsScheme} objects which drivers mapped individually.
 *
 * @see     <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 * @author  Christian Schlichtherle
 */
@Immutable
public final class SfxDriverMapFactory extends FsDriverMapFactory {
    @Override
    public Map<FsScheme, FsDriver> apply() {
        return FsDriverMapProviders.newMap(new Object[][] {
                { "exe", new ReadOnlySfxDriver() },
            });
    }
}
