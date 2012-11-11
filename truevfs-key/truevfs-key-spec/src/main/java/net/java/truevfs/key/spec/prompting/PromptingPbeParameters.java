package net.java.truevfs.key.spec.prompting;

import net.java.truevfs.key.spec.KeyStrength;
import net.java.truevfs.key.spec.PbeParameters;

/**
 * Parameters with properties for prompting for password based encryption (PBE)
 * parameters.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <P> the type of these prompting PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
public interface PromptingPbeParameters<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength>
extends PromptingKey<P>, PbeParameters<P, S> { }
