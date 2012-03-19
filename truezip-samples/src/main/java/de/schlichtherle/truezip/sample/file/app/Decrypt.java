/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.sample.file.RaesFiles;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Decrypts the contents of the RAES file provided as the first argument
 * into the file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to decrypt an RAES
 * encrypted ZIP file (usually a file with a {@code ".tzp"} or
 * {@code ".zip.rae"} suffix) back to a plain ZIP file.
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 * To decrypt an RAES encrypted ZIP file to a plain ZIP file, use the
 * {@code cp} command of the {@link Nzip} class instead.
 * This class knows about the correct character set charsets for the
 * various flavours of ZIP compatible files.
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
        RaesFiles.decrypt(args[0], args[1], true);
        return 0;
    }
}