/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.samples.access;

import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFileInputStream;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileComparator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.tar.driver.TarDriver;
import net.java.truevfs.comp.zip.driver.CheckedJarDriver;
import net.java.truevfs.comp.zip.driver.CheckedZipDriver;
import net.java.truevfs.driver.sfx.CheckedReadOnlySfxDriver;
import net.java.truevfs.driver.tar.bzip2.TarBZip2Driver;
import net.java.truevfs.driver.tar.gzip.TarGZipDriver;
import net.java.truevfs.driver.tar.xz.TarXZDriver;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import net.java.truevfs.kernel.spec.FsSyncException;

/**
 * A comprehensive command line utility which allows you to work
 * with entries in all supported archive files using Unix like commands
 * ({@code cat}, {@code cp}, {@code rm}, {@code mkdir},
 * {@code rmdir}, {@code ls} etc.).
 * <p>
 * Please note that TrueVFS is designed for optimum performance.
 * However, this utility features some optional archive drivers which
 * provide additional safety or otherwise unavailable features.
 * Some of these drivers are not used in their default configuration -
 * see {@link TArchiveDetector} for more information.
 * For example, the ZIP drivers used in this utility <em>always</em> check
 * the CRC-32 values provided in the ZIP file.
 * In addition, the SFX driver is used which allows you to browse
 * {@code .exe} files if they happen to be SelF eXtracting archives (SFX).
 * If they are not however, TrueVFS may spend some considerable amount of
 * time searching for the Central Directory required to be present in ZIP
 * (and hence SFX) files.
 * As a conclusion, this utility should not serve as a performance benchmark.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class Nzip extends Application {

    private static final ResourceBundle resources
            = ResourceBundle.getBundle(Nzip.class.getName());

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private final FieldPosition fpos = new FieldPosition(NumberFormat.INTEGER_FIELD);

    /**
     * May be overridden by subclasses to create the
     * {@link TArchiveDetector} which provides file system drivers which
     * should use the specified charset if supported.
     * <p>
     * Note that the archive detector which is returned by the implementation
     * in this class uses some archive drivers which may be pretty slow due to
     * some extra compatibility tests which they perform on every archive.
     */
    protected TArchiveDetector newArchiveDetector() {
        return new TArchiveDetector(TArchiveDetector.ALL,
            new Object[][] {
                { "exe", new CheckedReadOnlySfxDriver() }, // check CRC-32
                { "jar|war|ear", new CheckedJarDriver() }, // check CRC-32
                { "zip", new CheckedZipDriver() },         // check CRC-32
            });
    }

    /** @see #newArchiveDetector() */
    protected TArchiveDetector newArchiveDetector(final Charset charset) {
        assert null != charset;
        return new TArchiveDetector(TArchiveDetector.ALL,
                new Object[][] {
                    { "exe", new CheckedReadOnlySfxDriver() { // check CRC-32
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "jar|war|ear", new CheckedJarDriver() { // check CRC-32
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "tar", new TarDriver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "tar.bz2|tb2|tbz|tbz2", new TarBZip2Driver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "tar.gz|tgz", new TarGZipDriver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "tar.xz|txz", new TarXZDriver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "zip", new CheckedZipDriver() {         // check CRC-32
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                });
    }

    /** Equivalent to {@code System.exit(new Nzip().run(args));}. */
    public static void main(String[] args) throws FsSyncException {
        System.exit(new Nzip().run(args));
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
     * @throws IOException On any I/O error.
     */
    @Override
    protected int runChecked(String[] args)
    throws IllegalUsageException, IOException {
        if (1 > args.length) throw new IllegalUsageException();
        final String cmd = args[0].toLowerCase(Locale.ROOT);
        args = lshift(args);
        try {
            // Install custom archive detector.
            TConfig.push().setArchiveDetector(newArchiveDetector());
            switch (cmd) {
            case "ls":
                ls(args, false, false);
                break;
            case "ll":
                ls(args, true, false);
                break;
            case "llr":
                ls(args, true, true);
                break;
            case "cat":
                cat(args);
                break;
            case "compact":
                compact(args);
                break;
            case "cp":
                cpOrMv(args, false);
                break;
            case "mv":
                cpOrMv(args, true);
                break;
            case "touch":
                touch(args);
                break;
            case "mkdir":
                mkdir(args, false);
                break;
            case "mkdirs":
                mkdir(args, true);
                break;
            case "rm":
                rm(args, false);
                break;
            case "rmr":
                rm(args, true);
                break;
            case "isarchive":
                return isArchive(args) ? 0 : 1;
            case "isdirectory":
                return isDirectory(args) ? 0 : 1;
            case "isfile":
                return isFile(args) ? 0 : 1;
            case "exists":
                return exists(args) ? 0 : 1;
            case "length":
                return length(args) ? 0 : 1;
            default:
                throw new IllegalUsageException();
            }
        } finally {
            TConfig.pop();
        }
        return 0;
    }

    private static String[] lshift(final String[] args) {
        return lshift(args, 1);
    }

    private static String[] lshift(final String[] args, final int num) {
        final int rem = args.length - num;
        if (0 > rem) throw new IllegalArgumentException();
        final String[] ret = new String[rem];
        System.arraycopy(args, num, ret, 0, rem);
        return ret;
    }

    private void ls(
            String[] args,
            final boolean detailed,
            final boolean recursive)
    throws IOException {
        if (0 >= args.length) args = new String[] { "." };
        for (int i = 0; i < args.length; i++) {
            final TFile file = new TFile(args[i]);
            if (1 < args.length) out.println(args[i] + ":");
            if (file.isDirectory()) ls(file, "", detailed, recursive);
            else ls(file, file.getPath(), detailed, recursive);
        }
    }

    /**
     * Lists the given file with the given display path.
     */
    private void ls(
            final TFile file,
            final String path,
            final boolean detailed,
            final boolean recursive)
    throws IOException {
        if (file.isDirectory()) {
            final TFile[] entries = file.listFiles();
            if (null == entries)
                throw new IOException(path + " (" + resources.getString("ls.dia") + ")");
            // Sort directories to the start.
            Arrays.sort(entries, new TFileComparator());
            for (int i = 0; i < entries.length; i++) {
                final TFile entry = entries[i];
                final String entryPath = path.length() > 0
                        ? path + TFile.separator + entry.getName()
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
            final TFile file,
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
                    ? (file.isFile() ? "+" : TFile.separator)
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
        if (1 > args.length) throw new IllegalUsageException();
        for (int i = 0; i < args.length; i++) {
            try (final InputStream in = new TFileInputStream(args[i])) {
                TFile.cat(in, out);
            }
        }
    }

    private void compact(String[] args)
    throws IllegalUsageException, IOException {
        if (1 > args.length) throw new IllegalUsageException();
        for (int i = 0; i < args.length; i++) {
            final TFile file = new TFile(args[i]);
            if (file.isArchive()) {
                file.compact();
            } else {
                err.println(file + " (" + resources.getString("compact.na") + ")");
            }
        }
    }

    private void cpOrMv(final String[] args, final boolean mv)
    throws IllegalUsageException, IOException {
        if (2 > args.length) throw new IllegalUsageException();

        int srcI = 0;
        boolean unzip = false;
        boolean cp437out = false;
        boolean utf8out = false;
        boolean cp437in = false;
        boolean utf8in = false;
        boolean store = false;
        boolean compress = false;
        boolean grow = false;
        boolean encrypt = false;
        for (; srcI < args.length && args[srcI].charAt(0) == '-'; srcI++) {
            if (mv) // mv
                throw new IllegalUsageException();
            final String opt = args[srcI].toLowerCase(Locale.ROOT);
            switch (opt) {
            case "-unzip":
                unzip = true;
                break;
            case "-cp437out":
                cp437out = true;
                break;
            case "-utf8out":
                utf8out = true;
                break;
            case "-cp437in":
                cp437in = true;
                break;
            case "-utf8in":
                utf8in = true;
                break;
            case "-store":
                store = true;
                break;
            case "-compress":
                compress = true;
                break;
            case "-grow":
                grow = true;
                break;
            case "-encrypt":
                encrypt = true;
                break;
            default:
                throw new IllegalUsageException();
            }
        }

        final TArchiveDetector srcDetector;
        if (cp437in)
            srcDetector = newArchiveDetector(Charset.forName("IBM437"));
        else if (utf8in)
            srcDetector = newArchiveDetector(Charset.forName("UTF-8"));
        else
            srcDetector = TConfig.get().getArchiveDetector();

        final TArchiveDetector dstDetector;
        if (unzip)
            dstDetector = TArchiveDetector.NULL;
        else if (cp437out)
            dstDetector = newArchiveDetector(Charset.forName("IBM437"));
        else if (utf8out)
            dstDetector = newArchiveDetector(Charset.forName("UTF-8"));
        else
            dstDetector = TConfig.get().getArchiveDetector();

        final int dstI = args.length - 1;
        final TFile dst = new TFile(args[dstI], dstDetector);
        if (dstI - srcI < 1 || (dstI - srcI > 1
                && !dst.isArchive() && !dst.isDirectory()))
            throw new IllegalUsageException();
        try (final TConfig config = TConfig.push()) {
            config.setAccessPreferences(config.getAccessPreferences()
                    .set(STORE, store)
                    .set(COMPRESS, compress)
                    .set(GROW, grow)
                    .set(ENCRYPT, encrypt));

            for (int i = srcI; i < dstI; i++) {
                final TFile src = new TFile(args[i], srcDetector);
                final TFile tmp = dstI - srcI > 1 || dst.isDirectory()
                        ? new TFile(dst, src.getName(), dstDetector)
                        : dst;
                if (mv) {
                    try {
                        if (tmp.isFile())
                            tmp.rm();
                        src.mv(tmp);
                    } catch (IOException ex) {
                        throw new IOException(src + ": " + resources.getString("cpOrMv.cmt") + ": " + tmp, ex);
                    }
                } else { // cp
                    TFile.cp_rp(src, tmp, srcDetector, dstDetector);
                }
            }
        }
    }

    private void touch(final String[] args)
    throws IllegalUsageException, IOException {
        if (1 > args.length) throw new IllegalUsageException();
        for (int i = 0; i < args.length; i++) {
            final TFile file = new TFile(args[i]);
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
                throw new IOException(file + " (" + msg + ")");
            }
        }
    }

    private void mkdir(final String[] args, final boolean recursive)
    throws IllegalUsageException, IOException {
        if (1 > args.length) throw new IllegalUsageException();
        for (int i = 0; i < args.length; i++) {
            final TFile file = new TFile(args[i]);
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
                throw new IOException(file + " (" + msg + ")");
            }
        }
    }

    private void rm(final String[] args, final boolean recursive)
    throws IllegalUsageException, IOException {
        if (1 > args.length) throw new IllegalUsageException();
        for (int i = 0; i < args.length; i++) {
            final TFile file = new TFile(args[i]);
            try {
                if (recursive)
                    file.rm_r();
                else
                    file.rm();
            } catch (IOException ex) {
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
                throw new IOException(file + " (" + msg + ")", ex);
            }
        }
    }

    private boolean isArchive(final String[] args)
    throws IllegalUsageException {
        if (1 != args.length) throw new IllegalUsageException();
        final boolean success = new TFile(args[0]).isArchive();
        out.println(success);
        return success;
    }

    private boolean isDirectory(final String[] args)
    throws IllegalUsageException {
        if (1 != args.length) throw new IllegalUsageException();
        final boolean success = new TFile(args[0]).isDirectory();
        out.println(success);
        return success;
    }

    private boolean isFile(final String[] args)
    throws IllegalUsageException {
        if (1 != args.length) throw new IllegalUsageException();
        final boolean success = new TFile(args[0]).isFile();
        out.println(success);
        return success;
    }

    private boolean exists(final String[] args)
    throws IllegalUsageException {
        if (1 != args.length) throw new IllegalUsageException();
        final boolean success = new TFile(args[0]).exists();
        out.println(success);
        return success;
    }

    private boolean length(final String[] args)
    throws IllegalUsageException {
        if (1 != args.length) throw new IllegalUsageException();
        final long length = new TFile(args[0]).length();
        out.println(length);
        return true;
    }

    private static class IllegalUsageException
    extends net.java.truevfs.samples.access.IllegalUsageException {
        private static final long serialVersionUID = 2660653252314854276L;

        IllegalUsageException() {
            super(resources.getString("usage"));
        }
    }
}
