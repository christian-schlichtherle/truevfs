/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.samples.raes;

import de.truezip.kernel.FsSyncException;
import de.truezip.samples.file.Application;
import de.truezip.samples.file.IllegalUsageException;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Encrypts the contents of the file provided as the first argument
 * into the RAES file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to encrypt a plain
 * ZIP file to an RAES encrypted ZIP file (usually a files with a
 * {@code ".tzp"} or {@code ".zip.rae"} extension).
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 *
 * @author Christian Schlichtherle
 */
public class Encrypt extends Application {

    private static final String CLASS_NAME = Encrypt.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /** Equivalent to {@code System.exit(new Encrypt().run(args));}. */
    public static void main(final String[] args) throws FsSyncException {
        System.exit(new Encrypt().run(args));
    }

    @Override
    protected int runChecked(final String[] args)
    throws IllegalUsageException, IOException {
        if (args.length != 2)
            throw new IllegalUsageException(resources.getString("usage"));
        RaesFiles.encrypt(args[0], args[1]);
        return 0;
    }
}