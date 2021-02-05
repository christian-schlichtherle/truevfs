package global.namespace.truevfs.commons.key.api.prompting;

import global.namespace.truevfs.commons.key.api.KeyStrength;
import global.namespace.truevfs.commons.key.api.PbeParameters;

/**
 * Parameters with properties for prompting for password based encryption (PBE)
 * parameters.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * <p>
 * Implementations do not need to be thread-safe.
 *
 * @param  <P> the type of these prompting PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
public interface PromptingPbeParameters<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength>
extends PromptingKey<P>, PbeParameters<P, S> { }
