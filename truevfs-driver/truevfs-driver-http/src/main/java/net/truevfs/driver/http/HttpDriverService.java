/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.http;

import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverService;
import net.truevfs.kernel.spec.sl.IoPoolLocator;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * An immutable container of a map with a driver for accessing the {@code http}
 * and {@code https} schemes.
 * The map provided by this service consists of the following entries:
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
public final class HttpDriverService extends FsDriverService {

    private final Map<FsScheme, FsDriver>
            drivers = newMap(new Object[][] {
                { "http|https", new HttpDriver(IoPoolLocator.SINGLETON) },
            });

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }

    /** @return -100 */
    @Override
    public int getPriority() {
        return -100;
    }
}
