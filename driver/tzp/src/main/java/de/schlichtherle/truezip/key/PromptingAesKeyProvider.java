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

package de.schlichtherle.truezip.key;

/**
 * An implementation of {@link AesKeyProvider} which prompts the user for
 * a key and allows to select the cipher key strength when creating a new
 * AES encrypted resource or replacing the entire contents of an already
 * existing AES encrypted resource.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PromptingAesKeyProvider<K extends Cloneable>
extends PromptingKeyProvider<K>
implements AesKeyProvider<K> {

    private int keyStrength = KEY_STRENGTH_256;

    @Override
	public int getKeyStrength() {
        assert keyStrength == KEY_STRENGTH_128
                || keyStrength == KEY_STRENGTH_192
                || keyStrength == KEY_STRENGTH_256;
        return keyStrength;
    }

    @Override
	public void setKeyStrength(int keyStrength) {
        if (keyStrength != KEY_STRENGTH_128
                && keyStrength != KEY_STRENGTH_192
                && keyStrength != KEY_STRENGTH_256)
            throw new IllegalArgumentException();
        this.keyStrength = keyStrength;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in this class simply returns its class object,
     * {@code PromptingAesKeyProvider.class}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    protected Class<? extends PromptingAesKeyProvider<?>> getUITypeKey() {
        return (Class) PromptingAesKeyProvider.class;
    }

    /** Resets the key strength to 256 bits. */
    @Override
    protected void onReset() {
        keyStrength = KEY_STRENGTH_256;
    }

    /** Returns the current key strength. */
    @Override
    public String toString() {
        return "" + (128 + keyStrength * 64);
    }
}
