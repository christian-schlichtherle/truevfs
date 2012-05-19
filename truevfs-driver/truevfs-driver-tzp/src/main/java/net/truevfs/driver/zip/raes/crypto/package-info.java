/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Reads and writes files according to the Random Access Encryption
 * Specification (RAES).
 * RAES is an extensible file format specification which enables fast,
 * transparent random read access to encrypted arbitrary data (the
 * <i>pay load</i>) with multistep authentication.
 * <p>
 * RAES is <b>extensible</b>, which means that the file header contains a
 * single byte to identify the <i>Type</i> of the RAES file.
 * Each data type specifies the cryptographic parameters required to decrypt or
 * encrypt and authenticate the <i>pay load</i> data when reading or writing
 * files of this type.
 * <p>
 * Currently, only Type-0 is specified, but more types may be added in future.
 * Note that it is an error for an implementation to process an unknown type.
 * <p>
 * RAES features <b>transparent random read access</b> to the encrypted data.
 * In order to do so, RAES has some invariants:
 * <ul>
 * <li>RAES uses only block ciphers which are operated in Counter Mode (CTR).
 *     According to <a href="http://csrc.nist.gov/CryptoToolkit/modes/workshop1/papers/lipmaa-ctr.pdf">cryptanalysis</a>
 *     by the US National Institute of Standards and Technology (NIST), CTR
 *     mode is considered to provide a strong level of privacy for the
 *     encrypted data.
 * <li>RAES provides two methods to authenticate the pay load:
 *     <ol>
 *     <li>The first, mandatory method is the <i>cipher Key and text Length
 *         Authentication Code (KLAC)</i>.
 *     <li>The second, optional method is the well-known
 *         <i>Message Authentication Code</i> (MAC) on the full cipher text.
 *     </ol>
 * </ul>
 * <p>
 * The mandatory KLAC method authenticates the cipher key and the length of the
 * cipher text only.
 * This is a very limited authentication which does not prevent an opponent to
 * modify the contents of the cipher text.
 * However, its processing time is constant (O(1)).
 * Hence, relying on this authentication method alone may be acceptable if the
 * pay load data provides another considerably secure means of authentication.
 * <p>
 * The optional MAC method authenticates the full cipher text.
 * This is secure and should be used whenever the pay load does not to provide
 * additional means of authentication.
 * However, its processing time is linear to the size of the file (O(n)).
 * Please note that this option does not require the cipher text to be
 * decrypted first, which features comparably fast processing.
 * <p>
 * An example of a pay load which offers an authentication method by itself is
 * the ZIP file format specification: Entries in a ZIP file are checksummed
 * with CRC-32, which is applied to decompressed (deflated) source data.
 * It is considered computationally infeasible to attack the integrity of a ZIP
 * file which contains only compressed (deflated) entries and has a
 * considerable size (say, at least 512KB) so that after decryption of the
 * archive <em>and</em> decompression (inflation) the CRC-32 value of an entry
 * still matches!
 * This should hold true even though CRC-32 is not at all a good cryptographic
 * hash function because of its frequent collisions, its linear output and
 * small output size.
 * It is the ZIP inflation algorithm which actually comes to its rescue!
 *
 * <h3>RAES types</h3>
 * <p>
 * As said, RAES currently defines only <i>Type-0</i> files.
 * Type-0 has the following specifications:
 * <ul>
 * <li>The encryption scheme is password based according to
 *     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html">PKCS #5</a>
 *     V2.0 with the password base key derivation function (PBKDF) according to
 *     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-12/index.html">PKCS #12</a>
 *     V1.0.
 *     The latter allows to use Unicode password characters. </li>
 * <li>The cipher algorithm is the Advanced Encryption Standard (AES) with a
 *     key length of 128, 192 or 256 bits. </li> <li> The salt is the same
 *     length as the cipher key length.
 * <li>The digest used to derive the cipher key, Initialization Vector (IV) and
 *     authentication codes is the Secure Hash Algorithm (SHA) Version 2 with
 *     256 bits output length.
 * </ul>
 * <p>
 * As usual with PKCS #12 the Initialization Vector (IV) for the CTR mode is
 * derived from the password, the salt and the counter by the same Pseudo
 * Random Number Generation (PRNG) function which is also used to derive the
 * cipher key - so SHA-256. </p> <p> Type 0 also supports the use of key files
 * instead of passwords. If a key file is used for encryption, the first 512
 * bytes of the file are decoded into 16 bit Unicode characters in big endian
 * format (without the byte order indicator) so that the byte order is
 * preserved when the resulting character array is then again encoded into a
 * byte array according to PKCS #12.
 * <p>
 * A file which shall be used as a key file for <em>encryption</em> must not be
 * shorter than 512 bytes.
 * However, in order to provide a certain level of fault tolerance for bogus
 * RAES implementations, when using a key file for <em>decryption</em> an
 * implementation is only required to assert that no more than the first 512
 * bytes of the key file are used.
 * So the key file may actually be shorter.
 * <p>
 * As an optional step, an implementation should also check whether the key
 * file used for encryption is a reasonably good source of entropy.
 * The easiest way to achieve this is to use a compression API like
 * {@link java.util.zip.Deflater}.
 * After compression, the result should be no shorter than the maximum key size
 * (256 bits).
 * Assuming a worst-case overhead for the deflater's algorithm of 100%, the
 * minimum number of compressed bytes for the first 512 bytes of a key file
 * should be no less than 2 * 256 / 8 = 64 bytes.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package net.truevfs.driver.zip.raes.crypto;