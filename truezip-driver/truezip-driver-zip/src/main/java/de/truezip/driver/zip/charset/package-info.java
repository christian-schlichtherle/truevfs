/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides an implementation of the {@code IBM437} character set.
 * This is the original IBM PC character set for the USA, also known as
 * {@code CP437}, and is the original character set used in ZIP files.
 * <p>
 * This implementation is provided because the IBM437 character set does not
 * need to be present in a JRE.
 * In Oracle's JRE, if installed, it's contained in {@code lib/charsets.jar} in
 * the JRE home directory.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.driver.zip.charset;