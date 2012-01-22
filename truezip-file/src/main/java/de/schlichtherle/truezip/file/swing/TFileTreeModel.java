/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A {@link TreeModel} which traverses {@link TFile} instances.
 * The {@link TArchiveDetector} of the root of this tree model is used to
 * detect any archive files in the directory tree.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class TFileTreeModel implements TreeModel {

    /**
     * Used to cache the contents of directories.
     * Maps {@link TFile} -&gt; {@link TFile}[] instances.
     */
    private final Map<TFile, TFile[]> cache = new HashMap<TFile, TFile[]>();

    private final @CheckForNull TFile root;

    private final @CheckForNull FileFilter filter;

    /** A comparator for {@code TFile} or super classes. */
    private final Comparator<? super TFile> comparator;

    private final EventListenerList listeners = new EventListenerList();

    /**
     * Creates a new {@code TFileTreeModel} which browses the specified
     * {@code root} file.
     * If {@code file} is an instance of {@link TFile},
     * its archive detector is used to detect any archive files in the
     * directory tree.
     *
     * @param root The root of this {@code TFileTreeModel}.
     *        If this is {@code null}, an empty tree is created.
     * @param filter Used to filter the files and directories which are
     *        present in this {@code TreeModel}.
     *        If this is {@code null}, all files are accepted.
     * @param comparator A comparator for {@code TFile} instances
     *        or super classes.
     *        This must not be {@code null}.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @throws IllegalArgumentException If {@code root} isn't
     *         {@code null} and comparing it to itself didn't result in
     *         {@code 0}.
     * @throws ClassCastException If {@code root} isn't
     *         {@code null} and {@code comparator} isn't a
     *         {@code Comparator} for {@code TFile} or super
     *         class instances.
     */
    public TFileTreeModel(
            final @CheckForNull TFile root,
            final @CheckForNull FileFilter filter,
            final Comparator<? super TFile> comparator) {
        if (null != root && 0 != comparator.compare(root, root))
            throw new IllegalArgumentException();
        if (null == comparator)
            throw new NullPointerException();
        this.root = root;
        this.filter = filter;
        this.comparator = comparator;
    }

    @Override
    public TFile getRoot() {
        return root;
    }

    @Override
    public @Nullable TFile getChild(Object parent, int index) {
        final TFile[] children = getChildren((TFile) parent);
        return children != null ? children[index] : null;
    }

    @Override
    public int getChildCount(Object parent) {
        final TFile[] children = getChildren((TFile) parent);
        return children != null ? children.length : 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return !((TFile) node).isDirectory();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null)
            return -1;
        final TFile[] children = getChildren((TFile) parent);
        if (children == null)
            return -1;
        for (int i = 0, l = children.length; i < l; i++)
            if (children[i].equals(child))
                return i;
        return -1;
    }

    private @Nullable TFile[] getChildren(final TFile parent) {
        assert parent != null;
        TFile[] children = cache.get(parent);
        if (children == null) {
            if (cache.containsKey(parent))
                return null; // parent is file or inaccessible directory
            children = parent.listFiles(filter);
            // Order is important here: FILE_NAME_COMPARATOR causes a
            // recursion if the children contain an RAES encrypted ZIP file
            // for which a password needs to be prompted.
            // This is caused by the painting manager which repaints the tree
            // model in the background while the prompting dialog is showing
            // in the foreground.
            // In this case, we will simply return the unsorted result in the
            // recursion, which is then used for repainting.
            cache.put(parent, children);
            if (children != null)
                Arrays.sort(children, comparator);
        }
        return children;
    }

    /**
     * Returns a new {@link TreePath} for the given {@code node} or
     * {@code null} if {@code node} is not part of this file tree or
     * is {@code null}.
     */
    @Nullable TreePath newTreePath(final TFile node) {
        TFile[] elements = newPath(node);
        return null == elements ? null : new TreePath(elements);
    }

    /**
     * Returns an array of {@link TFile} objects indicating the path
     * from the root to the given node.
     *
     * @param node The {@code TFile} object to get the path for.
     * @return An array of {@code TFile} objects, suitable as a constructor
     *         argument for {@link TreePath} or {@code null} if
     *         {@code node} is not part of this file tree or is
     *         {@code null}.
     */
    private @Nullable TFile[] newPath(final TFile node) {
        if (null == root /*|| !TFile.contains(root, node)*/)
            return null;
        // Do not apply the filter here! The filter could depend on the file's
        // state and this method may get called before the node is initialized
        // to a state which would be accepted by the filter.
        /*if (filter != null && !((FileFilter) filter).accept(node))
            return null;*/
        return newPath(node, 1);
    }

    private @Nullable TFile[] newPath(final @CheckForNull TFile node, int level) {
        assert root != null; // FindBugs
        final TFile[] path;
        if (root.equals(node)) {
            path = new TFile[level];
            path[0] = root;
        } else if (null != node) {
            path = newPath(node.getParentFile(), level + 1);
            if (path != null)
                path[path.length - level] = node;
        } else {
            path = null;
        }
        return path;
    }

    /**
     * Creates {@code node} as a new file in the file system
     * and updates the tree accordingly.
     * If you would like to create a new file with initial content, please
     * use {@link #cp(InputStream, TFile)} instead.
     * Note that the current selection may get lost.
     *
     * @return Whether or not the file has been newly created.
     * @throws IOException if any I/O error occurs.
     */
    public boolean createNewFile(final TFile node)
    throws IOException {
        if (!node.createNewFile())
            return false;
        nodeInserted(node);
        return true;
    }

    /**
     * Ensures that {@code node} exists as a (virtual) directory in the
     * (federated) file system and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @param  recursive whether or not any missing ancestor directories shall
     *         get created if required.
     * @throws IOException if any I/O error occurs.
     */
    public void mkdir(final TFile node, final boolean recursive)
    throws IOException {
        node.mkdir(recursive);
        nodeInserted(node);
    }

    /**
     * Copies {@code in} to {@code node}
     * and updates the tree accordingly.
     * Note that the given stream is <em>always</em> closed and
     * that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void cp(final InputStream in, final TFile node) throws IOException {
        TFile.cp(in, node);
        nodeInsertedOrStructureChanged(node);
    }

    /**
     * Copies {@code oldNode} to {@code node}
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void cp(final TFile oldNode, final TFile node) throws IOException {
        TFile.cp(oldNode, node);
        nodeInsertedOrStructureChanged(node);
    }

    /**
     * Copies {@code oldNode} to {@code node} recursively
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void cp_r(final TFile oldNode, final TFile node) throws IOException {
        try {
            oldNode.cp_r(node);
        } finally {
            nodeInsertedOrStructureChanged(node);
        }
    }

    /**
     * Copies {@code oldNode} to {@code node}
     * preserving its last modification time
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void cp_p(final TFile oldNode, final TFile node) throws IOException {
        TFile.cp_p(oldNode, node);
        nodeInsertedOrStructureChanged(node);
    }

    /**
     * Copies {@code oldNode} to {@code node} recursively
     * preserving its last modification time
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void cp_rp(final TFile oldNode, final TFile node) throws IOException {
        try {
            oldNode.cp_rp(node);
        } finally {
            nodeInsertedOrStructureChanged(node);
        }
    }

    /**
     * Moves {@code oldNode} to {@code node}
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void mv(TFile oldNode, TFile node) throws IOException {
        oldNode.mv(node);
        nodeRemoved(oldNode);
        nodeInserted(node);
    }

    /**
     * Deletes the file or empty directory {@code node}
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void rm(TFile node) throws IOException {
        TFile.rm(node);
        nodeRemoved(node);
    }

    /**
     * Deletes the file or (probably not empty) directory {@code node}
     * and updates the tree accordingly.
     * Note that the current selection may get lost.
     *
     * @throws IOException if any I/O error occurs.
     */
    public void rm_r(TFile node) throws IOException {
        TFile.rm_r(node);
        nodeRemoved(node);
    }

    /**
     * Inserts the given node in the tree or reloads the tree structure for
     * the given node if it already exists.
     * This method calls {@link TreeModelListener#treeNodesInserted(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void nodeInsertedOrStructureChanged(final TFile node) {
        if (node == null)
            throw new NullPointerException();
        if (cache.containsKey(node))
            structureChanged(node);
        else
            nodeInserted(node);
    }

    /**
     * Inserts the given node in the tree.
     * If {@code node} already exists, nothing happens.
     * This method calls {@link TreeModelListener#treeNodesInserted(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void nodeInserted(final TFile node) {
        if (cache.containsKey(node))
            return;
        final TFile parent = node.getParentFile();
        forget(parent, false);
        final int index = getIndexOfChild(parent, node); // new index
        if (index == -1)
            return;
        fireTreeNodesInserted(new TreeModelEvent(
                this, newTreePath(parent),
                new int[] { index }, new TFile[] { node }));
    }

    /**
     * Updates the given node in the tree.
     * This method calls {@link TreeModelListener#treeNodesChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void nodeChanged(final TFile node) {
        final TFile parent = node.getParentFile();
        final int index = getIndexOfChild(parent, node); // old index
        if (index == -1)
            return;
        fireTreeNodesChanged(new TreeModelEvent(
                this, newTreePath(parent),
                new int[] { index }, new TFile[] { node }));
    }

    /**
     * Removes the given node from the tree.
     * This method calls {@link TreeModelListener#treeNodesRemoved(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void nodeRemoved(final TFile node) {
        final TFile parent = node.getParentFile();
        final int index = getIndexOfChild(parent, node); // old index
        if (index == -1)
            return;
        forget(node, true);
        forget(parent, false);
        // Fill cache again so that subsequent removes don't suffer a cache miss.
        // Otherwise, the display wouldn't mirror the cache anymore.
        getChildren(parent);
        fireTreeNodesRemoved(new TreeModelEvent(
                this, newTreePath(parent),
                new int[] { index }, new TFile[] { node }));
    }

    /**
     * Refreshes the tree structure for the entire tree.
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void refresh() {
        cache.clear();
        if (root != null)
            fireTreeStructureChanged(
                    new TreeModelEvent(this, newTreePath(root), null, null));
    }

    /** Alias for {@link #structureChanged(TFile)}. */
    public final void refresh(final TFile node) {
        structureChanged(node);
    }

    /**
     * Reloads the tree structure for the given node.
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     */
    public void structureChanged(final TFile node) {
        if (node == null)
            throw new NullPointerException();
        forget(node, true);
        fireTreeStructureChanged(
                new TreeModelEvent(this, newTreePath(node), null, null));
    }

    /**
     * Clears the internal cache associated with {@code node} and all
     * of its children.
     */
    void forget(TFile node) {
        forget(node, true);
    }

    /**
     * Clears the internal cache associated with {@code node}.
     *
     * @param childrenToo If and only if {@code true}, the internal
     *        cache for all children is cleared, too.
     */
    private void forget(
            final @Nullable TFile node,
            final boolean childrenToo) {
        final TFile[] children = cache.remove(node);
        if (null != children && childrenToo)
            for (int i = 0, l = children.length; i < l; i++)
                forget(children[i], childrenToo);
    }

    /**
     * Adds a listener to this model.
     *
     * @param l The listener to add.
     */
    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener from this model.
     *
     * @param l The listener to remove.
     */
    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(TreeModelListener.class, l);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesChanged(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesChanged(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesInserted(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesInserted(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesRemoved(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesRemoved(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this {@code TreeModel}.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeStructureChanged(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeStructureChanged(evt);
    }
}
