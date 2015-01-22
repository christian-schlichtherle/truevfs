/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Maps a file system driver for accessing the RAES encrypted ZIP file format,
 * alias ZIP.RAES or TZP.
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
<td>{@code tzp}, {@code zip.rae}, {@code zip.raes}</td>
<td>{@link SafeZipRaesDriver}</td>
</tr>
</tbody>
</table>
 *
 * @author  Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class ZipRaesDriverMapModifier extends FsDriverMapModifier {

    @Override
    public Map<FsScheme, FsDriver> apply(final Map<FsScheme, FsDriver> map) {
        // Select exactly ONE of the following drivers by
        // uncommenting it.
        // Please refer to the class Javadoc for more information.

        // If you're quite paranoid, then this driver is for you:
        // It authenticates every input archive file using
        // the Message Authentication Code (MAC) specified by the
        // RAES file format, which makes it comparably slow.
        // The driver also uses unencrypted byte arrays for
        // temporary storage of archive entries whenever required.
        // If you were completely paranoid, you would even want to
        // use encrypted byte arrays or wipe them with nulls after
        // use.
        // However, then you would have to write this yourself! ;-)
        /*final FsDriver driver = new ParanoidZipRaesDriver(new ByteArrayIOPoolProvider(2048));*/

        // If you're just a bit paranoid, then use this driver:
        // It authenticates every input archive file using
        // the Message Authentication Code (MAC) specified by the
        // RAES file format, which makes it comparably slow.
        // The driver also uses unencrypted temporary files for
        // archive entries whenever required.
        /*final FsDriver driver = new ParanoidZipRaesDriver(IOPoolLocator.SINGLETON);*/

        // For the rest of us, this driver is our choice:
        // It authenticates input archive files up to 512 KB using
        // the Message Authentication Code (MAC) specified by the
        // RAES file format.
        // For larger input archive files, it just checks the
        // CRC-32 value whenever an archive entry input stream is
        // closed.
        // CRC-32 has frequent collisions when compared to a MAC.
        // However, it should not be feasible to make an
        // undetectable modification.
        // The driver also uses unencrypted temporary files for
        // archive entries whenever required.
        final FsDriver driver = new SafeZipRaesDriver();

        for (final String extension : new ExtensionSet("tzp|zip.rae|zip.raes"))
            map.put(FsScheme.create(extension), driver);
        return map;
    }

    /** @return -100 */
    @Override
    public int getPriority() { return -100; }
}
