/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import net.jcip.annotations.NotThreadSafe;

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
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public interface ZipCryptoParameters extends ZipParameters {
}
