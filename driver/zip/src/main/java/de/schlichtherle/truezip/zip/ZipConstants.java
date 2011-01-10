/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

import java.nio.charset.Charset;

/**
 * A package private interface with some useful constants for ZIP archive files.
 * Public classes <em>must not</em> implement this interface - otherwise the
 * constants become part of the public API.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
interface ZipConstants {

    /**
     * This boolean field is set by the system property
     * {@code de.schlichtherle.truezip.io.zip.forceZIP64Ext}.
     * If this property is set to {@code true} (case is ignored),
     * then ZIP64 extensions are always added when writing a ZIP file,
     * regardless of its size.
     * This system property is intended for testing purposes only.
     * During normal operations, it should not be set as many
     * third party tools would not treat the redundant ZIP64 extensions
     * correctly.
     * Note that it's impossible to inhibit ZIP64 extensions if they are
     * required.
     */
    boolean FORCE_ZIP64_EXT
            = Boolean.getBoolean("de.schlichtherle.truezip.io.zip.forceZIP64Ext");

    /** Local File Header signature. */
    int LFH_SIG = 0x04034B50;
    
    /** Data Descriptor signature. */
    int DD_SIG = 0x08074B50;
    
    /** Central File Header signature. */
    int CFH_SIG = 0x02014B50;

    /** End Of Central Directory Record signature. */
    int EOCD_SIG = 0x06054B50;

    /** Zip64 End Of Central Directory Record. */
    int ZIP64_EOCD_SIG = 0x06064b50;

    /** Zip64 End Of Central Directory Locator. */
    int ZIP64_EOCDL_SIG = 0x07064b50;

    /** The minimum length of the Local File Header record. */
    int LFH_MIN_LEN =
            /* local file header signature     */ 4 +
            /* version needed to extract       */ 2 +
            /* general purpose bit flag        */ 2 +
            /* compression method              */ 2 +
            /* last mod file time              */ 2 +
            /* last mod file date              */ 2 +
            /* crc-32                          */ 4 +
            /* compressed size                 */ 4 +
            /* uncompressed size               */ 4 +
            /* file name length                */ 2 +
            /* extra field length              */ 2;

    /** The minimum length of the Central File Header record. */
    int CFH_MIN_LEN =
            /* central file header signature   */ 4 +
            /* version made by                 */ 2 +
            /* version needed to extract       */ 2 +
            /* general purpose bit flag        */ 2 +
            /* compression method              */ 2 +
            /* last mod file time              */ 2 +
            /* last mod file date              */ 2 +
            /* crc-32                          */ 4 +
            /* compressed size                 */ 4 +
            /* uncompressed size               */ 4 +
            /* filename length                 */ 2 +
            /* extra field length              */ 2 +
            /* file comment length             */ 2 +
            /* disk number start               */ 2 +
            /* internal file attributes        */ 2 +
            /* external file attributes        */ 4 +
            /* relative offset of local header */ 4;

    /** The minimum length of the End Of Central Directory record. */
    int EOCD_MIN_LEN =
            /* end of central dir signature    */ 4 +
            /* number of this disk             */ 2 +
            /* number of the disk with the     */
            /* start of the central directory  */ 2 +
            /* total number of entries in the  */
            /* central directory on this disk  */ 2 +
            /* total number of entries in      */
            /* the central directory           */ 2 +
            /* size of the central directory   */ 4 +
            /* offset of start of central      */
            /* directory with respect to       */
            /* the starting disk number        */ 4 +
            /* zipfile comment length          */ 2;

    /** The minimum length of the Zip64 End Of Central Directory record. */
    int ZIP64_EOCD_MIN_LEN =
            /* zip64 end of central dir        */
            /* signature                       */ 4 +
            /* size of zip64 end of central    */
            /* directory record                */ 8 +
            /* version made by                 */ 2 +
            /* version needed to extract       */ 2 +
            /* number of this disk             */ 4 +
            /* number of the disk with the     */
            /* start of the central directory  */ 4 +
            /* total number of entries in the  */
            /* central directory on this disk  */ 8 +
            /* total number of entries in      */
            /* the central directory           */ 8 +
            /* size of the central directory   */ 8 +
            /* offset of start of central      */
            /* directory with respect to       */
            /* the starting disk number        */ 8;

    /** The length of the Zip64 End Of Central Directory Locator. */
    int ZIP64_EOCDL_LEN =
            /* zip64 end of central dir locator*/
            /* signature                       */ 4 +
            /* number of the disk with the     */
            /* start of the zip64 end of       */
            /* central directory               */ 4 +
            /* relative offset of the zip64    */
            /* end of central directory record */ 8 +
            /* total number of disks           */ 4;

    Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The default character set used for entry names and comments in ZIP
     * archive files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     * Note that you should use &quot;IBM437&quot; for ordinary ZIP archive
     * files instead.
     */
    Charset DEFAULT_CHARSET = UTF8;

    /**
     * The buffer size used for deflating and inflating.
     * Optimized for reading and writing flash memory media.
     */
    int FLATER_BUF_LENGTH = 64 * 1024;

    /** An empty byte array. */
    byte[] EMPTY = new byte[0];
}