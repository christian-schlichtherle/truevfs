/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides uniform, transparent, thread-safe, read/write access to archive
 * files as if they were just plain directories by means of the {@code TFile*}
 * class et al.
 * <p>
 * This is the primary API for JSE&nbsp;6 compliant TrueVFS applications:
 * Like the API of the module TrueVFS Path, this API is just a facade for
 * the module TrueVFS Kernel.
 * In contrast to the TrueVFS Path API however, this API is limited to access
 * the platform file system and any archive files within the platform file
 * system.
 * In contrast to the TrueVFS Kernel API, both APIs are designed to be easy to
 * learn and convenient to use while providing a great level of flexibility.
 * Because all virtual file system state is managed by the TrueVFS Kernel
 * module, this module can concurrently access the same file systems than the
 * TrueVFS Path module.
 * <p>
 * For example, an application could access an entry within an archive file
 * using a {@code TFile} like this:
 * <pre>{@code 
 * File entry = new TFile("archive.zip/dir/HälloWörld.txt");
 * Writer writer = new TFileWriter(entry);
 * try {
 *     writer.write("Hello world!\n");
 * } finally {
 *     writer.close();
 * }
 * }</pre>
 * <p>
 * This example presumes that the JARs of the file system driver modules
 * TrueVFS Driver File and TrueVFS Driver TAR are present on the run time class
 * path.
 * <p>
 * Mind that a {@code TFile} is a {@code File}, so you can use it
 * polymorphically.
 * However, you cannot use it with a plain {@code File(In|Out)putStream} or a
 * plain {@code File(Reader|Writer)} to access prospective archive entries
 * because these classes were not designed for this task.
 * You have to use a {@code TFile(In|Out)putStream} or a
 * {@code TFile(Reader|Writer)} instead.
 * 
 * @author Christian Schlichtherle
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package net.truevfs.file;
