/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An immutable container of a map of drivers for the ZIP file format.
 * The map provided by this service consists of the following entries:
 * <p>
<table border=1 cellpadding=5 summary="">
<thead>
<tr>
<th>URI Schemes / Archive File Suffixes</th>
<th>File System Driver Class</th>
</tr>
</thead>
<tbody>
<tr>
<td>{@code ear}, {@code jar}, {@code war}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.JarDriver}</td>
</tr>
<tr>
<td>{@code exe}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.ReadOnlySfxDriver}</td>
</tr>
<tr>
<td>{@code odt}, {@code ott}, {@code odg}, {@code otg}, {@code odp}, {@code otp}, {@code ods}, {@code ots}, {@code odc}, {@code otc}, {@code odi}, {@code oti}, {@code odf}, {@code otf}, {@code odm}, {@code oth}, {@code odb}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.OdfDriver}</td>
</tr>
<tr>
<td>{@code zip}</td>
<td>{@link de.schlichtherle.truezip.fs.archive.zip.ZipDriver}</td>
</tr>
</tbody>
</table>
 * <p>
 * Note that the regular expression is actually decomposed into separate
 * {@link FsScheme} objects which get mapped individually.
 *
 * @see     <a href="http://docs.oasis-open.org/office/v1.2/OpenDocument-v1.2-part1.pdf">Open Document Format for Office Applications (OpenDocument) Version 1.2; Part 1: OpenDocument Schema; Appendix C: MIME Types and File Name Extensions (Non Normative)</a>
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ZipDriverService extends FsDriverService {

    private static final Map<FsScheme, FsDriver>
            DRIVERS = newMap(new Object[][] {
                { "zip", new ZipDriver(IOPoolLocator.SINGLETON) },
                { "ear|jar|war", new JarDriver(IOPoolLocator.SINGLETON) },
                { "odt|ott|odg|otg|odp|otp|ods|ots|odc|otc|odi|oti|odf|otf|odm|oth|odb", new OdfDriver(IOPoolLocator.SINGLETON) },
                { "exe", new ReadOnlySfxDriver(IOPoolLocator.SINGLETON) },
            });

    @Override
    public Map<FsScheme, FsDriver> get() {
        return DRIVERS;
    }
}
