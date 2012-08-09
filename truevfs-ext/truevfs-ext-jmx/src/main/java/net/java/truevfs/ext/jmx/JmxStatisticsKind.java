/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

/**
 * @author Christian Schlichtherle
 */
public enum JmxStatisticsKind {
    APPLICATION {
        @Override 
        public String toString() { return "Application I/O Statistics"; }
    },
    KERNEL {
        @Override 
        public String toString() { return "Kernel I/O Statistics"; }
    },
    BUFFER {
        @Override 
        public String toString() { return "Buffer I/O Statistics"; }
    };
}
