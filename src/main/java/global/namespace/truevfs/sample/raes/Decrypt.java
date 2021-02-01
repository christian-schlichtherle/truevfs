/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.sample.raes;

import global.namespace.truevfs.access.TPath;

import java.io.IOException;

/**
 * Decrypts the contents of the RAES file provided as the first argument
 * into the file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to decrypt an RAES
 * encrypted ZIP file (usually a file with a {@code ".tzp"} or
 * {@code ".zip.rae"} or {@code ".zip.raes"} extension) back to a plain ZIP
 * file.
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 *
 * @author Christian Schlichtherle
 */
public class Decrypt extends Application {
    
    public static void main(String[] args) throws IOException {
        System.exit(new Decrypt().run(args));
    }

    @Override
    void runChecked(final String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException();
        Raes.decrypt(   new TPath(args[0]).toNonArchivePath(),
                        new TPath(args[1]).toNonArchivePath(),
                        true);
    }
}
