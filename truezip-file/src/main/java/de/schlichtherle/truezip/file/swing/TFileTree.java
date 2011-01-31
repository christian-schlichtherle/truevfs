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
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import javax.swing.CellEditor;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A custom {@link JTree} to browse files and directories.
 * There are a couple of file creation/modification/removal methods added
 * which notify the tree of any changes in the file system and update the
 * current path expansions and selection.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class TFileTree extends JTree {

    /**
     * A collator for file names which considers case according to the
     * platform's standard.
     */
    private static final Collator COLLATOR = Collator.getInstance();
    static {
        // Set minimum requirements for maximum performance.
        COLLATOR.setDecomposition(Collator.NO_DECOMPOSITION);
        COLLATOR.setStrength(TFile.separatorChar == '\\'
                ? Collator.SECONDARY
                : Collator.TERTIARY);
    }

    /**
     * Compares two files by their status and path name, so that directories
     * are always ordered <em>before</em> other files.
     */
    private static final class FileComparator
    implements Comparator<TFile>, Serializable {
        private static final long serialVersionUID = 1234567890123456789L;

        @Override
        public int compare(TFile f1, TFile f2) {
            return f1.isDirectory()
                ? f2.isDirectory()
                        ? COLLATOR.compare(f1.getName(), f2.getName())
                        : -1
                : f2.isDirectory()
                        ? 1
                        : COLLATOR.compare(f1.getName(), f2.getName());
        }
    } // class FileComparator

    /** A comparator which sorts directory entries to the beginning. */
    public static final Comparator<TFile>
            FILE_NAME_COMPARATOR = new FileComparator();

    /** The name of the property {@code displayingSuffixes}. */
    private static final String PROPERTY_DISPLAYING_SUFFIXES = "displayingSuffixes"; // NOI18N

    /** The name of the property {@code editingSuffixes}. */
    private static final String PROPERTY_EDITING_SUFFIXES = "editingSuffixes"; // NOI18N

    /** The name of the property {@code defaultSuffix}. */
    private static final String PROPERTY_DEFAULT_SUFFIX = "defaultSuffix"; // NOI18N

    private static final long serialVersionUID = 1064787562479927601L;

    private final Controller controller = new Controller();

    private boolean displayingSuffixes = true;

    private boolean editingSuffixes = true;

    private @CheckForNull String defaultSuffix;

    private @CheckForNull TFile editedNode;

    /**
     * Creates an empty {@code TFileTree} with no root.
     * You shouldn't use this constructor.
     * It's only provided to implement the JavaBean pattern.
     */
    public TFileTree() {
        this(new TFileTreeModel(null, null, FILE_NAME_COMPARATOR));
    }

    /**
     * Creates a new {@code TFileTree} which traverses the given
     * root {@code root} file.
     */
    public TFileTree(TFile root) {
        this(new TFileTreeModel(root, null, FILE_NAME_COMPARATOR));
    }

    /**
     * Creates a new {@code TFileTree} which traverses the given
     * {@link TFileTreeModel}.
     */
    public TFileTree(TFileTreeModel model) {
        super(model);
        super.addTreeExpansionListener(controller);
        super.setCellRenderer(new TFileTreeCellRenderer(this));
    }

    @Override
    public TFileTreeModel getModel() {
        return (TFileTreeModel) super.getModel();
    }

    /**
     * {@inheritDoc}
     * <p>
     * @throws ClassCastException if {@code model} is not an instance
     *         of {@link TFileTreeModel}.
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("BC_UNCONFIRMED_CAST")
    public void setModel(TreeModel model) {
        if (null == model)
            throw new NullPointerException();
        super.setModel((TFileTreeModel) model);
    }

    @Override
    public void setEditable(final boolean editable) {
        if (editable) {
            super.setEditable(true);
            getCellEditor().addCellEditorListener(controller);
        } else {
            final CellEditor ce = getCellEditor();
            if (ce != null)
                ce.removeCellEditorListener(controller);
            super.setEditable(false);
        }
    }

    /**
     * Getter for bound property displayingSuffixes.
     *
     * @return Value of property displayingSuffixes.
     */
    public boolean isDisplayingSuffixes() {
        return this.displayingSuffixes;
    }

    /**
     * Setter for bound property displayingSuffixes.
     * If this is {@code false}, the suffix of files will not be displayed
     * in this tree.
     * Defaults to {@code true}.
     *
     * @param displayingSuffixes New value of property displayingSuffixes.
     */
    public void setDisplayingSuffixes(boolean displayingSuffixes) {
        boolean oldDisplayingSuffixes = this.displayingSuffixes;
        this.displayingSuffixes = displayingSuffixes;
        firePropertyChange(PROPERTY_DISPLAYING_SUFFIXES,
                oldDisplayingSuffixes, displayingSuffixes);
    }

    /**
     * Getter for bound property editingSuffixes.
     *
     * @return Value of property editingSuffixes.
     */
    public boolean isEditingSuffixes() {
        return this.editingSuffixes;
    }

    /**
     * Setter for bound property editingSuffixes.
     * If this is {@code false}, the suffix of a file will be truncated
     * before editing its name starts.
     * Defaults to {@code true}.
     *
     * @param editingSuffixes New value of property editingSuffixes.
     */
    public void setEditingSuffixes(boolean editingSuffixes) {
        boolean oldEditingSuffixes = this.editingSuffixes;
        this.editingSuffixes = editingSuffixes;
        firePropertyChange(PROPERTY_EDITING_SUFFIXES,
                oldEditingSuffixes, editingSuffixes);
    }

    /**
     * Getter for bound property defaultSuffix.
     *
     * @return Value of property defaultSuffix.
     */
    public @Nullable String getDefaultSuffix() {
        return this.defaultSuffix;
    }

    /**
     * Setter for bound property defaultSuffixes.
     * Sets the default suffix to use when suffixes are shown and allowed to
     * be edited, but the user did not provide a suffix when editing a file
     * name.
     * This property defaults to {@code null} and is ignored for
     * directories.
     *
     * @param defaultSuffix The new default suffix.
     *        If not {@code null}, this parameter is fixed to always
     *        start with a {@code '.'}.
     */
    public void setDefaultSuffix(@CheckForNull String defaultSuffix) {
        final String oldDefaultSuffix = this.defaultSuffix;
        if (null != defaultSuffix) {
            defaultSuffix = defaultSuffix.trim();
            if (defaultSuffix.length() <= 0)
                defaultSuffix = null;
            else if (defaultSuffix.charAt(0) != '.')
                defaultSuffix = "." + defaultSuffix;
        }
        this.defaultSuffix = defaultSuffix;
        firePropertyChange(PROPERTY_DEFAULT_SUFFIX,
                oldDefaultSuffix, defaultSuffix);
    }

    /** Returns the node that is currently edited, if any. */
    @Nullable TFile getEditedNode() {
        return editedNode;
    }

    @Override
    public boolean isEditing() {
        return null != editedNode;
    }

    @Override
    public void startEditingAtPath(TreePath path) {
        editedNode = (TFile) path.getLastPathComponent();
        super.startEditingAtPath(path);
    }

    @Override
    public void cancelEditing() {
        editedNode = null;
        super.cancelEditing();
    }

    @Override
    public boolean stopEditing() {
        final boolean stop = super.stopEditing();
        if (stop)
            editedNode = null;
        return stop;
    }

    /**
     * Called when the editing of a cell has been stopped.
     * The implementation in this class will rename the edited file,
     * obeying the rules for suffix handling and updating the expanded and
     * selected paths accordingly.
     *
     * @param evt The change event passed to
     *        {@link CellEditorListener#editingStopped(ChangeEvent)}.
     */
    protected void onEditingStopped(final ChangeEvent evt) {
        final TreeCellEditor tce = (TreeCellEditor) evt.getSource();
        String base = tce.getCellEditorValue().toString().trim();
        final TFile oldNode
                = (TFile) getLeadSelectionPath().getLastPathComponent();
        final TFile parent = oldNode.getParentFile();
        assert parent != null;
        if (!oldNode.isDirectory()) {
            if (isDisplayingSuffixes() && isEditingSuffixes()) {
                final String suffix = getSuffix(base);
                if (null == suffix) {
                    final String defaultSuffix = getDefaultSuffix();
                    if (defaultSuffix != null)
                        base += defaultSuffix;
                }
            } else {
                final String suffix = getSuffix(oldNode.getName());
                if (null != suffix)
                    base += suffix;
            }
        }
        final TFile node = new de.schlichtherle.truezip.file.TFile(parent, base);

        if (!renameTo(oldNode, node))
            Toolkit.getDefaultToolkit().beep();
    }

    private @Nullable String getSuffix(final String base) {
        final int i = base.lastIndexOf('.');
        return i != -1 ? base.substring(i) : null;
    }

    @Override
    public String convertValueToText(
            final Object value,
            final boolean selected,
            final boolean expanded,
            final boolean leaf,
            final int row,
            final boolean hasFocus) {
        final TFile node = (TFile) value;
        final TFile editedNode = getEditedNode();
        if (node != editedNode && !node.exists()) {
            // You will see this occur for files which have been deleted
            // concurrently or which are returned by TFile.listFiles(), but do
            // not actually TFile.exists(), such as "C:\hiberfile.sys" on the
            // Windows platform.
            return "?"; // NOI18N
        }

        final String base = node.getName();
        if (base.length() <= 0)
            return node.getPath(); // This is a file system root.
        if (node.isDirectory() ||
                isDisplayingSuffixes()
                && (!node.equals(editedNode) || isEditingSuffixes()))
            return base;
        final int i = base.lastIndexOf('.');
        return i != -1 ? base.substring(0, i) : base;
    }

    /**
     * Refreshes the entire tree,
     * restores the expanded and selected paths and scrolls to the lead
     * selection path if necessary.
     */
    public void refresh() {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(ftm.getRoot());
        if (null != path)
            refresh(new TreePath[] { path });
    }

    /**
     * Refreshes the subtree for the given node,
     * restores the expanded and selected paths and scrolls to the lead
     * selection path if necessary.
     *
     * @param node The file or directory to refresh.
     *        This may <em>not</em> be {@code null}.
     *
     */
    public void refresh(final TFile node) {
        if (node == null)
            throw new NullPointerException();

        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path != null)
            refresh(new TreePath[] { path });
    }

    /**
     * Refreshes the subtree for the given paths,
     * restores the expanded and selected paths and scrolls to the lead
     * selection path if necessary.
     *
     * @param paths The array of {@code TreePath}s to refresh.
     *        This may be {@code null}.
     */
    public void refresh(final TreePath paths[]) {
        if (paths == null || paths.length <= 0)
            return;

        final TFileTreeModel ftm = getModel();

        final TreePath lead = getLeadSelectionPath();
        final TreePath anchor = getAnchorSelectionPath();
        final TreePath[] selections = getSelectionPaths();

        for (int i = 0, l = paths.length; i < l; i++) {
            final TreePath path = paths[i];
            final Enumeration<TreePath> expansions = getExpandedDescendants(path);
            ftm.refresh((TFile) path.getLastPathComponent());
            setExpandedDescendants(expansions);
        }

        setSelectionPaths(selections);
        setAnchorSelectionPath(anchor);
        setLeadSelectionPath(lead);
        scrollPathToVisible(lead);
    }

    private void setExpandedDescendants(final Enumeration<TreePath> expansions) {
        if (expansions == null)
            return;
        while (expansions.hasMoreElements())
            setExpandedState(expansions.nextElement(), true);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the newly created file
     * is selected and visible.
     * If you would like to create a new file with initial content, please
     * check {@link #copyFrom(de.schlichtherle.truezip.file.TFile, InputStream)}.
     */
    public boolean createNewFile(final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.createNewFile(node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the newly created directory
     * is selected and visible.
     */
    public boolean mkdir(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.mkdir(node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the newly created directory
     * is selected and visible.
     */
    public boolean mkdirs(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.mkdirs(node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     */
    public boolean copyFrom(final de.schlichtherle.truezip.file.TFile node, final InputStream in) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.copyFrom(node, in))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     */
    public boolean copyTo(final de.schlichtherle.truezip.file.TFile oldNode, final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.copyTo(oldNode, node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the recursively copied node
     * is selected and visible.
     */
    public boolean copyAllTo(final de.schlichtherle.truezip.file.TFile oldNode, final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.copyAllTo(oldNode, node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     */
    public boolean archiveCopyTo(final de.schlichtherle.truezip.file.TFile oldNode, final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.archiveCopyTo(oldNode, node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the recursively copied node
     * is selected and visible.
     */
    public boolean archiveCopyAllTo(final de.schlichtherle.truezip.file.TFile oldNode, final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        if (!ftm.archiveCopyAllTo(oldNode, node))
            return false;

        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel},
     * restores the expanded paths, selects {@code node} and scrolls to
     * it if necessary.
     */
    public boolean renameTo(final TFile oldNode, final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;

        final Enumeration<TreePath> expansions;
        final TreePath oldPath = ftm.newTreePath(oldNode);
        if (oldPath != null)
            expansions = getExpandedDescendants(oldPath);
        else
            expansions = null;

        if (!ftm.renameTo(oldNode, node))
            return false;

        if (expansions != null)
            while (expansions.hasMoreElements())
                setExpandedState(
                        substPath(expansions.nextElement(), oldPath, path),
                        true);
        setSelectionPath(path);
        scrollPathToVisible(path);

        return true;
    }

    private TreePath substPath(
            final TreePath tp,
            final TreePath oldPath,
            final TreePath path) {
        final TFile file = (TFile) tp.getLastPathComponent();
        if (file.equals(oldPath.getLastPathComponent())) {
            return path;
        } else {
            final TreePath parent = substPath(tp.getParentPath(), oldPath, path);
            return parent.pathByAddingChild(
                    new de.schlichtherle.truezip.file.TFile((TFile) parent.getLastPathComponent(),
                             file.getName()));
        }
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the successor to the deleted node
     * is selected and visible.
     */
    public boolean delete(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;
        scrollPathToVisible(path);
        final int row = getRowForPath(path);

        if (!ftm.delete(node))
            return false;

        setSelectionRow(row);

        return true;
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the successor to the deleted node
     * is selected and visible.
     */
    public boolean deleteAll(final de.schlichtherle.truezip.file.TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path == null)
            return false;
        scrollPathToVisible(path);
        final int row = getRowForPath(path);

        if (!ftm.deleteAll(node))
            return false;

        setSelectionRow(row);

        return true;
    }

    public void setSelectionNode(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path != null) {
            setSelectionPath(path);
        }
    }

    public void setSelectionNodes(final TFile[] nodes) {
        final TFileTreeModel ftm = getModel();
        final java.util.List<TreePath> list = new LinkedList<TreePath>();
        TreePath lastPath = null;
        for (int i = 0, l = nodes.length; i < l; i++) {
            lastPath = ftm.newTreePath(nodes[i]);
            if (lastPath != null)
                list.add(lastPath);
        }

        final int size = list.size();
        if (size > 0) {
            final TreePath[] paths = new TreePath[size];
            list.toArray(paths);
            setSelectionPaths(paths);
        }
    }

    public void scrollNodeToVisible(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (path != null)
            scrollPathToVisible(path);
    }

    private final class Controller
    implements TreeExpansionListener, CellEditorListener, Serializable {
        private static final long serialVersionUID = 6402557248752695675L;

        @Override
        public void treeCollapsed(TreeExpansionEvent evt) {
            getModel().forget((TFile) evt.getPath().getLastPathComponent());
        }

        @Override
        public void treeExpanded(TreeExpansionEvent evt) {
        }

        @Override
        public void editingCanceled(ChangeEvent evt) {
        }

        @Override
        public void editingStopped(ChangeEvent evt) {
            onEditingStopped(evt);
        }
    } // class Controller
}
