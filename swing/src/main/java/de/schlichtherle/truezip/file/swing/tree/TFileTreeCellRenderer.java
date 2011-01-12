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
package de.schlichtherle.truezip.file.swing.tree;

import de.schlichtherle.truezip.file.swing.TFileTree;
import java.awt.Component;
import java.io.File;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * A tree cell renderer which uses an instance of
 * {@link de.schlichtherle.truezip.file.swing.TFileSystemView} to display the
 * system icon for each node in a {@link TFileTree} wherever possible.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TFileTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1045639746091876245L;

    private final FileSystemView view = de.schlichtherle.truezip.file.swing.TFileSystemView.getFileSystemView();
    private final TFileTree fileTree;

    public TFileTreeCellRenderer(final TFileTree fileTree) {
        this.fileTree = fileTree;
    }

    @Override
    public Icon getOpenIcon() {
        // This is a hack: When editing a node, the edited node is the ONLY
        // one for which the BasicTreeUI somehow BYPASSES the call to
        // getTreeCellRendererComponent(*) and calls this method directly in
        // order to determine the icon to draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final File node = fileTree.getEditedNode();
        return node != null
                ? view.getSystemIcon(node)
                : super.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() {
        // This is a hack: When editing a node, the edited node is the ONLY
        // one for which the BasicTreeUI somehow BYPASSES the call to
        // getTreeCellRendererComponent(*) and calls this method directly in
        // order to determine the icon to draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final File node = fileTree.getEditedNode();
        return node != null
                ? view.getSystemIcon(node)
                : super.getClosedIcon();
    }

    @Override
    public Icon getLeafIcon() {
        // This is a hack: When editing a node, the edited node is the ONLY
        // one for which the BasicTreeUI somehow BYPASSES the call to
        // getTreeCellRendererComponent(*) and calls this method directly in
        // order to determine the icon to draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final File node = fileTree.getEditedNode();
        return node != null
                ? view.getSystemIcon(node)
                : super.getLeafIcon();
    }

    @Override
    public Component getTreeCellRendererComponent(
            final JTree tree,
            final Object value,
            final boolean selected,
            final boolean expanded,
            final boolean leaf,
            final int row,
            final boolean hasFocus) {
        super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);
        setIcon(view.getSystemIcon((File) value));
        return this;
    }
}
