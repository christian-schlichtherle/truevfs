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
        // The constructors of the TFile class use the ArchiveDetector
        // interface to scan a path name for suffixes of archive files
        // which shall be treated like virtual directories.
        // You can either explicitly inject this ArchiveDetector dependency
        // into a TFile constructor or you can set the following class
        // property to the default ArchiveDetector.
        //   If you comment out the following statement, TrueZIP will
        // automatically recognize the canonical archive file suffixes
        // registered by any archive driver module which is present on the run
        // time class path - you can configure this using the pom.xml file.
        //   However, if you uncomment the following statement, you explicitly
        // state which file suffixes shall get recognized as archive files and
        // hence as virtual directories.
        //   The following statement registers the canonical archive file
        // suffixes for TAR, TAR.GZ, TAR.BZ2 and ZIP files and would require
        // the JARs for the archive driver modules truezip-driver-tar and
        // truezip-driver-zip to be present on the compile time class path.
        /*TFile.setDefaultArchiveDetector(
                new TDefaultArchiveDetector(
                    TDefaultArchiveDetector.NULL,
                    new Object[][] {
                        { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
                        { "tgz|tar.gz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
                        { "tbz|tb2|tar.bz2", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
                        { "zip", new ZipDriver(IOPoolLocator.SINGLETON)},
                    }));*/

        try {
            copy(args);
        } finally {
            // Commits all unsynchronized changes to the contents of all
            // federated file systems (i.e. archive files) to their respective
            // parent file system.
            //   In this context, this is an optional operation:
            // When the JVM shuts down, it performs the same tasks as this
            // method.
            TFile.umount();
        }
    }

    private static void copy(String[] args) throws IOException {
        // Setup the file operands.
        TFile src = new TFile(args[0]);
        TFile dst = new TFile(args[1]);
        // TrueZIP doesn't do path name completion, so we do it manually.
        if (dst.isArchive() || dst.isDirectory())
            dst = new TFile(dst, src.getName());
        
        // Perform a recursive archive copy.
        TFile.cp_rp(src, dst);
    }
}
