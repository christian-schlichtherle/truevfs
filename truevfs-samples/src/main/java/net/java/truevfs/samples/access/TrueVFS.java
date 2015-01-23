/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.samples.access;

import net.java.truecommons.shed.BitField;
import net.java.truevfs.access.*;
import net.java.truevfs.comp.tardriver.TarDriver;
import net.java.truevfs.comp.zipdriver.JarDriver;
import net.java.truevfs.comp.zipdriver.ZipDriver;
import net.java.truevfs.driver.sfx.ReadOnlySfxDriver;
import net.java.truevfs.driver.tar.bzip2.TarBZip2Driver;
import net.java.truevfs.driver.tar.gzip.TarGZipDriver;
import net.java.truevfs.driver.tar.xz.TarXZDriver;
import net.java.truevfs.kernel.spec.FsAccessOption;
import org.apache.commons.compress.utils.Charsets;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.nio.file.Files.*;

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

    // This enum needs to be first, all remaining enums should be sorted alphabetically.
    USAGE {
        @Override
        void run(Deque<String> args) {
            throw new IllegalArgumentException();
        }
    },

    CAT {
        @Override
        void run(final Deque<String> args) throws IOException {
            if (1 > args.size()) throw new NoSuchElementException();
            for (final String arg : args) {
                final TPath path = new TPath(arg);
                try (InputStream in = newInputStream(path)) {
                    TFile.cat(in, out);
                }
            }
        }
    },

    COMPACT {
        @Override
        void run(final Deque<String> args) throws IOException {
            if (1 > args.size()) throw new NoSuchElementException();
            for (final String arg : args) {
                final TFile file = new TFile(arg);
                if (file.isTopLevelArchive()) file.compact();
                else err.println(message("ntlaf", file));
            }
        }
    },

    CP {
        @Override
        void run(Deque<String> args) throws IOException {
            cpOrMv(options(args, CpOption.class), args);
        }
    },

    EXISTS {
        @Override
        void run(Deque<String> args) {
            out.println(exists(new TPath(args.pop())));
        }
    },

    HELP {
        @Override
        void run(Deque<String> args) {
            out.println(TrueVFS.valueOf(args.pop().toUpperCase(Locale.ENGLISH)).getUsage());
        }
    },

    ISARCHIVE {
        @Override
        void run(Deque<String> args) {
            out.println(new TPath(args.pop()).isArchive());
        }
    },

    ISDIRECTORY {
        @Override
        void run(Deque<String> args) {
            out.println(isDirectory(new TPath(args.pop())));
        }
    },

    ISFILE {
        @Override
        void run(Deque<String> args) {
            out.println(isRegularFile(new TPath(args.pop())));
        }
    },

    LS {
        @Override
        void run(Deque<String> args) throws IOException {
            ls(options(args, LsOption.class), args);
        }
    },

    LL {
        @Override
        void run(final Deque<String> args) throws IOException {
            args.push("-l");
            LS.run(args);
        }

        @Override
        String getUsage() { return LS.getUsage(); }
    },

    LLR {
        @Override
        void run(final Deque<String> args) throws IOException {
            args.push("-r");
            args.push("-l");
            LS.run(args);
        }

        @Override
        String getUsage() { return LS.getUsage(); }
    },

    MKDIR {
        @Override
        void run(final Deque<String> args) throws IOException {
            final boolean recursive = options(args, MkdirOption.class)
                    .get(MkdirOption.P);
            if (args.size() < 1) throw new IllegalArgumentException();
            for (final String arg : args) {
                final TPath path = new TPath(arg);
                if (recursive) createDirectories(path);
                else           createDirectory(path);
            }
        }
    },

    MKDIRS {
        @Override
        void run(final Deque<String> args) throws IOException {
            args.push("-p");
            MKDIR.run(args);
        }

        @Override
        String getUsage() { return MKDIR.getUsage(); }
    },

    MV {
        @Override
        void run(Deque<String> args) throws IOException {
            cpOrMv(BitField.noneOf(CpOption.class), args);
        }
    },

    RM {
        @Override
        void run(final Deque<String> args) throws IOException {
            final boolean recursive = options(args, RmOption.class)
                    .get(RmOption.R);
            if (1 > args.size()) throw new NoSuchElementException();
            for (final String arg : args) {
                final TPath path = new TPath(arg);
                if (recursive) walkFileTree(path, new RmVisitor());
                else           delete(path);
            }
        }
    },

    RMR {
        @Override
        void run(final Deque<String> args) throws IOException {
            args.push("-r");
            RM.run(args);
        }

        @Override
        String getUsage() { return RM.getUsage(); }
    },

    SIZE {
        @Override
        void run(Deque<String> args) throws IOException {
            out.println(size(new TPath(args.pop())));
        }
    },

    TOUCH {
        @Override
        void run(final Deque<String> args) throws IOException {
            if (1 > args.size()) throw new NoSuchElementException();
            for (final String arg : args) {
                final TPath path = new TPath(arg);
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

    VERSION {
        @Override
        void run(Deque<String> args) {
            out.println(message("version", TrueVFS.class.getSimpleName()));
        }
    };

    /**
     * Scans the given command parameters for options of the given class.
     * As a side effect, any found options are popped off the parameter stack.
     *
     * @param  <T> the type of the enum class for the options.
     * @param  args the command arguments.
     * @param  option the enum class for the options.
     * @return a bit field of the options found.
     */
    private static <T extends Enum<T>> BitField<T> options(
            final Deque<String> args,
            final Class<T> option) {
        BitField<T> options = BitField.noneOf(option);
        for (   String arg;
                null != (arg = args.peek()) && '-' == arg.charAt(0);
                args.pop()) {
            arg = arg.substring(1).toUpperCase(Locale.ENGLISH);
            options = options.set(valueOf(option, arg));
        }
        return options;
    }

    void cpOrMv(
            final BitField<CpOption> options,
            final Deque<String> args)
            throws IOException {
        try (final TConfig config = TConfig.open()) {
            TArchiveDetector srcDetector;
            if (options.get(CpOption.CP437IN))
                srcDetector = newArchiveDetector(Charset.forName("IBM437"));
            else if (options.get(CpOption.UTF8IN))
                srcDetector = newArchiveDetector(Charsets.UTF_8);
            else
                srcDetector = config.getArchiveDetector();

            TArchiveDetector dstDetector;
            if (options.get(CpOption.UNZIP))
                dstDetector = TArchiveDetector.NULL;
            else if (options.get(CpOption.CP437OUT))
                dstDetector = newArchiveDetector(Charset.forName("IBM437"));
            else if (options.get(CpOption.UTF8OUT))
                dstDetector = newArchiveDetector(Charsets.UTF_8);
            else
                dstDetector = config.getArchiveDetector();

            config.setAccessPreferences(config.getAccessPreferences()
                    .set(FsAccessOption.STORE, options.get(CpOption.STORE))
                    .set(FsAccessOption.COMPRESS, options.get(CpOption.COMPRESS))
                    .set(FsAccessOption.GROW, options.get(CpOption.GROW))
                    .set(FsAccessOption.ENCRYPT, options.get(CpOption.ENCRYPT)));

            final TFile last = new TFile(args.removeLast(), dstDetector);
            final boolean expandPath = last.isDirectory();
            if (args.isEmpty() || 1 < args.size() && !expandPath)
                throw new IllegalArgumentException();

            for (final String arg : args) {
                final TFile src = new TFile(arg, srcDetector);
                final TFile dst = expandPath
                        ? new TFile(last, src.getName(), dstDetector)
                        : last;
                if (equals(MV)) {
                    try {
                        if (dst.isFile()) dst.rm();
                        src.mv(dst);
                    } catch (final IOException ex) {
                        throw new IOException(message("cmt", src, dst), ex);
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

    private static TArchiveDetector newArchiveDetector(final Charset charset) {
        assert null != charset;
        return new TArchiveDetector(
                TArchiveDetector.ALL,
                new Object[][]{
                        {
                                "ear|jar|war",
                                new JarDriver()
                        },
                        {
                                "exe",
                                new ReadOnlySfxDriver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                        {
                                "tar",
                                new TarDriver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                        {
                                "tar.bz2|tar.bzip2|tb2|tbz|tbz2",
                                new TarBZip2Driver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                        {
                                "tar.gz|tar.gzip|tgz",
                                new TarGZipDriver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                        {
                                "tar.xz|txz",
                                new TarXZDriver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                        {
                                "zip",
                                new ZipDriver() {
                                    @Override public Charset getCharset() {
                                        return charset;
                                    }
                                }
                        },
                }
        );
    }

    void ls(
            final BitField<LsOption> options,
            final Deque<String> args)
            throws IOException {
        if (0 >= args.size()) args.push(".");
        final boolean multi = 1 < args.size();
        for (final String arg : args) {
            final TFile file = new TFile(arg);
            if (multi) out.println(arg + ":");
            if (file.isDirectory()) ls(options, file, "");
            else ls(options, file, file.getPath());
        }
    }

    private void ls(
            final BitField<LsOption> options,
            final TFile file,
            final String path)
            throws IOException {
        if (file.isDirectory()) {
            final TFile[] members = file.listFiles();
            if (null == members)
                throw new IOException(message("dina", path));
            // Sort directories to the start.
            Arrays.sort(members, new TFileComparator());
            for (final TFile member : members) {
                String memberPath = member.getName();
                if (!path.isEmpty())
                    memberPath = path + TFile.separator + memberPath;
                ls(options, member.toPath(), memberPath);
                if (options.get(LsOption.R) && member.isDirectory())
                    ls(options, member, memberPath);
            }
        } else if (file.exists()) {
            ls(options, file.toPath(), path);
        } else {
            throw new IOException(message("nsfod", path));
        }
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
            if (attr.isDirectory()) {
                if (attr.isRegularFile()) out.append('+'); // covariant
                else out.append(TFile.separator);
            }
            if (attr.isSymbolicLink()) out.append('>');
            if (attr.isOther()) out.append('?');
        }
        out.println();
    }

    public static void main(String[] args) {
        System.exit(main(new LinkedList<>(Arrays.asList(args))));
    }

    @SuppressWarnings("CallToThreadDumpStack")
    private static int main(final Deque<String> args) {
        final TrueVFS command;
        try {
            command = valueOf(args.pop().toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException | NoSuchElementException ex) {
            final StringBuilder builder = new StringBuilder(25 * 80);
            for (final TrueVFS truevfs : values()) {
                if (0 != builder.length()) builder.append('\n');
                builder.append(truevfs.getSynopsis());
            }
            err.println(builder.toString());
            return 1;
        }
        try {
            command.run(args);
        } catch (final IllegalArgumentException | NoSuchElementException ex) {
            err.println(command.getUsage());
            return 2;
        } catch (IOException ex) {
            ex.printStackTrace();
            return 3;
        }
        return 0;
    }

    String getSynopsis() { return message("synopsis", TrueVFS.class.getSimpleName()); }
    String getUsage() { return message("usage"); }

    String message(String key, Object... args) {
        return String.format(
                ResourceBundle
                        .getBundle(TrueVFS.class.getName() + "." + name())
                        .getString(key),
                args);
    }

    /**
     * Runs this command.
     * Implementations are free to modify the given deque.
     *
     * @param  args the command arguments.
     * @throws IOException on any I/O error.
     */
    abstract void run(Deque<String> args) throws IOException;

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
                final @Nullable IOException exc)
        throws IOException {
            if (null != exc) throw exc;
            delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}
