/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.sfx;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Maps a file system driver for read-only access to the SelF eXtracting ZIP
 * file format, alias SFX.
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
<td>{@code exe}</td>
<td>{@link ReadOnlySfxDriver}</td>
</tr>
</tbody>
</table>
 *
 * @see    <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class SfxDriverMapModifier extends FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        map.put(FsScheme.create("exe"), new ReadOnlySfxDriver());
        return map;
    }

    /** @return -100 */
    @Override
    public int getPriority() { return -100; }
}
