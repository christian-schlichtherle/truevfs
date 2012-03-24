/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides singletons to locate service providers for
 * <ul>
 * <li>file system driver maps,</li>
 * <li>file system managers and</li>
 * <li>I/O buffer pools</li>
 * </ul>
 * on the class path.
 * <p>
 * Note that the module TrueZIP Kernel has no dependency on this package
 * - using it is entirely optional.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.schlichtherle.truezip.sl;