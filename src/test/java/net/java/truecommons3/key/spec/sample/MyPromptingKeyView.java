/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.sample;

import java.net.URI;
import net.java.truecommons3.key.spec.PersistentUnknownKeyException;
import net.java.truecommons3.key.spec.UnknownKeyException;
import net.java.truecommons3.key.spec.common.AesKeyStrength;
import net.java.truecommons3.key.spec.common.AesPbeParameters;
import net.java.truecommons3.key.spec.prompting.PromptingKey;
import net.java.truecommons3.key.spec.prompting.PromptingKey.Controller;

/** @author Christian Schlichtherle */
public class MyPromptingKeyView
implements PromptingKey.View<AesPbeParameters> {

    @Override
    public void promptKeyForWriting(Controller<AesPbeParameters> controller)
    throws UnknownKeyException {
        // When prompting, you can obtain the URI of the protected resource
        // like this:
        URI resource = controller.getResource();

        // In this stub implementation, I just set a new fake key with a top
        // secret password.
        controller.setKeyClone(newFakeParameters());
    }

    @Override
    public void promptKeyForReading(
            Controller<AesPbeParameters> controller,
            boolean invalid)
    throws UnknownKeyException {
        // The invalid parameter is set to true if and only if a previous
        // call to this method resulted in an invalid key.
        // In this stub implementation, I throw up if that's been the case.
        // In a real application, you should notify the user and prompt her
        // for the key again.
        // It is an error to ignore this parameter and the result of doing so
        // would be an endless loop.
        if (invalid) throw new PersistentUnknownKeyException();

        // When prompting, you can obtain the URI of the protected resource
        // like this:
        URI resource = controller.getResource();

        // In this stub implementation, I just create new fake parameters with
        // a top secret password.
        AesPbeParameters param = newFakeParameters();

        // If the user wants to change the password on the next write access,
        // you should set the following property to true.
        // I set it to the default, so you could comment this statement out.
        param.setChangeRequested(false);

        // Finally, I set the new parameters.
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
