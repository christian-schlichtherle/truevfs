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

package de.schlichtherle.key.passwd.console;

import de.schlichtherle.key.*;

import java.io.*;

/**
 * Extends its base class to enable the user to select the key strength
 * for the AES cipher.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.4
 * @version $Revision$
 */
public class PromptingAesKeyProviderUI extends PromptingKeyProviderUI {

    protected void promptExtraData(PromptingKeyProvider provider) {
        // We can safely cast the parameter to PromptingAesKeyProvider, because
        // otherwise we would not have been called.
        final PromptingAesKeyProvider aesKeyProvider = ((PromptingAesKeyProvider) provider);

        printf(resources.getString("keyStrength.banner"));
        printf(resources.getString("keyStrength.medium"));
        printf(resources.getString("keyStrength.high"));
        printf(resources.getString("keyStrength.ultra"));

        prompting: while (true) {
            String keyStrength = readLine(
                    resources.getString("keyStrength.prompt"),
                    aesKeyProvider);
            if (keyStrength == null || keyStrength.length() <= 0)
                return;
            try {
                switch (Integer.parseInt(keyStrength)) {
                    case 128:
                        aesKeyProvider.setKeyStrength(
                                AesKeyProvider.KEY_STRENGTH_128);
                        break prompting;

                    case 192:
                        aesKeyProvider.setKeyStrength(
                                AesKeyProvider.KEY_STRENGTH_192);
                        break prompting;

                    case 256:
                        aesKeyProvider.setKeyStrength(
                                AesKeyProvider.KEY_STRENGTH_256);
                        break prompting;
                }
            } catch (NumberFormatException syntaxError) {
            }
        }
    }
}