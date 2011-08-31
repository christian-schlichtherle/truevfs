#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ${package};

import de.schlichtherle.truezip.nio.file.TPath;
import de.schlichtherle.truezip.file.TConfig;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;

/**
 * This command line utility non-recursively copies the first file or directory
 * argument to the second file or directory argument.
 * Instead of a directory, you can name any configured archive file type in
 * the path names, too.
 * If you name any archive files in the destination path name, they get
 * automatically created.
 * <p>
 * For example, if the JAR for the module {@code truezip-driver-zip} is
 * present on the run time class path and the destination path name is
 * {@code archive.zip}, a ZIP file with this name gets created unless it
 * already exists.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Copy extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Copy().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        // Setup the file operands.
        TPath src = new TPath(args[0]);
        TPath dst = new TPath(args[1]);

        // TFile  doesn't do path name completion, so we do it manually in
        // order to emulate the behavior of many copy command line utilities.
        if (TConfig.get().isLenient() && dst.isArchive() || Files.isDirectory(dst))
            dst = dst.resolve(src.getFileName());

        // If TConfig.get().setLenient(false) is never called in your
        // application, then you might as well shorten this to...
        /*if (dst.isArchive() || Files.isDirectory(dst))
            dst = dst.resolve(src.getFileName());*/

        // If you don't like path name completion for non-existent files which
        // just look like archive files according to their path name,
        // then you might even shorten this to...
        /*if (Files.isDirectory(dst))
            dst = dst.resolve(src.getFileName());*/

        // Perform a non-recursive archive copy.
        Files.copy(src, dst, COPY_ATTRIBUTES, REPLACE_EXISTING);
        
        // Okay, if this example should demonstrate a recursive copy, I'ld back
        // out to the TrueZIP File* API as follows because a recursive copy
        // with the NIO.2 API is way too complex for this most prominent use
        // case.
        //TFile.cp_rp(src.toFile(), dst.toFile(), TArchiveDetector.NULL);

        return 0;
    }
}
