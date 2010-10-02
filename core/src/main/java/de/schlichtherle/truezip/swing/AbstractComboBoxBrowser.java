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

package de.schlichtherle.truezip.swing;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * An observer for a {@link JComboBox} which provides auto completion
 * for the editable text in the drop down list in order to provide quick
 * browsing capabilities for the user.
 * Subclasses need to implement the {@link #update} method in order to update
 * the combo box model with the actual auto completion data.
 * <p>
 * This class is designed to be minimal intrusive: It works with any subclass
 * of {@code JComboBox} and doesn't require a special
 * {@link ComboBoxModel}, although its specific behaviour will only show
 * if the {@code JComboBox} is {@code editable} and uses an
 * instance of a {@link MutableComboBoxModel} (which, apart from the
 * {@code editable} property being set to {@code true}, is the
 * default for a plain {@code JComboBox}).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class AbstractComboBoxBrowser implements Serializable {
    private static final long serialVersionUID = 1065103960246722893L;

    private final Listener listener = new Listener();

    private JComboBox comboBox;

    /**
     * Used to inhibit mutual recursive event firing.
     */
    private transient boolean recursion; // = false;

    /**
     * Creates a new combo box auto completion browser.
     * {@link #setComboBox} must be called in order to use this object.
     */
    public AbstractComboBoxBrowser() {
    }

    /**
     * Creates a new combo box auto completion browser.
     * Note that this constructor does <em>not</em> call {@link #update}
     * and hence the drop down list of the combo box is left unchanged.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public AbstractComboBoxBrowser(final JComboBox comboBox) {
        changeComboBox(null, comboBox, false);
    }

    /**
     * Returns the combo box which this object is auto completing.
     * The default is {@code null}.
     */
    public JComboBox getComboBox() {
        return comboBox;
    }

    /**
     * Sets the combo box which this object is auto completing and updates
     * the drop down list with the auto completion for the currently selected
     * item.
     *
     * @param comboBox The combo box to enable browsing for auto completions.
     *        May be {@code null}.
     */
    public void setComboBox(final JComboBox comboBox) {
        changeComboBox(getComboBox(), comboBox, true);
    }

    private void changeComboBox(
            final JComboBox oldCB,
            final JComboBox newCB,
            final boolean update) {
        if (newCB == oldCB)
            return;

        ComboBoxEditor oldCBE = null;
        if (oldCB != null) {
            oldCB.removePropertyChangeListener("editor", listener);
            oldCBE = oldCB.getEditor();
            oldCB.setEditor(((ComboBoxEditorProxy) oldCBE).getEditor());
        }

        this.comboBox = newCB;

        ComboBoxEditor newCBE = null;
        if (newCB != null) {
            newCB.updateUI(); // ensure comboBoxEditor is initialized
            newCBE = new ComboBoxEditorProxy(newCB.getEditor());
            newCB.setEditor(newCBE);
            newCB.addPropertyChangeListener("editor", listener);
        }

        changeEditor(oldCBE, newCBE, update);
    }

    private void changeEditor(
            final ComboBoxEditor oldCBE,
            final ComboBoxEditor newCBE,
            final boolean update) {
        if (newCBE == oldCBE)
            return;

        JTextComponent oldText = null;
        if (oldCBE != null) {
            final Component component = oldCBE.getEditorComponent();
            if (component instanceof JTextComponent)
                oldText = (JTextComponent) component;
        }

        JTextComponent newText = null;
        if (newCBE != null) {
            final Component component = newCBE.getEditorComponent();
            if (component instanceof JTextComponent)
                newText = (JTextComponent) component;
        }

        changeText(oldText, newText, update);
    }

    private void changeText(
            final JTextComponent oldTC,
            final JTextComponent newTC,
            final boolean update) {
        if (newTC == oldTC)
            return;

        Document oldDocument = null;
        if (oldTC != null) {
            oldTC.removePropertyChangeListener("document", listener);
            oldDocument = oldTC.getDocument();
        }

        Document newDocument = null;
        if (newTC != null) {
            newDocument = newTC.getDocument();
            newTC.addPropertyChangeListener("document", listener);
        }

        changeDocument(oldDocument, newDocument, update);
    }

    private void changeDocument(
            final Document oldDoc,
            final Document newDoc,
            final boolean update) {
        if (newDoc == oldDoc)
            return;

        if (oldDoc != null)
            oldDoc.removeDocumentListener(listener);

        if (newDoc != null) {
            if (update) {
                String txt;
                try {
                    txt = newDoc.getText(0, newDoc.getLength());
                } catch (BadLocationException e) {
                    txt = null;
                }
                update(txt);
            }
            newDoc.addDocumentListener(listener);
        }
    }

    private void documentUpdated() {
        if (lock())
            return;
        try {
            final JComboBox cb = getComboBox();
            final ComboBoxEditor cbe = cb.getEditor();
            final JTextComponent tc = (JTextComponent) cbe.getEditorComponent();
            assert cb.isShowing() || !tc.isFocusOwner();
            if (!tc.isFocusOwner() /*|| !cb.isShowing()*/)
                return;

            //cb.setPopupVisible(update(tc.getText())); // doesn't work: adjusts popup size!
            cb.setPopupVisible(false);
            if (update(tc.getText()))
                cb.setPopupVisible(true);
        } finally {
            unlock();
        }
    }

    private void updateEditor(final ComboBoxEditor cbe, final Object item) {
        if (lock())
            return;
        try {
            cbe.setItem(item);
            if (!(item instanceof String))
                return;

            final JComboBox cb = getComboBox();
            final JTextComponent tc = (JTextComponent) cbe.getEditorComponent();
            assert cb.isShowing() || !tc.isFocusOwner();
            if (!tc.isFocusOwner() /*|| !cb.isShowing()*/)
                return;

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
     * {@link #getComboBox} is guaranteed to return non-{@code null} if
     * this method is called from this abstract base class.
     *
     * @param initials The text to auto complete. May be {@code null}.
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
        if (recursion)
            return true;
        recursion = true;
        return false;
    }

    /**
     * Unlocks mutual recursive event notification.
     * <b>Warning:</b> This method works in a synchronized or single threaded
     * environment only!
     */
    private void unlock() {
        recursion = false;
    }

    private final class Listener
            implements DocumentListener, PropertyChangeListener {
        public void insertUpdate(DocumentEvent e) {
            documentUpdated();
        }

        public void removeUpdate(DocumentEvent e) {
            documentUpdated();
        }

        public void changedUpdate(DocumentEvent e) {
            documentUpdated();
        }

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
     * This proxy controls access to the real {@code ComboBoxEditor}
     * installed by the client application or the pluggable look and feel.
     * It is used to lock out mutual recursion caused by modifications to
     * the list model in the {@code JComboBox}.
     * <p>
     * Note that there is a slight chance that the introduction of this proxy
     * breaks the look and feel if it does {@code instanceof} tests for
     * a particular class, but I'm not aware of any look and feel which is
     * actually affected.
     * In order to reduce this risk, this class is extended from
     * {@link BasicComboBoxEditor}, although it overrides all methods which
     * are defined in the {@link ComboBoxEditor} interface.
     */
    private final class ComboBoxEditorProxy extends BasicComboBoxEditor {
        private final ComboBoxEditor comboBoxEditor;

        public ComboBoxEditorProxy(ComboBoxEditor comboBoxEditor) {
            this.comboBoxEditor = comboBoxEditor;
        }

        public ComboBoxEditor getEditor() {
            return comboBoxEditor;
        }

        @Override
        public Component getEditorComponent() {
            return comboBoxEditor.getEditorComponent();
        }

        @Override
        public void setItem(final Object item) {
            updateEditor(comboBoxEditor, item);
        }

        @Override
        public Object getItem() {
            return comboBoxEditor.getItem();
        }

        @Override
        public void selectAll() {
            comboBoxEditor.selectAll();
        }

        @Override
        public void addActionListener(ActionListener actionListener) {
            comboBoxEditor.addActionListener(actionListener);
        }

        @Override
        public void removeActionListener(ActionListener actionListener) {
            comboBoxEditor.removeActionListener(actionListener);
        }
    }
}
