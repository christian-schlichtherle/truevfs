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

/**
 * A marker interface for ZIP crypto parameters.
 * ZIP files feature different types of encryption or authentication.
 * Each type determines the algorithms and parameter types used
 * to encrypt and decrypt the entry contents or meta data in the ZIP file.
 * Hence, for each type a separate parameter interface is used which extends
 * this marker interface.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ZipCryptoParameters {
}
