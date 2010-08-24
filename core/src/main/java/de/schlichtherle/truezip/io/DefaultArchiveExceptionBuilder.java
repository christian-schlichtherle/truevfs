package de.schlichtherle.truezip.io;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
class DefaultArchiveExceptionBuilder implements ArchiveExceptionBuilder {

    private final DefaultArchiveControllerExceptionBuilder builder;

    public DefaultArchiveExceptionBuilder(final DefaultArchiveControllerExceptionBuilder builder) {
        this.builder = builder;
    }

    public ArchiveException fail(final ArchiveException cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void warn(ArchiveException cause) throws ArchiveException {
        builder.reset(cause);
    }

    public void check() throws ArchiveException {
        builder.check();
    }

    public ArchiveException reset(ArchiveException throwable) {
        return builder.reset(throwable);
    }
}
