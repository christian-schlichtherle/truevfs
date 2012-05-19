/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.file.swing;

import de.truezip.file.TFile;
import de.truezip.file.TFileComparator;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
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
 */
public final class TFileTree extends JTree {

    private static final long serialVersionUID = 1064787562479927601L;

    /** The name of the property {@code displayingExtensions}. */
    private static final String PROPERTY_DISPLAYING_EXTENSIONS = "displayingExtensions"; // NOI18N

    /** The name of the property {@code editingExtensions}. */
    private static final String PROPERTY_EDITING_EXTENSIONS = "editingExtensions"; // NOI18N

    /** The name of the property {@code defaultExtension}. */
    private static final String PROPERTY_DEFAULT_EXTENSION = "defaultExtension"; // NOI18N

    private final Controller controller = new Controller();

    private boolean displayingExtensions = true;

    private boolean editingExtensions = true;

    private @CheckForNull String defaultExtension;

    private transient @CheckForNull TFile editedNode;

    /**
     * Creates an empty {@code TFileTree} with no root.
     * You shouldn't use this constructor.
     * It's only provided to implement the JavaBean pattern.
     */
    public TFileTree() {
        this(new TFileTreeModel(null, null, new TFileComparator()));
    }

    /**
     * Creates a new {@code TFileTree} which traverses the given
     * root {@code root} file.
     */
    public TFileTree(TFile root) {
        this(new TFileTreeModel(root, null, new TFileComparator()));
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
        super.setModel((TFileTreeModel) Objects.requireNonNull(model));
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
     * Getter for bound property displayingExtensions.
     *
     * @return Value of property displayingExtensions.
     */
    public boolean isDisplayingExtensions() {
        return this.displayingExtensions;
    }

    /**
     * Setter for bound property displayingExtensions.
     * If this is {@code false}, the extension of files will not be displayed
     * in this tree.
     * Defaults to {@code true}.
     *
     * @param displayingExtensions New value of property displayingExtensions.
     */
    public void setDisplayingExtensions(boolean displayingExtensions) {
        boolean oldDisplayingExtensions = this.displayingExtensions;
        this.displayingExtensions = displayingExtensions;
        firePropertyChange(PROPERTY_DISPLAYING_EXTENSIONS,
                oldDisplayingExtensions, displayingExtensions);
    }

    /**
     * Getter for bound property editingExtensions.
     *
     * @return Value of property editingExtensions.
     */
    public boolean isEditingExtensions() {
        return this.editingExtensions;
    }

    /**
     * Setter for bound property editingExtensions.
     * If this is {@code false}, the extension of a file will be truncated
     * before editing its name starts.
     * Defaults to {@code true}.
     *
     * @param editingExtensions New value of property editingExtensions.
     */
    public void setEditingExtensions(boolean editingExtensions) {
        boolean oldEditingExtensions = this.editingExtensions;
        this.editingExtensions = editingExtensions;
        firePropertyChange(PROPERTY_EDITING_EXTENSIONS,
                oldEditingExtensions, editingExtensions);
    }

    /**
     * Getter for bound property defaultExtension.
     *
     * @return Value of property defaultExtension.
     */
    public @Nullable String getDefaultExtension() {
        return this.defaultExtension;
    }

    /**
     * Setter for bound property defaultExtension.
     * Sets the default extension to use when extensions are shown and allowed to
     * be edited, but the user did not provide an extension when editing a file
     * name.
     * This property defaults to {@code null} and is ignored for
     * directories.
     *
     * @param defaultExtension The new default extension.
     *        If not {@code null}, this parameter is fixed to always
     *        start with a {@code '.'}.
     */
    public void setDefaultExtension(@CheckForNull String defaultExtension) {
        final String oldDefaultExtension = this.defaultExtension;
        if (null != defaultExtension) {
            defaultExtension = defaultExtension.trim();
            if (defaultExtension.length() <= 0)
                defaultExtension = null;
            else if (defaultExtension.charAt(0) != '.')
                defaultExtension = "." + defaultExtension;
        }
        this.defaultExtension = defaultExtension;
        firePropertyChange(PROPERTY_DEFAULT_EXTENSION,
                oldDefaultExtension, defaultExtension);
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
     * obeying the rules for extension handling and updating the expanded and
     * selected paths accordingly.
     *
     * @param evt The change event passed to
     *        {@link CellEditorListener#editingStopped(ChangeEvent)}.
     */
    protected void onEditingStopped(final ChangeEvent evt) {
        final TreeCellEditor tce = (TreeCellEditor) evt.getSource();
        String member = tce.getCellEditorValue().toString().trim();
        final TFile oldNode
                = (TFile) getLeadSelectionPath().getLastPathComponent();
        final TFile parent = oldNode.getParentFile();
        assert parent != null;
        if (!oldNode.isDirectory()) {
            if (isDisplayingExtensions() && isEditingExtensions()) {
                final String extension = getExtension(member);
                if (null == extension) {
                    final String defaultExtension = getDefaultExtension();
                    if (defaultExtension != null)
                        member += defaultExtension;
                }
            } else {
                final String extension = getExtension(oldNode.getName());
                if (null != extension)
                    member += extension;
            }
        }
        final TFile node = new TFile(parent, member);
        try {
            mv(oldNode, node);
        } catch (IOException ex) {
            Logger  .getLogger(TFileTree.class.getName())
                    .log(Level.WARNING, ex.toString(), ex);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private @Nullable String getExtension(final String base) {
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
                isDisplayingExtensions()
                && (!node.equals(editedNode) || isEditingExtensions()))
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
        Objects.requireNonNull(node);

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
     * check {@link #cp(InputStream, TFile)}.
     * 
     * @throws IOException on any I/O error.
     */
    public boolean createNewFile(final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
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
     * 
     * @throws IOException on any I/O error.
     */
    public void mkdir(final TFile node, final boolean recursive)
    throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            return;
        ftm.mkdir(node, recursive);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void cp(final @WillClose InputStream in, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        ftm.cp(in, node);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void cp(final TFile oldNode, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        ftm.cp(oldNode, node);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the recursively copied node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void cp_r(final TFile oldNode, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        ftm.cp_r(oldNode, node);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the copied node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void cp_p(final TFile oldNode, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        ftm.cp_p(oldNode, node);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the recursively copied node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void cp_rp(final TFile oldNode, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        ftm.cp_rp(oldNode, node);
        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel},
     * restores the expanded paths, selects {@code node} and scrolls to
     * it if necessary.
     * 
     * @throws IOException on any I/O error.
     */
    public void mv(final TFile oldNode, final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        final Enumeration<TreePath> expansions;
        final TreePath oldPath = ftm.newTreePath(oldNode);
        expansions = oldPath == null ? null : getExpandedDescendants(oldPath);
        ftm.mv(oldNode, node);
        if (null != expansions)
            while (expansions.hasMoreElements())
                setExpandedState(
                        substPath(expansions.nextElement(), oldPath, path),
                        true);
        setSelectionPath(path);
        scrollPathToVisible(path);
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
                    new de.truezip.file.TFile((TFile) parent.getLastPathComponent(),
                             file.getName()));
        }
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the successor to the deleted node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void rm(final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        scrollPathToVisible(path);
        final int row = getRowForPath(path);
        ftm.rm(node);
        setSelectionRow(row);
    }

    /**
     * Forwards the call to the {@link TFileTreeModel}
     * and scrolls the tree so that the successor to the deleted node
     * is selected and visible.
     * 
     * @throws IOException on any I/O error.
     */
    public void rm_r(final TFile node) throws IOException {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null == path)
            throw new IllegalArgumentException("node");
        scrollPathToVisible(path);
        final int row = getRowForPath(path);
        ftm.rm_r(node);
        setSelectionRow(row);
    }

    public void setSelectionNode(final TFile node) {
        final TFileTreeModel ftm = getModel();
        final TreePath path = ftm.newTreePath(node);
        if (null != path)
            setSelectionPath(path);
    }

    public void setSelectionNodes(final TFile[] nodes) {
        final TFileTreeModel ftm = getModel();
        final java.util.List<TreePath> list = new LinkedList<>();
        for (int i = 0, l = nodes.length; i < l; i++) {
            final TreePath lastPath = ftm.newTreePath(nodes[i]);
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
