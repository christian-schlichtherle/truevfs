/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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

package de.schlichtherle.truezip.key.passwd.swing;

import de.schlichtherle.truezip.key.PromptingAesKeyProvider;
import java.awt.EventQueue;
import javax.swing.JComponent;

/**
 * Extends its base class to enable the user to select the key strength
 * for the AES cipher.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
public class PromptingAesKeyProviderUI
        extends PromptingKeyProviderUI<PromptingAesKeyProvider<Cloneable>> {

    /**
     * A factory method to create the AES Key Strength Panel.
     */
    protected AesKeyStrengthPanel newAesKeyStrengthPanel() {
        return new AesKeyStrengthPanel();
    }

    @Override
    protected void promptCreateKey(
            final PromptingAesKeyProvider<Cloneable> provider,
            final JComponent extraDataUI) {
        assert null == extraDataUI;
        assert EventQueue.isDispatchThread();

        final Cloneable oldKey = provider.getKey();

        // Init panel with current key strength and prompt user.
        final AesKeyStrengthPanel keyStrengthPanel = newAesKeyStrengthPanel();
        keyStrengthPanel.setKeyStrength(provider.getKeyStrength());
        super.promptCreateKey(provider, keyStrengthPanel);

        // Update key strength only on valid input.
        if (oldKey != provider.getKey())
            provider.setKeyStrength(keyStrengthPanel.getKeyStrength());
    }
}
