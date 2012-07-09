/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.core.io;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A marker interface for ZIP crypto parameters.
 * Crypto parameters are required for writing and reading entries which
 * are encrypted or authenticated.
 * ZIP files feature different encryption and authentication types.
 * Each type determines the algorithms and parameter types used
 * to encrypt or authenticate the entry contents or meta data in the ZIP file.
 * For each supported type a separate parameter interface exists which extends
 * this marker interface.
 * For example, the interface {@link WinZipAesParameters} supports WinZip's
 * <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
 * scheme.
 * 
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
@SuppressWarnings("MarkerInterface")
public interface ZipCryptoParameters extends ZipParameters {
}
