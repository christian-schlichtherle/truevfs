/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truecommons.shed.ExtensionSet;

/**
 * Creates maps with drivers for accessing the {@code http} and {@code https}
 * schemes.
 * The maps created by this factory consist of the following entries:
 * <p>
<table border=1 cellpadding=5 summary="">
<thead>
<tr>
<th>URI Schemes</th>
<th>File System Driver Class</th>
</tr>
</thead>
<tbody>
<tr>
<td>{@code http}, {@code https}</td>
<td>{@link net.truevfs.driver.http.HttpDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class HttpDriverMapModifier extends FsDriverMapModifier {
    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        final FsDriver driver = new HttpDriver();
        for (final String extension : new ExtensionSet("http|https"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }
}
