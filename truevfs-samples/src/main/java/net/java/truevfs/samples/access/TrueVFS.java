/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.samples.access;

import java.io.IOException;
import java.io.InputStream;
import static java.lang.System.*;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileComparator;
import net.java.truevfs.access.TPath;
import net.java.truevfs.comp.tardriver.TarDriver;
import net.java.truevfs.comp.zipdriver.JarDriver;
import net.java.truevfs.comp.zipdriver.ZipDriver;
import net.java.truevfs.driver.sfx.ReadOnlySfxDriver;
import net.java.truevfs.driver.tar.bzip2.TarBZip2Driver;
import net.java.truevfs.driver.tar.gzip.TarGZipDriver;
import net.java.truevfs.driver.tar.xz.TarXZDriver;
import net.java.truevfs.kernel.spec.FsAccessOption;

/**
 * A comprehensive command line utility which allows you to work
 * with entries in all supported archive files using Unix like commands
 * ({@code cat}, {@code cp}, {@code rm}, {@code mkdir},
 * {@code rmdir}, {@code ls} etc.).
 * <p>
 * Please note that this utility class features some optional archive drivers
 * which provide additional safety or otherwise unavailable features.
 * Some of these features are not normally used because of their negative
 * performance impact and hence this utility class should not be used for a
 * performance benchmark.
 * <p>
 * For example, the ZIP drivers used in this utility <em>always</em> check
 * the CRC-32 values provided in the ZIP file.
 * In addition, the SFX driver is used which allows you to browse
 * {@code .exe} files if they happen to be SelF eXtracting archives (SFX).
 * If they are not however, some considerable amount of time is spent searching
 * for the Central Directory required to be present in ZIP (and hence SFX)
 * files.
 *
 * @author Christian Schlichtherle
 */
public enum TrueVFS {

    USAGE {
        @Override
        void run(final Deque<String> params) {
            throw new IllegalArgumentException();
        }
    },

    HELP {
        @Override
        void run(final Deque<String> params) {
            out.println(valueOf(params.pop().toUpperCase(Locale.ROOT)).getHelp());
        }
    },

    VERSION {
        @Override
        void run(final Deque<String> params) {
            out.println(message("version", TrueVFS.class.getSimpleName()));
        }
    },

    LS {
        @Override
        void run(final Deque<String> params) throws Exception {
            final BitField<LsOption> options = options(params, LsOption.class);
            ls(this, options, params);
        }
    },

    LL {
        @Override
        void run(final Deque<String> params) throws Exception {
            params.push("-l");
            LS.run(params);
        }

        @Override
        String getHelp() { return LS.getHelp(); }
    },

    LLR {
        @Override
        void run(final Deque<String> params) throws Exception {
            params.push("-r");
            params.push("-l");
            LS.run(params);
        }

        @Override
        String getHelp() { return LS.getHelp(); }
    },

    CAT {
        @Override
        void run(final Deque<String> params) throws IOException {
            if (1 > params.size()) throw new NoSuchElementException();
            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TPath path = new TPath(param);
                try (final InputStream in = newInputStream(path)) {
                    TFile.cat(in, out);
                }
            }
        }
    },

    CP {
        @Override
        void run(final Deque<String> params) throws IOException {
            final BitField<CpOption> options = options(params, CpOption.class);
            cpOrMv(this, options, params);
        }
    },

    MV {
        @Override
        void run(final Deque<String> params) throws IOException {
            cpOrMv(this, BitField.noneOf(CpOption.class), params);
        }
    },

    TOUCH {
        @Override
        void run(final Deque<String> params) throws IOException {
            if (1 > params.size()) throw new NoSuchElementException();
            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TPath path = new TPath(param);
                try {
                    createFile(path);
                } catch (FileAlreadyExistsException ex) {
                    setLastModifiedTime(
                            path,
                            FileTime.fromMillis(System.currentTimeMillis()));
                }
            }
        }
    },

    MKDIR {
        @Override
        void run(final Deque<String> params) throws IOException {
            final boolean recursive = options(params, MkdirOption.class)
                    .get(MkdirOption.P);
            if (1 > params.size()) throw new NoSuchElementException();
            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TPath path = new TPath(param);
                if (recursive) createDirectories(path);
                else           createDirectory(path);
            }
        }
    },

    MKDIRS {
        @Override
        void run(final Deque<String> params) throws Exception {
            params.push("-p");
            MKDIR.run(params);
        }

        @Override
        String getHelp() { return MKDIR.getHelp(); }
    },

    RM {
        @Override
        void run(final Deque<String> params) throws IOException {
            final boolean recursive = options(params, RmOption.class)
                    .get(RmOption.R);
            if (1 > params.size()) throw new NoSuchElementException();
            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TPath path = new TPath(param);
                if (recursive) walkFileTree(path, new RmVisitor());
                else           delete(path);
            }
        }
    },

    RMR {
        @Override
        void run(final Deque<String> params) throws Exception {
            params.push("-r");
            RM.run(params);
        }

        @Override
        String getHelp() { return RM.getHelp(); }
    },

    COMPACT {
        @Override
        void run(final Deque<String> params) throws IOException {
            if (1 > params.size()) throw new NoSuchElementException();
            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TFile file = new TFile(param);
                if (file.isTopLevelArchive()) file.compact();
                else err.println(message("ntlaf", file));
            }
        }
    },

    ISARCHIVE {
        @Override
        void run(final Deque<String> params) {
            out.println(new TPath(params.pop()).isArchive());
        }
    },

    ISDIRECTORY {
        @Override
        void run(final Deque<String> params) {
            out.println(isDirectory(new TPath(params.pop())));
        }
    },

    ISFILE {
        @Override
        void run(final Deque<String> params) {
            out.println(isRegularFile(new TPath(params.pop())));
        }
    },

    EXISTS {
        @Override
        void run(final Deque<String> params) {
            out.println(exists(new TPath(params.pop())));
        }
    },

    SIZE {
        @Override
        void run(final Deque<String> params) throws IOException {
            out.println(size(new TPath(params.pop())));
        }
    };

    /**
     * Scans the given command parameters for options of the given class.
     * As a side effect, any found options are popped off the parameter stack.
     *
     * @param  <T> the type of the enum class for the options.
     * @param  params the command parameters.
     * @param  option the enum class for the options.
     * @return a bit field of the options found.
     */
    private static <T extends Enum<T>> BitField<T> options(
            final Deque<String> params,
            final Class<T> option) {
        BitField<T> options = BitField.noneOf(option);
        for (   String param;
                null != (param = params.peek()) && '-' == param.charAt(0);
                params.pop()) {
            param = param.substring(1).toUpperCase(Locale.ROOT);
            options = options.set(valueOf(option, param));
        }
        return options;
    }

    private static void ls(
            final BitField<LsOption> options,
            final TPath file,
            final String path)
    throws IOException {
        final BasicFileAttributes attr =
                readAttributes(file, BasicFileAttributes.class);
        final boolean detailed = options.get(LsOption.L);
        if (detailed)
            out.printf("%,11d %tF %<tT ", attr.size(), attr.lastModifiedTime().toMillis());
        out.append(path);
        if (detailed) {
            if (attr.isDirectory())
                if (attr.isRegularFile())   out.append('+'); // covariant
                else                        out.append(TFile.separator);
            if (attr.isSymbolicLink())      out.append('>');
            if (attr.isOther())             out.append('?');
        }
        out.println();
    }

    private static void ls(
            final TrueVFS command,
            BitField<LsOption> options,
            final TFile file,
            final String path)
    throws IOException {
        if (file.isDirectory()) {
            final TFile[] members = file.listFiles();
            if (null == members)
                throw new IOException(command.message("dina", path));
            // Sort directories to the start.
            Arrays.sort(members, new TFileComparator());
            for (final TFile member : members) {
                String memberPath = member.getName();
                if (!path.isEmpty())
                    memberPath = path + TFile.separator + memberPath;
                ls(options, member.toPath(), memberPath);
                if (options.get(LsOption.R) && member.isDirectory())
                    ls(command, options, member, memberPath);
            }
        } else if (file.exists()) {
            ls(options, file.toPath(), path);
        } else {
            throw new IOException(command.message("nsfod", path));
        }
    }

    static void ls(
            final TrueVFS command,
            BitField<LsOption> options,
            final Deque<String> params)
    throws IOException {
        if (0 >= params.size()) params.push(".");
        final boolean multi = 1 < params.size();
        for (   String param;
                null != (param = params.poll());
                ) {
            final TFile file = new TFile(param);
            if (multi) out.println(param + ":");
            if (file.isDirectory()) ls(command, options, file, "");
            else ls(command, options, file, file.getPath());
        }
    }

    private static TArchiveDetector newArchiveDetector(final Charset charset) {
        assert null != charset;
        return new TArchiveDetector(TArchiveDetector.ALL,
                new Object[][] {
                    { "ear|jar|war", new JarDriver() },
                    { "exe", new ReadOnlySfxDriver() {
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
                    { "tar.bz2|tar.bzip2|tb2|tbz|tbz2", new TarBZip2Driver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                    { "tar.gz|tar.gzip|tgz", new TarGZipDriver() {
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
                    { "zip", new ZipDriver() {
                        @Override
                        public Charset getCharset() {
                            return charset;
                        }
                    } },
                });
    }

    static void cpOrMv(
            final TrueVFS command,
            BitField<CpOption> options,
            final Deque<String> params)
    throws IOException {
        try (final TConfig config = TConfig.open()) {
            TArchiveDetector srcDetector;
            if (options.get(CpOption.CP437IN))
                srcDetector = newArchiveDetector(Charset.forName("IBM437"));
            else if (options.get(CpOption.UTF8IN))
                srcDetector = newArchiveDetector(Charset.forName("UTF-8"));
            else
                srcDetector = config.getArchiveDetector();

            TArchiveDetector dstDetector;
            if (options.get(CpOption.UNZIP))
                dstDetector = TArchiveDetector.NULL;
            else if (options.get(CpOption.CP437OUT))
                dstDetector = newArchiveDetector(Charset.forName("IBM437"));
            else if (options.get(CpOption.UTF8OUT))
                dstDetector = newArchiveDetector(Charset.forName("UTF-8"));
            else
                dstDetector = config.getArchiveDetector();

            config.setAccessPreferences(config.getAccessPreferences()
                    .set(FsAccessOption.STORE, options.get(CpOption.STORE))
                    .set(FsAccessOption.COMPRESS, options.get(CpOption.COMPRESS))
                    .set(FsAccessOption.GROW, options.get(CpOption.GROW))
                    .set(FsAccessOption.ENCRYPT, options.get(CpOption.ENCRYPT)));

            final TFile last = new TFile(params.removeLast(), dstDetector);
            if (1 > params.size()) throw new NoSuchElementException();
            if (1 < params.size() && !last.isArchive() && !last.isDirectory())
                throw new IllegalArgumentException();

            for (   String param;
                    null != (param = params.poll());
                    ) {
                final TFile src = new TFile(param, srcDetector);
                final TFile dst = 1 < params.size() || last.isDirectory()
                        ? new TFile(last, src.getName(), dstDetector)
                        : last;
                if (MV.equals(command)) {
                    try {
                        if (dst.isFile()) dst.rm();
                        src.mv(dst);
                    } catch (final IOException ex) {
                        throw new IOException(command.message("cmt", src, dst), ex);
                    }
                } else { // cp
                    if (options.get(CpOption.R))
                        if (options.get(CpOption.P))
                            TFile.cp_rp(src, dst, srcDetector, dstDetector);
                        else
                            TFile.cp_r(src, dst, srcDetector, dstDetector);
                    else
                        if (options.get(CpOption.P))
                            TFile.cp_p(src, dst);
                        else
                            TFile.cp(src, dst);
                }
            }
        }
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public static void main(String[] args) {
        try {
            final Deque<String> parameters = new LinkedList<>(Arrays.asList(args));
            final String command = parameters.pop().toUpperCase(Locale.ROOT);
            valueOf(command).run(parameters);
        } catch (final IllegalArgumentException | NoSuchElementException ex) {
            final StringBuilder builder = new StringBuilder(25 * 80);
            for (final TrueVFS truevfs : values())
                builder.append('\n').append(truevfs.getUsage());
            err.println(builder.toString());
            System.exit(2);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    String getUsage() { return message("usage", TrueVFS.class.getSimpleName()); }
    String getHelp() { return message("help"); }

    String message(String key, Object... args) {
        return String.format(
                ResourceBundle
                    .getBundle(TrueVFS.class.getCanonicalName() + "." + name())
                    .getString(key),
                args);
    }

    /**
     * Runs this command.
     * Implementations are free to modify the given deque.
     *
     * @param  params the command parameters.
     * @throws Exception on any error.
     */
    abstract void run(Deque<String> params) throws Exception;

    private enum LsOption { L, R }
    private enum CpOption { P, R, UNZIP, CP437IN, CP437OUT, UTF8IN, UTF8OUT, STORE, COMPRESS, GROW, ENCRYPT }
    private enum MkdirOption { P }
    private enum RmOption { R }

    private static class RmVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(
                final Path file,
                final BasicFileAttributes attrs)
        throws IOException {
            delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(
                final Path dir,
                final IOException exc)
        throws IOException {
            if (null != exc) throw exc;
            delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
