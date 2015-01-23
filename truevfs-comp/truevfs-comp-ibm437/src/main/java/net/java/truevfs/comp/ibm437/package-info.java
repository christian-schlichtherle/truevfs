/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides the IBM437 character set, which was implicitly assumed to be used
 * for ZIP files before version 6-3-0 of PKWARE's ZIP File Format Specification
 * defined it explicitly and added UTF-8 as an option.
 * If the JRE provides its own implementation, this package will not be used
 * and may be safely removed from the class path.
 *
 * @see    <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>
 * @author Christian Schlichtherle
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truevfs.comp.ibm437;
