#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ${package};

//import de.schlichtherle.truezip.file.TDefaultArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
//import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
//import de.schlichtherle.truezip.fs.archive.tar.TarDriver;
//import de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver;
//import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import de.schlichtherle.truezip.fs.FsSyncException;
//import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.io.IOException;

/**
 * Recursively copies the first file or directory argument to the second file
 * or directory argument.
 * Instead of a directory, you can name any supported archive file type in the
 * path names, too.
 * If you name any archive files in the destination path name, they get
 * automatically created.
 * E.g. if the JAR for the module {@code truezip-driver-zip} is present on the
 * run time class path and the destination path name is {@code archive.zip} a
 * ZIP file with this name gets created unless it already exists.
 * 
 * @author Christian Schlichtherle
 */
public class Copy {
    public static void main(String[] args) throws IOException {
        // A typical TrueZIP File* application life cycle contains of the
        // following three phases:
        setup(); // phase #1 (optional)
        do {
            try {
                work(args); // phase #2
            } finally {
                // ALWAYS calls this in a finally-block after the work phase!
                sync(); // phase #3
            }
        } while (false);
        // The loop is redundant in this example and only kept to demonstrate
        // the general procedure.
    }

    /**
     * Runs the setup phase of this TrueZIP File* application.
     * <p>
     * The setup phase should be entered only once at the start of the
     * application.
     * Its task is to configure the File* API so that it recognizes the archive
     * file suffixes which shall be treated like virtual directories.
     * <p>
     * Note that this phase is completely optional: If it's omitted, the File*
     * module will automatically recognize the canonical archive file suffixes
     * registered by any archive driver module which is present on the class
     * path at run time - which can be configured by editing the pom.xml file.
     */
    private static void setup() {
        // The constructors of the TFile class use the ArchiveDetector
        // interface to scan a path name for suffixes of archive files
        // which shall be treated like virtual directories.
        // You can either explicitly inject this ArchiveDetector dependency
        // into a TFile constructor or you can set the following class
        // property to the default ArchiveDetector.
        //   If you comment out the following statement, the File* module will
        // automatically recognize the canonical archive file suffixes
        // registered by any archive driver module which is present on the
        // class path at run time - which can be configured by editing the
        // pom.xml file.
        //   However, if you uncomment the following statement, you explicitly
        // state which file suffixes shall get recognized as archive files and
        // hence as virtual directories.
        //   The following statement registers the canonical archive file
        // suffixes for TAR, TAR.GZ, TAR.BZ2 and ZIP files and would require
        // the JARs for the archive driver modules truezip-driver-tar and
        // truezip-driver-zip to be present on the class path at compile time:
        /*TFile.setDefaultArchiveDetector(
                new TDefaultArchiveDetector(
                    TDefaultArchiveDetector.NULL,
                    new Object[][] {
                        { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
                        { "tgz|tar.gz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
                        { "tbz|tb2|tar.bz2", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
                        { "zip", new ZipDriver(IOPoolLocator.SINGLETON)},
                    }));*/
    }

    /**
     * Runs the work phase of this TrueZIP File* application.
     * <p>
     * The work phase may be executed several times based on some events
     * triggered by the application.
     * When it's finished, the {@link #sync() sync phase} must be started next.
     */
    private static void work(String[] args) throws IOException {
        // Setup the file operands.
        TFile src = new TFile(args[0]);
        TFile dst = new TFile(args[1]);
        // TrueZIP doesn't do path name completion, so we do it manually.
        if (dst.isArchive() || dst.isDirectory())
            dst = new TFile(dst, src.getName());
        
        // Perform a recursive archive copy.
        TFile.cp_rp(src, dst);
    }

    /**
     * Runs the sync phase of this TrueZIP File* application.
     * <p>
     * The sync phase commits all unsynchronized changes to the contents of all
     * federated file systems (i.e. archive files) to their respective parent
     * file system, releases these archive files for access by third parties,
     * e.g. other processes, and cleans up any temporary files.
     * Note that temporary files may get used even if the work phase solely
     * executed read-only operations.
     * <p>
     * BTW: In the isolated context of this example, this is an optional
     * operation: When the JVM shuts down, it performs the same tasks as this
     * method.
     */
    private static void sync() throws FsSyncException {
        // Avoid calling this method in a loop which updates the same archive
        // file in each iteration: This would degrade the overall performance
        // from O(n) to O(m*n), where m is the number of new or modified
        // entries and n is the number of all entries in the archive!
        TFile.umount();
    }
}
