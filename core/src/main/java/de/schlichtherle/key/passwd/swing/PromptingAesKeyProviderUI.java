/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.awt.*;
import de.schlichtherle.key.*;

import javax.swing.*;

/**
 * Extends its base class to enable the user to select the key strength
 * for the AES cipher.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.0
 * @version $Id$
 */
public class PromptingAesKeyProviderUI extends PromptingKeyProviderUI {

    /**
     * @deprecated This field is not used anymore and will be removed for the
     *             next major release number.
     */
    private AesKeyStrengthPanel aesKeyStrengthPanel;

    /**
     * @deprecated This method is not used anymore and will be removed for the
     *             next major release number.
     *             It's use may dead lock the GUI.
     *             Use {@link #createAesKeyStrengthPanel} instead.
     */
    protected AesKeyStrengthPanel getAesKeyStrengthPanel() {
        if (aesKeyStrengthPanel == null)
            aesKeyStrengthPanel = createAesKeyStrengthPanel();
        return aesKeyStrengthPanel;
    }

    /**
     * A factory method to create the AES Key Strength Panel.
     */
    protected AesKeyStrengthPanel createAesKeyStrengthPanel() {
        return new AesKeyStrengthPanel();
    }

    protected void promptCreateKey(final PromptingKeyProvider provider, JComponent extraDataUI) {
        assert null == extraDataUI;
        assert EventQueue.isDispatchThread();

        // We can safely cast the parameter to PromptingAesKeyProvider, because
        // otherwise we would not have been called.
        final PromptingAesKeyProvider aesKeyProvider = ((PromptingAesKeyProvider) provider);

        final Object oldKey = aesKeyProvider.getKey();

        // Init panel with current key strength and prompt user.
        final AesKeyStrengthPanel aesKeyStrengthPanel = createAesKeyStrengthPanel();
        aesKeyStrengthPanel.setKeyStrength(aesKeyProvider.getKeyStrength());
        super.promptCreateKey(aesKeyProvider, aesKeyStrengthPanel);

        // Update key strength only on valid input.
        if (oldKey != aesKeyProvider.getKey())
            aesKeyProvider.setKeyStrength(aesKeyStrengthPanel.getKeyStrength());
    }
}
