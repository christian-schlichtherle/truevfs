/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx

import net.java.truecommons.key.spec.common.AesPbeParameters
import net.java.truecommons.key.spec.prompting.KeyPromptingDisabledException
import net.java.truecommons.key.spec.{KeyManager, KeyManagerITSuite, KeyProvider, UnknownKeyException}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar._

/**
 * @author Christian Schlichtherle
 */
class OsxAesPbeKeyManagerITSpec extends KeyManagerITSuite[AesPbeParameters] {

  override protected def keyManager: KeyManager[AesPbeParameters] = {
    val parameters: AesPbeParameters = new AesPbeParameters
    parameters.setPassword("test1234".toCharArray)
    val provider = mock[KeyProvider[AesPbeParameters]]
    try {
      when(provider.getKeyForWriting).thenReturn(parameters)
      when(provider.getKeyForReading(false)).thenReturn(parameters)
      when(provider.getKeyForReading(true)).thenThrow(classOf[KeyPromptingDisabledException])
    } catch {
      case e: UnknownKeyException =>
        throw new AssertionError
    }
    val manager = mock[KeyManager[AesPbeParameters]]
    when(manager.provider(uri)).thenReturn(provider)
    return new OsxKeyManager[AesPbeParameters](manager, classOf[AesPbeParameters])
  }
}
