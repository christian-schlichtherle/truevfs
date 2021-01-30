/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.util;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * An observer for a {@link JComboBox} which provides auto completion for the
 * editable text in the drop down list in order to provide quick browsing
 * capabilities for the user. Subclasses need to implement the {@link #update}
 * method in order to update the combo box model with the actual auto
 * completion data.
 * <p>
 * This class is designed to be minimal intrusive: It's designed to work with
 * any {@code JComboBox} and doesn't require a special {@link ComboBoxModel},
 * although its specific behaviour will only show if the {@code JComboBox} is
 * {@code editable} and uses a {@link MutableComboBoxModel} (which, besides the
 * {@code editable} property being set to {@code true}, is the default setup
 * for a vanilla {@code JComboBox}).
 *
 * @param <E> the type of the elements of this combo box browser.
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public abstract class AbstractComboBoxBrowser<E> implements Serializable {

    private static final long serialVersionUID = 1065103960246722893L;

    private final Listener listener = new Listener();

    private @Nullable JComboBox<E> comboBox;

    /**
     * Used to inhibit mutual recursive event firing.
     */
    private transient boolean recursion;

    /**
     * Creates a new combo box auto completion browser.
     * {@link #setComboBox} must be called in order to use this object.
     */
    protected AbstractComboBoxBrowser() { }

    /**
     * Creates a new combo box auto completion browser. Note that this
     * constructor does <em>not</em> call {@link #update} and hence the drop
     * down list of the combo box is left unchanged.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     * May be {@code null}.
     */
    protected AbstractComboBoxBrowser(final @Nullable JComboBox<E> comboBox) {
        changeComboBox(null, comboBox, false);
    }

    /**
     * Returns the combo box which this object is auto completing.
     * The default is {@code null}.
     *
     * @return The combo box which this object is auto completing.
     */
    public @Nullable JComboBox<E> getComboBox() { return comboBox; }

    /**
     * Sets the combo box which this object is auto completing and updates the
     * drop down list with the auto completion for the currently selected item.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public void setComboBox(final @Nullable JComboBox<E> comboBox) {
        changeComboBox(getComboBox(), comboBox, true);
    }

    private void changeComboBox(
            final @Nullable JComboBox<E> oldCB,
            final @Nullable JComboBox<E> newCB,
            final boolean update) {
        if (newCB == oldCB) return;

        ComboBoxEditor oldCBE = null;
        if (null != oldCB) {
            oldCB.removePropertyChangeListener("editor", listener);
            oldCBE = oldCB.getEditor();
            oldCB.setEditor(((AbstractComboBoxBrowser<?>.DecoratingComboBoxEditor) oldCBE).getEditor());
        }

        this.comboBox = newCB;

        ComboBoxEditor newCBE = null;
        if (null != newCB) {
            newCB.updateUI(); // ensure comboBoxEditor is initialized
            newCBE = new DecoratingComboBoxEditor(newCB.getEditor());
            newCB.setEditor(newCBE);
            newCB.addPropertyChangeListener("editor", listener);
        }

        changeEditor(oldCBE, newCBE, update);
    }

    private void changeEditor(
            final @Nullable ComboBoxEditor oldCBE,
            final @Nullable ComboBoxEditor newCBE,
            final boolean update) {
        if (newCBE == oldCBE) return;

        JTextComponent oldText = null;
        if (null != oldCBE) {
            final Component component = oldCBE.getEditorComponent();
            if (component instanceof JTextComponent)
                oldText = (JTextComponent) component;
        }

        JTextComponent newText = null;
        if (null != newCBE) {
            final Component component = newCBE.getEditorComponent();
            if (component instanceof JTextComponent)
                newText = (JTextComponent) component;
        }

        changeText(oldText, newText, update);
    }

    private void changeText(
            final @Nullable JTextComponent oldTC,
            final @Nullable JTextComponent newTC,
            final boolean update) {
        if (newTC == oldTC) return;

        Document oldDocument = null;
        if (null != oldTC) {
            oldTC.removePropertyChangeListener("document", listener);
            oldDocument = oldTC.getDocument();
        }

        Document newDocument = null;
        if (null != newTC) {
            newDocument = newTC.getDocument();
            newTC.addPropertyChangeListener("document", listener);
        }

        changeDocument(oldDocument, newDocument, update);
    }

    private void changeDocument(
            final @Nullable Document oldDoc,
            final @Nullable Document newDoc,
            final boolean update) {
        if (newDoc == oldDoc) return;

        if (null != oldDoc) oldDoc.removeDocumentListener(listener);

        if (null != newDoc) {
            if (update) {
                String txt;
                try {
                    txt = newDoc.getText(0, newDoc.getLength());
                } catch (BadLocationException e) {
                    txt = "";
                }
                update(txt);
            }
            newDoc.addDocumentListener(listener);
        }
    }

    private void documentUpdated() {
        if (lock()) return;
        try {
            final JComboBox<E> cb = getComboBox();
            final ComboBoxEditor cbe = cb.getEditor();
            final JTextComponent tc = (JTextComponent) cbe.getEditorComponent();
            assert cb.isShowing() || !tc.isFocusOwner();
            if (!tc.isFocusOwner() /* || !cb.isShowing() */) return;

            //cb.setPopupVisible(update(tc.getText())); // doesn't work: adjusts popup size!
            cb.setPopupVisible(false);
            if (update(tc.getText())) cb.setPopupVisible(true);
        } finally {
            unlock();
        }
    }

    private void updateEditor(final ComboBoxEditor cbe, final @Nullable Object item) {
        if (lock()) return;
        try {
            cbe.setItem(item);
            if (!(item instanceof String)) return;

            final JComboBox<E> cb = getComboBox();
            final JTextComponent tc = (JTextComponent) cbe.getEditorComponent();
            assert cb.isShowing() || !tc.isFocusOwner();
            if (!tc.isFocusOwner() /* || !cb.isShowing() */) return;

            // Compensate for an issue with some look and feels
            // which select the entire tc if an item is changed.
            // This is inconvenient for auto completion because the
            // next typed character would replace the entire tc...
            final Caret caret = tc.getCaret();
            caret.setDot(((String) item).length());
        } finally {
            unlock();
        }
    }

    /**
     * Subclasses are expected to update the auto completion elements in the
     * model of this combo box based on the specified {@code initials}.
     * They should not do any other work within this method.
     * In particular, they should not update the visual appearance of this
     * component.
     * <p>
     * {@link #getComboBox} is guaranteed to return non-{@code null} if this
     * method is called from this abstract base class.
     *
     * @param  initials The text to auto complete..
     * @return Whether or not the combo box should pop up to show the updated
     *         contents of its model.
     */
    protected abstract boolean update(String initials);

    /**
     * Locks out mutual recursive event notification.
     * <b>Warning:</b> This method works in a synchronized or single threaded
     * environment only!
     *
     * @return Whether or not updating the combo box model was already locked.
     */
    private boolean lock() {
        if (recursion) return true;
        recursion = true;
        return false;
    }

    /**
     * Unlocks mutual recursive event notification.
     * <b>Warning:</b> This method works in a synchronized or single threaded
     * environment only!
     */
    private void unlock() { recursion = false; }

    private class Listener implements DocumentListener, PropertyChangeListener {

        @Override
        public void insertUpdate(DocumentEvent e) { documentUpdated(); }

        @Override
        public void removeUpdate(DocumentEvent e) { documentUpdated(); }

        @Override
        public void changedUpdate(DocumentEvent e) { documentUpdated(); }

        @Override
        public void propertyChange(final PropertyChangeEvent e) {
            final String property = e.getPropertyName();
            if ("editor".equals(property))
                changeEditor(   (ComboBoxEditor) e.getOldValue(),
                                (ComboBoxEditor) e.getNewValue(),
                                true);
            else if ("document".equals(property))
                changeDocument( (Document) e.getOldValue(),
                                (Document) e.getNewValue(),
                                true);
            else
                throw new AssertionError(
                        "Received change event for unknown property: "
                        + property);
        }
    }

    /**
     * This proxy controls access to the real {@code ComboBoxEditor} installed
     * by the client application or the pluggable look and feel.
     * It is used to inhibit mutual recursion caused by modifications to the
     * list model in the {@code JComboBox}.
     */
    private final class DecoratingComboBoxEditor implements ComboBoxEditor {
        private final ComboBoxEditor editor;

        DecoratingComboBoxEditor(ComboBoxEditor editor) {
            assert null != editor;
            this.editor = editor;
        }

        /** Returns the decorated combo box editor. */
        ComboBoxEditor getEditor() { return editor; }

        @Override
        public Component getEditorComponent() {
            return editor.getEditorComponent();
        }

        @Override
        public void setItem(final @Nullable Object item) {
            updateEditor(editor, item);
        }

        @Override
        public @Nullable Object getItem() { return editor.getItem(); }

        @Override
        public void selectAll() { editor.selectAll(); }

        @Override
        public void addActionListener(ActionListener actionListener) {
            editor.addActionListener(actionListener);
        }

        @Override
        public void removeActionListener(ActionListener actionListener) {
            editor.removeActionListener(actionListener);
        }
    }
}
