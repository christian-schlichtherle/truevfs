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
package de.schlichtherle.truezip.key.passwd.console;

import de.schlichtherle.truezip.key.PromptingAesKeyProvider;

import static de.schlichtherle.truezip.key.AesKeyProvider.KEY_STRENGTH_128;
import static de.schlichtherle.truezip.key.AesKeyProvider.KEY_STRENGTH_192;
import static de.schlichtherle.truezip.key.AesKeyProvider.KEY_STRENGTH_256;

/**
 * Extends its base class to enable the user to select the key strength
 * for the AES cipher.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ConsolePromptingAesKeyProviderUI<P extends PromptingAesKeyProvider<char[]>>
extends ConsolePromptingKeyProviderUI<P> {

    @Override
    protected void promptExtraData(final P provider) {
        con.printf(resources.getString("keyStrength.banner"));
        con.printf(resources.getString("keyStrength.medium"));
        con.printf(resources.getString("keyStrength.high"));
        con.printf(resources.getString("keyStrength.ultra"));

        prompting: while (true) {
            String keyStrength = con.readLine(
                    resources.getString("keyStrength.prompt"),
                    provider);
            if (keyStrength == null || keyStrength.length() <= 0)
                return;
            try {
                switch (Integer.parseInt(keyStrength)) {
                    case 128:
                        provider.setKeyStrength(KEY_STRENGTH_128);
                        break prompting;

                    case 192:
                        provider.setKeyStrength(KEY_STRENGTH_192);
                        break prompting;

                    case 256:
                        provider.setKeyStrength(KEY_STRENGTH_256);
                        break prompting;
                }
            } catch (NumberFormatException syntaxError) {
            }
        }
    }
}
