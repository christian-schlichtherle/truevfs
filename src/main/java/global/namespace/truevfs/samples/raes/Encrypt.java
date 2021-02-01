/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.samples.raes;

import java.io.IOException;
import global.namespace.truevfs.access.TPath;

/**
 * Encrypts the contents of the file provided as the first argument
 * into the RAES file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to encrypt a plain
 * ZIP file to an RAES encrypted ZIP file (usually a file with a
 * {@code ".zip.rae"} or {@code ".zip.raes"} extension).
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 *
 * @author Christian Schlichtherle
 */
public class Encrypt extends Application {

    public static void main(String[] args) throws IOException {
        System.exit(new Encrypt().run(args));
    }

    @Override
    void runChecked(final String[] args) throws IOException {
        if (2 != args.length) throw new IllegalArgumentException();
        Raes.encrypt(   new TPath(args[0]).toNonArchivePath(),
                        new TPath(args[1]).toNonArchivePath());
    }
}
