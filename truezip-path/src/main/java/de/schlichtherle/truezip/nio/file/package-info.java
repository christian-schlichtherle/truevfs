/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides uniform, transparent, thread-safe, read/write access to archive
 * files as if they were just plain directories in a file system path by means
 * of the {@link de.schlichtherle.truezip.nio.file.TPath} class and its
 * dependent classes.
 * <p>
 * This is the primary API for JSE&nbsp;7 compliant TrueZIP applications:
 * Like the API of the module TrueZIP File*, this API is just a facade for the
 * module TrueZIP Kernel.
 * In contrast to the TrueZIP File* API however, this API can access any
 * (virtual) file system, not just the platform file system and any archive
 * files within the platform file system.
 * In contrast to the TrueZIP Kernel API, both APIs are designed to be easy to
 * learn and convenient to use while providing a great level of flexibility.
 * Because all virtual file system state is managed by the TrueZIP Kernel
 * module, this module can concurrently access the same file systems than the
 * TrueZIP File* module.
 * <p>
 * For example, an application could access an entry within an archive file
 * which is located at a web site using a {@code TPath} like this:
 * <pre>{@code 
 * Path path = new TPath(new URI("http://acme.com/download/everything.tar.gz/README.TXT"));
 * try (InputStream in = Files.newInputStream(path)) {
 *     // Read archive entry contents here.
 *     ...
 * }
 * }</pre>
 * <p>
 * This example presumes that the JARs of the file system driver modules
 * TrueZIP Driver HTTP(S) and TrueZIP Driver TAR are present on the run time
 * class path.
 * <p>
 * Mind that a {@code TPath} is a {@code Path}, so you can use it
 * polymorphically with the NIO.2 API.
 * 
 * <h3><a name="fspsl"/>File System Provider Service Location</h3>
 * <p>
 * This package provides a JSE&nbsp;7 compliant
 * {@link java.nio.file.spi.FileSystemProvider file system provider}
 * implementation in its class
 * {@link de.schlichtherle.truezip.nio.file.TFileSystemProvider}.
 * If the JAR of this package is present on the run time class path, an
 * application can transparently access archive files without a compile time
 * dependency on this API.
 * However, some constraints apply in this case because the NIO.2 API does not
 * support file system federation:
 * <ul>
 * <li>A {@code FileSystemProvider} instance is limited to support exactly only
 *     one {@link java.nio.file.spi.FileSystemProvider#getScheme() file system
 *     provider scheme}.
 *     So the installed TrueZIP file system provider
 *     {@linkplain de.schlichtherle.truezip.nio.file.TFileSystemProvider#TFileSystemProvider() instance} limits
 *     the access to the platform file system, which is identified by the
 *     custom URI {@link java.net.URI#getScheme() scheme} "{@code tpath}".
 * <li>The TrueZIP file system provider instance competes with the
 *     {@code ZipFileSystemProvider} instance provided by JSE&nbsp;7.
 *     So when
 *     {@linkplain java.nio.file.FileSystems#newFileSystem(java.nio.file.Path, java.lang.ClassLoader) probing}
 *     ZIP or JAR files, it's undefined which provider will be used - see below.
 * <li>When using {@link java.nio.file.Paths#get(String, String[])}, the
 *     returned {@link java.nio.file.Path} instance is always associated with
 *     the default file system provider, which depends on the JVM platform.
 * <li>When using a {@code Path} object to resolve another {@code Path} object,
 *     e.g. by calling {@link java.nio.file.Path#resolve(Path)}, then the new
 *     {@code Path} object is typically associated with the same
 *     {@code FileSystemProvider} object.
 *     So unless the original {@code Path} object was an instance of
 *     {@code TPath}, when traversing a directory tree which contains a
 *     prospective archive file, the new {@code Path} object will not be a
 *     {@code TPath} instance, too, and so the application will most likely not
 *     be able to access the archive file transparently as if it were just a
 *     plain directory.
 * </ul>
 * <p>
 * So the only way how an application can use the TrueZIP file system
 * provider instance without a compile time dependency is to use
 * {@link java.nio.file.FileSystems#newFileSystem(java.nio.file.Path, java.lang.ClassLoader)}.
 * However, this is unlikely to get used in most applications.
 * 
 * <a name="ru"/><h3>Recommended Usage</h3>
 * <p>
 * To overcome these <a href="#fspsl">constraints</a>, an application should
 * not rely on File System Provider Service Location and directly create
 * {@link de.schlichtherle.truezip.nio.file.TPath} instances instead by calling
 * one of the public class constructors.
 * Once created, it's safe to use {@code TPath} instances polymorphically as
 * {@link java.nio.file.Path} instances.
 * 
 * <h3>General Constraints</h3>
 * <p>
 * Mind that the NIO.2 API provides some features which are not supported by
 * the current implementation of this package, e.g. a file system permissions
 * or watch services.
 * Consequently, if an unsupported method is called, an
 * {@link java.lang.UnsupportedOperationException} gets thrown.
 * 
 * <!--h3><a name="comparison"/>{@code TFileSystemProvider} versus
 * {@code ZipFileSystemProvider}</h3>
 * <p>
 * The following table compares the available features for accessing ZIP or JAR
 * files with TrueZIP's {@code TFileSystemProvider} and
 * {@code ZipFileSystemProvider} in JSE&nbsp;7.
 * This comparison does not apply to any other archive file format because
 * these are the only archive file formats supported by the
 * {@code ZipFileSystemProvider} in JSE&nbsp;7.
 * <table>
 * <caption>Feature Comparison</caption>
 * <thead>
 * <tr>
 *   <td>Feature</td>
 *   <td>{@link de.schlichtherle.truezip.nio.file.TFileSystemProvider}</td>
 *   <td>{@code ZipFileSystemProvider}</td>
 *   </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>Configurable character sets.</td>
 *   <td>Yes</td>
 *   <td>No</td>
 * </tr>
 * <tr>
 *   <td>Default character set for creating ZIP files.</td>
 *   <td>{@code CP437 (UTF-8 configurable)}</td>
 *   <td>{@code UTF-8}</td>
 * </tr>
 * <tr>
 *   <td>Character set when reading ZIP files.</td>
 *   <td>{@code CP437 or UTF-8 (file dependent)}</td>
 *   <td>{@code CP437 or UTF-8 (file dependent)}</td>
 * </tr>
 * </tbody>
 * </table-->
 * 
 * @author Christian Schlichtherle
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.schlichtherle.truezip.nio.file;
