/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.samples.raes;

import net.truevfs.kernel.spec.FsSyncException;
import net.truevfs.access.TPath;
import net.truevfs.samples.access.Application;
import net.truevfs.samples.access.IllegalUsageException;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Decrypts the contents of the RAES file provided as the first argument
 * into the file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to decrypt an RAES
 * encrypted ZIP file (usually a file with a {@code ".tzp"} or
 * {@code ".zip.rae"} extension) back to a plain ZIP file.
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 *
 * @author Christian Schlichtherle
 */
public class Decrypt extends Application {

    private static final String CLASS_NAME = Decrypt.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /** Equivalent to {@code System.exit(new Decrypt().run(args));}. */
    public static void main(final String[] args) throws FsSyncException {
        System.exit(new Decrypt().run(args));
    }

    @Override
    protected int runChecked(final String[] args)
    throws IllegalUsageException, IOException {
        if (args.length != 2)
            throw new IllegalUsageException(resources.getString("usage"));
        Raes.decrypt(   new TPath(args[0]).toNonArchivePath(),
                        new TPath(args[1]).toNonArchivePath(),
                        true);
        return 0;
    }
}