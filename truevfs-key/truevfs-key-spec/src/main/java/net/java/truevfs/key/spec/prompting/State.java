/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.key.spec.PersistentUnknownKeyException;
import net.java.truevfs.key.spec.UnknownKeyException;

/**
 * Implements the behavior strategy for prompting key providers.
 *
 * @author Christian Schlichtherle
 */
@Immutable
enum State {

    RESET {
        @Override
        <K extends PromptingKey<K>> void setupKeyForWriting(
                final PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            provider.promptKeyForWriting();
        }

        @Override
        <K extends PromptingKey<K>> void setupKeyForReading(
                final PromptingKeyProvider<K> provider,
                final boolean invalid)
        throws UnknownKeyException {
            provider.promptKeyForReading(invalid);
        }

        @Override
        <K extends PromptingKey<K>> void resetCancelledKey(
                SharedKeyProvider<K> provider) {
        }
    },

    SET {
        @Override
        <K extends PromptingKey<K>> void setupKeyForWriting(
                final PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            if (provider.isChangeRequested())
                provider.resetUnconditionally();
        }

        @Override
        <K extends PromptingKey<K>> void setupKeyForReading(
                final PromptingKeyProvider<K> provider,
                final boolean invalid)
        throws UnknownKeyException {
            if (invalid) provider.resetUnconditionally();
        }

        @Override
        <K extends PromptingKey<K>> void resetCancelledKey(
                SharedKeyProvider<K> provider) {
        }
    },

    CANCELLED {
        @Override
        <K extends PromptingKey<K>> void setupKeyForWriting(
                PromptingKeyProvider<K> provider)
        throws UnknownKeyException {
            throw exception(provider);
        }

        @Override
        <K extends PromptingKey<K>> void setupKeyForReading(
                PromptingKeyProvider<K> provider,
                boolean invalid)
        throws UnknownKeyException {
            throw exception(provider);
        }

        <K extends PromptingKey<K>> PersistentUnknownKeyException exception(
                PromptingKeyProvider<K> provider) {
            PersistentUnknownKeyException ex = provider.getException();
            if (null == ex)
                provider.setException(ex = new KeyPromptingCancelledException());
            return ex;
        }

        @Override
        <K extends PromptingKey<K>> void resetCancelledKey(
                SharedKeyProvider<K> provider) {
            provider.reset();
        }
    };

    abstract <K extends PromptingKey<K>> void setupKeyForWriting(
            PromptingKeyProvider<K> provider)
    throws UnknownKeyException;

    abstract <K extends PromptingKey<K>> void setupKeyForReading(
            PromptingKeyProvider<K> provider,
            boolean invalid)
    throws UnknownKeyException;

    abstract <K extends PromptingKey<K>> void resetCancelledKey(
            SharedKeyProvider<K> provider);
}
