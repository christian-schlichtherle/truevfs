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
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * A tree cell renderer which uses an instance of {@link TFileSystemView} to
 * display the system icon for each node in a {@link TFileTree} if required.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class TFileTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1045639746091876245L;

    private transient @CheckForNull TFileSystemView fileSystemView;

    private final TFileTree fileTree;

    TFileTreeCellRenderer(final TFileTree fileTree) {
        if (null == fileTree)
            throw new NullPointerException();
        this.fileTree = fileTree;
    }

    /**
     * Returns the file system view.
     * If this property has been set to {@code null} before, it's reinitialized
     * by calling {@link TFileSystemView#TFileSystemView() new TFileSystemView()}.
     *
     * @return The file system view.
     */
    private TFileSystemView getFileSystemView() {
        final TFileSystemView fileSystemView = this.fileSystemView;
        return null != fileSystemView
                ? fileSystemView
                : (this.fileSystemView = new TFileSystemView());

    }

    @Override
    public @Nullable Icon getOpenIcon() {
        // When editing a node, the edited node is the ONLY one for which the
        // BasicTreeUI BYPASSES the call to getTreeCellRendererComponent(*)
        // and calls this method directly in order to determine the icon to
        // draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final TFile node;
        final Icon icon = super.getOpenIcon();
        return null != icon
                ? icon
                : null != (node = fileTree.getEditedNode())
                    ? getFileSystemView().getSystemIcon(node)
                    : null;
    }

    @Override
    public @Nullable Icon getClosedIcon() {
        // When editing a node, the edited node is the ONLY one for which the
        // BasicTreeUI BYPASSES the call to getTreeCellRendererComponent(*)
        // and calls this method directly in order to determine the icon to
        // draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final TFile node;
        final Icon icon = super.getClosedIcon();
        return null != icon
                ? icon
                : null != (node = fileTree.getEditedNode())
                    ? getFileSystemView().getSystemIcon(node)
                    : null;
    }

    @Override
    public @Nullable Icon getLeafIcon() {
        // When editing a node, the edited node is the ONLY one for which the
        // BasicTreeUI BYPASSES the call to getTreeCellRendererComponent(*)
        // and calls this method directly in order to determine the icon to
        // draw for the edited node.
        // So we simply return the icon for the edited node for ALL nodes
        // while editing and rely on getTreeCellRendererComponent(*) to fix
        // the icon for all other nodes than the edited one.
        final TFile node;
        final Icon icon = super.getLeafIcon();
        return null != icon
                ? icon
                : null != (node = fileTree.getEditedNode())
                    ? getFileSystemView().getSystemIcon(node)
                    : null;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {
        super.getTreeCellRendererComponent( tree, value, selected, expanded,
                                            leaf, row, hasFocus);
        setIcon(getFileSystemView().getSystemIcon((TFile) value));
        return this;
    }
}
