package net.java.truevfs.access.exp;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class TFile {

    private final TConfig config;

    public TFile(String path) { this(TConfigs.get(), path); }

    TFile(final TConfig config, final String path) {
        assert null != config;
        this.config = config;
    }

    public TConfig getConfig() {
        return config;
    }
}
