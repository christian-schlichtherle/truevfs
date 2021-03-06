/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.ibm437

import java.nio.charset.Charset

class Ibm437CharsetSpec extends OctetCharsetSuite {

  override val charset: Charset = new Ibm437Charset
}
