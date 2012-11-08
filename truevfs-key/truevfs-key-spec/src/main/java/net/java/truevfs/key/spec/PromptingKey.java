/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

/**
 * A prompting key for writing and reading protected resources.
 * <p>
 * Implementations do <em>not</em> need to be safe for multi-threading.
 *
 * @param  <K> the type of this prompting key.
 * @author Christian Schlichtherle
 */
public interface PromptingKey<K extends PromptingKey<K>> extends SafeKey<K> {

    /*boolean isChangeRequested();
    void setChangeRequested(boolean changeRequested);*/
}
