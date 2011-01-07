/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.samples;

import de.schlichtherle.truezip.io.file.ArchiveDetector;
import de.schlichtherle.truezip.io.file.DefaultArchiveDetector;
import de.schlichtherle.truezip.io.file.File;
import de.schlichtherle.truezip.io.file.FileInputStream;
import de.schlichtherle.truezip.io.fs.archive.driver.tar.TarBZip2Driver;
import de.schlichtherle.truezip.io.fs.archive.driver.tar.TarDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.tar.TarGZipDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.CheckedJarDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.CheckedReadOnlySfxDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.CheckedZipDriver;
import de.schlichtherle.truezip.io.swing.FileTreeModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A comprehensive command line utility which allows you to work
 * with entries in all supported archive files using Unix like commands
 * ({@code cat}, {@code cp}, {@code rm}, {@code mkdir},
 * {@code rmdir}, {@code ls} etc.).
 * <p>
 * Please note that TrueZIP is designed for optimum performance.
 * However, this utility features some optional archive drivers which
 * provide additional safety or otherwise unavailable features.
 * Some of these drivers are not used in their default configuration -
 * see {@link de.schlichtherle.truezip.io.file.DefaultArchiveDetector} for more
 * information.
 * For example, the ZIP drivers used in this utility <em>always</em> check
 * the CRC-32 values provided in the ZIP file.
 * In addition, the SFX driver is used which allows you to browse
 * {@code .exe} files if they happen to be SelF eXtracting archives (SFX).
 * If they are not however, TrueZIP may spend some considerable amount of
 * time searching for the Central Directory required to be present in ZIP
 * (and hence SFX) files.
 * As a conclusion, this utility should not serve as a performance benchmark.
 * <p>
 * This class is <em>not</em> thread safe.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class NZip extends CommandLineUtility {

    private static final String CLASS_NAME = NZip.class.getName();
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private final FieldPosition fpos = new FieldPosition(NumberFormat.INTEGER_FIELD);

    public NZip() {
    }

    public NZip(OutputStream out, OutputStream err, boolean autoFlush) {
        super(out, err, autoFlush);
    }

    /**
     * May be overridden by subclasses to create the {@link ArchiveDetector}
     * which shall be used as the
     * {@link File#getDefaultArchiveDetector default archive detector} while
     * a command is running.
     * The {@link de.schlichtherle.truezip.io.archive.driver.ArchiveDriver}s returned
     * by the archive detector should use their default charsets.
     * <p>
     * Note that the archive detector which is returned by the implementation
     * in this class uses some archive drivers which may be pretty slow due to
     * some extra compatibility tests which they perform on every archive.
     */
    protected DefaultArchiveDetector newDefaultArchiveDetector() {
        return new DefaultArchiveDetector(ArchiveDetector.ALL,
            new Object[] {
                "ear|jar|war", new CheckedJarDriver(), // check CRC-32
                "zip", new CheckedZipDriver(), // check CRC-32
                "exe", new CheckedReadOnlySfxDriver(), // check CRC-32
            });
    }

    /**
     * May be overridden by subclasses to create the {@link ArchiveDetector}
     * which returns {@link de.schlichtherle.truezip.io.archive.driver.ArchiveDriver}s
     * which should use the specified charset if supported.
     * This is used by selected commands in this class.
     * <p>
     * Note that the archive detector which is returned by the implementation
     * in this class uses some archive drivers which may be pretty slow due to
     * some extra compatibility tests which they perform on every archive.
     */
    protected DefaultArchiveDetector newDefaultArchiveDetector(
            final Charset charset) {
        assert charset != null;
        return new DefaultArchiveDetector(ArchiveDetector.ALL,
                new Object[] {
                    "ear|jar|war|zip", new CheckedZipDriver(charset), // check CRC-32
                    "exe", new CheckedReadOnlySfxDriver(charset), // check CRC-32
                    "tar", new TarDriver(charset),
                    "tgz|tar.gz", new TarGZipDriver(charset),
                    "tbz|tar.bz2", new TarBZip2Driver(charset),
                });
    }

    /** Equivalent to {@code System.exit(new NZip().run(args));}. */
    public static void main(final String[] args) {
        System.exit(new NZip().run(args));
    }

    /**
     * Runs this command line utility.
     * Throws an exception if an error occurs.
     *
     * @param args A non-empty array of Unix-like commands and optional
     *        parameters.
     * @return {@code false} iff the command is a test which fails,
     *         {@code true} otherwise.
     * @throws IllegalUsageException If {@code args} does not contain
     *         correct commands or parameters.
     * @throws IOException On any I/O related exception.
     */
    @Override
	public int runChecked(String[] args)
    throws IllegalUsageException, IOException {
        if (args.length < 1)
            throw new IllegalUsageException();

        final String cmd = args[0].toLowerCase(Locale.ENGLISH);
        args = lshift(args);

        final ArchiveDetector oldDetector = File.getDefaultArchiveDetector();
        try {
            // Let this utility recognize all archive types for which an
            // archive driver is installed.
            // Note that this may be pretty slow if there are SFX EXE files or
            // TAR(.GZ|.BZ2) archives to ls.
            // In addition, for access to RAES encrypted ZIP archives (".tzp",
            // ".zip.rae", ".zip.raes" suffix) Bouncy Castle's Lightweight
            // Crypto API for JDK 1.4, version 1.30 or higher needs to be on
            // the run time class path.
            // Furthermore, for access to TAR, TAR.GZ and TAR.BZ2 archives,
            // "ant.jar" from Apache's Ant, version 1.6.5 or higher needs to
            // be on the run time class path (version 1.7 or higher is
            // recommended).
            // Finally, for regular ZIP files we want to check the CRC32 value
            // when an entry input stream is closed.
            File.setDefaultArchiveDetector(newDefaultArchiveDetector());

            if ("ls".equals(cmd)) {
                ls(args, false, false);
            } else if ("ll".equals(cmd)) {
                ls(args, true, false);
            } else if ("llr".equals(cmd)) {
                ls(args, true, true);
            } else if ("cat".equals(cmd)) {
                cat(args);
            } else if ("cp".equals(cmd)) {
                cpOrMv(args, false);
            } else if ("mv".equals(cmd)) {
                cpOrMv(args, true);
            } else if ("touch".equals(cmd)) {
                touch(args);
            } else if ("mkdir".equals(cmd)) {
                mkdir(args, false);
            } else if ("mkdirs".equals(cmd)) {
                mkdir(args, true);
            } else if ("rm".equals(cmd)) {
                rm(args, false);
            } else if ("rmr".equals(cmd)) {
                rm(args, true);
            } else if ("isarchive".equals(cmd)) {
                return isArchive(args) ? 0 : 1;
            } else if ("isdirectory".equals(cmd)) {
                return isDirectory(args) ? 0 : 1;
            } else if ("isfile".equals(cmd)) {
                return isFile(args) ? 0 : 1;
            } else if ("exists".equals(cmd)) {
                return exists(args) ? 0 : 1;
            } else if ("length".equals(cmd)) {
                return length(args) ? 0 : 1;
            } else {
                throw new IllegalUsageException();
            }
        } finally {
            File.setDefaultArchiveDetector(oldDetector);
        }

        return 0;
    }

    private static String[] lshift(final String[] args) {
        return lshift(args, 1);
    }

    private static String[] lshift(final String[] args, final int num) {
        final int rem = args.length - num;
        if (rem < 0)
            throw new IllegalArgumentException();
        final String[] ret = new String[rem];
        System.arraycopy(args, num, ret, 0, rem);
        return ret;
    }

    private void ls(
            String[] args,
            final boolean detailed,
            final boolean recursive)
    throws IOException {
        if (args.length <= 0)
            args = new String[] { "." };
        for (int i = 0; i < args.length; i++) {
            final File file = new File(args[i]);
            if (args.length > 1)
                out.println(args[i] + ":");
            if (file.isDirectory())
                ls(file, "", detailed, recursive);
            else
                ls(file, file.getPath(), detailed, recursive);
        }
    }

    /**
     * Lists the given file with the given display path.
     */
    private void ls(
            final java.io.File file,
            final String path,
            final boolean detailed,
            final boolean recursive)
    throws IOException {
        if (file.isDirectory()) {
            final java.io.File[] entries = file.listFiles();
            if (entries == null)
                throw new IOException(path + " (" + resources.getString("ls.dia") + ")");
            // Sort directories to the start.
            Arrays.sort(entries, FileTreeModel.FILE_NAME_COMPARATOR);
            for (int i = 0; i < entries.length; i++) {
                final java.io.File entry = entries[i];
                final String entryPath = path.length() > 0
                        ? path + File.separator + entry.getName()
                        : entry.getName();
                ls(entry, entryPath, detailed);
                if (recursive && entry.isDirectory())
                    ls(entries[i], entryPath, detailed, true);
            }
        } else if (file.exists()) {
            ls(file, path, detailed);
        } else {
            throw new IOException(path + " (" + resources.getString("ls.nsfod") + ")");
        }
    }

    private void ls(
            final java.io.File file,
            final String path,
            final boolean detailed) {
        final StringBuffer buf = new StringBuffer();
        if (detailed) {
            align(buf, file.length(), 11);
            buf.append(' ');
            buf.append(dateFormat.format(new Date(file.lastModified())));
            buf.append(' ');
        }
        buf.append(path);
        if (detailed)
            buf.append(file.isDirectory()
                    ? File.separator
                    : file.isFile()
                        ? ""
                        : file.exists()
                            ? "?"
                            : "\u2020"); // dagger '†'
        out.println(buf.toString());
    }

    private void align(StringBuffer buf, long number, int spacing) {
        final int length = buf.length();
        numberFormat.format(number, buf, fpos);
        for (int i = spacing - fpos.getEndIndex(); --i >= 0; )
            buf.insert(length, ' ');
    }

    private void cat(final String[] args)
    throws IllegalUsageException, IOException {
        if (args.length < 1)
            throw new IllegalUsageException();

        for (int i = 0; i < args.length; i++) {
            final InputStream in = new FileInputStream(args[i]);
            try {
                File.cat(in, out);
            } finally {
                in.close();
            }
        }
    }

    private void cpOrMv(final String[] args, final boolean mv)
    throws IllegalUsageException, IOException {
        if (args.length < 2)
            throw new IllegalUsageException();

        int srcI = 0;
        boolean unzip = false;
        boolean cp437out = false;
        boolean utf8out = false;
        boolean cp437in = false;
        boolean utf8in = false;
        int in = 0, out = 0;
        for (; srcI < args.length && args[srcI].charAt(0) == '-'; srcI++) {
            if (mv) // mv
                throw new IllegalUsageException();
            final String opt = args[srcI].toLowerCase(Locale.ENGLISH);
            if ("-unzip".equals(opt)) {
                unzip = true;
                out++;
            } else if ("-cp437out".equals(opt)) {
                cp437out = true;
                out++;
            } else if ("-utf8out".equals(opt)) {
                utf8out = true;
                out++;
            } else if ("-cp437in".equals(opt)) {
                cp437in = true;
                in++;
            } else if ("-utf8in".equals(opt)) {
                utf8in = true;
                in++;
            } else {
                throw new IllegalUsageException();
            }
        }
        if (in > 1 || out > 1)
            throw new IllegalUsageException();

        final ArchiveDetector srcDetector;
        if (cp437in)
            srcDetector = newDefaultArchiveDetector(Charset.forName("IBM437"));
        else if (utf8in)
            srcDetector = newDefaultArchiveDetector(Charset.forName("UTF-8"));
        else
            srcDetector = File.getDefaultArchiveDetector();

        final ArchiveDetector dstDetector;
        if (unzip)
            dstDetector = ArchiveDetector.NULL;
        else if (cp437out)
            dstDetector = newDefaultArchiveDetector(Charset.forName("IBM437"));
        else if (utf8out)
            dstDetector = newDefaultArchiveDetector(Charset.forName("UTF-8"));
        else
            dstDetector = File.getDefaultArchiveDetector();

        final int dstI = args.length - 1;
        final File dst = new File(args[dstI], dstDetector);
        if (dstI - srcI < 1 || (dstI - srcI > 1
                && !dst.isArchive() && !dst.isDirectory()))
            throw new IllegalUsageException();

        if (dst.isArchive() || dst.isEntry())
            monitor.start();

        for (int i = srcI; i < dstI; i++) {
            final File src = new File(args[i], srcDetector);
            final File tmp;
            if (dstI - srcI > 1 || dst.isDirectory())
                tmp = new File(dst, src.getName(), dstDetector);
            else
                tmp = dst;
            if (mv) {
                if ((tmp.isFile() && !tmp.delete()) || !src.renameTo(tmp))
                    throw new IOException(src + ": " + resources.getString("cpOrMv.cmt") + ": " + tmp);
            } else { // cp
                if (!src.archiveCopyAllTo(tmp, srcDetector, dstDetector))
                    throw new IOException(src + ": " + resources.getString("cpOrMv.cct") + ": " + tmp + " (" + resources.getString("cpOrMv.co") + ")");
            }
        }
    }

    private void touch(final String[] args)
    throws IllegalUsageException, IOException {
        if (args.length < 1)
            throw new IllegalUsageException();

        for (int i = 0; i < args.length; i++) {
            final File file = new File(args[i]);
            final boolean ok;
            if (!file.exists())
                ok = file.createNewFile();
            else 
                ok = file.setLastModified(System.currentTimeMillis());
            if (!ok) {
                final String msg;
                if (!file.exists())
                    msg = resources.getString("touch.ccf");
                else if (file.isDirectory())
                    msg = resources.getString("touch.culmtod");
                else if (file.isFile())
                    msg = resources.getString("touch.culmtof");
                else
                    msg = resources.getString("touch.culmtosfod");
                throw new IOException(file.getPath() + " (" + msg + ")");
            }
        }
    }

    private void mkdir(final String[] args, final boolean recursive)
    throws IllegalUsageException, IOException {
        if (args.length < 1)
            throw new IllegalUsageException();

        for (int i = 0; i < args.length; i++) {
            final File file = new File(args[i]);
            final boolean ok = recursive ? file.mkdirs() : file.mkdir();
            if (!ok) {
                final String msg;
                if (!file.exists())
                    msg = resources.getString("mkdir.ccd");
                else if (file.isDirectory())
                    msg = resources.getString("mkdir.dea");
                else if (file.isFile())
                    msg = resources.getString("mkdir.fea");
                else
                    msg = resources.getString("mkdir.sfodea");
                throw new IOException(file.getPath() + " (" + msg + ")");
            }
        }
    }

    private void rm(final String[] args, final boolean recursive)
    throws IllegalUsageException, IOException {
        if (args.length < 1)
            throw new IllegalUsageException();

        for (int i = 0; i < args.length; i++) {
            final File file = new File(args[i]);
            final boolean ok = recursive
                    ? file.deleteAll()
                    : file.delete();
            if (!ok) {
                final String msg;
                if (!file.exists())
                    msg = resources.getString("rm.nsfod");
                else if (file.isDirectory())
                    if (file.list().length > 0)
                        msg = resources.getString("rm.dne");
                    else
                        msg = resources.getString("rm.crd");
                else if (file.isFile())
                    msg = resources.getString("rm.crf");
                else
                    msg = resources.getString("rm.crsfod");
                throw new IOException(file.getPath() + " (" + msg + ")");
            }
        }
    }

    private boolean isArchive(final String[] args)
    throws IllegalUsageException {
        if (args.length != 1)
            throw new IllegalUsageException();

        final boolean success = new File(args[0]).isArchive();
        out.println(success);
        return success;
    }

    private boolean isDirectory(final String[] args)
    throws IllegalUsageException {
        if (args.length != 1)
            throw new IllegalUsageException();

        final boolean success = new File(args[0]).isDirectory();
        out.println(success);
        return success;
    }

    private boolean isFile(final String[] args)
    throws IllegalUsageException {
        if (args.length != 1)
            throw new IllegalUsageException();

        final boolean success = new File(args[0]).isFile();
        out.println(success);
        return success;
    }

    private boolean exists(final String[] args)
    throws IllegalUsageException {
        if (args.length != 1)
            throw new IllegalUsageException();

        final boolean success = new File(args[0]).exists();
        out.println(success);
        return success;
    }

    private boolean length(final String[] args)
    throws IllegalUsageException {
        if (args.length != 1)
            throw new IllegalUsageException();

        final long length = new File(args[0]).length();
        out.println(length);
        return true;
    }

    public class IllegalUsageException
            extends CommandLineUtility.IllegalUsageException {
        private static final long serialVersionUID = 2660653252314854276L;

        private IllegalUsageException() {
            super(resources.getString("usage")); // use Resource Bundle
        }
    }
}
