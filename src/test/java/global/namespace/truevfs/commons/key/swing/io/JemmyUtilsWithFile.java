/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.swing.io;

import global.namespace.truevfs.commons.key.swing.KeyPanelTestSuite;
import global.namespace.truevfs.commons.key.swing.util.JemmyUtils;
import org.junit.After;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 */
public abstract class JemmyUtilsWithFile extends JemmyUtils {

    protected final File file;

    public JemmyUtilsWithFile() throws IOException {
        file = File.createTempFile(
                KeyPanelTestSuite.class.getSimpleName(),
                null,
                FileSystemView.getFileSystemView().getDefaultDirectory());
    }

    @After public void deleteFile() throws IOException {
        if (!file.delete()) throw new FileNotFoundException(file.getPath());
    }
}
