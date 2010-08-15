/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.swing.tree;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.event.*;
import javax.swing.tree.*;

/**
 * A {@link TreeModel} which traverses {@link java.io.File java.io.File}
 * instances.
 * If the root of this tree model is actually an instance of
 * {@link de.schlichtherle.io.File de.schlichtherle.io.File}, its archive
 * detector is used to detect any archive files in the directory tree.
 * This allows you to traverse archive files just like directories.
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 5.1
 */
public class FileTreeModel implements TreeModel {

    /**
     * A collator for file names which considers case according to the
     * platform's standard.
     */
    private static final Collator collator = Collator.getInstance();
    static {
        // Set minimum requirements for maximum performance.
        collator.setDecomposition(Collator.NO_DECOMPOSITION);
        collator.setStrength(java.io.File.separatorChar == '\\'
                ? Collator.SECONDARY
                : Collator.TERTIARY);
    }

    /** A comparator which sorts directory entries to the beginning. */
    public static final Comparator FILE_NAME_COMPARATOR = new Comparator() {
        public final int compare(Object o1, Object o2) {
            return compare((java.io.File) o1, (java.io.File) o2);
        }

        public int compare(java.io.File f1, java.io.File f2) {
            if (f1.isDirectory())
                return f2.isDirectory()
                        ? collator.compare(f1.getName(), f2.getName())
                        : -1;
            else
                return f2.isDirectory()
                        ? 1
                        : collator.compare(f1.getName(), f2.getName());
        }
    };

    /**
     * Used to cache the contents of directories.
     * Maps {@link java.io.File} -&gt; {@link java.io.File}[] instances.
     */
    // Tactical note: Working with a WeakHashMap shows strange results.
    private final Map cache = new HashMap();

    private final java.io.File root;

    private final FileFilter filter;

    /** A comparator for <code>java.io.File</code> or super classes. */
    private final Comparator comparator;

    private final EventListenerList listeners = new EventListenerList();

    /**
     * Equivalent to {@link #FileTreeModel(java.io.File, FileFilter, Comparator)
     * FileTreeModel(null, null, FILE_NAME_COMPARATOR)}.
     * This constructor isn't particularly useful - it's only provided to
     * implement the JavaBean pattern.
     */
    public FileTreeModel() {
        this(null, null, FILE_NAME_COMPARATOR);
    }
    
    /**
     * Equivalent to {@link #FileTreeModel(java.io.File, FileFilter, Comparator)
     * FileTreeModel(root, null, FILE_NAME_COMPARATOR)}.
     */
    public FileTreeModel(final java.io.File root) {
        this(root, null, FILE_NAME_COMPARATOR);
    }
    
    /**
     * Equivalent to {@link #FileTreeModel(java.io.File, FileFilter, Comparator)
     * FileTreeModel(root, filter, FILE_NAME_COMPARATOR)}.
     */
    public FileTreeModel(
            final java.io.File root,
            final FileFilter filter) {
        this(root, filter, FILE_NAME_COMPARATOR);
    }
    
    /**
     * Creates a new <code>FileTreeModel</code> which browses the specified
     * <code>root</code> file.
     * If <code>file</code> is an instance of {@link de.schlichtherle.io.File},
     * its archive detector is used to detect any archive files in the
     * directory tree.
     *
     * @param root The root of this <code>FileTreeModel</code>.
     *        If this is <code>null</code>, an empty tree is created.
     * @param filter Used to filter the files and directories which are
     *        present in this <code>TreeModel</code>.
     *        If this is <code>null</code>, all files are accepted.
     * @param comparator A comparator for <code>java.io.File</code> instances
     *        or super classes.
     *        This must not be <code>null</code>.
     * @throws NullPointerException If <code>comparator</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>root</code> isn't
     *         <code>null</code> and comparing it to itself didn't result in
     *         <code>0</code>.
     * @throws ClassCastException If <code>root</code> isn't
     *         <code>null</code> and <code>comparator</code> isn't a
     *         <code>Comparator</code> for <code>java.io.File</code> or super
     *         class instances.
     * @since TrueZIP 6.5.4 (this constructor)
     */
    public FileTreeModel(
            final java.io.File root,
            final FileFilter filter,
            final Comparator comparator) {
        if (comparator == null)
            throw new NullPointerException();
        if (root != null && comparator.compare(root, root) != 0)
            throw new IllegalArgumentException();
        this.root = root;
        this.filter = filter;
        this.comparator = comparator;
    }

    //
    // TreeModel implementation.
    //

    /**
     * Returns the root of this tree model.
     * This is actually an instance of {@link java.io.File java.io.File} or
     * a subclass, like
     * {@link de.schlichtherle.io.File de.schlichtherle.io.File}.
     * 
     * @return A <code>File</code> object or <code>null</code> if this tree
     *         model has not been created with a <code>File</code> object.
     */
    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        final java.io.File[] children = getChildren((java.io.File) parent);
        return children != null ? children[index] : null;
    }

    public int getChildCount(Object parent) {
        final java.io.File[] children = getChildren((java.io.File) parent);
        return children != null ? children.length : 0;
    }

    public boolean isLeaf(Object node) {
        return !((java.io.File) node).isDirectory();
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null)
            return -1;
        final java.io.File[] children = getChildren((java.io.File) parent);
        if (children == null)
            return -1;
        for (int i = 0, l = children.length; i < l; i++)
            if (children[i].equals(child))
                return i;
        return -1;
    }

    private java.io.File[] getChildren(final java.io.File parent) {
        assert parent != null;
        java.io.File[] children = (java.io.File[]) cache.get(parent);
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
                Arrays.sort(children, FILE_NAME_COMPARATOR);
        }
        return children;
    }

    //
    // TreePath retrieval.
    //

    /**
     * Forwards the call to {@link #createTreePath}.
     *
     * @deprecated Use {@link #createTreePath} instead.
     */
    public TreePath getTreePath(java.io.File node) {
        return createTreePath(node);
    }

    /**
     * Returns a {@link TreePath} for the given <code>node</code> or
     * <code>null</code> if the node is not part of this file tree.
     */
    public TreePath createTreePath(java.io.File node) {
        java.io.File[] elements = createPath(node);
        return elements != null ? new TreePath(elements) : null;
    }

    /**
     * Returns an array of {@link java.io.File} objects indicating the path
     * from the root to the given node.
     * 
     * @param node The <code>File</code> object to get the path for.
     * @return An array of <code>File</code> objects, suitable as a constructor
     *         argument for {@link TreePath} or <code>null</code> if
     *         <code>node</code> is not part of this file tree.
     */
    private java.io.File[] createPath(java.io.File node) {
        if (root == null /*|| !de.schlichtherle.io.File.contains(root, node)*/)
            return null;
        // Do not apply the filter here! The filter could depend on the file's
        // state and this method may get called before the node is initialized
        // to a state which would be accepted by the filter.
        /*if (filter != null && !((FileFilter) filter).accept(node))
            return null;*/
        return createPath(node, 1);
    }

    private java.io.File[] createPath(final java.io.File node, int level) {
        assert root != null; // FindBugs
        final java.io.File[] path;
        if (/*node == null ||*/ root.equals(node)) {
            path = new java.io.File[level];
            path[0] = root;
        } else if (node != null) {
            path = createPath(node.getParentFile(), level + 1);
            if (path != null)
                path[path.length - level] = node;
        } else {
            path = null;
        }
        return path;
    }

    //
    // File system operations.
    //

    /**
     * Creates <code>node</code> as a new file in the file system
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     * If you would like to create a new file with initial content, please
     * use {@link #copyFrom(de.schlichtherle.io.File, InputStream)}.
     *
     * @return Whether or not the file has been newly created.
     * @throws IOException If an I/O error occurs.
     */
    public boolean createNewFile(final java.io.File node)
    throws IOException {
        if (!node.createNewFile())
            return false;
        nodeInserted(node);
        return true;
    }

    /**
     * Creates <code>node</code> as a new directory
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been newly created.
     * @throws IOException If an I/O error occurs.
     */
    public boolean mkdir(final java.io.File node) {
        if (!node.mkdir())
            return false;
        nodeInserted(node);
        return true;
    }

    /**
     * Creates <code>node</code> as a new directory, including all parents,
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been newly created.
     * @throws IOException If an I/O error occurs.
     */
    public boolean mkdirs(final java.io.File node) {
        if (!node.mkdirs())
            return false;
        nodeInserted(node);
        return true;
    }

    /**
     * Creates <code>node</code> as a new file with the contents read from
     * <code>in</code> and updates the tree accordingly.
     * However, the current selection may get lost.
     * Note that the given stream is <em>always</em> closed.
     *
     * @return Whether or not the file has been newly created.
     * @throws IOException If an I/O error occurs.
     */
    public boolean copyFrom(final de.schlichtherle.io.File node, final InputStream in) {
        if (!node.copyFrom(in))
            return false;
        nodeInsertedOrStructureChanged(node);
        return true;
    }

    /**
     * Copies <code>oldNode</code> to <code>node</code>
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been successfully renamed.
     */
    public boolean copyTo(final de.schlichtherle.io.File oldNode, final java.io.File node) {
        if (!oldNode.copyTo(node))
            return false;
        nodeInsertedOrStructureChanged(node);
        return true;
    }

    /**
     * Copies <code>oldNode</code> to <code>node</code> recursively
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been successfully renamed.
     */
    public boolean copyAllTo(final de.schlichtherle.io.File oldNode, final java.io.File node) {
        final boolean ok = oldNode.copyAllTo(node);
        nodeInsertedOrStructureChanged(node);
        return ok;
    }

    /**
     * Copies <code>oldNode</code> to <code>node</code>, preserving
     * its last modification time
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been successfully renamed.
     */
    public boolean archiveCopyTo(final de.schlichtherle.io.File oldNode, final java.io.File node) {
        if (!oldNode.archiveCopyTo(node))
            return false;
        nodeInsertedOrStructureChanged(node);
        return true;
    }

    /**
     * Copies <code>oldNode</code> to <code>node</code> recursively, preserving
     * its last modification time
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been successfully renamed.
     */
    public boolean archiveCopyAllTo(final de.schlichtherle.io.File oldNode, final java.io.File node) {
        final boolean ok = oldNode.archiveCopyAllTo(node);
        nodeInsertedOrStructureChanged(node);
        return ok;
    }

    /**
     * Renames <code>oldNode</code> to <code>newNode</code>
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file has been successfully renamed.
     */
    public boolean renameTo(
            final java.io.File oldNode,
            final java.io.File newNode) {
        if (!oldNode.renameTo(newNode))
            return false;
        nodeRemoved(oldNode);
        nodeInserted(newNode);
        return true;
    }

    /**
     * Deletes the file or empty directory <code>node</code>
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file or directory has been successfully deleted.
     * @throws IOException If an I/O error occurs.
     */
    public boolean delete(final java.io.File node) {
        if (!node.delete())
            return false;
        nodeRemoved(node);
        return true;
    }

    /**
     * Deletes the file or (probably not empty) directory <code>node</code>
     * and updates the tree accordingly.
     * However, the current selection may get lost.
     *
     * @return Whether or not the file or directory has been successfully deleted.
     * @throws IOException If an I/O error occurs.
     */
    public boolean deleteAll(final de.schlichtherle.io.File node) {
        if (!node.deleteAll())
            return false;
        nodeRemoved(node);
        return true;
    }

    //
    // File system change notifications.
    //

    /**
     * Inserts the given node in the tree or reloads the tree structure for
     * the given node if it already exists.
     * This method calls {@link TreeModelListener#treeNodesInserted(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void nodeInsertedOrStructureChanged(final java.io.File node) {
        if (node == null)
            throw new NullPointerException();
        if (cache.containsKey(node))
            structureChanged(node);
        else
            nodeInserted(node);
    }

    /**
     * Inserts the given node in the tree.
     * If <code>node</code> already exists, nothing happens.
     * This method calls {@link TreeModelListener#treeNodesInserted(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void nodeInserted(final java.io.File node) {
        if (cache.containsKey(node))
            return;
        final java.io.File parent = node.getParentFile();
        forget(parent, false);
        final int index = getIndexOfChild(parent, node); // new index
        if (index == -1)
            return;
        fireTreeNodesInserted(new TreeModelEvent(
                this, createTreePath(parent),
                new int[] { index }, new java.io.File[] { node }));
    }

    /**
     * Updates the given node in the tree.
     * This method calls {@link TreeModelListener#treeNodesChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void nodeChanged(final java.io.File node) {
        final java.io.File parent = node.getParentFile();
        final int index = getIndexOfChild(parent, node); // old index
        if (index == -1)
            return;
        fireTreeNodesChanged(new TreeModelEvent(
                this, createTreePath(parent),
                new int[] { index }, new java.io.File[] { node }));
    }

    /**
     * Removes the given node from the tree.
     * This method calls {@link TreeModelListener#treeNodesRemoved(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void nodeRemoved(final java.io.File node) {
        final java.io.File parent = node.getParentFile();
        final int index = getIndexOfChild(parent, node); // old index
        if (index == -1)
            return;
        forget(node, true);
        forget(parent, false);
        // Fill cache again so that subsequent removes don't suffer a cache miss.
        // Otherwise, the display wouldn't mirror the cache anymore.
        getChildren(parent);
        fireTreeNodesRemoved(new TreeModelEvent(
                this, createTreePath(parent),
                new int[] { index }, new java.io.File[] { node }));
    }

    /**
     * Refreshes the tree structure for the entire tree.
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void refresh() {
        cache.clear();
        if (root != null)
            fireTreeStructureChanged(
                    new TreeModelEvent(this, createTreePath(root), null, null));
    }

    /** Alias for {@link #structureChanged(java.io.File)}. */
    public final void refresh(final java.io.File node) {
        structureChanged(node);
    }

    /**
     * Reloads the tree structure for the given node.
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     */
    public void structureChanged(final java.io.File node) {
        if (node == null)
            throw new NullPointerException();
        forget(node);
        fireTreeStructureChanged(
                new TreeModelEvent(this, createTreePath(node), null, null));
    }

    /**
     * Clears the internal cache associated with <code>node</code> and all
     * of its children.
     *
     * @deprecated This method is only public in order to make it available to
     *             {@link de.schlichtherle.io.swing.JFileTree}
     *             - it is <em>not</em> intended for public use.
     *             In particular, this method does <em>not</em> notify the
     *             tree of any structural changes in the file system.
     */
    public final void forget(final java.io.File node) {
        forget(node, true);
    }

    /**
     * Clears the internal cache associated with <code>node</code>.
     *
     * @param childrenToo If and only if <code>true</code>, the internal
     *        cache for all children is cleared, too.
     */
    private void forget(
            final java.io.File node,
            final boolean childrenToo) {
        final java.io.File[] children = (java.io.File[]) cache.remove(node);
        if (children != null && childrenToo)
            for (int i = 0, l = children.length; i < l; i++)
                forget(children[i], childrenToo);
    }

    //
    // Event handling.
    //

    /**
     * Adds a listener to this model.
     *
     * @param l The listener to add.
     */
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener from this model.
     *
     * @param l The listener to remove.
     */
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(TreeModelListener.class, l);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesChanged(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesChanged(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesInserted(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesInserted(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeNodesRemoved(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeNodesRemoved(evt);
    }

    /**
     * This method calls {@link TreeModelListener#treeStructureChanged(TreeModelEvent)}
     * on all listeners of this <code>TreeModel</code>.
     * May be used to tell the listeners about a change in the file system.
     */
    protected void fireTreeStructureChanged(final TreeModelEvent evt) {
        final EventListener[] l = listeners.getListeners(TreeModelListener.class);
        for (int i = 0, ll = l.length; i < ll; i++)
            ((TreeModelListener) l[i]).treeStructureChanged(evt);
    }
}
