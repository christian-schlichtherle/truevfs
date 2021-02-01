/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing.io;

import java.io.*;
import javax.swing.filechooser.FileSystemView;
import global.namespace.truevfs.comp.key.swing.KeyPanelTestSuite;
import global.namespace.truevfs.comp.key.swing.util.JemmyUtils;
import org.junit.After;

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
