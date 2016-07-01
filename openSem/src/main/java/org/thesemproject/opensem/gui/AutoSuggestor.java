/* 
 * Copyright 2016 The Sem Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thesemproject.opensem.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Implementa un suggeritore alla digitazione. E' utilizzato per suggerire nome
 * tabella o termine di dizionario una volta digitato un # che nella notazione
 * di SEM identifica un oggetto complesso
 */
public class AutoSuggestor {

    private final JTextComponent textComp;
    private final Window container;
    private JPanel suggestionsPanel;
    private JWindow autoSuggestionPopUpWindow;
    private String typedWord;
    private final List<String> dictionary = new ArrayList<>();
    private int currentIndexOfSpace, tW, tH;
    private DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            checkForAndShowSuggestions();
        }
    };
    private final Color suggestionsTextColor;
    private final Color suggestionFocusedColor;

    /**
     * Istanzia il suggeritore su un componente testo
     *
     * @param textComp finestra di testo su cui istanziare il suggeritore
     * @param mainWindow finestra principale
     * @param words lista di parole
     * @param popUpBackground colore dello sfondo
     * @param textColor colore del testo
     * @param suggestionFocusedColor colore del suggeritore
     * @param opacity indice di opacità
     */
    public AutoSuggestor(JTextComponent textComp, Window mainWindow, List<String> words, Color popUpBackground, Color textColor, Color suggestionFocusedColor, float opacity) {
        this.textComp = textComp;
        this.suggestionsTextColor = textColor;
        this.container = mainWindow;
        this.suggestionFocusedColor = suggestionFocusedColor;
        this.textComp.getDocument().addDocumentListener(documentListener);

        setDictionary(words);

        typedWord = "";
        currentIndexOfSpace = 0;
        tW = 0;
        tH = 0;

        autoSuggestionPopUpWindow = new JWindow(mainWindow);
        autoSuggestionPopUpWindow.setOpacity(opacity);

        suggestionsPanel = new JPanel();
        suggestionsPanel.setLayout(new GridLayout(0, 1));
        suggestionsPanel.setBackground(popUpBackground);
        suggestionsPanel.setBorder(new LineBorder(Color.black));

        addKeyBindingToRequestFocusInPopUpWindow();
    }

    private int lastFocusableIndex = 0;

    private void addKeyBindingToRequestFocusInPopUpWindow() {
        textComp.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "Down released");
        textComp.getActionMap().put("Down released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {//focuses the first label on popwindow
                for (int i = 0; i < suggestionsPanel.getComponentCount(); i++) {
                    if (suggestionsPanel.getComponent(i) instanceof SuggestionLabel) {
                        ((SuggestionLabel) suggestionsPanel.getComponent(i)).setFocused(true);
                        autoSuggestionPopUpWindow.toFront();
                        autoSuggestionPopUpWindow.requestFocusInWindow();
                        suggestionsPanel.requestFocusInWindow();
                        suggestionsPanel.getComponent(i).requestFocusInWindow();
                        break;
                    }
                }
            }
        });

        suggestionsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "Down released");
        suggestionsPanel.getActionMap().put("Down released", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {//allows scrolling of labels in pop window (I know very hacky for now :))

                ArrayList<SuggestionLabel> sls = getAddedSuggestionLabels();
                int max = sls.size();

                if (max > 1) {//more than 1 suggestion
                    for (int i = 0; i < max; i++) {
                        SuggestionLabel sl = sls.get(i);
                        if (sl.isFocused()) {
                            if (lastFocusableIndex == max - 1) {
                                lastFocusableIndex = 0;
                                sl.setFocused(false);
                                autoSuggestionPopUpWindow.setVisible(false);
                                setFocusToTextField();
                                checkForAndShowSuggestions();//fire method as if document listener change occured and fired it

                            } else {
                                sl.setFocused(false);
                                lastFocusableIndex = i;
                            }
                        } else if (lastFocusableIndex <= i) {
                            if (i < max) {
                                sl.setFocused(true);
                                autoSuggestionPopUpWindow.toFront();
                                autoSuggestionPopUpWindow.requestFocusInWindow();
                                suggestionsPanel.requestFocusInWindow();
                                suggestionsPanel.getComponent(i).requestFocusInWindow();
                                lastFocusableIndex = i;
                                break;
                            }
                        }
                    }
                } else {//only a single suggestion was given
                    autoSuggestionPopUpWindow.setVisible(false);
                    setFocusToTextField();
                    checkForAndShowSuggestions();//fire method as if document listener change occured and fired it
                }
            }
        });
        suggestionsPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "Up released");
        suggestionsPanel.getActionMap().put("Up released", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {//allows scrolling of labels in pop window (I know very hacky for now :))

                ArrayList<SuggestionLabel> sls = getAddedSuggestionLabels();
                int max = sls.size();

                if (max > 1) {//more than 1 suggestion
                    for (int i = max - 1; i >= 0; i--) {
                        SuggestionLabel sl = sls.get(i);
                        if (sl.isFocused()) {
                            if (lastFocusableIndex == 0) {
                                lastFocusableIndex = max - 1;
                                sl.setFocused(false);
                                autoSuggestionPopUpWindow.setVisible(false);
                                setFocusToTextField();
                                checkForAndShowSuggestions();//fire method as if document listener change occured and fired it

                            } else {
                                sl.setFocused(false);
                                lastFocusableIndex = i;
                            }
                        } else if (lastFocusableIndex >= i) {
                            if (i >= 0) {
                                sl.setFocused(true);
                                autoSuggestionPopUpWindow.toFront();
                                autoSuggestionPopUpWindow.requestFocusInWindow();
                                suggestionsPanel.requestFocusInWindow();
                                suggestionsPanel.getComponent(i).requestFocusInWindow();
                                lastFocusableIndex = i;
                                break;
                            }
                        }
                    }
                } else {//only a single suggestion was given
                    autoSuggestionPopUpWindow.setVisible(false);
                    setFocusToTextField();
                    checkForAndShowSuggestions();//fire method as if document listener change occured and fired it
                }
            }
        });
    }

    private void setFocusToTextField() {
        container.toFront();
        container.requestFocusInWindow();
        textComp.requestFocusInWindow();
    }

    /**
     * Ritorna la lista di label suggerite
     *
     * @return lista
     */
    public ArrayList<SuggestionLabel> getAddedSuggestionLabels() {
        ArrayList<SuggestionLabel> sls = new ArrayList<>();
        for (int i = 0; i < suggestionsPanel.getComponentCount(); i++) {
            if (suggestionsPanel.getComponent(i) instanceof SuggestionLabel) {
                SuggestionLabel sl = (SuggestionLabel) suggestionsPanel.getComponent(i);
                sls.add(sl);
            }
        }
        return sls;
    }

    private void checkForAndShowSuggestions() {
        typedWord = getCurrentlyTypedWord();

        suggestionsPanel.removeAll();//remove previos words/jlabels that were added

        //used to calcualte size of JWindow as new Jlabels are added
        tW = 0;
        tH = 0;

        boolean added = wordTyped(typedWord);

        if (!added) {
            if (autoSuggestionPopUpWindow.isVisible()) {
                autoSuggestionPopUpWindow.setVisible(false);
            }
        } else {
            showPopUpWindow();
            setFocusToTextField();
        }
    }

    /**
     * Aggiunge una parola al suggeritore
     *
     * @param word parola de aggiungere
     */
    protected void addWordToSuggestions(String word) {
        SuggestionLabel suggestionLabel = new SuggestionLabel(word, suggestionFocusedColor, suggestionsTextColor, this);

        calculatePopUpWindowSize(suggestionLabel);

        suggestionsPanel.add(suggestionLabel);
    }

    /**
     * Ritorna la nuova parola dopo l'ultimo spazio bianco oppure se si tratta
     * della prima parola se non ci sono spazi
     *
     * @return parola
     */
    public String getCurrentlyTypedWord() {//
        String text = textComp.getText();
        String wordBeingTyped = "";
        text = text.replaceAll("(\\r|\\n)", " ");
        if (text.contains(" ")) {
            int tmp = text.lastIndexOf(" ");
            if (tmp >= currentIndexOfSpace) {
                currentIndexOfSpace = tmp;
                wordBeingTyped = text.substring(text.lastIndexOf(" "));
            }
        } else {
            wordBeingTyped = text;
        }
        return wordBeingTyped.trim();
    }

    private void calculatePopUpWindowSize(JLabel label) {
        //so we can size the JWindow correctly
        if (tW < label.getPreferredSize().width) {
            tW = label.getPreferredSize().width;
        }
        tH += label.getPreferredSize().height;
    }

    private void showPopUpWindow() {
        autoSuggestionPopUpWindow.getContentPane().add(suggestionsPanel);
        autoSuggestionPopUpWindow.setMinimumSize(new Dimension(200, 30));
        autoSuggestionPopUpWindow.setSize(Math.min(200, tW), tH);
        autoSuggestionPopUpWindow.setVisible(true);

        int windowX = 0;
        int windowY = 0;

        if (textComp instanceof JTextField) {//calculate x and y for JWindow at bottom of JTextField
            windowX = container.getX() + textComp.getX() + 5;
            if (suggestionsPanel.getHeight() > autoSuggestionPopUpWindow.getMinimumSize().height) {
                windowY = container.getY() + textComp.getY() + textComp.getHeight() + autoSuggestionPopUpWindow.getMinimumSize().height;
            } else {
                windowY = container.getY() + textComp.getY() + textComp.getHeight() + autoSuggestionPopUpWindow.getHeight();
            }
        } else {//calculate x and y for JWindow on any JTextComponent using the carets position

            Point pt = new Point(textComp.getLocation());
            SwingUtilities.convertPointToScreen(pt, textComp);
            Rectangle rect = null;
            try {
                rect = textComp.getUI().modelToView(textComp, textComp.getCaret().getDot());//get carets position
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }

            windowX = pt.x + (int) (rect.getX());
            windowY = textComp.getBaseline(0, 0) + pt.y;

        }

        //show the pop up
        autoSuggestionPopUpWindow.setLocation(windowX, windowY);
        autoSuggestionPopUpWindow.setMinimumSize(new Dimension(200, 30));
        autoSuggestionPopUpWindow.setMaximumSize(new Dimension(200, 30));
        autoSuggestionPopUpWindow.setPreferredSize(new Dimension(200, 30));
        autoSuggestionPopUpWindow.revalidate();
        autoSuggestionPopUpWindow.repaint();

    }

    /**
     * Imposta il dizionario delle parole da suggerire
     * @param words lista delle parole da suggerire
     */
    public void setDictionary(List<String> words) {
        dictionary.clear();
        if (words == null) {
            return;//so we can call constructor with null value for dictionary without exception thrown
        }
        for (String word : words) {
            dictionary.add(word);
        }
    }

    /**
     * Ritorna la finestra del suggeritore
     * @return finestra del suggeritore
     */
    public JWindow getAutoSuggestionPopUpWindow() {
        return autoSuggestionPopUpWindow;
    }

    /**
     * Ritorna il windows container
     * @return container
     */
    public Window getContainer() {
        return container;
    }

    /**
     * Ritorna il text field
     * @return Text component
     */
    public JTextComponent getTextField() {
        return textComp;
    }

    /**
     * Aggiunge una parola al dizionario
     * @param word parola da aggiungere
     */
    public void addToDictionary(String word) {
        dictionary.add(word);
    }

    boolean wordTyped(String tw) {
        if (tw.isEmpty()) {
            return false;
        }
        String typedWord = tw;
        int posDash = typedWord.indexOf("#");
        if (posDash != 0 && posDash != -1) {
            typedWord = typedWord.substring(posDash);
        }
        boolean suggestionAdded = false;
        int count = 0;
        for (String word : dictionary) {//get words in the dictionary which we added
            boolean fullymatches = true;
            for (int i = 0; i < typedWord.length(); i++) {//each string in the word
                if (!typedWord.toLowerCase().startsWith(String.valueOf(word.toLowerCase().charAt(i)), i)) {//check for match
                    fullymatches = false;
                    break;
                }
            }
            if (fullymatches) {
                addWordToSuggestions(word);
                suggestionAdded = true;
                count++;
            }
            if (count >= 5) {
                break;
            }
        }
        return suggestionAdded;
    }
}

class SuggestionLabel extends JLabel {

    private boolean focused = false;
    private final JWindow autoSuggestionsPopUpWindow;
    private final JTextComponent textComponent;
    private final AutoSuggestor autoSuggestor;
    private Color suggestionsTextColor, suggestionBorderColor;

    public SuggestionLabel(String string, final Color borderColor, Color suggestionsTextColor, AutoSuggestor autoSuggestor) {
        super(string);

        this.suggestionsTextColor = suggestionsTextColor;
        this.autoSuggestor = autoSuggestor;
        this.textComponent = autoSuggestor.getTextField();
        this.suggestionBorderColor = borderColor;
        this.autoSuggestionsPopUpWindow = autoSuggestor.getAutoSuggestionPopUpWindow();

        initComponent();
    }

    private void initComponent() {
        setFocusable(true);
        setForeground(suggestionsTextColor);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);

                replaceWithSuggestedText();

                autoSuggestionsPopUpWindow.setVisible(false);
            }
        });

        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "Enter released");
        getActionMap().put("Enter released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                replaceWithSuggestedText();
                autoSuggestionsPopUpWindow.setVisible(false);
            }
        });
    }

    public void setFocused(boolean focused) {
        if (focused) {
            setBorder(new LineBorder(suggestionBorderColor));
            setBackground(suggestionBorderColor);
            setForeground(Color.WHITE);
            setOpaque(true);
        } else {
            setBorder(null);
            setForeground(suggestionsTextColor);
            setOpaque(false);

        }
        repaint();
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    private void replaceWithSuggestedText() {
        String suggestedWord = getText();

        int pos = suggestedWord.indexOf(" ");
        if (pos != -1) {
            suggestedWord = suggestedWord.substring(0, pos + 1);
        }

        String text = textComponent.getText();
        String typedWord = autoSuggestor.getCurrentlyTypedWord();
        if (!typedWord.startsWith("#")) {
            suggestedWord = typedWord.substring(0, typedWord.indexOf("#")) + suggestedWord;
        }
        String t = text.substring(0, text.lastIndexOf(typedWord));
        String tmp = t + text.substring(text.lastIndexOf(typedWord)).replace(typedWord, suggestedWord);
        textComponent.setText(tmp);
    }
}
