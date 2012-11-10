/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.sample;

import java.net.URI;
import net.java.truevfs.key.spec.PersistentUnknownKeyException;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.prompting.PromptingKey;
import net.java.truevfs.key.spec.prompting.PromptingKey.Controller;

/** @author Christian Schlichtherle */
public class MyPromptingKeyView
implements PromptingKey.View<AesPbeParameters> {

    @Override
    public void promptKeyForWriting(Controller<AesPbeParameters> controller)
    throws UnknownKeyException {
        // In a real implementation, you should actually prompt the user now
        // for the password required for write access to the URI.
        // You can obtain the URI of the encrypted resource like this:
        URI resource = controller.getResource();

        // In this stub implementation, I just set a new fake key with our top
        // secret password.
        // Mind you, this really has to be a new key because old keys get wiped
        // out for security.
        // Also, a protective copy of the parameters is made.
        controller.setKeyClone(newFakeParameters());
    }

    @Override
    public void promptKeyForReading(
            Controller<AesPbeParameters> controller,
            boolean invalid)
    throws UnknownKeyException {
        // In a real implementation, you should actually prompt the user now
        // for the password required for read access to the URI.
        // You can obtain the URI of the encrypted resource like this:
        URI resource = controller.getResource();

        // In this stub implementation, I just check if I have provided an
        // invalid password on a previous attempt and throw up if that's been
        // the case.
        if (invalid) throw new PersistentUnknownKeyException();

        // Otherwise, I create new fake paramters with our top secret password.
        AesPbeParameters param = newFakeParameters();

        // If the user wants to change the password on the next write access,
        // you should set the following property to true.
        // I set it to the default, so you could comment this statement out.
        param.setChangeRequested(false);

        // Finally, I set the new parameters.
        // Mind you, this really has to be a new key because old keys get reset
        // for security.
        controller.setKeyClone(param);
    }

    private AesPbeParameters newFakeParameters() {
        AesPbeParameters param = new AesPbeParameters();

        // Actually, this is the default, so you could comment this out.
        param.setKeyStrength(AesKeyStrength.BITS_128);

        // In a real implementation, you should NOT hardcode the password here!
        param.setPassword("top secret".toCharArray());

        return param;
    }
}
