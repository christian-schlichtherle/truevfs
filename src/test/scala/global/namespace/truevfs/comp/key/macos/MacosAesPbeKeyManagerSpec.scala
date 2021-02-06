/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos

import global.namespace.truevfs.comp.key.api.aes.AesPbeParameters
import global.namespace.truevfs.comp.key.api.prompting.KeyPromptingDisabledException
import global.namespace.truevfs.comp.key.api.{KeyManager, KeyManagerSuite, KeyProvider}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar._

/**
 * @author Christian Schlichtherle
 */
class MacosAesPbeKeyManagerSpec extends KeyManagerSuite[AesPbeParameters] {

  override protected lazy val keyManager: KeyManager[AesPbeParameters] = {
    val parameters = new AesPbeParameters
    parameters.setPassword("test1234".toCharArray)
    val provider = mock[KeyProvider[AesPbeParameters]]
    when(provider.getKeyForWriting).thenReturn(parameters)
    when(provider.getKeyForReading(false)).thenReturn(parameters)
    when(provider.getKeyForReading(true)).thenThrow(classOf[KeyPromptingDisabledException])
    val manager = mock[KeyManager[AesPbeParameters]]
    when(manager.provider(uri)).thenReturn(provider)
    MacosKeyManager.create(manager, classOf[AesPbeParameters])
  }
}
