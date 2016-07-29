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

import org.thesemproject.opensem.gui.utils.StopWordsUtils;
import org.thesemproject.opensem.gui.utils.SegmentsUtils;
import org.thesemproject.opensem.gui.utils.DataProvidersUtils;
import org.thesemproject.opensem.gui.utils.LuceneIndexUtils;
import org.thesemproject.opensem.gui.utils.DictionaryUtils;
import org.thesemproject.opensem.gui.utils.CapturesUtils;
import org.thesemproject.opensem.gui.utils.GuiUtils;
import org.thesemproject.opensem.gui.modelEditor.ModelEditor;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.classification.MulticlassEngine;
import org.thesemproject.opensem.classification.NodeData;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeRelationshipNode;
import org.thesemproject.opensem.gui.modelEditor.SegmentTreeNode;
import org.thesemproject.opensem.gui.modelEditor.TableTreeNode;
import org.thesemproject.opensem.gui.utils.ChangedUtils;
import org.thesemproject.opensem.gui.utils.FilesAndSegmentsUtils;
import org.thesemproject.opensem.gui.utils.PatternsUtils;
import org.thesemproject.opensem.gui.utils.TablesUtils;
import org.thesemproject.opensem.parser.DocumentParser;
import org.thesemproject.opensem.gui.process.ReadClassifyWrite;
import org.thesemproject.opensem.gui.process.ReadSegmentWrite;
import org.thesemproject.opensem.gui.process.ReadFolderToTable;
import org.thesemproject.opensem.segmentation.DataProviderConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationUtils;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentEngine;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import com.jtattoo.plaf.aero.AeroLookAndFeel;
import java.awt.Color;
import java.awt.Dimension;
import static java.awt.Event.DELETE;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.thesemproject.opensem.utils.FinalBoolean;

/**
 *
 * Gestisce l'interfaccia grafica della tecnologia SEM. La maggiornaza del
 * lavoro è fatto nelle classi statiche del package .gui.utils. In questa classe
 * si cerca di gestire grafica ed eventi. La grafica è gestita automaticamente
 * da Netbeans.
 *
 * E' una classe che nelle prossime versioni, probabilmente verrà ulteriormente
 * riformattata e ristilizzata
 */
public class SemGui extends javax.swing.JFrame {

    /**
     * Crea la form
     */
    public SemGui() {
        cc = new SemConfiguration();
        initComponents();
        GuiUtils.prepareTables(this);
        GuiUtils.initSubMenus(this);
        me = new ModelEditor(this, modelElements);
        segmentPatternSuggestor = new AutoSuggestor(segmentPatternDefinition, this, me.getSuggestions((DefaultMutableTreeNode) modelTree.getModel().getRoot()), Color.WHITE.brighter(), Color.BLACK, Color.BLUE.brighter(), 0.95f);
        capturePatternSuggestor = new AutoSuggestor(capturePatternDefinition, this, me.getSuggestions((DefaultMutableTreeNode) modelTree.getModel().getRoot()), Color.WHITE.brighter(), Color.BLACK, Color.BLUE.brighter(), 0.95f);
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        this.setMaximizedBounds(env.getMaximumWindowBounds());

        this.setExtendedState(this.getExtendedState() | this.MAXIMIZED_BOTH);
        LogGui.setjTextArea(logInizializzazione);
        LogGui.setMemInfo(memInfo);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isSaving) {
                    GuiUtils.showErrorDialog("E' in corso il salvataggio di un file\nImpossibile chiudere l'applicazione", "Impossibile chiudere");
                } else if (GuiUtils.showConfirmDialog("Sei sicuro di voler uscire?", "Confermi uscita?")) {
                    ME.closeAllReaders();
                    SE.closeAllReaders();
                    System.exit(0);
                }
            }
        });
        modelEditorFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                doDockModelEditor();
            }
        });
        StopWordsUtils.addStopWordsMenu(fileText, this);
        StopWordsUtils.addStopWordsMenu(segmentText, this);
        StopWordsUtils.addStopWordsMenu(segmentTokens, this);
        StopWordsUtils.addStopWordsMenu(token, this);
        StopWordsUtils.addStopWordsMenu(testo, this);
        StopWordsUtils.addStopWordsMenu(testoDaSegmentare, this);
        DictionaryUtils.addCreateDefinitionMenu(segmentPatternDefinition, this);
        DictionaryUtils.addCreateDefinitionMenu(capturePatternDefinition, this);
        LogGui.info("System started...");
        Runnable memCount = new Runnable() {
            @Override
            public void run() {
                LogGui.printMemorySummary();
            }
        };
        scheduler.scheduleAtFixedRate(memCount, 0, 1, TimeUnit.MINUTES);
        int cores = Runtime.getRuntime().availableProcessors();
        processori2.setSelectedIndex(cores - 1);

        //Imposta il reshaping della garfica in funzione della dimensione dello schermo
        int halfScreenWidth = (int) (env.getMaximumWindowBounds().width * 0.45);
        int twoThirdScreenWidth = (int) (env.getMaximumWindowBounds().width * 0.65);
        int halfScreenHeight = (int) (env.getMaximumWindowBounds().height * 0.45);
        int oneThirdScreenWidth = (int) (env.getMaximumWindowBounds().width * 0.3);
        int oneQuarterScreenWidth = (int) (env.getMaximumWindowBounds().width * 0.2);
        modelEditorContainer.setDividerLocation(twoThirdScreenWidth);

        jSplitPane6.setDividerLocation(halfScreenWidth);
        jSplitPane4.setDividerLocation(halfScreenWidth);

        coverageSplitPanel.setDividerLocation(halfScreenWidth);

        jSplitPane5.setDividerLocation((int) (halfScreenWidth * .7));

        jSplitPane1.setDividerLocation(oneQuarterScreenWidth);
        segments.setDividerLocation(oneQuarterScreenWidth);
        changes.setDividerLocation(oneQuarterScreenWidth);
        modelEditor.setDividerLocation(oneQuarterScreenWidth);
        jSplitPane8.setDividerLocation(oneQuarterScreenWidth);

        dictionarySplit.setDividerLocation(halfScreenHeight);
        segmentsSplit.setDividerLocation(halfScreenHeight);
        captureSplit.setDividerLocation(halfScreenHeight);

        jSplitPane9.setDividerLocation((int) (halfScreenHeight * 1.3));

        filesSplitPanel.setDividerLocation((int) (halfScreenHeight * 1.1));
        jSplitPane2.setDividerLocation((int) (halfScreenHeight * 1.1));
        segmentsConsolleSplitPanel.setDividerLocation((int) (halfScreenHeight * 1.1));
        coverage.setDividerLocation((int) (halfScreenHeight * 1.1));
        dataproviderSplit.setDividerLocation((int) (halfScreenHeight * 1.1));
        dataproviderRelationship.setDividerLocation((int) (halfScreenHeight * 1.1));

        jSplitPane7.setDividerLocation((int) (halfScreenHeight * .7));

        filesPanelHtml.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        filesPanelHtmlFormatted.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        filesPanelHtml1.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        filesPanelHtmlFormatted1.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        help.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        htmlFormatted.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        htmlResult.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        htmlTimeline.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectIndexFolder = new javax.swing.JDialog();
        p1IndexFileChooser = new javax.swing.JFileChooser();
        selectIndexFoderIstruzione = new javax.swing.JDialog();
        p2IndexFileChooser = new javax.swing.JFileChooser();
        selectExcelFile = new javax.swing.JDialog();
        excelFileChooser = new javax.swing.JFileChooser();
        selectStopWords2 = new javax.swing.JDialog();
        stopWordsFileChooser2 = new javax.swing.JFileChooser();
        selectExcelFileClass = new javax.swing.JDialog();
        excelFileChooserClass = new javax.swing.JFileChooser();
        jLabel1 = new javax.swing.JLabel();
        colonnaDescrizione = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        processori = new javax.swing.JComboBox<>();
        selectFileToSegment = new javax.swing.JDialog();
        segmentFileChooser1 = new javax.swing.JFileChooser();
        selectFolderToProcess = new javax.swing.JDialog();
        folderChooser = new javax.swing.JFileChooser();
        jLabel2 = new javax.swing.JLabel();
        salvaHTML = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        processori1 = new javax.swing.JComboBox<>();
        selectFolderToLoad = new javax.swing.JDialog();
        folderToLoadChooser = new javax.swing.JFileChooser();
        selectSaveStorageAs = new javax.swing.JDialog();
        saveAsFileChooser = new javax.swing.JFileChooser();
        selectOpenStorage = new javax.swing.JDialog();
        openFileChooser = new javax.swing.JFileChooser();
        selectExportTable = new javax.swing.JDialog();
        expotTableFileChooser = new javax.swing.JFileChooser();
        selectImportTable = new javax.swing.JDialog();
        importTableFileChooser = new javax.swing.JFileChooser();
        selectExcelFileSer = new javax.swing.JDialog();
        excelCorpusChooser = new javax.swing.JFileChooser();
        modelEditorFrame = new javax.swing.JFrame();
        selectExportPatterns = new javax.swing.JDialog();
        expotPatternsFileChooser = new javax.swing.JFileChooser();
        selectImportPatterns = new javax.swing.JDialog();
        importPatternsFileChooser = new javax.swing.JFileChooser();
        globalCapturesSegmentsRelationship = new javax.swing.JDialog();
        jScrollPane5 = new javax.swing.JScrollPane();
        captureRelationshipTable = new javax.swing.JTable();
        jToolBar7 = new javax.swing.JToolBar();
        saveRelationship = new javax.swing.JButton();
        jSeparator32 = new javax.swing.JToolBar.Separator();
        captureClassificationRelationship = new javax.swing.JDialog();
        jToolBar8 = new javax.swing.JToolBar();
        saveRelationship1 = new javax.swing.JButton();
        jSeparator33 = new javax.swing.JToolBar.Separator();
        jButton10 = new javax.swing.JButton();
        jSeparator35 = new javax.swing.JToolBar.Separator();
        catClass = new javax.swing.JTextField();
        pannelloAlbero1 = new javax.swing.JPanel();
        jScrollPane29 = new javax.swing.JScrollPane();
        classificationTree1 = new javax.swing.JTree();
        jTextField2 = new javax.swing.JTextField();
        selectExportExcel = new javax.swing.JDialog();
        expotExcelFileChooser = new javax.swing.JFileChooser();
        configurationDialog = new javax.swing.JDialog();
        jPanel2 = new javax.swing.JPanel();
        percorsoIndice = new javax.swing.JTextField();
        selezionaIndice = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        onlySegment = new javax.swing.JCheckBox();
        rebuildIndex = new javax.swing.JCheckBox();
        selezionaOCR = new javax.swing.JButton();
        percorsoOCR = new javax.swing.JTextField();
        initLabel = new javax.swing.JLabel();
        selectExportTree = new javax.swing.JDialog();
        exportTreeFileChooser = new javax.swing.JFileChooser();
        selectImportTree = new javax.swing.JDialog();
        importTreeFileChooser = new javax.swing.JFileChooser();
        selectFileToImport = new javax.swing.JDialog();
        importFileChooser = new javax.swing.JFileChooser();
        globalTagCloud = new javax.swing.JFrame();
        selectCSVDataProvider = new javax.swing.JDialog();
        csvdpchooser = new javax.swing.JFileChooser();
        jSeparator45 = new javax.swing.JSeparator();
        selectOCRFolder = new javax.swing.JDialog();
        ocrFileChooser = new javax.swing.JFileChooser();
        selectExportExcelIndex = new javax.swing.JDialog();
        expotExcelIndexFileChooser = new javax.swing.JFileChooser();
        consolleToolbar = new javax.swing.JToolBar();
        configuration = new javax.swing.JButton();
        jSeparator25 = new javax.swing.JToolBar.Separator();
        segmentaEClassifica = new javax.swing.JButton();
        segmentaEBasta = new javax.swing.JButton();
        interrompi = new javax.swing.JButton();
        jSeparator26 = new javax.swing.JToolBar.Separator();
        jLabel7 = new javax.swing.JLabel();
        soglia = new javax.swing.JTextField();
        jSeparator22 = new javax.swing.JToolBar.Separator();
        jLabel12 = new javax.swing.JLabel();
        classStartLevel = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        fattoreK = new javax.swing.JTextField();
        jSeparator24 = new javax.swing.JToolBar.Separator();
        jLabel15 = new javax.swing.JLabel();
        learningFactor = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel3 = new javax.swing.JLabel();
        processori2 = new javax.swing.JComboBox<>();
        jSeparator41 = new javax.swing.JToolBar.Separator();
        memInfo = new javax.swing.JLabel();
        jSeparator42 = new javax.swing.JToolBar.Separator();
        jButton13 = new javax.swing.JButton();
        filesTab = new javax.swing.JTabbedPane();
        files = new javax.swing.JPanel();
        filesSplitPanel = new javax.swing.JSplitPane();
        jScrollPane7 = new javax.swing.JScrollPane();
        filesTable = new javax.swing.JTable();
        jSplitPane4 = new javax.swing.JSplitPane();
        filesSx = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        filesPanelSegmenta = new javax.swing.JButton();
        jTabbedPane3 = new javax.swing.JTabbedPane();
        jScrollPane13 = new javax.swing.JScrollPane();
        filesPanelSegmentTree = new javax.swing.JTree();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane17 = new javax.swing.JScrollPane();
        filesPanleCapturesTable = new javax.swing.JTable();
        filesDx = new javax.swing.JPanel();
        jTabbedPane4 = new javax.swing.JTabbedPane();
        jScrollPane15 = new javax.swing.JScrollPane();
        fileText = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane16 = new javax.swing.JScrollPane();
        filesPanelHtml = new javax.swing.JTextPane();
        jScrollPane25 = new javax.swing.JScrollPane();
        filesPanelHtmlFormatted = new javax.swing.JTextPane();
        filesInfoLabel = new javax.swing.JLabel();
        filesToolbar = new javax.swing.JToolBar();
        menuCarica = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        salvaStorage = new javax.swing.JButton();
        exportToExcel = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        tagCloud = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jButton6 = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        jLabel13 = new javax.swing.JLabel();
        filterFile = new javax.swing.JTextField();
        segments = new javax.swing.JSplitPane();
        classificationTreePanel = new javax.swing.JPanel();
        etichettaAlberoSegmenti = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        cercaCategoriaSegmentsPanel = new javax.swing.JTextField();
        jScrollPane19 = new javax.swing.JScrollPane();
        categorieSegmentsPanel = new javax.swing.JTree();
        segmentsConsolleSplitPanel = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        segmentsTable = new javax.swing.JTable();
        jToolBar3 = new javax.swing.JToolBar();
        firstLevelOnly = new javax.swing.JButton();
        underTreshold = new javax.swing.JButton();
        notMarked = new javax.swing.JButton();
        changed = new javax.swing.JButton();
        alert = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        removeFilters = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        jLabel11 = new javax.swing.JLabel();
        filterSegments = new javax.swing.JTextField();
        statusSegments = new javax.swing.JLabel();
        jSplitPane5 = new javax.swing.JSplitPane();
        jScrollPane18 = new javax.swing.JScrollPane();
        segmentClassificationResult = new javax.swing.JTree();
        jTabbedPane6 = new javax.swing.JTabbedPane();
        jScrollPane14 = new javax.swing.JScrollPane();
        segmentText = new javax.swing.JTextArea();
        jScrollPane20 = new javax.swing.JScrollPane();
        segmentTokens = new javax.swing.JTextArea();
        changes = new javax.swing.JSplitPane();
        changesTableScrollPanel = new javax.swing.JScrollPane();
        changedTable = new javax.swing.JTable();
        changesTreeScrollPanel = new javax.swing.JScrollPane();
        changedFilterTree = new javax.swing.JTree();
        coverage = new javax.swing.JSplitPane();
        coverageSplitPanel = new javax.swing.JSplitPane();
        jScrollPane23 = new javax.swing.JScrollPane();
        captureValues = new javax.swing.JTable();
        jScrollPane24 = new javax.swing.JScrollPane();
        coverageDocumentsTable = new javax.swing.JTable();
        coverageTableScrollPanel = new javax.swing.JScrollPane();
        coverageTable = new javax.swing.JTable();
        classificazione = new javax.swing.JPanel();
        jToolBar4 = new javax.swing.JToolBar();
        batch = new javax.swing.JButton();
        classificaTesto = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        pannelloAlbero = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        classificationTree = new javax.swing.JTree();
        jTextField1 = new javax.swing.JTextField();
        jSplitPane2 = new javax.swing.JSplitPane();
        pannelloTesto = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        testo = new javax.swing.JTextArea();
        labelTesto = new javax.swing.JLabel();
        jSplitPane3 = new javax.swing.JSplitPane();
        pannelloTokenizzazione = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        token = new javax.swing.JTextArea();
        pannelloClassificazione = new javax.swing.JPanel();
        classificationStatus = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        classificationResult = new javax.swing.JTree();
        jSplitPane9 = new javax.swing.JSplitPane();
        jScrollPane31 = new javax.swing.JScrollPane();
        htmlTimeline = new javax.swing.JEditorPane();
        segmentazione = new javax.swing.JPanel();
        jToolBar5 = new javax.swing.JToolBar();
        segmenta = new javax.swing.JButton();
        segmentaFile = new javax.swing.JButton();
        segmentaCartella = new javax.swing.JButton();
        nuvoletta = new javax.swing.JButton();
        resetSegmenta = new javax.swing.JButton();
        jSplitPane6 = new javax.swing.JSplitPane();
        jPanel8 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        testoDaSegmentare = new javax.swing.JTextArea();
        jScrollPane22 = new javax.swing.JScrollPane();
        htmlFormatted = new javax.swing.JEditorPane();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        htmlResult = new javax.swing.JEditorPane();
        jSplitPane7 = new javax.swing.JSplitPane();
        jScrollPane8 = new javax.swing.JScrollPane();
        segmentTree = new javax.swing.JTree();
        jScrollPane9 = new javax.swing.JScrollPane();
        segmentTextArea = new javax.swing.JTextArea();
        imagesScrollPanel = new javax.swing.JScrollPane();
        imagesPanel = new javax.swing.JPanel();
        modelEditorContainer = new javax.swing.JSplitPane();
        modelEditor = new javax.swing.JSplitPane();
        modelElements = new javax.swing.JTabbedPane();
        dictionarySplit = new javax.swing.JSplitPane();
        dictionaryPanel = new javax.swing.JPanel();
        dictionaryToolbar = new javax.swing.JToolBar();
        renameDefinition = new javax.swing.JButton();
        deleteDefinition = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        removeDefinitionFilters = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        jLabel16 = new javax.swing.JLabel();
        searchDefinition = new javax.swing.JTextField();
        dictionaryStatus = new javax.swing.JLabel();
        dictionaryTableScrollPanel = new javax.swing.JScrollPane();
        dictionaryTable = new javax.swing.JTable();
        definitionPanel = new javax.swing.JPanel();
        definitionPatternToolbar = new javax.swing.JToolBar();
        newDefinition = new javax.swing.JButton();
        jSeparator18 = new javax.swing.JToolBar.Separator();
        confirmDefinitionPattern = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        testDefinitionRegex = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JToolBar.Separator();
        testDefinitionMatch = new javax.swing.JButton();
        definitionPatternEditPanel = new javax.swing.JPanel();
        definitionStatus = new javax.swing.JLabel();
        definitionDefinitionPanel = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        definitionName = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        definitionPatternScrollPanel = new javax.swing.JScrollPane();
        definitionPattern = new javax.swing.JTextArea();
        jLabel20 = new javax.swing.JLabel();
        definitionPatternTestScrollPanel = new javax.swing.JScrollPane();
        definitionPatternTest = new javax.swing.JTextArea();
        segmentsSplit = new javax.swing.JSplitPane();
        segmentPanel = new javax.swing.JPanel();
        segmentsSplitPanel = new javax.swing.JSplitPane();
        segmentPatternsPanel = new javax.swing.JPanel();
        segmentPatternsToolbar = new javax.swing.JToolBar();
        segmentPatternDelete = new javax.swing.JButton();
        jSeparator47 = new javax.swing.JToolBar.Separator();
        moveUp1 = new javax.swing.JButton();
        moveDown1 = new javax.swing.JButton();
        moveTop1 = new javax.swing.JButton();
        moveBottom1 = new javax.swing.JButton();
        segmentPatternsScrollPanel = new javax.swing.JScrollPane();
        segmentPatternsTable = new javax.swing.JTable();
        segmentConfigurationPanel = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        segmentName = new javax.swing.JLabel();
        defaultYN = new javax.swing.JComboBox<>();
        multipleYN = new javax.swing.JComboBox<>();
        classifyYN = new javax.swing.JComboBox<>();
        segmentPatternPanel = new javax.swing.JPanel();
        segmentPatternToolbar = new javax.swing.JToolBar();
        segmentPatternAdd = new javax.swing.JButton();
        jSeparator19 = new javax.swing.JToolBar.Separator();
        confirmSegmentPattern = new javax.swing.JButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        testSegmentPattern = new javax.swing.JButton();
        jSeparator15 = new javax.swing.JToolBar.Separator();
        matchSegmentPattern = new javax.swing.JButton();
        segmentPatternDefinitionPanel = new javax.swing.JPanel();
        segmentPatternStatus = new javax.swing.JLabel();
        segmentPatternConfigurationPanel = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        segmentPatternDefinitionScrollPanel = new javax.swing.JScrollPane();
        segmentPatternDefinition = new javax.swing.JTextArea();
        jLabel23 = new javax.swing.JLabel();
        segmentPatternScrollPanelTestArea = new javax.swing.JScrollPane();
        segmentPatternTestArea = new javax.swing.JTextArea();
        tablePanel = new javax.swing.JPanel();
        tableToolbar = new javax.swing.JToolBar();
        tableImport = new javax.swing.JButton();
        tableExport = new javax.swing.JButton();
        jSeparator44 = new javax.swing.JToolBar.Separator();
        fromDataProvider = new javax.swing.JCheckBox();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        tableAddRecord = new javax.swing.JButton();
        tableDeleteRecord = new javax.swing.JButton();
        jSeparator12 = new javax.swing.JToolBar.Separator();
        removeTableFilter = new javax.swing.JButton();
        jLabel26 = new javax.swing.JLabel();
        searchTable = new javax.swing.JTextField();
        tableScrollpanel = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        captureSplit = new javax.swing.JSplitPane();
        capturePanel = new javax.swing.JPanel();
        captureConfigurationSplitPanel = new javax.swing.JSplitPane();
        capturePatternsPanel = new javax.swing.JPanel();
        capturePatternsToolbar = new javax.swing.JToolBar();
        deleteCapturePattern = new javax.swing.JButton();
        jSeparator30 = new javax.swing.JToolBar.Separator();
        patternsImport = new javax.swing.JButton();
        patternsExport = new javax.swing.JButton();
        jSeparator46 = new javax.swing.JToolBar.Separator();
        moveUp = new javax.swing.JButton();
        moveDown = new javax.swing.JButton();
        moveTop = new javax.swing.JButton();
        moveBottom = new javax.swing.JButton();
        jSeparator31 = new javax.swing.JToolBar.Separator();
        removeSearchFilter = new javax.swing.JButton();
        jSeparator37 = new javax.swing.JToolBar.Separator();
        jLabel35 = new javax.swing.JLabel();
        searchNormalization = new javax.swing.JTextField();
        capturePatternsScrollPanel = new javax.swing.JScrollPane();
        capturePatternTable = new javax.swing.JTable();
        captureConfigurationSuperPanel = new javax.swing.JPanel();
        jToolBar9 = new javax.swing.JToolBar();
        openSegmentRelationshipPanel = new javax.swing.JButton();
        jSeparator34 = new javax.swing.JToolBar.Separator();
        classifyPattern = new javax.swing.JButton();
        captureConfigurationPanel = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        captureName = new javax.swing.JLabel();
        captureType = new javax.swing.JComboBox<>();
        captureFormat = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        captureTarget = new javax.swing.JComboBox<>();
        tempCapture = new javax.swing.JCheckBox();
        endTimeInterval = new javax.swing.JCheckBox();
        startTimeInterval = new javax.swing.JCheckBox();
        capturePatternPanel = new javax.swing.JPanel();
        capturePatternToolbar = new javax.swing.JToolBar();
        addCapturePattern = new javax.swing.JButton();
        jSeparator20 = new javax.swing.JToolBar.Separator();
        confirmCapturePattern = new javax.swing.JButton();
        jSeparator16 = new javax.swing.JToolBar.Separator();
        testCapturePattern = new javax.swing.JButton();
        jSeparator17 = new javax.swing.JToolBar.Separator();
        testCaptureMatch = new javax.swing.JButton();
        capturePatternEditPanel = new javax.swing.JPanel();
        capturePatternStatus = new javax.swing.JLabel();
        capturePatternContentTable = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        capturePatternContentScrollPanel = new javax.swing.JScrollPane();
        capturePatternDefinition = new javax.swing.JTextArea();
        jLabel32 = new javax.swing.JLabel();
        jScrollPane35 = new javax.swing.JScrollPane();
        capturePatternTestText = new javax.swing.JTextArea();
        jLabel33 = new javax.swing.JLabel();
        capturePatternSpinner = new javax.swing.JSpinner();
        jLabel34 = new javax.swing.JLabel();
        capturePatternFixedValue = new javax.swing.JTextField();
        modeEditorInfo = new javax.swing.JPanel();
        jScrollPane21 = new javax.swing.JScrollPane();
        help = new javax.swing.JEditorPane();
        dataproviderSplit = new javax.swing.JSplitPane();
        dataproviderPanel = new javax.swing.JPanel();
        dataproviderSplitPanel = new javax.swing.JSplitPane();
        dpFieldsPanel = new javax.swing.JPanel();
        dpFieldsToolbar = new javax.swing.JToolBar();
        deleteDpFields = new javax.swing.JButton();
        jSeparator38 = new javax.swing.JToolBar.Separator();
        importDpFields = new javax.swing.JButton();
        jSeparator40 = new javax.swing.JToolBar.Separator();
        burnToStorage = new javax.swing.JButton();
        dbFieldsScrollPanel = new javax.swing.JScrollPane();
        dpFieldsTable = new javax.swing.JTable();
        dpDefinition = new javax.swing.JPanel();
        dpConfigurationPanel = new javax.swing.JPanel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        dpName = new javax.swing.JLabel();
        dpFileName = new javax.swing.JTextField();
        jLabel50 = new javax.swing.JLabel();
        dpDelimitatore = new javax.swing.JTextField();
        dpType = new javax.swing.JComboBox<>();
        jLabel52 = new javax.swing.JLabel();
        dpEscape = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        dpQuote = new javax.swing.JTextField();
        jLabel56 = new javax.swing.JLabel();
        dpLineSep = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        dataproviderFieldsPanel = new javax.swing.JPanel();
        capturePatternToolbar1 = new javax.swing.JToolBar();
        addDpField = new javax.swing.JButton();
        jSeparator43 = new javax.swing.JToolBar.Separator();
        confirmDpField = new javax.swing.JButton();
        capturePatternEditPanel1 = new javax.swing.JPanel();
        dpFieldId = new javax.swing.JLabel();
        capturePatternStatus1 = new javax.swing.JLabel();
        capturePatternContentTable1 = new javax.swing.JPanel();
        jLabel51 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        dpFieldName = new javax.swing.JTextField();
        dpFieldType = new javax.swing.JComboBox<>();
        jLabel55 = new javax.swing.JLabel();
        dpFieldPosition = new javax.swing.JSpinner();
        jLabel57 = new javax.swing.JLabel();
        dpFieldTableRelationship = new javax.swing.JComboBox<>();
        dataproviderRelationship = new javax.swing.JSplitPane();
        dprPanel = new javax.swing.JPanel();
        captureConfigurationSplitPanel3 = new javax.swing.JSplitPane();
        capturePatternsPanel3 = new javax.swing.JPanel();
        capturePatternsScrollPanel3 = new javax.swing.JScrollPane();
        dprTable = new javax.swing.JTable();
        captureConfigurationSuperPanel3 = new javax.swing.JPanel();
        captureConfigurationPanel5 = new javax.swing.JPanel();
        jLabel64 = new javax.swing.JLabel();
        dprName = new javax.swing.JLabel();
        dprPriority = new javax.swing.JCheckBox();
        jLabel65 = new javax.swing.JLabel();
        dprSegment = new javax.swing.JComboBox<>();
        dprRelationshipPanel = new javax.swing.JPanel();
        capturePatternToolbar3 = new javax.swing.JToolBar();
        dprSave = new javax.swing.JButton();
        capturePatternEditPanel3 = new javax.swing.JPanel();
        capturePatternStatus3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        dprFieldName = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        dprCapture = new javax.swing.JComboBox<>();
        dprEnrich = new javax.swing.JCheckBox();
        dprKey = new javax.swing.JCheckBox();
        modelTreeSplitPanel = new javax.swing.JPanel();
        modelTreeScrollPanel = new javax.swing.JScrollPane();
        modelTree = new javax.swing.JTree();
        jToolBar1 = new javax.swing.JToolBar();
        jButton8 = new javax.swing.JButton();
        jSeparator29 = new javax.swing.JToolBar.Separator();
        saveModel = new javax.swing.JButton();
        jSeparator21 = new javax.swing.JToolBar.Separator();
        compileModel = new javax.swing.JButton();
        jSeparator23 = new javax.swing.JToolBar.Separator();
        segmenta1 = new javax.swing.JButton();
        jSeparator28 = new javax.swing.JToolBar.Separator();
        resetModel = new javax.swing.JButton();
        filesDx1 = new javax.swing.JPanel();
        jTabbedPane5 = new javax.swing.JTabbedPane();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane27 = new javax.swing.JScrollPane();
        filesPanelHtml1 = new javax.swing.JTextPane();
        jScrollPane28 = new javax.swing.JScrollPane();
        filesPanelHtmlFormatted1 = new javax.swing.JTextPane();
        jScrollPane26 = new javax.swing.JScrollPane();
        fileText1 = new javax.swing.JTextArea();
        gestioneIndice = new javax.swing.JPanel();
        jToolBar6 = new javax.swing.JToolBar();
        jLabel36 = new javax.swing.JLabel();
        linguaAnalizzatoreIstruzione = new javax.swing.JComboBox<>();
        gestioneIndiceTabbedPanel = new javax.swing.JTabbedPane();
        manageStopWordsPanel = new javax.swing.JPanel();
        tableToolbar1 = new javax.swing.JToolBar();
        addStopWord = new javax.swing.JButton();
        deleteStopWord = new javax.swing.JButton();
        jSeparator36 = new javax.swing.JToolBar.Separator();
        removeStopWordFilter = new javax.swing.JButton();
        jLabel37 = new javax.swing.JLabel();
        searchStopWords = new javax.swing.JTextField();
        stopWordsScrollPanel = new javax.swing.JScrollPane();
        stopWordsTable = new javax.swing.JTable();
        manageStopWrodsStatus = new javax.swing.JLabel();
        manageDocuments = new javax.swing.JPanel();
        manageDocumentsStatus = new javax.swing.JLabel();
        jSplitPane8 = new javax.swing.JSplitPane();
        manageClassificationTreePanel = new javax.swing.JPanel();
        jScrollPane30 = new javax.swing.JScrollPane();
        manageClassificationTree = new javax.swing.JTree();
        jToolBar10 = new javax.swing.JToolBar();
        jButton9 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jSeparator27 = new javax.swing.JToolBar.Separator();
        jLabel5 = new javax.swing.JLabel();
        searchManageClassification = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        documentsTableScrollPanel = new javax.swing.JScrollPane();
        documentsTable = new javax.swing.JTable();
        tableToolbar2 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        exportIndex = new javax.swing.JButton();
        deleteDocument = new javax.swing.JButton();
        jSeparator39 = new javax.swing.JToolBar.Separator();
        removeDocumentFilter = new javax.swing.JButton();
        jLabel38 = new javax.swing.JLabel();
        serachDocumentBody = new javax.swing.JTextField();
        createIndexPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        logIstruzione = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        selezionaIndiceIstruzione = new javax.swing.JButton();
        percorsoIndice1 = new javax.swing.JTextField();
        selezionaStop = new javax.swing.JButton();
        stopWords2 = new javax.swing.JTextField();
        selezionaExcel = new javax.swing.JButton();
        fileExcel = new javax.swing.JTextField();
        startBuildIndex = new javax.swing.JButton();
        usaCategorie = new javax.swing.JCheckBox();
        statusGestioneIndice = new javax.swing.JLabel();
        systemPanel = new javax.swing.JPanel();
        logPanel = new javax.swing.JScrollPane();
        logInizializzazione = new javax.swing.JTextArea();
        jToolBar11 = new javax.swing.JToolBar();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();

        selectIndexFolder.setMinimumSize(new java.awt.Dimension(590, 380));

        p1IndexFileChooser.setApproveButtonText("Seleziona");
        p1IndexFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        p1IndexFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        p1IndexFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        p1IndexFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p1IndexFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectIndexFolderLayout = new javax.swing.GroupLayout(selectIndexFolder.getContentPane());
        selectIndexFolder.getContentPane().setLayout(selectIndexFolderLayout);
        selectIndexFolderLayout.setHorizontalGroup(
            selectIndexFolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(p1IndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectIndexFolderLayout.setVerticalGroup(
            selectIndexFolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(p1IndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectIndexFoderIstruzione.setMinimumSize(new java.awt.Dimension(590, 380));

        p2IndexFileChooser.setApproveButtonText("Seleziona");
        p2IndexFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        p2IndexFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        p2IndexFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        p2IndexFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p2IndexFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectIndexFoderIstruzioneLayout = new javax.swing.GroupLayout(selectIndexFoderIstruzione.getContentPane());
        selectIndexFoderIstruzione.getContentPane().setLayout(selectIndexFoderIstruzioneLayout);
        selectIndexFoderIstruzioneLayout.setHorizontalGroup(
            selectIndexFoderIstruzioneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(p2IndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectIndexFoderIstruzioneLayout.setVerticalGroup(
            selectIndexFoderIstruzioneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(p2IndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectExcelFile.setMinimumSize(new java.awt.Dimension(590, 380));

        excelFileChooser.setApproveButtonText("Seleziona");
        excelFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        excelFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        excelFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                excelFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExcelFileLayout = new javax.swing.GroupLayout(selectExcelFile.getContentPane());
        selectExcelFile.getContentPane().setLayout(selectExcelFileLayout);
        selectExcelFileLayout.setHorizontalGroup(
            selectExcelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(excelFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExcelFileLayout.setVerticalGroup(
            selectExcelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(excelFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectStopWords2.setMinimumSize(new java.awt.Dimension(590, 380));

        stopWordsFileChooser2.setApproveButtonText("Seleziona");
        stopWordsFileChooser2.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        stopWordsFileChooser2.setMaximumSize(new java.awt.Dimension(425, 245));
        stopWordsFileChooser2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopWordsFileChooser2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectStopWords2Layout = new javax.swing.GroupLayout(selectStopWords2.getContentPane());
        selectStopWords2.getContentPane().setLayout(selectStopWords2Layout);
        selectStopWords2Layout.setHorizontalGroup(
            selectStopWords2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stopWordsFileChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectStopWords2Layout.setVerticalGroup(
            selectStopWords2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stopWordsFileChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectExcelFileClass.setMinimumSize(new java.awt.Dimension(620, 425));

        excelFileChooserClass.setApproveButtonText("Seleziona");
        excelFileChooserClass.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        excelFileChooserClass.setMaximumSize(new java.awt.Dimension(425, 245));
        excelFileChooserClass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                excelFileChooserClassActionPerformed(evt);
            }
        });

        jLabel1.setText("Colonna contenente la descrizione");

        colonnaDescrizione.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "Y", "U", "V", "W", "X", "Y", "Z", "AA", "AB", "AC", "AD", "AE", "AF" }));
        colonnaDescrizione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colonnaDescrizioneActionPerformed(evt);
            }
        });

        jLabel9.setText("Processori");

        processori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" }));
        processori.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processoriActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExcelFileClassLayout = new javax.swing.GroupLayout(selectExcelFileClass.getContentPane());
        selectExcelFileClass.getContentPane().setLayout(selectExcelFileClassLayout);
        selectExcelFileClassLayout.setHorizontalGroup(
            selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectExcelFileClassLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectExcelFileClassLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(colonnaDescrizione, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel9)
                        .addGap(18, 18, 18)
                        .addComponent(processori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(excelFileChooserClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        selectExcelFileClassLayout.setVerticalGroup(
            selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectExcelFileClassLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(excelFileChooserClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addGroup(selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(colonnaDescrizione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(selectExcelFileClassLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel9)
                        .addComponent(processori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        selectFileToSegment.setMinimumSize(new java.awt.Dimension(590, 380));

        segmentFileChooser1.setApproveButtonText("Seleziona");
        segmentFileChooser1.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        segmentFileChooser1.setMaximumSize(new java.awt.Dimension(425, 245));
        segmentFileChooser1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentFileChooser1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectFileToSegmentLayout = new javax.swing.GroupLayout(selectFileToSegment.getContentPane());
        selectFileToSegment.getContentPane().setLayout(selectFileToSegmentLayout);
        selectFileToSegmentLayout.setHorizontalGroup(
            selectFileToSegmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(segmentFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectFileToSegmentLayout.setVerticalGroup(
            selectFileToSegmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(segmentFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectFolderToProcess.setMinimumSize(new java.awt.Dimension(620, 425));

        folderChooser.setApproveButtonText("Seleziona");
        folderChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        folderChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        folderChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderChooserActionPerformed(evt);
            }
        });

        jLabel2.setText("Salva anche versione HTML");

        salvaHTML.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Si", "No" }));
        salvaHTML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salvaHTMLActionPerformed(evt);
            }
        });

        jLabel10.setText("Processori");

        processori1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" }));
        processori1.setSelectedItem("2");
        processori1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processori1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectFolderToProcessLayout = new javax.swing.GroupLayout(selectFolderToProcess.getContentPane());
        selectFolderToProcess.getContentPane().setLayout(selectFolderToProcessLayout);
        selectFolderToProcessLayout.setHorizontalGroup(
            selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectFolderToProcessLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectFolderToProcessLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(salvaHTML, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)
                        .addComponent(processori1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(folderChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        selectFolderToProcessLayout.setVerticalGroup(
            selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectFolderToProcessLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(folderChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addGroup(selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(salvaHTML, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(selectFolderToProcessLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(processori1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        selectFolderToLoad.setTitle("Seleziona cartella da caricare");
        selectFolderToLoad.setMinimumSize(new java.awt.Dimension(620, 425));

        folderToLoadChooser.setApproveButtonText("Seleziona");
        folderToLoadChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        folderToLoadChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        folderToLoadChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        folderToLoadChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderToLoadChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectFolderToLoadLayout = new javax.swing.GroupLayout(selectFolderToLoad.getContentPane());
        selectFolderToLoad.getContentPane().setLayout(selectFolderToLoadLayout);
        selectFolderToLoadLayout.setHorizontalGroup(
            selectFolderToLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(folderToLoadChooser, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
        );
        selectFolderToLoadLayout.setVerticalGroup(
            selectFolderToLoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectFolderToLoadLayout.createSequentialGroup()
                .addComponent(folderToLoadChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 365, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        selectSaveStorageAs.setMinimumSize(new java.awt.Dimension(590, 380));

        saveAsFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        saveAsFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        saveAsFileChooser.setFileFilter(new ExtensionFileFilter(".ser storage", new String[]{"SER"}));
        saveAsFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        saveAsFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectSaveStorageAsLayout = new javax.swing.GroupLayout(selectSaveStorageAs.getContentPane());
        selectSaveStorageAs.getContentPane().setLayout(selectSaveStorageAsLayout);
        selectSaveStorageAsLayout.setHorizontalGroup(
            selectSaveStorageAsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(saveAsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectSaveStorageAsLayout.setVerticalGroup(
            selectSaveStorageAsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(saveAsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectOpenStorage.setMinimumSize(new java.awt.Dimension(590, 380));

        openFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        openFileChooser.setFileFilter(new ExtensionFileFilter(".ser storage", new String[]{"SER"}));
        openFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        openFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectOpenStorageLayout = new javax.swing.GroupLayout(selectOpenStorage.getContentPane());
        selectOpenStorage.getContentPane().setLayout(selectOpenStorageLayout);
        selectOpenStorageLayout.setHorizontalGroup(
            selectOpenStorageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(openFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectOpenStorageLayout.setVerticalGroup(
            selectOpenStorageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(openFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectExportTable.setMinimumSize(new java.awt.Dimension(590, 380));

        expotTableFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        expotTableFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        expotTableFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        expotTableFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        expotTableFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expotTableFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExportTableLayout = new javax.swing.GroupLayout(selectExportTable.getContentPane());
        selectExportTable.getContentPane().setLayout(selectExportTableLayout);
        selectExportTableLayout.setHorizontalGroup(
            selectExportTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotTableFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExportTableLayout.setVerticalGroup(
            selectExportTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotTableFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectImportTable.setMinimumSize(new java.awt.Dimension(590, 380));

        importTableFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        importTableFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        importTableFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        importTableFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTableFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectImportTableLayout = new javax.swing.GroupLayout(selectImportTable.getContentPane());
        selectImportTable.getContentPane().setLayout(selectImportTableLayout);
        selectImportTableLayout.setHorizontalGroup(
            selectImportTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importTableFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectImportTableLayout.setVerticalGroup(
            selectImportTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importTableFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectExcelFileSer.setMinimumSize(new java.awt.Dimension(590, 380));

        excelCorpusChooser.setApproveButtonText("Seleziona");
        excelCorpusChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        excelCorpusChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        excelCorpusChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                excelCorpusChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExcelFileSerLayout = new javax.swing.GroupLayout(selectExcelFileSer.getContentPane());
        selectExcelFileSer.getContentPane().setLayout(selectExcelFileSerLayout);
        selectExcelFileSerLayout.setHorizontalGroup(
            selectExcelFileSerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(excelCorpusChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExcelFileSerLayout.setVerticalGroup(
            selectExcelFileSerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(excelCorpusChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        modelEditorFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        modelEditorFrame.setTitle("Model Editor");
        modelEditorFrame.setMinimumSize(new java.awt.Dimension(1024, 768));
        modelEditorFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                modelEditorFrameWindowClosed(evt);
            }
        });

        selectExportPatterns.setMinimumSize(new java.awt.Dimension(590, 380));

        expotPatternsFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        expotPatternsFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        expotPatternsFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        expotPatternsFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        expotPatternsFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expotPatternsFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExportPatternsLayout = new javax.swing.GroupLayout(selectExportPatterns.getContentPane());
        selectExportPatterns.getContentPane().setLayout(selectExportPatternsLayout);
        selectExportPatternsLayout.setHorizontalGroup(
            selectExportPatternsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotPatternsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExportPatternsLayout.setVerticalGroup(
            selectExportPatternsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotPatternsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectImportPatterns.setMinimumSize(new java.awt.Dimension(590, 380));

        importPatternsFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        importPatternsFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        importPatternsFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        importPatternsFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPatternsFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectImportPatternsLayout = new javax.swing.GroupLayout(selectImportPatterns.getContentPane());
        selectImportPatterns.getContentPane().setLayout(selectImportPatternsLayout);
        selectImportPatternsLayout.setHorizontalGroup(
            selectImportPatternsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importPatternsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectImportPatternsLayout.setVerticalGroup(
            selectImportPatternsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importPatternsFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        globalCapturesSegmentsRelationship.setTitle("Definizione visibilità cattura globale");
        globalCapturesSegmentsRelationship.setAlwaysOnTop(true);
        globalCapturesSegmentsRelationship.setMinimumSize(new java.awt.Dimension(450, 350));
        globalCapturesSegmentsRelationship.setModal(true);
        globalCapturesSegmentsRelationship.setName("Relazione"); // NOI18N
        globalCapturesSegmentsRelationship.setResizable(false);

        captureRelationshipTable.setAutoCreateRowSorter(true);
        captureRelationshipTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null}
            },
            new String [] {
                "Segmento", "Abilitato"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        captureRelationshipTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane5.setViewportView(captureRelationshipTable);

        globalCapturesSegmentsRelationship.getContentPane().add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jToolBar7.setRollover(true);

        saveRelationship.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/plugin.png"))); // NOI18N
        saveRelationship.setText("Salva e chiudi");
        saveRelationship.setFocusable(false);
        saveRelationship.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        saveRelationship.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        saveRelationship.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveRelationshipActionPerformed(evt);
            }
        });
        jToolBar7.add(saveRelationship);
        jToolBar7.add(jSeparator32);

        globalCapturesSegmentsRelationship.getContentPane().add(jToolBar7, java.awt.BorderLayout.PAGE_START);

        captureClassificationRelationship.setTitle("Selezione categoria in cui classificare");
        captureClassificationRelationship.setAlwaysOnTop(true);
        captureClassificationRelationship.setMinimumSize(new java.awt.Dimension(450, 550));
        captureClassificationRelationship.setModal(true);
        captureClassificationRelationship.setName("Relazione"); // NOI18N
        captureClassificationRelationship.setResizable(false);

        jToolBar8.setRollover(true);

        saveRelationship1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/plugin.png"))); // NOI18N
        saveRelationship1.setText("Salva e chiudi");
        saveRelationship1.setFocusable(false);
        saveRelationship1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        saveRelationship1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        saveRelationship1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveRelationship1ActionPerformed(evt);
            }
        });
        jToolBar8.add(saveRelationship1);
        jToolBar8.add(jSeparator33);

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross_octagon.png"))); // NOI18N
        jButton10.setText("Nessuna categoria");
        jButton10.setFocusable(false);
        jButton10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton10.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jToolBar8.add(jButton10);
        jToolBar8.add(jSeparator35);

        catClass.setEditable(false);
        jToolBar8.add(catClass);

        captureClassificationRelationship.getContentPane().add(jToolBar8, java.awt.BorderLayout.PAGE_START);

        pannelloAlbero1.setLayout(new java.awt.BorderLayout());

        jScrollPane29.setAutoscrolls(true);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Nessun Albero");
        classificationTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        classificationTree1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        classificationTree1.setVisibleRowCount(10);
        classificationTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                classificationTree1MouseClicked(evt);
            }
        });
        jScrollPane29.setViewportView(classificationTree1);

        pannelloAlbero1.add(jScrollPane29, java.awt.BorderLayout.CENTER);

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });
        jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField2KeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField2KeyTyped(evt);
            }
        });
        pannelloAlbero1.add(jTextField2, java.awt.BorderLayout.NORTH);

        captureClassificationRelationship.getContentPane().add(pannelloAlbero1, java.awt.BorderLayout.CENTER);

        selectExportExcel.setMinimumSize(new java.awt.Dimension(590, 380));

        expotExcelFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        expotExcelFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        expotExcelFileChooser.setFileFilter(new ExtensionFileFilter("Microsoft Excel", new String[]{"xslx"}));
        expotExcelFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        expotExcelFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expotExcelFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExportExcelLayout = new javax.swing.GroupLayout(selectExportExcel.getContentPane());
        selectExportExcel.getContentPane().setLayout(selectExportExcelLayout);
        selectExportExcelLayout.setHorizontalGroup(
            selectExportExcelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotExcelFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExportExcelLayout.setVerticalGroup(
            selectExportExcelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotExcelFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        configurationDialog.setTitle("Configurazione sistema");
        configurationDialog.setMinimumSize(new java.awt.Dimension(630, 200));
        configurationDialog.setResizable(false);
        configurationDialog.setSize(new java.awt.Dimension(630, 150));

        jPanel2.setMaximumSize(new java.awt.Dimension(630, 140));
        jPanel2.setMinimumSize(new java.awt.Dimension(630, 140));
        jPanel2.setPreferredSize(new java.awt.Dimension(630, 140));

        percorsoIndice.setEditable(false);
        percorsoIndice.setText(cc.getIndexFolder());
        percorsoIndice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                percorsoIndiceActionPerformed(evt);
            }
        });

        selezionaIndice.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_database.png"))); // NOI18N
        selezionaIndice.setText("SEM Storage");
        selezionaIndice.setFocusable(false);
        selezionaIndice.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        selezionaIndice.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        selezionaIndice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selezionaIndiceActionPerformed(evt);
            }
        });

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/target.png"))); // NOI18N
        jButton2.setText("Inizializza");
        jButton2.setFocusable(false);
        jButton2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        onlySegment.setText("Solo segmentatore");
        onlySegment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onlySegmentActionPerformed(evt);
            }
        });

        rebuildIndex.setText("Reindex");
        rebuildIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rebuildIndexActionPerformed(evt);
            }
        });

        selezionaOCR.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_lightning.png"))); // NOI18N
        selezionaOCR.setText("OCR Path");
        selezionaOCR.setFocusable(false);
        selezionaOCR.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        selezionaOCR.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        selezionaOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selezionaOCRActionPerformed(evt);
            }
        });

        percorsoOCR.setEditable(false);
        percorsoOCR.setText(cc.getOcrPath());
        percorsoOCR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                percorsoOCRActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(selezionaOCR, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selezionaIndice, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(onlySegment)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 123, Short.MAX_VALUE)
                        .addComponent(rebuildIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(percorsoOCR)
                    .addComponent(percorsoIndice, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(selezionaIndice)
                    .addComponent(percorsoIndice, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(selezionaOCR)
                    .addComponent(percorsoOCR, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(onlySegment)
                    .addComponent(rebuildIndex))
                .addContainerGap(81, Short.MAX_VALUE))
        );

        configurationDialog.getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        initLabel.setText(".");
        initLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        configurationDialog.getContentPane().add(initLabel, java.awt.BorderLayout.SOUTH);

        selectExportTree.setMinimumSize(new java.awt.Dimension(590, 380));

        exportTreeFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        exportTreeFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        exportTreeFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        exportTreeFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        exportTreeFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTreeFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExportTreeLayout = new javax.swing.GroupLayout(selectExportTree.getContentPane());
        selectExportTree.getContentPane().setLayout(selectExportTreeLayout);
        selectExportTreeLayout.setHorizontalGroup(
            selectExportTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(exportTreeFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExportTreeLayout.setVerticalGroup(
            selectExportTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(exportTreeFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectImportTree.setMinimumSize(new java.awt.Dimension(590, 380));

        importTreeFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        importTreeFileChooser.setFileFilter(new ExtensionFileFilter("CSV (Comma Separated Values)", new String[]{"CSV"}));
        importTreeFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        importTreeFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTreeFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectImportTreeLayout = new javax.swing.GroupLayout(selectImportTree.getContentPane());
        selectImportTree.getContentPane().setLayout(selectImportTreeLayout);
        selectImportTreeLayout.setHorizontalGroup(
            selectImportTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importTreeFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectImportTreeLayout.setVerticalGroup(
            selectImportTreeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importTreeFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectFileToImport.setMinimumSize(new java.awt.Dimension(590, 380));

        importFileChooser.setApproveButtonText("Seleziona");
        importFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        importFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        importFileChooser.setMultiSelectionEnabled(true);
        importFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectFileToImportLayout = new javax.swing.GroupLayout(selectFileToImport.getContentPane());
        selectFileToImport.getContentPane().setLayout(selectFileToImportLayout);
        selectFileToImportLayout.setHorizontalGroup(
            selectFileToImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectFileToImportLayout.setVerticalGroup(
            selectFileToImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        globalTagCloud.setTitle("Documents Tag Cloud");
        globalTagCloud.setMinimumSize(new java.awt.Dimension(800, 600));
        globalTagCloud.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                globalTagCloudComponentResized(evt);
            }
        });

        selectCSVDataProvider.setMinimumSize(new java.awt.Dimension(590, 380));

        csvdpchooser.setApproveButtonText("Seleziona");
        csvdpchooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        csvdpchooser.setMaximumSize(new java.awt.Dimension(425, 245));
        csvdpchooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                csvdpchooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectCSVDataProviderLayout = new javax.swing.GroupLayout(selectCSVDataProvider.getContentPane());
        selectCSVDataProvider.getContentPane().setLayout(selectCSVDataProviderLayout);
        selectCSVDataProviderLayout.setHorizontalGroup(
            selectCSVDataProviderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(csvdpchooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectCSVDataProviderLayout.setVerticalGroup(
            selectCSVDataProviderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(csvdpchooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectOCRFolder.setMinimumSize(new java.awt.Dimension(590, 380));

        ocrFileChooser.setApproveButtonText("Seleziona");
        ocrFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        ocrFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        ocrFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        ocrFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ocrFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectOCRFolderLayout = new javax.swing.GroupLayout(selectOCRFolder.getContentPane());
        selectOCRFolder.getContentPane().setLayout(selectOCRFolderLayout);
        selectOCRFolderLayout.setHorizontalGroup(
            selectOCRFolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ocrFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectOCRFolderLayout.setVerticalGroup(
            selectOCRFolderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ocrFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        selectExportExcelIndex.setMinimumSize(new java.awt.Dimension(590, 380));

        expotExcelIndexFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        expotExcelIndexFileChooser.setCurrentDirectory(new java.io.File("C:\\Program Files\\NetBeans 8.1"));
        expotExcelIndexFileChooser.setFileFilter(new ExtensionFileFilter("Microsoft Excel", new String[]{"xslx"}));
        expotExcelIndexFileChooser.setMaximumSize(new java.awt.Dimension(425, 245));
        expotExcelIndexFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expotExcelIndexFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout selectExportExcelIndexLayout = new javax.swing.GroupLayout(selectExportExcelIndex.getContentPane());
        selectExportExcelIndex.getContentPane().setLayout(selectExportExcelIndexLayout);
        selectExportExcelIndexLayout.setHorizontalGroup(
            selectExportExcelIndexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotExcelIndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        selectExportExcelIndexLayout.setVerticalGroup(
            selectExportExcelIndexLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(expotExcelIndexFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("SemGui");
        setLocation(new java.awt.Point(0, 0));
        setMinimumSize(new java.awt.Dimension(1024, 768));

        consolleToolbar.setRollover(true);

        configuration.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/books.png"))); // NOI18N
        configuration.setText("Configurazione");
        configuration.setFocusable(false);
        configuration.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        configuration.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        configuration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configurationActionPerformed(evt);
            }
        });
        consolleToolbar.add(configuration);
        consolleToolbar.add(jSeparator25);

        segmentaEClassifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/doc_resize.png"))); // NOI18N
        segmentaEClassifica.setText("Segmenta e classifica");
        segmentaEClassifica.setToolTipText("Segmenta e classifica");
        segmentaEClassifica.setEnabled(false);
        segmentaEClassifica.setFocusable(false);
        segmentaEClassifica.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        segmentaEClassifica.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentaEClassifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentaEClassificaActionPerformed(evt);
            }
        });
        consolleToolbar.add(segmentaEClassifica);

        segmentaEBasta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/text_indent.png"))); // NOI18N
        segmentaEBasta.setText("Segmenta");
        segmentaEBasta.setEnabled(false);
        segmentaEBasta.setFocusable(false);
        segmentaEBasta.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmentaEBasta.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentaEBasta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentaEBastaActionPerformed(evt);
            }
        });
        consolleToolbar.add(segmentaEBasta);

        interrompi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bomb.png"))); // NOI18N
        interrompi.setText("Interrompi");
        interrompi.setEnabled(false);
        interrompi.setFocusable(false);
        interrompi.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        interrompi.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        interrompi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                interrompiActionPerformed(evt);
            }
        });
        consolleToolbar.add(interrompi);
        consolleToolbar.add(jSeparator26);

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_side_tree.png"))); // NOI18N
        jLabel7.setText("Soglia Bayes ");
        consolleToolbar.add(jLabel7);

        soglia.setText(cc.getThreshold());
        soglia.setToolTipText("Valore della soglia per accettare una classificazione");
        soglia.setMaximumSize(new java.awt.Dimension(72, 20));
        soglia.setMinimumSize(new java.awt.Dimension(72, 20));
        soglia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sogliaActionPerformed(evt);
            }
        });
        consolleToolbar.add(soglia);
        consolleToolbar.add(jSeparator22);

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/ruler_crop.png"))); // NOI18N
        jLabel12.setText("Livello di classificazione");
        consolleToolbar.add(jLabel12);

        classStartLevel.setModel(getClassTreeDepth());
        classStartLevel.setToolTipText("Indica il levello a partire dal quale il sistema deve classificare");
        classStartLevel.setMaximumSize(new java.awt.Dimension(50, 22));
        classStartLevel.setMinimumSize(new java.awt.Dimension(50, 22));
        classStartLevel.setPreferredSize(new java.awt.Dimension(50, 22));
        classStartLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classStartLevelActionPerformed(evt);
            }
        });
        consolleToolbar.add(classStartLevel);

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/inbox.png"))); // NOI18N
        jLabel8.setText("Fattore K ");
        consolleToolbar.add(jLabel8);

        fattoreK.setText(cc.getkFactor());
        fattoreK.setMaximumSize(new java.awt.Dimension(72, 20));
        fattoreK.setMinimumSize(new java.awt.Dimension(72, 20));
        fattoreK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fattoreKActionPerformed(evt);
            }
        });
        consolleToolbar.add(fattoreK);
        consolleToolbar.add(jSeparator24);

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/ui_saccordion.png"))); // NOI18N
        jLabel15.setText("Peso istruzione ");
        consolleToolbar.add(jLabel15);

        learningFactor.setText(cc.getLearningFactor());
        learningFactor.setMaximumSize(new java.awt.Dimension(72, 20));
        learningFactor.setMinimumSize(new java.awt.Dimension(72, 20));
        learningFactor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                learningFactorActionPerformed(evt);
            }
        });
        consolleToolbar.add(learningFactor);
        consolleToolbar.add(jSeparator1);

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cog.png"))); // NOI18N
        jLabel3.setText("Processori");
        consolleToolbar.add(jLabel3);

        processori2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" }));
        processori2.setSelectedIndex(1);
        processori2.setMaximumSize(new java.awt.Dimension(50, 22));
        processori2.setMinimumSize(new java.awt.Dimension(50, 22));
        processori2.setPreferredSize(new java.awt.Dimension(50, 22));
        processori2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processori2ActionPerformed(evt);
            }
        });
        consolleToolbar.add(processori2);
        consolleToolbar.add(jSeparator41);

        memInfo.setText(".");
        consolleToolbar.add(memInfo);
        consolleToolbar.add(jSeparator42);

        jButton13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/compass.png"))); // NOI18N
        jButton13.setText("Garbage Collector");
        jButton13.setFocusable(false);
        jButton13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton13.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });
        consolleToolbar.add(jButton13);

        getContentPane().add(consolleToolbar, java.awt.BorderLayout.NORTH);

        files.setLayout(new java.awt.BorderLayout());

        filesSplitPanel.setDividerLocation(400);
        filesSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        filesTable.setAutoCreateRowSorter(true);
        filesTable.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        filesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "File", "Lingua", "#Segmenti", "#Seg classificabili", "#Catture", "#Frasi", "#Classificazioni", "Anteprima testo", "Html", "KPI"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        filesTable.getTableHeader().setReorderingAllowed(false);
        filesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                filesTableMouseClicked(evt);
            }
        });
        filesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                filesTableKeyTyped(evt);
            }
        });
        jScrollPane7.setViewportView(filesTable);
        if (filesTable.getColumnModel().getColumnCount() > 0) {
            filesTable.getColumnModel().getColumn(0).setMinWidth(50);
            filesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            filesTable.getColumnModel().getColumn(0).setMaxWidth(50);
            filesTable.getColumnModel().getColumn(1).setMinWidth(300);
            filesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
            filesTable.getColumnModel().getColumn(1).setMaxWidth(300);
            filesTable.getColumnModel().getColumn(2).setMinWidth(50);
            filesTable.getColumnModel().getColumn(2).setPreferredWidth(50);
            filesTable.getColumnModel().getColumn(2).setMaxWidth(50);
            filesTable.getColumnModel().getColumn(9).setResizable(false);
            filesTable.getColumnModel().getColumn(9).setPreferredWidth(0);
            filesTable.getColumnModel().getColumn(10).setMinWidth(50);
            filesTable.getColumnModel().getColumn(10).setPreferredWidth(50);
            filesTable.getColumnModel().getColumn(10).setMaxWidth(150);
        }
        int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap inputMap = filesTable.getInputMap(condition);
        ActionMap actionMap = filesTable.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
        actionMap.put(DELETE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                filesTableDelete();
            }
        });

        filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (filesTable.getSelectedRow() > -1) {
                    // print first column value from selected row
                    filesTableSelectedRow();
                }
            }
        });

        filesSplitPanel.setLeftComponent(jScrollPane7);

        jSplitPane4.setDividerLocation(350);

        filesSx.setLayout(new java.awt.BorderLayout());

        jToolBar2.setRollover(true);

        filesPanelSegmenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/doc_resize.png"))); // NOI18N
        filesPanelSegmenta.setText("Segmenta e classifica");
        filesPanelSegmenta.setToolTipText("Segmenta e classifica");
        filesPanelSegmenta.setEnabled(false);
        filesPanelSegmenta.setFocusable(false);
        filesPanelSegmenta.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        filesPanelSegmenta.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        filesPanelSegmenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filesPanelSegmentaActionPerformed(evt);
            }
        });
        jToolBar2.add(filesPanelSegmenta);

        filesSx.add(jToolBar2, java.awt.BorderLayout.PAGE_END);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        filesPanelSegmentTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane13.setViewportView(filesPanelSegmentTree);

        jTabbedPane3.addTab("Segmentazione", jScrollPane13);

        jPanel6.setLayout(new java.awt.BorderLayout());

        filesPanleCapturesTable.setAutoCreateRowSorter(true);
        filesPanleCapturesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Segmento", "Nome", "Valore"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        filesPanleCapturesTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane17.setViewportView(filesPanleCapturesTable);

        jPanel6.add(jScrollPane17, java.awt.BorderLayout.CENTER);

        jTabbedPane3.addTab("Catture", jPanel6);

        filesSx.add(jTabbedPane3, java.awt.BorderLayout.CENTER);

        jSplitPane4.setLeftComponent(filesSx);

        filesDx.setLayout(new java.awt.BorderLayout());

        fileText.setEditable(false);
        fileText.setColumns(20);
        fileText.setLineWrap(true);
        fileText.setRows(10);
        jScrollPane15.setViewportView(fileText);

        jTabbedPane4.addTab("Testo Documento", jScrollPane15);

        jPanel5.setLayout(new java.awt.BorderLayout());

        filesPanelHtml.setEditable(false);
        jScrollPane16.setViewportView(filesPanelHtml);

        jPanel5.add(jScrollPane16, java.awt.BorderLayout.CENTER);

        jTabbedPane4.addTab("HTML", jPanel5);

        filesPanelHtmlFormatted.setEditable(false);
        filesPanelHtmlFormatted.setContentType("text/html"); // NOI18N
        jScrollPane25.setViewportView(filesPanelHtmlFormatted);

        jTabbedPane4.addTab("Testo Formattato", jScrollPane25);

        filesDx.add(jTabbedPane4, java.awt.BorderLayout.CENTER);

        jSplitPane4.setRightComponent(filesDx);

        filesSplitPanel.setRightComponent(jSplitPane4);

        files.add(filesSplitPanel, java.awt.BorderLayout.CENTER);

        filesInfoLabel.setText("Documenti letti:");
        filesInfoLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        files.add(filesInfoLabel, java.awt.BorderLayout.PAGE_END);

        filesToolbar.setRollover(true);

        menuCarica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/drive_go.png"))); // NOI18N
        menuCarica.setText("Carica dati");
        menuCarica.setFocusable(false);
        menuCarica.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        menuCarica.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        menuCarica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCaricaActionPerformed(evt);
            }
        });
        filesToolbar.add(menuCarica);
        filesToolbar.add(jSeparator4);

        salvaStorage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/disk.png"))); // NOI18N
        salvaStorage.setText("Salva storage");
        salvaStorage.setToolTipText("Salva storage");
        salvaStorage.setFocusable(false);
        salvaStorage.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        salvaStorage.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        salvaStorage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salvaStorageActionPerformed(evt);
            }
        });
        filesToolbar.add(salvaStorage);

        exportToExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_excel.png"))); // NOI18N
        exportToExcel.setText("Esporta Excel");
        exportToExcel.setFocusable(false);
        exportToExcel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        exportToExcel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        exportToExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportToExcelActionPerformed(evt);
            }
        });
        filesToolbar.add(exportToExcel);
        filesToolbar.add(jSeparator3);

        tagCloud.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/eye.png"))); // NOI18N
        tagCloud.setText("Tag Cloud");
        tagCloud.setEnabled(false);
        tagCloud.setFocusable(false);
        tagCloud.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tagCloud.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        tagCloud.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagCloudActionPerformed(evt);
            }
        });
        filesToolbar.add(tagCloud);

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_undo.png"))); // NOI18N
        jButton5.setToolTipText("Reset");
        jButton5.setFocusable(false);
        jButton5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        filesToolbar.add(jButton5);
        filesToolbar.add(jSeparator2);

        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        jButton6.setToolTipText("Rimuovi tutti i filtri");
        jButton6.setFocusable(false);
        jButton6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        filesToolbar.add(jButton6);
        filesToolbar.add(jSeparator9);

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel13.setText("Cerca nel testo: ");
        filesToolbar.add(jLabel13);

        filterFile.setColumns(30);
        filterFile.setMinimumSize(new java.awt.Dimension(80, 20));
        filterFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterFileActionPerformed(evt);
            }
        });
        filterFile.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filterFileKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                filterFileKeyTyped(evt);
            }
        });
        filesToolbar.add(filterFile);

        files.add(filesToolbar, java.awt.BorderLayout.NORTH);

        filesTab.addTab("Storage", files);

        segments.setDividerLocation(250);

        classificationTreePanel.setLayout(new java.awt.BorderLayout());

        etichettaAlberoSegmenti.setText("Albero categorie - ");
        classificationTreePanel.add(etichettaAlberoSegmenti, java.awt.BorderLayout.PAGE_START);

        jPanel9.setLayout(new java.awt.BorderLayout());

        cercaCategoriaSegmentsPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cercaCategoriaSegmentsPanelActionPerformed(evt);
            }
        });
        jPanel9.add(cercaCategoriaSegmentsPanel, java.awt.BorderLayout.PAGE_START);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        categorieSegmentsPanel.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        categorieSegmentsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                categorieSegmentsPanelMouseClicked(evt);
            }
        });
        jScrollPane19.setViewportView(categorieSegmentsPanel);

        jPanel9.add(jScrollPane19, java.awt.BorderLayout.CENTER);

        classificationTreePanel.add(jPanel9, java.awt.BorderLayout.CENTER);

        segments.setLeftComponent(classificationTreePanel);

        segmentsConsolleSplitPanel.setDividerLocation(400);
        segmentsConsolleSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel4.setLayout(new java.awt.BorderLayout());

        segmentsTable.setAutoCreateRowSorter(true);
        segmentsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Bayes 1", "Bayes 2", "Lingua", "Testo", "File", "Chk"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        segmentsTable.getTableHeader().setReorderingAllowed(false);
        segmentsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                segmentsTableMouseClicked(evt);
            }
        });
        segmentsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                segmentsTableKeyTyped(evt);
            }
        });
        jScrollPane12.setViewportView(segmentsTable);
        segmentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (segmentsTable.getSelectedRow() > -1) {
                    // print first column value from selected row
                    segmentsTableSelectedRow();
                }
            }
        });

        int segCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        InputMap segInputMap = segmentsTable.getInputMap(segCondition);
        ActionMap segActionMap = segmentsTable.getActionMap();

        segInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
        segActionMap.put(DELETE, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                hilightSegment();
            }
        });

        jPanel4.add(jScrollPane12, java.awt.BorderLayout.CENTER);

        jToolBar3.setRollover(true);

        firstLevelOnly.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/ui_slider_2.png"))); // NOI18N
        firstLevelOnly.setText("Livello 1");
        firstLevelOnly.setToolTipText("Livello 1");
        firstLevelOnly.setFocusable(false);
        firstLevelOnly.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        firstLevelOnly.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        firstLevelOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstLevelOnlyActionPerformed(evt);
            }
        });
        jToolBar3.add(firstLevelOnly);

        underTreshold.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/smiley_confuse.png"))); // NOI18N
        underTreshold.setText("Non a soglia");
        underTreshold.setToolTipText("Non a soglia");
        underTreshold.setFocusable(false);
        underTreshold.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        underTreshold.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        underTreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                underTresholdActionPerformed(evt);
            }
        });
        jToolBar3.add(underTreshold);

        notMarked.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_add.png"))); // NOI18N
        notMarked.setText("Non marcati");
        notMarked.setToolTipText("Non marcati");
        notMarked.setFocusable(false);
        notMarked.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        notMarked.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        notMarked.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                notMarkedActionPerformed(evt);
            }
        });
        jToolBar3.add(notMarked);

        changed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_edit.png"))); // NOI18N
        changed.setText("Cambiati");
        changed.setToolTipText("Cambiati");
        changed.setFocusable(false);
        changed.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        changed.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        changed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changedActionPerformed(evt);
            }
        });
        jToolBar3.add(changed);

        alert.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_error.png"))); // NOI18N
        alert.setText("Allerta");
        alert.setToolTipText("Allerta");
        alert.setFocusable(false);
        alert.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        alert.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        alert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alertActionPerformed(evt);
            }
        });
        jToolBar3.add(alert);
        jToolBar3.add(jSeparator7);

        removeFilters.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeFilters.setToolTipText("Rimuovi tutti i filtri");
        removeFilters.setFocusable(false);
        removeFilters.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeFilters.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        removeFilters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFiltersActionPerformed(evt);
            }
        });
        jToolBar3.add(removeFilters);
        jToolBar3.add(jSeparator8);

        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel11.setText("Cerca nel testo: ");
        jToolBar3.add(jLabel11);

        filterSegments.setColumns(20);
        filterSegments.setMinimumSize(new java.awt.Dimension(80, 20));
        filterSegments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterSegmentsActionPerformed(evt);
            }
        });
        filterSegments.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                filterSegmentsKeyTyped(evt);
            }
        });
        jToolBar3.add(filterSegments);

        jPanel4.add(jToolBar3, java.awt.BorderLayout.PAGE_START);

        statusSegments.setText("Totale elementi: ");
        jPanel4.add(statusSegments, java.awt.BorderLayout.PAGE_END);

        segmentsConsolleSplitPanel.setLeftComponent(jPanel4);

        jSplitPane5.setDividerLocation(351);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        segmentClassificationResult.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        segmentClassificationResult.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                segmentClassificationResultMouseClicked(evt);
            }
        });
        jScrollPane18.setViewportView(segmentClassificationResult);

        jSplitPane5.setLeftComponent(jScrollPane18);

        segmentText.setEditable(false);
        segmentText.setColumns(20);
        segmentText.setLineWrap(true);
        segmentText.setRows(5);
        segmentText.setWrapStyleWord(true);
        jScrollPane14.setViewportView(segmentText);

        jTabbedPane6.addTab("Testo", jScrollPane14);

        segmentTokens.setColumns(20);
        segmentTokens.setLineWrap(true);
        segmentTokens.setRows(5);
        jScrollPane20.setViewportView(segmentTokens);

        jTabbedPane6.addTab("Tokens", jScrollPane20);

        jSplitPane5.setRightComponent(jTabbedPane6);

        segmentsConsolleSplitPanel.setRightComponent(jSplitPane5);

        segments.setRightComponent(segmentsConsolleSplitPanel);

        filesTab.addTab("Segmenti", segments);

        changes.setDividerLocation(250);
        changes.setLastDividerLocation(300);

        changedTable.setAutoCreateRowSorter(true);
        changedTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Elemento", "Testo", "Vecchio valore", "Nuovo valore", "Classe"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        changedTable.getTableHeader().setReorderingAllowed(false);
        changedTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                changedTableMouseClicked(evt);
            }
        });
        changesTableScrollPanel.setViewportView(changedTable);

        changes.setRightComponent(changesTableScrollPanel);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Cambiamenti");
        changedFilterTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        changedFilterTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                changedFilterTreeMouseClicked(evt);
            }
        });
        changesTreeScrollPanel.setViewportView(changedFilterTree);

        changes.setLeftComponent(changesTreeScrollPanel);

        filesTab.addTab("Cambiamenti", changes);

        coverage.setDividerLocation(400);
        coverage.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        coverageSplitPanel.setDividerLocation(400);

        captureValues.setAutoCreateRowSorter(true);
        captureValues.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Segmento", "Cattura", "Valore", "Files"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        captureValues.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                captureValuesMouseClicked(evt);
            }
        });
        jScrollPane23.setViewportView(captureValues);

        coverageSplitPanel.setLeftComponent(jScrollPane23);

        coverageDocumentsTable.setAutoCreateRowSorter(true);
        coverageDocumentsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Testo"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        coverageDocumentsTable.getTableHeader().setReorderingAllowed(false);
        coverageDocumentsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                coverageDocumentsTableMouseClicked(evt);
            }
        });
        jScrollPane24.setViewportView(coverageDocumentsTable);

        coverageSplitPanel.setRightComponent(jScrollPane24);

        coverage.setBottomComponent(coverageSplitPanel);

        coverageTable.setAutoCreateRowSorter(true);
        coverageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Segmento", "Nome", "#Catture", "#Seg Candidati", "#Valori", "Copertura"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        coverageTable.getTableHeader().setReorderingAllowed(false);
        coverageTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                coverageTableMouseClicked(evt);
            }
        });
        coverageTableScrollPanel.setViewportView(coverageTable);

        coverage.setLeftComponent(coverageTableScrollPanel);

        filesTab.addTab("Copertura Catture", coverage);

        classificazione.setLayout(new java.awt.BorderLayout());

        jToolBar4.setRollover(true);

        batch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/doc_excel_table.png"))); // NOI18N
        batch.setText("Batch");
        batch.setToolTipText("Batch");
        batch.setEnabled(false);
        batch.setFocusable(false);
        batch.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        batch.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        batch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                batchActionPerformed(evt);
            }
        });
        jToolBar4.add(batch);

        classificaTesto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_view_columns.png"))); // NOI18N
        classificaTesto.setText("Classifica");
        classificaTesto.setToolTipText("Classifica");
        classificaTesto.setEnabled(false);
        classificaTesto.setFocusable(false);
        classificaTesto.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        classificaTesto.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        classificaTesto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classificaTestoActionPerformed(evt);
            }
        });
        jToolBar4.add(classificaTesto);

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_undo.png"))); // NOI18N
        jButton3.setText("Reset");
        jButton3.setToolTipText("Reset");
        jButton3.setEnabled(false);
        jButton3.setFocusable(false);
        jButton3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar4.add(jButton3);

        classificazione.add(jToolBar4, java.awt.BorderLayout.PAGE_START);

        jSplitPane1.setDividerLocation(500);

        pannelloAlbero.setLayout(new java.awt.BorderLayout());

        jScrollPane10.setAutoscrolls(true);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Nessun Albero");
        classificationTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        classificationTree.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        classificationTree.setVisibleRowCount(10);
        classificationTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                classificationTreeMouseClicked(evt);
            }
        });
        jScrollPane10.setViewportView(classificationTree);

        pannelloAlbero.add(jScrollPane10, java.awt.BorderLayout.CENTER);

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });
        pannelloAlbero.add(jTextField1, java.awt.BorderLayout.NORTH);

        jSplitPane1.setLeftComponent(pannelloAlbero);

        jSplitPane2.setDividerLocation(250);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        pannelloTesto.setLayout(new java.awt.BorderLayout());

        testo.setColumns(20);
        testo.setLineWrap(true);
        testo.setRows(5);
        testo.setDragEnabled(true);
        jScrollPane3.setViewportView(testo);

        pannelloTesto.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        labelTesto.setText("Testo da classificare");
        pannelloTesto.add(labelTesto, java.awt.BorderLayout.NORTH);

        jSplitPane2.setTopComponent(pannelloTesto);

        jSplitPane3.setDividerLocation(150);
        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        pannelloTokenizzazione.setLayout(new java.awt.BorderLayout());

        jLabel4.setText("Testo tokenizzato");
        pannelloTokenizzazione.add(jLabel4, java.awt.BorderLayout.NORTH);

        token.setEditable(false);
        token.setColumns(20);
        token.setLineWrap(true);
        token.setRows(5);
        jScrollPane2.setViewportView(token);

        pannelloTokenizzazione.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jSplitPane3.setTopComponent(pannelloTokenizzazione);

        pannelloClassificazione.setLayout(new java.awt.BorderLayout());

        classificationStatus.setText("-");
        pannelloClassificazione.add(classificationStatus, java.awt.BorderLayout.NORTH);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Nessuna Classificazione");
        classificationResult.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane4.setViewportView(classificationResult);

        pannelloClassificazione.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jSplitPane3.setRightComponent(pannelloClassificazione);

        jSplitPane2.setRightComponent(jSplitPane3);

        jSplitPane1.setRightComponent(jSplitPane2);

        classificazione.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        filesTab.addTab("Classificazione", classificazione);

        jSplitPane9.setDividerLocation(501);
        jSplitPane9.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        htmlTimeline.setEditable(false);
        jScrollPane31.setViewportView(htmlTimeline);

        jSplitPane9.setBottomComponent(jScrollPane31);

        segmentazione.setLayout(new java.awt.BorderLayout());

        jToolBar5.setRollover(true);

        segmenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/text_indent.png"))); // NOI18N
        segmenta.setText("Segmeta testo");
        segmenta.setToolTipText("Segmeta testo");
        segmenta.setEnabled(false);
        segmenta.setFocusable(false);
        segmenta.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmenta.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentaActionPerformed(evt);
            }
        });
        jToolBar5.add(segmenta);

        segmentaFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/script.png"))); // NOI18N
        segmentaFile.setText("Segmenta File");
        segmentaFile.setToolTipText("Segmenta File");
        segmentaFile.setEnabled(false);
        segmentaFile.setFocusable(false);
        segmentaFile.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmentaFile.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentaFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentaFileActionPerformed(evt);
            }
        });
        jToolBar5.add(segmentaFile);

        segmentaCartella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/folder.png"))); // NOI18N
        segmentaCartella.setText("Batch cartella");
        segmentaCartella.setToolTipText("Batch cartella");
        segmentaCartella.setEnabled(false);
        segmentaCartella.setFocusable(false);
        segmentaCartella.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmentaCartella.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentaCartella.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentaCartellaActionPerformed(evt);
            }
        });
        jToolBar5.add(segmentaCartella);

        nuvoletta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/eye.png"))); // NOI18N
        nuvoletta.setText("Tag Cloud");
        nuvoletta.setEnabled(false);
        nuvoletta.setFocusable(false);
        nuvoletta.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        nuvoletta.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        nuvoletta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nuvolettaActionPerformed(evt);
            }
        });
        jToolBar5.add(nuvoletta);
        nuvoletta.getAccessibleContext().setAccessibleDescription("");

        resetSegmenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_undo.png"))); // NOI18N
        resetSegmenta.setText("Reset");
        resetSegmenta.setToolTipText("Reset");
        resetSegmenta.setEnabled(false);
        resetSegmenta.setFocusable(false);
        resetSegmenta.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        resetSegmenta.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        resetSegmenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetSegmentaActionPerformed(evt);
            }
        });
        jToolBar5.add(resetSegmenta);

        segmentazione.add(jToolBar5, java.awt.BorderLayout.PAGE_START);

        jSplitPane6.setDividerLocation(500);

        jPanel8.setLayout(new java.awt.BorderLayout());

        testoDaSegmentare.setColumns(20);
        testoDaSegmentare.setLineWrap(true);
        testoDaSegmentare.setRows(5);
        testoDaSegmentare.setWrapStyleWord(true);
        testoDaSegmentare.setDragEnabled(true);
        jScrollPane6.setViewportView(testoDaSegmentare);

        jTabbedPane1.addTab("Testo da segmentare", jScrollPane6);

        htmlFormatted.setEditable(false);
        htmlFormatted.setContentType("text/html"); // NOI18N
        jScrollPane22.setViewportView(htmlFormatted);

        jTabbedPane1.addTab("Testo formattato", jScrollPane22);

        jPanel8.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        jSplitPane6.setLeftComponent(jPanel8);

        jPanel11.setLayout(new java.awt.BorderLayout());

        htmlResult.setEditable(false);
        jScrollPane11.setViewportView(htmlResult);

        jPanel11.add(jScrollPane11, java.awt.BorderLayout.CENTER);

        jTabbedPane2.addTab("HTML", jPanel11);

        jSplitPane7.setDividerLocation(500);
        jSplitPane7.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Nessun segmento");
        segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        segmentTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                segmentTreeMouseClicked(evt);
            }
        });
        segmentTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                segmentTreeValueChanged(evt);
            }
        });
        jScrollPane8.setViewportView(segmentTree);

        jSplitPane7.setLeftComponent(jScrollPane8);

        segmentTextArea.setEditable(false);
        segmentTextArea.setColumns(20);
        segmentTextArea.setLineWrap(true);
        segmentTextArea.setRows(5);
        jScrollPane9.setViewportView(segmentTextArea);

        jSplitPane7.setRightComponent(jScrollPane9);

        jTabbedPane2.addTab("Segmentazione", jSplitPane7);

        imagesScrollPanel.setBackground(new java.awt.Color(255, 255, 255));

        imagesPanel.setBackground(new java.awt.Color(255, 255, 255));
        imagesPanel.setMinimumSize(new java.awt.Dimension(100, 200));
        imagesPanel.setPreferredSize(new java.awt.Dimension(680, 200));

        javax.swing.GroupLayout imagesPanelLayout = new javax.swing.GroupLayout(imagesPanel);
        imagesPanel.setLayout(imagesPanelLayout);
        imagesPanelLayout.setHorizontalGroup(
            imagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1322, Short.MAX_VALUE)
        );
        imagesPanelLayout.setVerticalGroup(
            imagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 440, Short.MAX_VALUE)
        );

        imagesScrollPanel.setViewportView(imagesPanel);

        jTabbedPane2.addTab("Immagini", imagesScrollPanel);

        jSplitPane6.setRightComponent(jTabbedPane2);

        segmentazione.add(jSplitPane6, java.awt.BorderLayout.CENTER);

        jSplitPane9.setTopComponent(segmentazione);

        filesTab.addTab("Segmentazione", jSplitPane9);

        modelEditorContainer.setDividerLocation(1100);

        modelEditor.setDividerLocation(400);

        dictionarySplit.setDividerLocation(360);
        dictionarySplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        dictionarySplit.setLastDividerLocation(320);

        dictionaryPanel.setLayout(new java.awt.BorderLayout());

        dictionaryToolbar.setRollover(true);

        renameDefinition.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_edit.png"))); // NOI18N
        renameDefinition.setToolTipText("Rinomina");
        renameDefinition.setEnabled(false);
        renameDefinition.setFocusable(false);
        renameDefinition.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        renameDefinition.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        renameDefinition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renameDefinitionActionPerformed(evt);
            }
        });
        dictionaryToolbar.add(renameDefinition);

        deleteDefinition.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        deleteDefinition.setToolTipText("Cancella");
        deleteDefinition.setFocusable(false);
        deleteDefinition.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        deleteDefinition.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        deleteDefinition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDefinitionActionPerformed(evt);
            }
        });
        dictionaryToolbar.add(deleteDefinition);
        dictionaryToolbar.add(jSeparator5);

        removeDefinitionFilters.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeDefinitionFilters.setToolTipText("Rimuovi filtri");
        removeDefinitionFilters.setFocusable(false);
        removeDefinitionFilters.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeDefinitionFilters.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        removeDefinitionFilters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefinitionFiltersActionPerformed(evt);
            }
        });
        dictionaryToolbar.add(removeDefinitionFilters);
        dictionaryToolbar.add(jSeparator6);

        jLabel16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel16.setText("Cerca ");
        dictionaryToolbar.add(jLabel16);

        searchDefinition.setColumns(10);
        searchDefinition.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchDefinitionKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchDefinitionKeyTyped(evt);
            }
        });
        dictionaryToolbar.add(searchDefinition);

        dictionaryPanel.add(dictionaryToolbar, java.awt.BorderLayout.PAGE_START);
        dictionaryPanel.add(dictionaryStatus, java.awt.BorderLayout.PAGE_END);

        dictionaryTable.setAutoCreateRowSorter(true);
        dictionaryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome", "Pattern"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dictionaryTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        dictionaryTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        dictionaryTable.getTableHeader().setReorderingAllowed(false);
        dictionaryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dictionaryTableMouseClicked(evt);
            }
        });
        dictionaryTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                dictionaryTableKeyTyped(evt);
            }
        });
        dictionaryTableScrollPanel.setViewportView(dictionaryTable);
        if (dictionaryTable.getColumnModel().getColumnCount() > 0) {
            dictionaryTable.getColumnModel().getColumn(0).setMinWidth(100);
            dictionaryTable.getColumnModel().getColumn(0).setPreferredWidth(150);
            dictionaryTable.getColumnModel().getColumn(0).setMaxWidth(300);
        }
        dictionaryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (dictionaryTable.getSelectedRow() > -1) {
                    // print first column value from selected row
                    dictionaryTableAction();
                }
            }
        });

        dictionaryPanel.add(dictionaryTableScrollPanel, java.awt.BorderLayout.CENTER);

        dictionarySplit.setTopComponent(dictionaryPanel);

        definitionPanel.setLayout(new java.awt.BorderLayout());

        definitionPatternToolbar.setRollover(true);

        newDefinition.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        newDefinition.setToolTipText("Nuovo");
        newDefinition.setFocusable(false);
        newDefinition.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        newDefinition.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        newDefinition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newDefinitionActionPerformed(evt);
            }
        });
        definitionPatternToolbar.add(newDefinition);
        definitionPatternToolbar.add(jSeparator18);

        confirmDefinitionPattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_save.png"))); // NOI18N
        confirmDefinitionPattern.setToolTipText("Conferma");
        confirmDefinitionPattern.setFocusable(false);
        confirmDefinitionPattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        confirmDefinitionPattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        confirmDefinitionPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmDefinitionPatternActionPerformed(evt);
            }
        });
        definitionPatternToolbar.add(confirmDefinitionPattern);
        definitionPatternToolbar.add(jSeparator10);

        testDefinitionRegex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_swoosh.png"))); // NOI18N
        testDefinitionRegex.setText("Testa pattern");
        testDefinitionRegex.setToolTipText("Testa pattern");
        testDefinitionRegex.setFocusable(false);
        testDefinitionRegex.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        testDefinitionRegex.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testDefinitionRegex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testDefinitionRegexActionPerformed(evt);
            }
        });
        definitionPatternToolbar.add(testDefinitionRegex);
        definitionPatternToolbar.add(jSeparator11);

        testDefinitionMatch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/sockets.png"))); // NOI18N
        testDefinitionMatch.setText("Verifica match");
        testDefinitionMatch.setToolTipText("Verifica match");
        testDefinitionMatch.setFocusable(false);
        testDefinitionMatch.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        testDefinitionMatch.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testDefinitionMatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testDefinitionMatchActionPerformed(evt);
            }
        });
        definitionPatternToolbar.add(testDefinitionMatch);

        definitionPanel.add(definitionPatternToolbar, java.awt.BorderLayout.PAGE_START);

        definitionPatternEditPanel.setLayout(new java.awt.BorderLayout());

        definitionStatus.setText("...");
        definitionPatternEditPanel.add(definitionStatus, java.awt.BorderLayout.PAGE_END);

        jLabel18.setText("Nome");

        jLabel19.setText("Pattern");

        definitionPattern.setColumns(20);
        definitionPattern.setLineWrap(true);
        definitionPattern.setRows(5);
        definitionPattern.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                definitionPatternKeyTyped(evt);
            }
        });
        definitionPatternScrollPanel.setViewportView(definitionPattern);

        jLabel20.setText("Testo di prova");

        definitionPatternTest.setColumns(20);
        definitionPatternTest.setLineWrap(true);
        definitionPatternTest.setRows(5);
        definitionPatternTestScrollPanel.setViewportView(definitionPatternTest);

        javax.swing.GroupLayout definitionDefinitionPanelLayout = new javax.swing.GroupLayout(definitionDefinitionPanel);
        definitionDefinitionPanel.setLayout(definitionDefinitionPanelLayout);
        definitionDefinitionPanelLayout.setHorizontalGroup(
            definitionDefinitionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(definitionDefinitionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(definitionDefinitionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(definitionPatternScrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 763, Short.MAX_VALUE)
                    .addComponent(definitionName)
                    .addComponent(definitionPatternTestScrollPanel)
                    .addGroup(definitionDefinitionPanelLayout.createSequentialGroup()
                        .addGroup(definitionDefinitionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel18)
                            .addComponent(jLabel19)
                            .addComponent(jLabel20))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        definitionDefinitionPanelLayout.setVerticalGroup(
            definitionDefinitionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(definitionDefinitionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(definitionName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(definitionPatternScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(definitionPatternTestScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        definitionPatternEditPanel.add(definitionDefinitionPanel, java.awt.BorderLayout.CENTER);

        definitionPanel.add(definitionPatternEditPanel, java.awt.BorderLayout.CENTER);

        dictionarySplit.setRightComponent(definitionPanel);

        modelElements.addTab("Dizionario", dictionarySplit);

        segmentsSplit.setDividerLocation(380);
        segmentsSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        segmentPanel.setLayout(new java.awt.BorderLayout());

        segmentsSplitPanel.setDividerLocation(160);
        segmentsSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        segmentPatternsPanel.setLayout(new java.awt.BorderLayout());

        segmentPatternsToolbar.setRollover(true);

        segmentPatternDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        segmentPatternDelete.setToolTipText("Cancella");
        segmentPatternDelete.setFocusable(false);
        segmentPatternDelete.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmentPatternDelete.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentPatternDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentPatternDeleteActionPerformed(evt);
            }
        });
        segmentPatternsToolbar.add(segmentPatternDelete);
        segmentPatternsToolbar.add(jSeparator47);

        moveUp1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_fat_up.gif"))); // NOI18N
        moveUp1.setToolTipText("Muovi su");
        moveUp1.setFocusable(false);
        moveUp1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveUp1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveUp1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUp1ActionPerformed(evt);
            }
        });
        segmentPatternsToolbar.add(moveUp1);

        moveDown1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_fat_down.gif"))); // NOI18N
        moveDown1.setToolTipText("Muovi Giu");
        moveDown1.setFocusable(false);
        moveDown1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveDown1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveDown1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDown1ActionPerformed(evt);
            }
        });
        segmentPatternsToolbar.add(moveDown1);

        moveTop1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_dash_up.gif"))); // NOI18N
        moveTop1.setToolTipText("Muovi Inizio");
        moveTop1.setFocusable(false);
        moveTop1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveTop1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveTop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTop1ActionPerformed(evt);
            }
        });
        segmentPatternsToolbar.add(moveTop1);

        moveBottom1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_dash_down.gif"))); // NOI18N
        moveBottom1.setToolTipText("Muovi alla fine");
        moveBottom1.setFocusable(false);
        moveBottom1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveBottom1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveBottom1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveBottom1ActionPerformed(evt);
            }
        });
        segmentPatternsToolbar.add(moveBottom1);

        segmentPatternsPanel.add(segmentPatternsToolbar, java.awt.BorderLayout.PAGE_START);

        segmentPatternsTable.setAutoCreateRowSorter(true);
        segmentPatternsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Pattern"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        segmentPatternsTable.getTableHeader().setReorderingAllowed(false);
        segmentPatternsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                segmentPatternsTableMouseClicked(evt);
            }
        });
        segmentPatternsScrollPanel.setViewportView(segmentPatternsTable);
        if (segmentPatternsTable.getColumnModel().getColumnCount() > 0) {
            segmentPatternsTable.getColumnModel().getColumn(0).setMinWidth(0);
            segmentPatternsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            segmentPatternsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        }
        segmentPatternsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (segmentPatternsTable.getSelectedRow() > -1) {
                    // print first column value from selected row
                    segmentPatternsTableAction();
                }
            }
        });

        segmentPatternsPanel.add(segmentPatternsScrollPanel, java.awt.BorderLayout.CENTER);

        segmentsSplitPanel.setBottomComponent(segmentPatternsPanel);

        jLabel17.setText("Nome");

        jLabel21.setText("Default");

        jLabel24.setText("Multiple");

        jLabel25.setText("Classify");

        segmentName.setText("Nome del segmento");

        defaultYN.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Yes", "No" }));
        defaultYN.setSelectedIndex(1);
        defaultYN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultYNActionPerformed(evt);
            }
        });

        multipleYN.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Yes", "No" }));
        multipleYN.setSelectedIndex(1);
        multipleYN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                multipleYNActionPerformed(evt);
            }
        });

        classifyYN.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Yes", "No" }));
        classifyYN.setSelectedIndex(1);
        classifyYN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classifyYNActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout segmentConfigurationPanelLayout = new javax.swing.GroupLayout(segmentConfigurationPanel);
        segmentConfigurationPanel.setLayout(segmentConfigurationPanelLayout);
        segmentConfigurationPanelLayout.setHorizontalGroup(
            segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(segmentConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(multipleYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(defaultYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(segmentName, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(classifyYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(574, Short.MAX_VALUE))
        );
        segmentConfigurationPanelLayout.setVerticalGroup(
            segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(segmentConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(segmentName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(defaultYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(multipleYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(segmentConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(classifyYN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        segmentsSplitPanel.setLeftComponent(segmentConfigurationPanel);

        segmentPanel.add(segmentsSplitPanel, java.awt.BorderLayout.CENTER);

        segmentsSplit.setTopComponent(segmentPanel);

        segmentPatternPanel.setLayout(new java.awt.BorderLayout());

        segmentPatternToolbar.setRollover(true);

        segmentPatternAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        segmentPatternAdd.setToolTipText("Nuovo");
        segmentPatternAdd.setFocusable(false);
        segmentPatternAdd.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmentPatternAdd.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmentPatternAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmentPatternAddActionPerformed(evt);
            }
        });
        segmentPatternToolbar.add(segmentPatternAdd);
        segmentPatternToolbar.add(jSeparator19);

        confirmSegmentPattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_save.png"))); // NOI18N
        confirmSegmentPattern.setToolTipText("Conferma");
        confirmSegmentPattern.setFocusable(false);
        confirmSegmentPattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        confirmSegmentPattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        confirmSegmentPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmSegmentPatternActionPerformed(evt);
            }
        });
        segmentPatternToolbar.add(confirmSegmentPattern);
        segmentPatternToolbar.add(jSeparator14);

        testSegmentPattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_swoosh.png"))); // NOI18N
        testSegmentPattern.setText("Testa pattern");
        testSegmentPattern.setToolTipText("Testa pattern");
        testSegmentPattern.setFocusable(false);
        testSegmentPattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        testSegmentPattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testSegmentPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testSegmentPatternActionPerformed(evt);
            }
        });
        segmentPatternToolbar.add(testSegmentPattern);
        segmentPatternToolbar.add(jSeparator15);

        matchSegmentPattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/sockets.png"))); // NOI18N
        matchSegmentPattern.setText("Verifica match");
        matchSegmentPattern.setToolTipText("Verifica match");
        matchSegmentPattern.setFocusable(false);
        matchSegmentPattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        matchSegmentPattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        matchSegmentPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                matchSegmentPatternActionPerformed(evt);
            }
        });
        segmentPatternToolbar.add(matchSegmentPattern);

        segmentPatternPanel.add(segmentPatternToolbar, java.awt.BorderLayout.PAGE_START);

        segmentPatternDefinitionPanel.setLayout(new java.awt.BorderLayout());
        segmentPatternDefinitionPanel.add(segmentPatternStatus, java.awt.BorderLayout.PAGE_END);

        jLabel22.setText("Pattern");

        segmentPatternDefinition.setColumns(20);
        segmentPatternDefinition.setRows(5);
        segmentPatternDefinition.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                segmentPatternDefinitionFocusGained(evt);
            }
        });
        segmentPatternDefinitionScrollPanel.setViewportView(segmentPatternDefinition);

        jLabel23.setText("Testo di prova");

        segmentPatternTestArea.setColumns(20);
        segmentPatternTestArea.setRows(5);
        segmentPatternScrollPanelTestArea.setViewportView(segmentPatternTestArea);

        javax.swing.GroupLayout segmentPatternConfigurationPanelLayout = new javax.swing.GroupLayout(segmentPatternConfigurationPanel);
        segmentPatternConfigurationPanel.setLayout(segmentPatternConfigurationPanelLayout);
        segmentPatternConfigurationPanelLayout.setHorizontalGroup(
            segmentPatternConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(segmentPatternConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(segmentPatternConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(segmentPatternScrollPanelTestArea, javax.swing.GroupLayout.DEFAULT_SIZE, 763, Short.MAX_VALUE)
                    .addComponent(segmentPatternDefinitionScrollPanel)
                    .addGroup(segmentPatternConfigurationPanelLayout.createSequentialGroup()
                        .addGroup(segmentPatternConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel22)
                            .addComponent(jLabel23))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        segmentPatternConfigurationPanelLayout.setVerticalGroup(
            segmentPatternConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(segmentPatternConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(segmentPatternDefinitionScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(segmentPatternScrollPanelTestArea, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        segmentPatternDefinitionPanel.add(segmentPatternConfigurationPanel, java.awt.BorderLayout.CENTER);

        segmentPatternPanel.add(segmentPatternDefinitionPanel, java.awt.BorderLayout.CENTER);

        segmentsSplit.setRightComponent(segmentPatternPanel);

        modelElements.addTab("Segmento", segmentsSplit);

        tablePanel.setLayout(new java.awt.BorderLayout());

        tableToolbar.setRollover(true);

        tableImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_in.png"))); // NOI18N
        tableImport.setToolTipText("Importa");
        tableImport.setFocusable(false);
        tableImport.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tableImport.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        tableImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableImportActionPerformed(evt);
            }
        });
        tableToolbar.add(tableImport);

        tableExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_out.png"))); // NOI18N
        tableExport.setToolTipText("Esporta");
        tableExport.setFocusable(false);
        tableExport.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tableExport.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        tableExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableExportActionPerformed(evt);
            }
        });
        tableToolbar.add(tableExport);
        tableToolbar.add(jSeparator44);

        fromDataProvider.setText(" Tabella da dataprovider");
        fromDataProvider.setFocusable(false);
        fromDataProvider.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        fromDataProvider.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        fromDataProvider.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromDataProviderActionPerformed(evt);
            }
        });
        tableToolbar.add(fromDataProvider);
        fromDataProvider.getAccessibleContext().setAccessibleDescription("");

        tableToolbar.add(jSeparator13);

        tableAddRecord.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        tableAddRecord.setToolTipText("Aggiungi");
        tableAddRecord.setFocusable(false);
        tableAddRecord.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tableAddRecord.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        tableAddRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableAddRecordActionPerformed(evt);
            }
        });
        tableToolbar.add(tableAddRecord);

        tableDeleteRecord.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        tableDeleteRecord.setToolTipText("Cancella");
        tableDeleteRecord.setFocusable(false);
        tableDeleteRecord.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        tableDeleteRecord.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        tableDeleteRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableDeleteRecordActionPerformed(evt);
            }
        });
        tableToolbar.add(tableDeleteRecord);
        tableToolbar.add(jSeparator12);

        removeTableFilter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeTableFilter.setToolTipText("Rimuovi filtri");
        removeTableFilter.setFocusable(false);
        removeTableFilter.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeTableFilter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        removeTableFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeTableFilterActionPerformed(evt);
            }
        });
        tableToolbar.add(removeTableFilter);

        jLabel26.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel26.setText("Cerca ");
        tableToolbar.add(jLabel26);

        searchTable.setColumns(10);
        searchTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTableActionPerformed(evt);
            }
        });
        searchTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchTableKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchTableKeyTyped(evt);
            }
        });
        tableToolbar.add(searchTable);

        tablePanel.add(tableToolbar, java.awt.BorderLayout.PAGE_START);

        table.setAutoCreateRowSorter(true);
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Record"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tableKeyTyped(evt);
            }
        });
        tableScrollpanel.setViewportView(table);
        Action actionTable = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                TableCellListener tcl = (TableCellListener)e.getSource();

                TableTreeNode node = (TableTreeNode) me.getCurrentNode();
                if (node != null) {
                    node.deleteRecord((String) tcl.getOldValue());
                    node.addRecord((String) tcl.getNewValue());
                }
            }
        };

        TableCellListener tcl = new TableCellListener(table, actionTable);

        tablePanel.add(tableScrollpanel, java.awt.BorderLayout.CENTER);

        modelElements.addTab("Tabella", tablePanel);

        captureSplit.setDividerLocation(380);
        captureSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        capturePanel.setLayout(new java.awt.BorderLayout());

        captureConfigurationSplitPanel.setDividerLocation(180);
        captureConfigurationSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        capturePatternsPanel.setLayout(new java.awt.BorderLayout());

        capturePatternsToolbar.setRollover(true);

        deleteCapturePattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        deleteCapturePattern.setToolTipText("Cancella");
        deleteCapturePattern.setFocusable(false);
        deleteCapturePattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        deleteCapturePattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        deleteCapturePattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCapturePatternActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(deleteCapturePattern);
        capturePatternsToolbar.add(jSeparator30);

        patternsImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_in.png"))); // NOI18N
        patternsImport.setToolTipText("Importa");
        patternsImport.setFocusable(false);
        patternsImport.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        patternsImport.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        patternsImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                patternsImportActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(patternsImport);

        patternsExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_out.png"))); // NOI18N
        patternsExport.setToolTipText("Esporta");
        patternsExport.setFocusable(false);
        patternsExport.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        patternsExport.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        patternsExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                patternsExportActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(patternsExport);
        capturePatternsToolbar.add(jSeparator46);

        moveUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_fat_up.gif"))); // NOI18N
        moveUp.setToolTipText("Muovi su");
        moveUp.setFocusable(false);
        moveUp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveUp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(moveUp);

        moveDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_fat_down.gif"))); // NOI18N
        moveDown.setToolTipText("Muovi Giu");
        moveDown.setFocusable(false);
        moveDown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveDown.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(moveDown);

        moveTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_dash_up.gif"))); // NOI18N
        moveTop.setToolTipText("Muovi Inizio");
        moveTop.setFocusable(false);
        moveTop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveTop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTopActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(moveTop);

        moveBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons/arrow_dash_down.gif"))); // NOI18N
        moveBottom.setToolTipText("Muovi alla fine");
        moveBottom.setFocusable(false);
        moveBottom.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moveBottom.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moveBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveBottomActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(moveBottom);
        capturePatternsToolbar.add(jSeparator31);

        removeSearchFilter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeSearchFilter.setToolTipText("Rimuovi filtri");
        removeSearchFilter.setFocusable(false);
        removeSearchFilter.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeSearchFilter.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeSearchFilter.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removeSearchFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSearchFilterActionPerformed(evt);
            }
        });
        capturePatternsToolbar.add(removeSearchFilter);
        capturePatternsToolbar.add(jSeparator37);

        jLabel35.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel35.setText("Cerca ");
        capturePatternsToolbar.add(jLabel35);

        searchNormalization.setColumns(10);
        searchNormalization.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchNormalizationActionPerformed(evt);
            }
        });
        searchNormalization.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchNormalizationKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchNormalizationKeyTyped(evt);
            }
        });
        capturePatternsToolbar.add(searchNormalization);

        capturePatternsPanel.add(capturePatternsToolbar, java.awt.BorderLayout.PAGE_START);

        capturePatternTable.setAutoCreateRowSorter(true);
        capturePatternTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Pattern", "#", "Norm"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        capturePatternTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                capturePatternTableMouseClicked(evt);
            }
        });
        capturePatternsScrollPanel.setViewportView(capturePatternTable);
        if (capturePatternTable.getColumnModel().getColumnCount() > 0) {
            capturePatternTable.getColumnModel().getColumn(0).setMinWidth(0);
            capturePatternTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            capturePatternTable.getColumnModel().getColumn(0).setMaxWidth(0);
            capturePatternTable.getColumnModel().getColumn(2).setMinWidth(100);
            capturePatternTable.getColumnModel().getColumn(2).setPreferredWidth(100);
            capturePatternTable.getColumnModel().getColumn(2).setMaxWidth(100);
            capturePatternTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        }
        capturePatternTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (capturePatternTable.getSelectedRow() > -1) {
                    // print first column value from selected row
                    capturePatternTableAction();
                }
            }
        });

        capturePatternsPanel.add(capturePatternsScrollPanel, java.awt.BorderLayout.CENTER);

        captureConfigurationSplitPanel.setBottomComponent(capturePatternsPanel);

        captureConfigurationSuperPanel.setLayout(new java.awt.BorderLayout());

        jToolBar9.setRollover(true);

        openSegmentRelationshipPanel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_link.png"))); // NOI18N
        openSegmentRelationshipPanel.setText("Segmenti");
        openSegmentRelationshipPanel.setToolTipText("Segmenti");
        openSegmentRelationshipPanel.setEnabled(false);
        openSegmentRelationshipPanel.setFocusable(false);
        openSegmentRelationshipPanel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        openSegmentRelationshipPanel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        openSegmentRelationshipPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSegmentRelationshipPanelActionPerformed(evt);
            }
        });
        jToolBar9.add(openSegmentRelationshipPanel);
        jToolBar9.add(jSeparator34);

        classifyPattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/sitemap.png"))); // NOI18N
        classifyPattern.setToolTipText("Classificazione");
        classifyPattern.setFocusable(false);
        classifyPattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        classifyPattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        classifyPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classifyPatternActionPerformed(evt);
            }
        });
        jToolBar9.add(classifyPattern);

        captureConfigurationSuperPanel.add(jToolBar9, java.awt.BorderLayout.NORTH);

        jLabel27.setText("Nome");

        jLabel28.setText("Type");

        jLabel29.setText("Format");

        captureName.setText("Nome della cattura");

        captureType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "text", "date", "integer", "real", "number", "boolean" }));
        captureType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureTypeActionPerformed(evt);
            }
        });

        captureFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureFormatActionPerformed(evt);
            }
        });

        jLabel30.setText("Scope");

        captureTarget.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "local", "sentence" }));
        captureTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureTargetActionPerformed(evt);
            }
        });

        tempCapture.setText("Temporanea");
        tempCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tempCaptureActionPerformed(evt);
            }
        });

        endTimeInterval.setText("Fine periodo");
        endTimeInterval.setEnabled(false);
        endTimeInterval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endTimeIntervalActionPerformed(evt);
            }
        });

        startTimeInterval.setText("Inizio periodo");
        startTimeInterval.setEnabled(false);
        startTimeInterval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startTimeIntervalActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureConfigurationPanelLayout = new javax.swing.GroupLayout(captureConfigurationPanel);
        captureConfigurationPanel.setLayout(captureConfigurationPanelLayout);
        captureConfigurationPanelLayout.setHorizontalGroup(
            captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel30, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel27, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel29, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(captureType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(captureFormat)
                    .addGroup(captureConfigurationPanelLayout.createSequentialGroup()
                        .addComponent(captureName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(104, 104, 104))
                    .addGroup(captureConfigurationPanelLayout.createSequentialGroup()
                        .addComponent(captureTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(tempCapture)
                        .addGap(18, 18, 18)
                        .addComponent(startTimeInterval)
                        .addGap(18, 18, 18)
                        .addComponent(endTimeInterval)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        captureConfigurationPanelLayout.setVerticalGroup(
            captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(captureName))
                .addGap(9, 9, 9)
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(captureType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(captureFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(captureConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30)
                    .addComponent(captureTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tempCapture)
                    .addComponent(endTimeInterval)
                    .addComponent(startTimeInterval))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        captureConfigurationSuperPanel.add(captureConfigurationPanel, java.awt.BorderLayout.CENTER);

        captureConfigurationSplitPanel.setTopComponent(captureConfigurationSuperPanel);

        capturePanel.add(captureConfigurationSplitPanel, java.awt.BorderLayout.CENTER);

        captureSplit.setTopComponent(capturePanel);

        capturePatternPanel.setLayout(new java.awt.BorderLayout());

        capturePatternToolbar.setRollover(true);

        addCapturePattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        addCapturePattern.setToolTipText("Nuovo");
        addCapturePattern.setFocusable(false);
        addCapturePattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        addCapturePattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        addCapturePattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCapturePatternActionPerformed(evt);
            }
        });
        capturePatternToolbar.add(addCapturePattern);
        capturePatternToolbar.add(jSeparator20);

        confirmCapturePattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_save.png"))); // NOI18N
        confirmCapturePattern.setToolTipText("Conferma");
        confirmCapturePattern.setFocusable(false);
        confirmCapturePattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        confirmCapturePattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        confirmCapturePattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmCapturePatternActionPerformed(evt);
            }
        });
        capturePatternToolbar.add(confirmCapturePattern);
        capturePatternToolbar.add(jSeparator16);

        testCapturePattern.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_swoosh.png"))); // NOI18N
        testCapturePattern.setText("Testa pattern");
        testCapturePattern.setToolTipText("Testa pattern");
        testCapturePattern.setFocusable(false);
        testCapturePattern.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        testCapturePattern.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testCapturePattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testCapturePatternActionPerformed(evt);
            }
        });
        capturePatternToolbar.add(testCapturePattern);
        capturePatternToolbar.add(jSeparator17);

        testCaptureMatch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/sockets.png"))); // NOI18N
        testCaptureMatch.setText("Verifica match");
        testCaptureMatch.setToolTipText("Verifica match");
        testCaptureMatch.setFocusable(false);
        testCaptureMatch.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        testCaptureMatch.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testCaptureMatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testCaptureMatchActionPerformed(evt);
            }
        });
        capturePatternToolbar.add(testCaptureMatch);

        capturePatternPanel.add(capturePatternToolbar, java.awt.BorderLayout.PAGE_START);

        capturePatternEditPanel.setLayout(new java.awt.BorderLayout());
        capturePatternEditPanel.add(capturePatternStatus, java.awt.BorderLayout.PAGE_END);

        jLabel31.setText("Pattern");

        capturePatternDefinition.setColumns(20);
        capturePatternDefinition.setRows(5);
        capturePatternDefinition.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                capturePatternDefinitionFocusGained(evt);
            }
        });
        capturePatternContentScrollPanel.setViewportView(capturePatternDefinition);

        jLabel32.setText("Testo di prova");

        capturePatternTestText.setColumns(20);
        capturePatternTestText.setRows(5);
        jScrollPane35.setViewportView(capturePatternTestText);

        jLabel33.setText("Cattura");

        capturePatternSpinner.setModel(new javax.swing.SpinnerNumberModel());
        capturePatternSpinner.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                capturePatternSpinnerFocusGained(evt);
            }
        });

        jLabel34.setText("Nomralizzata");

        capturePatternFixedValue.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                capturePatternFixedValueFocusGained(evt);
            }
        });

        javax.swing.GroupLayout capturePatternContentTableLayout = new javax.swing.GroupLayout(capturePatternContentTable);
        capturePatternContentTable.setLayout(capturePatternContentTableLayout);
        capturePatternContentTableLayout.setHorizontalGroup(
            capturePatternContentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturePatternContentTableLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(capturePatternContentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(capturePatternContentScrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 763, Short.MAX_VALUE)
                    .addComponent(jScrollPane35, javax.swing.GroupLayout.DEFAULT_SIZE, 763, Short.MAX_VALUE)
                    .addGroup(capturePatternContentTableLayout.createSequentialGroup()
                        .addGroup(capturePatternContentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel32))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(capturePatternContentTableLayout.createSequentialGroup()
                        .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(capturePatternSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(capturePatternFixedValue)))
                .addContainerGap())
        );
        capturePatternContentTableLayout.setVerticalGroup(
            capturePatternContentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturePatternContentTableLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(capturePatternContentScrollPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(capturePatternContentTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel33)
                    .addComponent(capturePatternSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34)
                    .addComponent(capturePatternFixedValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel32)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane35, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        capturePatternEditPanel.add(capturePatternContentTable, java.awt.BorderLayout.CENTER);

        capturePatternPanel.add(capturePatternEditPanel, java.awt.BorderLayout.CENTER);

        captureSplit.setRightComponent(capturePatternPanel);

        modelElements.addTab("Catture", captureSplit);

        modeEditorInfo.setLayout(new java.awt.BorderLayout());

        help.setEditable(false);
        help.setContentType("text/html"); // NOI18N
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n<h1>Modello</h1>\nUn modello è un insieme di regole che permettono di segmentare (dividere) un documento ed estrarre informazioni.<br>\nLe regole sono espresse attraverso <i>espressioni regolari</i> che costituiscono i così detti pattern <i>pattern</i>.<br>\nIll pattern matching è l'azione di controllo della presenza di un certo <i>pattern</i> all'interno di una stringa di caratter<br>\nUn modello SEM si trovano <b>dizionari</b>, <b>tabelle</b>, <b>segmenti</b> e <b>catture</b>.<br>\n<h2>Dizionario</h2>\nIl <i>dizionario</i> è una collezione di definizioni.<br>\nUna <i>definizione</i> è costituita da un <i>nome</i> e da un <i>pattern</i>. Una definizione può essere utilizzata come oggetto atomico nella definizione \ndi altri pattern, rendendo così la definizione dei pattern stessi, ricorsiva<br>\nUna <i>definizione</i> può essere riferita con la sintassi <i><b>#nome_definizione </b></i>.\nIl dizionario è unico per tutto il modello.\n<h2>Tabelle</h2>\nOgni <i>tabella</i>, riferibile tramite <i>nome</i> è una lista finita di valori fissi o <i>espressioni regolari</i> che caratterizzano un oggetto.<br>\nUna <i>tabella</i> può essere costituita, per esempio, un elenco di nomi propri di persona che possono essere utlizzati per individuare il nome scritto in una particolare\nzona del documento. Avere queste liste, che possono essere caricate anche da file .csv, permette di andare a ricercarcare particolari elementi nel testo, quando questi \npossono assumere un numero finito e ben identificabili di valori.<br>Come le definizioni del <i>dizionario</i> le <i>tabelle</i> possono essere utilizzate come elemento \natomico nella definizione di altri pattern all'interno di <i>segmenti</i> o <i>catture</i>.\n<br>\nUna <i>tabella</i> può essere riferita con la sintassi <i><b>#nome_tabella </b></i>.\n<h2>Segmenti</h2>\nI <i>segmenti</i> sono aree in cui il documento deve essere divisio. Normalmente identificano zone con contenuti precisi e caratterizzati (gli articoli di una legge, le sezioni di un cv).<br> \nUn documento, in fase di analisi viene divisio in in frasi dette <i>sentenze</i>. SEM analizza le sentenze sequenzialmente e quando una sentenza soddisfa almeno un <i>pattern</i>\ndi definizione di un segmento, divide il documento creando il segmento. All'interno del segmento possono essere attivate le <i>catture</i> per estrarre le informazioni.<br>\nUn modello può essere configurato per riconoscere da 1 a n segmenti. Un <i>segmento</i> può essere marcato come segmento di <i>default</i>, e può essere ricercato una sola volta in \nun documento o può essere <i>multiplo</i> e quindi identificato più volte. In un CV ad esempio le <i>informazioni anagrafiche</i> sono normalmente racchiuse in unico segmento, mentre le\n<i>esperienze lavorative</i> si ripetono tante volte quante le esperienze lavorative di una persona.<br>\nUn segmento può essere marcato come un segmento sul cui testo può agire il <i>classificatore bayesiano</i>.\n<h3>Catture</h3>\nLe <i>catture</i> sono una collezione di <i>pattern</i> che, in caso di match positivo permettono di estrarre informazioni da una o più sentenze.<br>\nQuando una <i>cattura</i> viene definita deve essere indicato anche la posizione del sotto-pattern che deve essere estratto.<br>\nLe <i>catture</i> possono agire a livello di intero testo di segmento oppure a livello di singola sentenza. Le <i>catture</i> possono agire anche sul risultato di un'altra cattura, attraverso \nuna definizione gerarchica.<br>\nUna <i>cattura</i> può essere configurata anche per classificare un segmento in un particolare sottoramo di classificazione.<br>\nIl valore estratto da una <i>cattura</i> è tipizzato e può essere normalizzato con valori fissi o attraverso una sintassi propria del componente String Formatter di java.\n<br>Informazione disponibili all'indirizzo <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html\">https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html</a>.\n\n  </body>\r\n</html>\r\n");
        help.setCaretPosition(0);
        jScrollPane21.setViewportView(help);

        modeEditorInfo.add(jScrollPane21, java.awt.BorderLayout.CENTER);

        modelElements.addTab("Model Editor", modeEditorInfo);

        dataproviderSplit.setDividerLocation(420);
        dataproviderSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        dataproviderPanel.setLayout(new java.awt.BorderLayout());

        dataproviderSplitPanel.setDividerLocation(200);
        dataproviderSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        dpFieldsPanel.setLayout(new java.awt.BorderLayout());

        dpFieldsToolbar.setRollover(true);

        deleteDpFields.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        deleteDpFields.setToolTipText("Cancella");
        deleteDpFields.setFocusable(false);
        deleteDpFields.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        deleteDpFields.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        deleteDpFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDpFieldsActionPerformed(evt);
            }
        });
        dpFieldsToolbar.add(deleteDpFields);
        dpFieldsToolbar.add(jSeparator38);

        importDpFields.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_wrench.png"))); // NOI18N
        importDpFields.setToolTipText("Leggi intestazioni file");
        importDpFields.setFocusable(false);
        importDpFields.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        importDpFields.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        importDpFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importDpFieldsActionPerformed(evt);
            }
        });
        dpFieldsToolbar.add(importDpFields);
        dpFieldsToolbar.add(jSeparator40);

        burnToStorage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/drive_burn.png"))); // NOI18N
        burnToStorage.setToolTipText("Carica in SEM Storage");
        burnToStorage.setFocusable(false);
        burnToStorage.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        burnToStorage.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        burnToStorage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                burnToStorageActionPerformed(evt);
            }
        });
        dpFieldsToolbar.add(burnToStorage);

        dpFieldsPanel.add(dpFieldsToolbar, java.awt.BorderLayout.PAGE_START);

        dpFieldsTable.setAutoCreateRowSorter(true);
        dpFieldsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Campo", "Tipo", "Posizione", "Tabella"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dpFieldsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dpFieldsTableMouseClicked(evt);
            }
        });
        dbFieldsScrollPanel.setViewportView(dpFieldsTable);
        if (dpFieldsTable.getColumnModel().getColumnCount() > 0) {
            dpFieldsTable.getColumnModel().getColumn(0).setMinWidth(0);
            dpFieldsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            dpFieldsTable.getColumnModel().getColumn(0).setMaxWidth(0);
            dpFieldsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
            dpFieldsTable.getColumnModel().getColumn(3).setMinWidth(100);
            dpFieldsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            dpFieldsTable.getColumnModel().getColumn(3).setMaxWidth(100);
        }

        dpFieldsPanel.add(dbFieldsScrollPanel, java.awt.BorderLayout.CENTER);

        dataproviderSplitPanel.setBottomComponent(dpFieldsPanel);

        dpDefinition.setLayout(new java.awt.BorderLayout());

        jLabel47.setText("Nome");

        jLabel48.setText("Type");

        dpName.setText("Nome del dataprovider");

        dpFileName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpFileNameActionPerformed(evt);
            }
        });
        dpFileName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dpFileNameKeyReleased(evt);
            }
        });

        jLabel50.setText("Delimitatore");

        dpDelimitatore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpDelimitatoreActionPerformed(evt);
            }
        });
        dpDelimitatore.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dpDelimitatoreKeyReleased(evt);
            }
        });

        dpType.setModel(new javax.swing.DefaultComboBoxModel<>(DataProviderConfiguration.SOURCE_TYPE));
        dpType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpTypeActionPerformed(evt);
            }
        });

        jLabel52.setText("Carattere di escaping");

        dpEscape.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpEscapeActionPerformed(evt);
            }
        });
        dpEscape.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dpEscapeKeyReleased(evt);
            }
        });

        jLabel53.setText("Carattere di quoting");

        dpQuote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpQuoteActionPerformed(evt);
            }
        });
        dpQuote.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dpQuoteKeyReleased(evt);
            }
        });

        jLabel56.setText("Separatore di righe");

        dpLineSep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpLineSepActionPerformed(evt);
            }
        });
        dpLineSep.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dpLineSepKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                dpLineSepKeyTyped(evt);
            }
        });

        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/doc_break.png"))); // NOI18N
        jButton7.setText("File CSV");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jCheckBox1.setText("Salta prima riga");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dpConfigurationPanelLayout = new javax.swing.GroupLayout(dpConfigurationPanel);
        dpConfigurationPanel.setLayout(dpConfigurationPanelLayout);
        dpConfigurationPanelLayout.setHorizontalGroup(
            dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dpConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel47, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel48, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel50, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel53, javax.swing.GroupLayout.PREFERRED_SIZE, 151, Short.MAX_VALUE))
                    .addComponent(jButton7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dpConfigurationPanelLayout.createSequentialGroup()
                        .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dpName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(dpType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dpDelimitatore, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dpQuote, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(dpConfigurationPanelLayout.createSequentialGroup()
                                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel52, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                                    .addComponent(jLabel56, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(dpLineSep, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(dpEscape, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(dpFileName))
                .addGap(178, 178, 178))
        );
        dpConfigurationPanelLayout.setVerticalGroup(
            dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dpConfigurationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel47)
                    .addComponent(dpName))
                .addGap(9, 9, 9)
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(dpType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dpFileName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(dpDelimitatore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel52)
                    .addComponent(dpEscape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel50))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dpConfigurationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel53)
                    .addComponent(dpQuote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel56)
                    .addComponent(dpLineSep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dpDefinition.add(dpConfigurationPanel, java.awt.BorderLayout.CENTER);

        dataproviderSplitPanel.setTopComponent(dpDefinition);

        dataproviderPanel.add(dataproviderSplitPanel, java.awt.BorderLayout.CENTER);

        dataproviderSplit.setTopComponent(dataproviderPanel);

        dataproviderFieldsPanel.setLayout(new java.awt.BorderLayout());

        capturePatternToolbar1.setRollover(true);

        addDpField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        addDpField.setText("Nuovo");
        addDpField.setToolTipText("Nuovo");
        addDpField.setFocusable(false);
        addDpField.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        addDpField.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        addDpField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDpFieldActionPerformed(evt);
            }
        });
        capturePatternToolbar1.add(addDpField);
        capturePatternToolbar1.add(jSeparator43);

        confirmDpField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_save.png"))); // NOI18N
        confirmDpField.setText(" Salva");
        confirmDpField.setToolTipText("Conferma");
        confirmDpField.setFocusable(false);
        confirmDpField.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        confirmDpField.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        confirmDpField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmDpFieldActionPerformed(evt);
            }
        });
        capturePatternToolbar1.add(confirmDpField);

        dataproviderFieldsPanel.add(capturePatternToolbar1, java.awt.BorderLayout.PAGE_START);

        capturePatternEditPanel1.setLayout(new java.awt.BorderLayout());

        dpFieldId.setText("jLabel14");
        capturePatternEditPanel1.add(dpFieldId, java.awt.BorderLayout.SOUTH);
        capturePatternEditPanel1.add(capturePatternStatus1, java.awt.BorderLayout.PAGE_END);

        jLabel51.setText("Campo");

        jLabel54.setText("Tipo");

        dpFieldName.setEnabled(false);

        dpFieldType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "text", "date", "integer", "real", "number", "boolean" }));
        dpFieldType.setEnabled(false);
        dpFieldType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpFieldTypeActionPerformed(evt);
            }
        });

        jLabel55.setText("Posizione");

        dpFieldPosition.setEnabled(false);

        jLabel57.setText("Collega a tabella");

        dpFieldTableRelationship.setEnabled(false);
        dpFieldTableRelationship.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpFieldTableRelationshipActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout capturePatternContentTable1Layout = new javax.swing.GroupLayout(capturePatternContentTable1);
        capturePatternContentTable1.setLayout(capturePatternContentTable1Layout);
        capturePatternContentTable1Layout.setHorizontalGroup(
            capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturePatternContentTable1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel51, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel54, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel55, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                    .addComponent(jLabel57, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dpFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dpFieldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dpFieldPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dpFieldTableRelationship, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(225, 225, 225))
        );
        capturePatternContentTable1Layout.setVerticalGroup(
            capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturePatternContentTable1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel51)
                    .addComponent(dpFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel54)
                    .addComponent(dpFieldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel55)
                    .addComponent(dpFieldPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(capturePatternContentTable1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dpFieldTableRelationship, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel57))
                .addContainerGap(723, Short.MAX_VALUE))
        );

        capturePatternEditPanel1.add(capturePatternContentTable1, java.awt.BorderLayout.CENTER);

        dataproviderFieldsPanel.add(capturePatternEditPanel1, java.awt.BorderLayout.CENTER);

        dataproviderSplit.setBottomComponent(dataproviderFieldsPanel);

        modelElements.addTab("Data provider", dataproviderSplit);

        dataproviderRelationship.setDividerLocation(390);
        dataproviderRelationship.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        dprPanel.setLayout(new java.awt.BorderLayout());

        captureConfigurationSplitPanel3.setDividerLocation(120);
        captureConfigurationSplitPanel3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        capturePatternsPanel3.setLayout(new java.awt.BorderLayout());

        dprTable.setAutoCreateRowSorter(true);
        dprTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Field", "Cattura", "Chiave", "Importa"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dprTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dprTableMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                dprTableMouseEntered(evt);
            }
        });
        capturePatternsScrollPanel3.setViewportView(dprTable);
        if (dprTable.getColumnModel().getColumnCount() > 0) {
            dprTable.getColumnModel().getColumn(2).setMinWidth(100);
            dprTable.getColumnModel().getColumn(2).setPreferredWidth(100);
            dprTable.getColumnModel().getColumn(2).setMaxWidth(100);
            dprTable.getColumnModel().getColumn(3).setMinWidth(100);
            dprTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            dprTable.getColumnModel().getColumn(3).setMaxWidth(100);
        }

        capturePatternsPanel3.add(capturePatternsScrollPanel3, java.awt.BorderLayout.CENTER);

        captureConfigurationSplitPanel3.setBottomComponent(capturePatternsPanel3);

        captureConfigurationSuperPanel3.setLayout(new java.awt.BorderLayout());

        jLabel64.setText("Nome");

        dprName.setText("Nome della relazione");

        dprPriority.setText("Prevalenze sulle catture");
        dprPriority.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dprPriorityActionPerformed(evt);
            }
        });

        jLabel65.setText("Segmento");

        dprSegment.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dprSegmentItemStateChanged(evt);
            }
        });
        dprSegment.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dprSegmentMouseClicked(evt);
            }
        });
        dprSegment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dprSegmentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureConfigurationPanel5Layout = new javax.swing.GroupLayout(captureConfigurationPanel5);
        captureConfigurationPanel5.setLayout(captureConfigurationPanel5Layout);
        captureConfigurationPanel5Layout.setHorizontalGroup(
            captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureConfigurationPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel65, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(jLabel64, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(captureConfigurationPanel5Layout.createSequentialGroup()
                        .addComponent(dprName, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(dprPriority))
                    .addComponent(dprSegment, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(207, Short.MAX_VALUE))
        );
        captureConfigurationPanel5Layout.setVerticalGroup(
            captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureConfigurationPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(dprPriority)
                    .addComponent(dprName)
                    .addComponent(jLabel64))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(captureConfigurationPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel65)
                    .addComponent(dprSegment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(60, Short.MAX_VALUE))
        );

        captureConfigurationSuperPanel3.add(captureConfigurationPanel5, java.awt.BorderLayout.CENTER);

        captureConfigurationSplitPanel3.setTopComponent(captureConfigurationSuperPanel3);

        dprPanel.add(captureConfigurationSplitPanel3, java.awt.BorderLayout.CENTER);

        dataproviderRelationship.setTopComponent(dprPanel);

        dprRelationshipPanel.setLayout(new java.awt.BorderLayout());

        capturePatternToolbar3.setRollover(true);

        dprSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_save.png"))); // NOI18N
        dprSave.setText("Salva mappatura");
        dprSave.setToolTipText("Conferma");
        dprSave.setEnabled(false);
        dprSave.setFocusable(false);
        dprSave.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        dprSave.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        dprSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dprSaveActionPerformed(evt);
            }
        });
        capturePatternToolbar3.add(dprSave);

        dprRelationshipPanel.add(capturePatternToolbar3, java.awt.BorderLayout.PAGE_START);

        capturePatternEditPanel3.setLayout(new java.awt.BorderLayout());
        capturePatternEditPanel3.add(capturePatternStatus3, java.awt.BorderLayout.PAGE_END);

        jLabel6.setText("Field:");

        dprFieldName.setEnabled(false);

        jLabel39.setText("Cattura");

        dprCapture.setEnabled(false);
        dprCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dprCaptureActionPerformed(evt);
            }
        });

        dprEnrich.setText("Importa");
        dprEnrich.setEnabled(false);

        dprKey.setText("Chiave");
        dprKey.setEnabled(false);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel7Layout.createSequentialGroup()
                            .addComponent(jLabel39, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(dprCapture, 0, 150, Short.MAX_VALUE))
                        .addComponent(dprEnrich, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(dprKey, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(dprFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(417, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(dprFieldName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dprCapture, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel39))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(dprEnrich)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(dprKey)
                .addContainerGap(744, Short.MAX_VALUE))
        );

        capturePatternEditPanel3.add(jPanel7, java.awt.BorderLayout.CENTER);

        dprRelationshipPanel.add(capturePatternEditPanel3, java.awt.BorderLayout.CENTER);

        dataproviderRelationship.setRightComponent(dprRelationshipPanel);

        modelElements.addTab("Relazioni Data Provider", dataproviderRelationship);

        modelEditor.setRightComponent(modelElements);

        modelTreeSplitPanel.setLayout(new java.awt.BorderLayout());

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Modello");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Dizionario");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Catture");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Segmenti");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Tabelle");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Data providers");
        treeNode1.add(treeNode2);
        modelTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        modelTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                modelTreeMouseClicked(evt);
            }
        });
        modelTreeScrollPanel.setViewportView(modelTree);

        modelTreeSplitPanel.add(modelTreeScrollPanel, java.awt.BorderLayout.CENTER);

        jToolBar1.setRollover(true);

        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/application_cascade.png"))); // NOI18N
        jButton8.setToolTipText("UnDock");
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton8);
        jToolBar1.add(jSeparator29);

        saveModel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/disk.png"))); // NOI18N
        saveModel.setText("Salva");
        saveModel.setToolTipText("Salva");
        saveModel.setEnabled(false);
        saveModel.setFocusable(false);
        saveModel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        saveModel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        saveModel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveModelActionPerformed(evt);
            }
        });
        jToolBar1.add(saveModel);
        jToolBar1.add(jSeparator21);

        compileModel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/doc_convert.png"))); // NOI18N
        compileModel.setText("Compila");
        compileModel.setToolTipText("Compila");
        compileModel.setFocusable(false);
        compileModel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        compileModel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        compileModel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compileModelActionPerformed(evt);
            }
        });
        jToolBar1.add(compileModel);
        jToolBar1.add(jSeparator23);

        segmenta1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/text_indent.png"))); // NOI18N
        segmenta1.setText("Segmenta");
        segmenta1.setToolTipText("Segmenta");
        segmenta1.setFocusable(false);
        segmenta1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        segmenta1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        segmenta1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                segmenta1ActionPerformed(evt);
            }
        });
        jToolBar1.add(segmenta1);
        jToolBar1.add(jSeparator28);

        resetModel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_undo.png"))); // NOI18N
        resetModel.setText("Reset");
        resetModel.setToolTipText("Reset");
        resetModel.setFocusable(false);
        resetModel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        resetModel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        resetModel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetModelActionPerformed(evt);
            }
        });
        jToolBar1.add(resetModel);

        modelTreeSplitPanel.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        modelEditor.setLeftComponent(modelTreeSplitPanel);

        modelEditorContainer.setLeftComponent(modelEditor);

        filesDx1.setLayout(new java.awt.BorderLayout());

        jPanel12.setLayout(new java.awt.BorderLayout());

        filesPanelHtml1.setEditable(false);
        jScrollPane27.setViewportView(filesPanelHtml1);

        jPanel12.add(jScrollPane27, java.awt.BorderLayout.CENTER);

        jTabbedPane5.addTab("HTML", jPanel12);

        filesPanelHtmlFormatted1.setEditable(false);
        filesPanelHtmlFormatted1.setContentType("text/html"); // NOI18N
        jScrollPane28.setViewportView(filesPanelHtmlFormatted1);

        jTabbedPane5.addTab("Testo Formattato", jScrollPane28);

        fileText1.setColumns(20);
        fileText1.setRows(10);
        jScrollPane26.setViewportView(fileText1);

        jTabbedPane5.addTab("Testo Documento", jScrollPane26);

        filesDx1.add(jTabbedPane5, java.awt.BorderLayout.CENTER);

        modelEditorContainer.setRightComponent(filesDx1);

        filesTab.addTab("Model Editor", modelEditorContainer);

        gestioneIndice.setLayout(new java.awt.BorderLayout());

        jToolBar6.setRollover(true);

        jLabel36.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/world.png"))); // NOI18N
        jLabel36.setText("Lingua analizzatore sintattico ");
        jToolBar6.add(jLabel36);

        linguaAnalizzatoreIstruzione.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "it", "en", "bg", "br", "cz", "de", "fr", "es", "nl", "pl", "pt", "ro", "ru", "sk", "tr" }));
        linguaAnalizzatoreIstruzione.setSelectedItem(cc.getLanguage());
        linguaAnalizzatoreIstruzione.setMaximumSize(new java.awt.Dimension(50, 22));
        linguaAnalizzatoreIstruzione.setMinimumSize(new java.awt.Dimension(50, 22));
        linguaAnalizzatoreIstruzione.setPreferredSize(new java.awt.Dimension(50, 22));
        linguaAnalizzatoreIstruzione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linguaAnalizzatoreIstruzioneActionPerformed(evt);
            }
        });
        jToolBar6.add(linguaAnalizzatoreIstruzione);

        gestioneIndice.add(jToolBar6, java.awt.BorderLayout.PAGE_START);

        gestioneIndiceTabbedPanel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                gestioneIndiceTabbedPanelFocusGained(evt);
            }
        });

        manageStopWordsPanel.setLayout(new java.awt.BorderLayout());

        tableToolbar1.setRollover(true);

        addStopWord.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/add.png"))); // NOI18N
        addStopWord.setText("Aggiungi stop word");
        addStopWord.setToolTipText("Aggiungi");
        addStopWord.setFocusable(false);
        addStopWord.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        addStopWord.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        addStopWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStopWordActionPerformed(evt);
            }
        });
        tableToolbar1.add(addStopWord);

        deleteStopWord.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        deleteStopWord.setText("Cancella Selezionati");
        deleteStopWord.setToolTipText("Cancella");
        deleteStopWord.setFocusable(false);
        deleteStopWord.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        deleteStopWord.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        deleteStopWord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteStopWordActionPerformed(evt);
            }
        });
        tableToolbar1.add(deleteStopWord);
        tableToolbar1.add(jSeparator36);

        removeStopWordFilter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeStopWordFilter.setToolTipText("Rimuovi filtri");
        removeStopWordFilter.setFocusable(false);
        removeStopWordFilter.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeStopWordFilter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        removeStopWordFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeStopWordFilterActionPerformed(evt);
            }
        });
        tableToolbar1.add(removeStopWordFilter);

        jLabel37.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel37.setText("Cerca ");
        tableToolbar1.add(jLabel37);

        searchStopWords.setColumns(10);
        searchStopWords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchStopWordsActionPerformed(evt);
            }
        });
        searchStopWords.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchStopWordsKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchStopWordsKeyTyped(evt);
            }
        });
        tableToolbar1.add(searchStopWords);

        manageStopWordsPanel.add(tableToolbar1, java.awt.BorderLayout.PAGE_START);

        stopWordsTable.setAutoCreateRowSorter(true);
        stopWordsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Record"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        stopWordsTable.getTableHeader().setReorderingAllowed(false);
        stopWordsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                stopWordsTableKeyTyped(evt);
            }
        });
        stopWordsScrollPanel.setViewportView(stopWordsTable);
        Action actionStopWordsTable = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                TableCellListener tcl = (TableCellListener)e.getSource();
                changeStopWord(tcl);

            }
        };

        TableCellListener tclStopWords = new TableCellListener(stopWordsTable, actionStopWordsTable);

        manageStopWordsPanel.add(stopWordsScrollPanel, java.awt.BorderLayout.CENTER);

        manageStopWrodsStatus.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        manageStopWordsPanel.add(manageStopWrodsStatus, java.awt.BorderLayout.PAGE_END);

        gestioneIndiceTabbedPanel.addTab("Stop words", manageStopWordsPanel);

        manageDocuments.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                manageDocumentsFocusGained(evt);
            }
        });
        manageDocuments.setLayout(new java.awt.BorderLayout());

        manageDocumentsStatus.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        manageDocuments.add(manageDocumentsStatus, java.awt.BorderLayout.PAGE_END);

        jSplitPane8.setDividerLocation(340);
        jSplitPane8.setLastDividerLocation(350);

        manageClassificationTreePanel.setLayout(new java.awt.BorderLayout());

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        manageClassificationTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        manageClassificationTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                manageClassificationTreeMouseClicked(evt);
            }
        });
        jScrollPane30.setViewportView(manageClassificationTree);

        manageClassificationTreePanel.add(jScrollPane30, java.awt.BorderLayout.CENTER);

        jToolBar10.setRollover(true);

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_in.png"))); // NOI18N
        jButton9.setText("Importa");
        jButton9.setFocusable(false);
        jButton9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton9.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jToolBar10.add(jButton9);

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_out.png"))); // NOI18N
        jButton4.setText("Esporta");
        jButton4.setFocusable(false);
        jButton4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar10.add(jButton4);
        jToolBar10.add(jSeparator27);

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel5.setText(" ");
        jToolBar10.add(jLabel5);

        searchManageClassification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchManageClassificationActionPerformed(evt);
            }
        });
        jToolBar10.add(searchManageClassification);

        manageClassificationTreePanel.add(jToolBar10, java.awt.BorderLayout.PAGE_START);

        jSplitPane8.setLeftComponent(manageClassificationTreePanel);

        jPanel1.setLayout(new java.awt.BorderLayout());

        documentsTable.setAutoCreateRowSorter(true);
        documentsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Testo", "Level1", "Level2", "Level3", "Level4", "Level5", "Level6"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        documentsTable.getTableHeader().setReorderingAllowed(false);
        documentsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                documentsTableKeyTyped(evt);
            }
        });
        documentsTableScrollPanel.setViewportView(documentsTable);
        if (documentsTable.getColumnModel().getColumnCount() > 0) {
            documentsTable.getColumnModel().getColumn(0).setMinWidth(0);
            documentsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
            documentsTable.getColumnModel().getColumn(0).setMaxWidth(0);
            documentsTable.getColumnModel().getColumn(1).setResizable(false);
            documentsTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        }
        Action actionDocumentsTable = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                TableCellListener tcl = (TableCellListener)e.getSource();
                changeDocument(tcl);

                //     node.deleteRecord((String) tcl.getOldValue());
                //     node.addRecord((String) tcl.getNewValue());

            }
        };

        TableCellListener tclDocumentsTable = new TableCellListener(documentsTable, actionDocumentsTable);

        jPanel1.add(documentsTableScrollPanel, java.awt.BorderLayout.CENTER);

        tableToolbar2.setRollover(true);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/table.png"))); // NOI18N
        jButton1.setText("Ricarica Indice");
        jButton1.setFocusable(false);
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        tableToolbar2.add(jButton1);

        exportIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/arrow_out.png"))); // NOI18N
        exportIndex.setText("Esporta Indice");
        exportIndex.setFocusable(false);
        exportIndex.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        exportIndex.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        exportIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportIndexActionPerformed(evt);
            }
        });
        tableToolbar2.add(exportIndex);

        deleteDocument.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/bin_closed.png"))); // NOI18N
        deleteDocument.setText("Cancella selezionati");
        deleteDocument.setToolTipText("Cancella");
        deleteDocument.setFocusable(false);
        deleteDocument.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        deleteDocument.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        deleteDocument.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDocumentActionPerformed(evt);
            }
        });
        tableToolbar2.add(deleteDocument);
        tableToolbar2.add(jSeparator39);

        removeDocumentFilter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/cross.png"))); // NOI18N
        removeDocumentFilter.setToolTipText("Rimuovi filtri");
        removeDocumentFilter.setFocusable(false);
        removeDocumentFilter.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        removeDocumentFilter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        removeDocumentFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDocumentFilterActionPerformed(evt);
            }
        });
        tableToolbar2.add(removeDocumentFilter);

        jLabel38.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/magnifier.png"))); // NOI18N
        jLabel38.setText("Cerca  nel testo o nei level ");
        tableToolbar2.add(jLabel38);

        serachDocumentBody.setColumns(10);
        serachDocumentBody.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serachDocumentBodyActionPerformed(evt);
            }
        });
        serachDocumentBody.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                serachDocumentBodyKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                serachDocumentBodyKeyTyped(evt);
            }
        });
        tableToolbar2.add(serachDocumentBody);

        jPanel1.add(tableToolbar2, java.awt.BorderLayout.PAGE_START);

        jSplitPane8.setRightComponent(jPanel1);

        manageDocuments.add(jSplitPane8, java.awt.BorderLayout.CENTER);

        gestioneIndiceTabbedPanel.addTab("Documenti Indice", manageDocuments);

        createIndexPanel.setLayout(new java.awt.BorderLayout());

        logIstruzione.setEditable(false);
        logIstruzione.setColumns(20);
        logIstruzione.setRows(5);
        jScrollPane1.setViewportView(logIstruzione);

        createIndexPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        selezionaIndiceIstruzione.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_database.png"))); // NOI18N
        selezionaIndiceIstruzione.setText("Indice Lucene");
        selezionaIndiceIstruzione.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        selezionaIndiceIstruzione.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selezionaIndiceIstruzioneActionPerformed(evt);
            }
        });

        percorsoIndice1.setEditable(false);
        percorsoIndice1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                percorsoIndice1ActionPerformed(evt);
            }
        });

        selezionaStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_delete.png"))); // NOI18N
        selezionaStop.setText("Stop Words");
        selezionaStop.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        selezionaStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selezionaStopActionPerformed(evt);
            }
        });

        stopWords2.setEditable(false);
        stopWords2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopWords2ActionPerformed(evt);
            }
        });

        selezionaExcel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_excel.png"))); // NOI18N
        selezionaExcel.setText("File excel");
        selezionaExcel.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        selezionaExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selezionaExcelActionPerformed(evt);
            }
        });

        fileExcel.setEditable(false);
        fileExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileExcelActionPerformed(evt);
            }
        });

        startBuildIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/page_white_stack.png"))); // NOI18N
        startBuildIndex.setText("Istruisci");
        startBuildIndex.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        startBuildIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBuildIndexActionPerformed(evt);
            }
        });

        usaCategorie.setText("Usa nomi categorie");
        usaCategorie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usaCategorieActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(selezionaStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selezionaIndiceIstruzione, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addComponent(selezionaExcel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startBuildIndex, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(fileExcel, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(stopWords2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 754, Short.MAX_VALUE)
                            .addComponent(percorsoIndice1)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(usaCategorie, javax.swing.GroupLayout.PREFERRED_SIZE, 744, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(891, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(percorsoIndice1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selezionaIndiceIstruzione))
                .addGap(6, 6, 6)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stopWords2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selezionaStop))
                .addGap(8, 8, 8)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selezionaExcel)
                    .addComponent(fileExcel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startBuildIndex)
                    .addComponent(usaCategorie))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        createIndexPanel.add(jPanel3, java.awt.BorderLayout.NORTH);

        gestioneIndiceTabbedPanel.addTab("Costruzione Indice", createIndexPanel);

        gestioneIndice.add(gestioneIndiceTabbedPanel, java.awt.BorderLayout.CENTER);
        gestioneIndice.add(statusGestioneIndice, java.awt.BorderLayout.PAGE_END);

        filesTab.addTab("Gestione Indice", gestioneIndice);

        systemPanel.setLayout(new java.awt.BorderLayout());

        logInizializzazione.setColumns(20);
        logInizializzazione.setRows(5);
        logPanel.setViewportView(logInizializzazione);

        systemPanel.add(logPanel, java.awt.BorderLayout.CENTER);

        jToolBar11.setRollover(true);

        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/chart_curve.png"))); // NOI18N
        jButton11.setText("Info Sistema");
        jButton11.setFocusable(false);
        jButton11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton11.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jToolBar11.add(jButton11);

        jButton12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/thesemproject/opensem/gui/icons16/compass.png"))); // NOI18N
        jButton12.setText("Garbage Collector");
        jButton12.setFocusable(false);
        jButton12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton12.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        jToolBar11.add(jButton12);

        systemPanel.add(jToolBar11, java.awt.BorderLayout.PAGE_START);

        filesTab.addTab("System", systemPanel);

        getContentPane().add(filesTab, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void percorsoIndice1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_percorsoIndice1ActionPerformed
    }//GEN-LAST:event_percorsoIndice1ActionPerformed

    private void fileExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileExcelActionPerformed
    }//GEN-LAST:event_fileExcelActionPerformed

    private void p1IndexFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p1IndexFileChooserActionPerformed
        percorsoIndice.setText(p1IndexFileChooser.getSelectedFile().getAbsolutePath());
        selectIndexFolder.setVisible(false);
    }//GEN-LAST:event_p1IndexFileChooserActionPerformed

    private void p2IndexFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p2IndexFileChooserActionPerformed
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            percorsoIndice1.setText(p2IndexFileChooser.getSelectedFile().getAbsolutePath());
        }
        selectIndexFoderIstruzione.setVisible(false);
    }//GEN-LAST:event_p2IndexFileChooserActionPerformed

    private void selezionaIndiceIstruzioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selezionaIndiceIstruzioneActionPerformed
        String path = percorsoIndice1.getText();
        if (path.length() == 0) {
            path = cc.getIndexFolder();
        }
        p2IndexFileChooser.setCurrentDirectory(new File(path));
        selectIndexFoderIstruzione.setVisible(true);
    }//GEN-LAST:event_selezionaIndiceIstruzioneActionPerformed

    private void selezionaExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selezionaExcelActionPerformed
        String path = fileExcel.getText();
        if (path.length() == 0) {
            setLastFolder(excelFileChooser);
        } else {
            excelFileChooser.setCurrentDirectory(new File(path));
        }
        selectExcelFile.setVisible(true);

    }//GEN-LAST:event_selezionaExcelActionPerformed

    private void excelFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_excelFileChooserActionPerformed
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            updateLastSelectFolder(excelFileChooser.getSelectedFile().getAbsolutePath());
            fileExcel.setText(excelFileChooser.getSelectedFile().getAbsolutePath());
        }
        selectExcelFile.setVisible(false);
    }//GEN-LAST:event_excelFileChooserActionPerformed

    private void startBuildIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startBuildIndexActionPerformed
        LuceneIndexUtils.buildIndex(this);
    }//GEN-LAST:event_startBuildIndexActionPerformed

    private void selezionaStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selezionaStopActionPerformed
        String path = stopWords2.getText();
        stopWordsFileChooser2.setCurrentDirectory(new File(path));
        selectStopWords2.setVisible(true);
    }//GEN-LAST:event_selezionaStopActionPerformed

    private void stopWords2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopWords2ActionPerformed
    }//GEN-LAST:event_stopWords2ActionPerformed

    private void stopWordsFileChooser2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopWordsFileChooser2ActionPerformed
        selectStopWords2.setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            stopWords2.setText(stopWordsFileChooser2.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_stopWordsFileChooser2ActionPerformed

    private void usaCategorieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usaCategorieActionPerformed
    }//GEN-LAST:event_usaCategorieActionPerformed

    private void excelFileChooserClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_excelFileChooserClassActionPerformed
        doReadClassifyWriteExcel(evt);
    }//GEN-LAST:event_excelFileChooserClassActionPerformed

    private void processoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processoriActionPerformed
    }//GEN-LAST:event_processoriActionPerformed

    private void colonnaDescrizioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colonnaDescrizioneActionPerformed
    }//GEN-LAST:event_colonnaDescrizioneActionPerformed

    private void segmentFileChooser1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentFileChooser1ActionPerformed
        doFileSegmentation(evt);
    }//GEN-LAST:event_segmentFileChooser1ActionPerformed

    private void folderChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderChooserActionPerformed
        if (evt.getActionCommand().equals("ApproveSelection")) {
            doReadTagWrite();
        }
    }//GEN-LAST:event_folderChooserActionPerformed

    private void salvaHTMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salvaHTMLActionPerformed
    }//GEN-LAST:event_salvaHTMLActionPerformed

    private void processori1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processori1ActionPerformed
    }//GEN-LAST:event_processori1ActionPerformed

    private void classificaTestoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classificaTestoActionPerformed
        doTextClassification();
    }//GEN-LAST:event_classificaTestoActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        token.setText("");
        testo.setText("");
        classificationStatus.setText("");
        javax.swing.tree.DefaultMutableTreeNode clResults = new javax.swing.tree.DefaultMutableTreeNode("Classificazione");
        classificationResult.setModel(new DefaultTreeModel(clResults));
    }//GEN-LAST:event_jButton3ActionPerformed

    private void batchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_batchActionPerformed
        setLastFolder(excelFileChooserClass);
        selectExcelFileClass.setVisible(true);
    }//GEN-LAST:event_batchActionPerformed

    private void classificationTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_classificationTreeMouseClicked
        GuiUtils.treeActionPerformed(classificationTree, testo, token.getText(), evt, true, this);
        categorieSegmentsPanel.setModel(classificationTree.getModel());
        classificationTree1.setModel(classificationTree.getModel());
        manageClassificationTree.setModel(classificationTree.getModel());
        categorieSegmentsPanel.setModel(classificationTree.getModel());
    }//GEN-LAST:event_classificationTreeMouseClicked

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        String searched = jTextField1.getText();
        if (searched.length() > 0) {
            List<TreePath> paths = GuiUtils.find((DefaultMutableTreeNode) classificationTree.getModel().getRoot(), searched, false);
            GuiUtils.scrollToPath(classificationTree, paths);
        }
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyTyped

    }//GEN-LAST:event_jTextField1KeyTyped

    private void segmentaCartellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentaCartellaActionPerformed
        setLastFolder(folderChooser);
        selectFolderToProcess.setVisible(true);
    }//GEN-LAST:event_segmentaCartellaActionPerformed

    private void segmentaFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentaFileActionPerformed
        setLastFolder(segmentFileChooser1);
        selectFileToSegment.setVisible(true);
    }//GEN-LAST:event_segmentaFileActionPerformed

    private void resetSegmentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetSegmentaActionPerformed
        testoDaSegmentare.setText("");
        htmlResult.setText("");
        htmlTimeline.setText("");
        htmlFormatted.setText("");
        segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(new DefaultMutableTreeNode("Nessun segmento")));
        segmentTextArea.setText("");
        imagesPanel.removeAll();
    }//GEN-LAST:event_resetSegmentaActionPerformed

    private void segmentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentaActionPerformed
        doTextSegmentation();
    }//GEN-LAST:event_segmentaActionPerformed

    private void segmentTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_segmentTreeValueChanged
        segmentTreeSetText();
    }//GEN-LAST:event_segmentTreeValueChanged

    private void segmentTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_segmentTreeMouseClicked
        int selRow = segmentTree.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = segmentTree.getPathForLocation(evt.getX(), evt.getY());
        segmentTree.setSelectionPath(selPath);
        segmentTreeSetText();
    }//GEN-LAST:event_segmentTreeMouseClicked

    private void folderToLoadChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderToLoadChooserActionPerformed
        selectFolderToLoad.setVisible(false);
        if (evt.getActionCommand().equals("ApproveSelection")) {
            File sourceDir = folderToLoadChooser.getSelectedFile();
            FilesAndSegmentsUtils.filesTableReadFolder(sourceDir.getAbsolutePath(), null, this);
        }
    }//GEN-LAST:event_folderToLoadChooserActionPerformed

    private void segmentaEClassificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentaEClassificaActionPerformed
        FilesAndSegmentsUtils.doFilesTableSegment(true, this);
    }//GEN-LAST:event_segmentaEClassificaActionPerformed

    private void salvaStorageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salvaStorageActionPerformed
        if (isClassify) {
            return;
        }
        setLastFolder(saveAsFileChooser);
        selectSaveStorageAs.setVisible(true);
    }//GEN-LAST:event_salvaStorageActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        FilesAndSegmentsUtils.resetSegmentationsActionManagement(evt, this);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void saveAsFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsFileChooserActionPerformed
        selectSaveStorageAs.setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    isSaving = true;
                    filesInfoLabel.setText("Salvataggio in corso...");
                    FileOutputStream fout;
                    try {
                        String path = saveAsFileChooser.getSelectedFile().getAbsolutePath();
                        GuiUtils.makeBackup(path);
                        updateLastSelectFolder(path);
                        if (!path.endsWith(".ser")) {
                            path = path + ".ser";
                        }
                        fout = new FileOutputStream(new File(path));
                    } catch (Exception e) {
                        LogGui.printException(e);
                        return;
                    }
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(fout);
                        oos.writeObject(tableData);
                    } catch (Exception e) {
                        LogGui.printException(e);
                        return;
                    }
                    try {
                        fout.close();
                    } catch (Exception e) {
                        LogGui.printException(e);
                    }
                    filesInfoLabel.setText("Salvataggio effettuato");
                    isSaving = false;
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }//GEN-LAST:event_saveAsFileChooserActionPerformed

    private void openFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileChooserActionPerformed
        FilesAndSegmentsUtils.doImportSER(evt, this);
    }//GEN-LAST:event_openFileChooserActionPerformed

    private void filterSegmentsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterSegmentsKeyTyped
        if (isClassify) {
            return;
        }
        GuiUtils.filterTable(segmentsTable, filterSegments.getText(), 4);
        statusSegments.setText("Totale filtrati elementi: " + segmentsTable.getRowCount());
    }//GEN-LAST:event_filterSegmentsKeyTyped

    private void filterSegmentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterSegmentsActionPerformed
    }//GEN-LAST:event_filterSegmentsActionPerformed

    private void segmentsTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_segmentsTableKeyTyped
    }//GEN-LAST:event_segmentsTableKeyTyped

    private void segmentsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_segmentsTableMouseClicked
        FilesAndSegmentsUtils.segmentsTableMouseEventManagement(evt, this);
    }//GEN-LAST:event_segmentsTableMouseClicked


    private void categorieSegmentsPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_categorieSegmentsPanelMouseClicked
        GuiUtils.treeActionPerformed(categorieSegmentsPanel, segmentText, segmentTokens.getText(), evt, false, this);
        classificationTree.setModel(categorieSegmentsPanel.getModel());
        classificationTree1.setModel(categorieSegmentsPanel.getModel());
    }//GEN-LAST:event_categorieSegmentsPanelMouseClicked

    private void cercaCategoriaSegmentsPanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cercaCategoriaSegmentsPanelActionPerformed
        String searched = cercaCategoriaSegmentsPanel.getText();
        if (searched.length() > 0) {

            List<TreePath> paths = GuiUtils.find((DefaultMutableTreeNode) categorieSegmentsPanel.getModel().getRoot(), searched, false);
            GuiUtils.scrollToPath(categorieSegmentsPanel, paths);

        }
    }//GEN-LAST:event_cercaCategoriaSegmentsPanelActionPerformed

    private void filesPanelSegmentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filesPanelSegmentaActionPerformed
        if (isClassify) {
            return;
        }
        ChangedUtils.prepareChanged(this);
        int currentFilesPosition = filesTable.getSelectedRow();
        int id = (Integer) filesTable.getValueAt(currentFilesPosition, 0);
        String text = filesTable.getValueAt(currentFilesPosition, 8).toString();
        doDocumentSegmentation(text, id, currentFilesPosition);
    }//GEN-LAST:event_filesPanelSegmentaActionPerformed

    private void filesTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filesTableKeyTyped
    }//GEN-LAST:event_filesTableKeyTyped

    private void filesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_filesTableMouseClicked
        FilesAndSegmentsUtils.filesTableEventsManagement(evt, this);
    }//GEN-LAST:event_filesTableMouseClicked

    private void filterFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterFileActionPerformed
    }//GEN-LAST:event_filterFileActionPerformed

    private void filterFileKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterFileKeyTyped
    }//GEN-LAST:event_filterFileKeyTyped

    private void changedFilterTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_changedFilterTreeMouseClicked
        ChangedUtils.changedFilterTree(evt, this);
    }//GEN-LAST:event_changedFilterTreeMouseClicked


    private void firstLevelOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstLevelOnlyActionPerformed
        if (isClassify) {
            return;
        }
        FilesAndSegmentsUtils.segmentsTableFilterOnFirstLevel(this);
    }//GEN-LAST:event_firstLevelOnlyActionPerformed


    private void underTresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_underTresholdActionPerformed
        if (isClassify) {
            return;
        }
        FilesAndSegmentsUtils.segmentsTableUnderTreshold(this);
    }//GEN-LAST:event_underTresholdActionPerformed


    private void removeFiltersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFiltersActionPerformed
        if (isClassify) {
            return;
        }

        GuiUtils.filterTable(segmentsTable, null, 4);
        statusSegments.setText("Totale filtrati elementi: " + segmentsTable.getRowCount());
    }//GEN-LAST:event_removeFiltersActionPerformed

    private void changedTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_changedTableMouseClicked
        if (isClassify) {
            return;
        }
        ChangedUtils.changedTableMouseEventManagement(this);
    }//GEN-LAST:event_changedTableMouseClicked


    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        if (isClassify) {
            return;
        }
        resetFilesFilters();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void segmentClassificationResultMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_segmentClassificationResultMouseClicked
        int selRow = segmentClassificationResult.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = segmentClassificationResult.getPathForLocation(evt.getX(), evt.getY());
        changedFilterTree.setSelectionPath(selPath);
        if (selRow != -1) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) segmentClassificationResult.getLastSelectedPathComponent();
            String search = node.toString();
            int pos = search.indexOf("(");
            if (pos != -1) {
                search = search.substring(0, pos);
            }
            cercaCategoriaSegmentsPanel.setText(search);
            cercaCategoriaSegmentsPanelActionPerformed(null);
        }

    }//GEN-LAST:event_segmentClassificationResultMouseClicked

    private void alertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alertActionPerformed
        GuiUtils.filterOnStatus("A", null, this);
    }//GEN-LAST:event_alertActionPerformed

    private void notMarkedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_notMarkedActionPerformed
        GuiUtils.filterOnStatus("", "C", this);
    }//GEN-LAST:event_notMarkedActionPerformed

    private void changedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changedActionPerformed
        GuiUtils.filterOnStatus("C", null, this);
    }//GEN-LAST:event_changedActionPerformed

    private void coverageTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseClicked
        if (isClassify) {
            return;
        }
        int currentPosition = coverageTable.getSelectedRow();
        String segment = (String) coverageTable.getValueAt(currentPosition, 0);
        String capture = (String) coverageTable.getValueAt(currentPosition, 1);
        if (capcov != null) {
            GuiUtils.clearTable(captureValues);
            Set<String> values = capcov.getValues(segment, capture);
            DefaultTableModel model = (DefaultTableModel) captureValues.getModel();
            values.stream().map((value) -> {
                Object[] row = new Object[4];
                row[0] = segment;
                row[1] = capture;
                row[2] = value;
                Set<String> vc = capcov.getIdsForCaptureValue(segment, capture, value);
                row[3] = vc.size();
                return row;
            }).forEach((row) -> {
                model.addRow(row);
            });
        }
    }//GEN-LAST:event_coverageTableMouseClicked

    private void captureValuesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_captureValuesMouseClicked
        if (isClassify) {
            return;
        }
        int currentPosition = captureValues.getSelectedRow();
        String segment = (String) captureValues.getValueAt(currentPosition, 0);
        String capture = (String) captureValues.getValueAt(currentPosition, 1);
        String value = (String) captureValues.getValueAt(currentPosition, 2);
        if (capcov != null) {
            GuiUtils.clearTable(coverageDocumentsTable);
            Set<String> values = capcov.getIdsForCaptureValue(segment, capture, value);
            DefaultTableModel model = (DefaultTableModel) coverageDocumentsTable.getModel();
            for (String id : values) {
                Object[] row = new Object[2];
                row[0] = id;
                row[1] = tableData.get(Integer.parseInt(id)).getRow()[8];
                model.addRow(row);
            }
        }
    }//GEN-LAST:event_captureValuesMouseClicked

    private void coverageDocumentsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageDocumentsTableMouseClicked
        if (isClassify) {
            return;
        }
        int currentPosition = coverageDocumentsTable.getSelectedRow();
        String sid = (String) coverageDocumentsTable.getValueAt(currentPosition, 0);
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) segmentsTable.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("^" + sid + "\\.", 0));
        segmentsTable.setRowSorter(sorter);
        statusSegments.setText("Totale filtrati elementi: " + segmentsTable.getRowCount());
        TableRowSorter<TableModel> sorterF = (TableRowSorter<TableModel>) filesTable.getRowSorter();
        sorterF.setRowFilter(RowFilter.regexFilter("^" + sid + "$", 0));
        filesTable.setRowSorter(sorterF);
    }//GEN-LAST:event_coverageDocumentsTableMouseClicked

    private void modelTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modelTreeMouseClicked
        if (isClassify) {
            return;
        }
        me.segmentsActionPerformed(modelTree, evt);
    }//GEN-LAST:event_modelTreeMouseClicked

    private void confirmDefinitionPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmDefinitionPatternActionPerformed
        DictionaryUtils.confirmDefinitionPattern(this);
    }//GEN-LAST:event_confirmDefinitionPatternActionPerformed

    private void confirmSegmentPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmSegmentPatternActionPerformed
        SegmentsUtils.confirmSegmentPattern(this);
    }//GEN-LAST:event_confirmSegmentPatternActionPerformed

    private void tableImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableImportActionPerformed
        setLastFolder(importTableFileChooser);
        selectImportTable.setVisible(true);
    }//GEN-LAST:event_tableImportActionPerformed

    private void confirmCapturePatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmCapturePatternActionPerformed
        CapturesUtils.confirmCapturePattern(this);
    }//GEN-LAST:event_confirmCapturePatternActionPerformed

    private void newDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newDefinitionActionPerformed
        DictionaryUtils.clearDefinitionPanel(this);
    }//GEN-LAST:event_newDefinitionActionPerformed

    private void segmentPatternAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentPatternAddActionPerformed
        SegmentsUtils.clearSegmentPatternPanel(this);
    }//GEN-LAST:event_segmentPatternAddActionPerformed

    private void addCapturePatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCapturePatternActionPerformed
        CapturesUtils.clearCapturePatternPanel(this);
    }//GEN-LAST:event_addCapturePatternActionPerformed

    private void dictionaryTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dictionaryTableKeyTyped
        DictionaryUtils.dictionaryTableAction(this);
    }//GEN-LAST:event_dictionaryTableKeyTyped

    private void dictionaryTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dictionaryTableMouseClicked
        DictionaryUtils.dictionaryTableAction(this);
    }//GEN-LAST:event_dictionaryTableMouseClicked

    private void definitionPatternKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_definitionPatternKeyTyped
        confirmDefinitionPattern.setEnabled(true);
    }//GEN-LAST:event_definitionPatternKeyTyped

    private void deleteDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDefinitionActionPerformed
        DictionaryUtils.deleteDefinition(this);
    }//GEN-LAST:event_deleteDefinitionActionPerformed

    private void testDefinitionRegexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testDefinitionRegexActionPerformed
        String value = definitionPattern.getText();
        if (!PatternsUtils.testPattern(value, true, this)) {
            confirmDefinitionPattern.setEnabled(false);
        }
    }//GEN-LAST:event_testDefinitionRegexActionPerformed

    private void testDefinitionMatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testDefinitionMatchActionPerformed
        String value = definitionPattern.getText();
        PatternsUtils.testPattern(value, true, definitionPatternTest.getText(), this);
    }//GEN-LAST:event_testDefinitionMatchActionPerformed

    private void searchDefinitionKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchDefinitionKeyTyped
    }//GEN-LAST:event_searchDefinitionKeyTyped

    private void removeDefinitionFiltersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefinitionFiltersActionPerformed
        GuiUtils.filterTable(dictionaryTable, null, 1);
    }//GEN-LAST:event_removeDefinitionFiltersActionPerformed

    private void segmentPatternsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_segmentPatternsTableMouseClicked
        SegmentsUtils.segmentPatternsTableAction(this);
    }//GEN-LAST:event_segmentPatternsTableMouseClicked

    private void defaultYNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultYNActionPerformed
        SegmentTreeNode node = (SegmentTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setDefault(defaultYN.getSelectedItem().equals("Yes"));
        }
    }//GEN-LAST:event_defaultYNActionPerformed

    private void multipleYNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_multipleYNActionPerformed
        SegmentTreeNode node = (SegmentTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setMultiple(multipleYN.getSelectedItem().equals("Yes"));
        }
    }//GEN-LAST:event_multipleYNActionPerformed

    private void classifyYNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classifyYNActionPerformed
        SegmentTreeNode node = (SegmentTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setClassify(classifyYN.getSelectedItem().equals("Yes"));
        }
    }//GEN-LAST:event_classifyYNActionPerformed

    private void segmentPatternDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentPatternDeleteActionPerformed
        SegmentsUtils.segmentPatternDelete(this);
    }//GEN-LAST:event_segmentPatternDeleteActionPerformed

    private void testSegmentPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testSegmentPatternActionPerformed
        if (!PatternsUtils.testPattern(segmentPatternDefinition.getText(), true, this)) {
            confirmSegmentPattern.setEnabled(false);
        }
    }//GEN-LAST:event_testSegmentPatternActionPerformed

    private void matchSegmentPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_matchSegmentPatternActionPerformed
        PatternsUtils.testPattern(segmentPatternDefinition.getText(), true, segmentPatternTestArea.getText(), this);
    }//GEN-LAST:event_matchSegmentPatternActionPerformed

    private void capturePatternTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_capturePatternTableMouseClicked
        CapturesUtils.capturePatternTableAction(this);
    }//GEN-LAST:event_capturePatternTableMouseClicked

    private void deleteCapturePatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteCapturePatternActionPerformed
        CapturesUtils.deleteCapturePattern(this);
    }//GEN-LAST:event_deleteCapturePatternActionPerformed

    private void testCapturePatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testCapturePatternActionPerformed
        String value = capturePatternDefinition.getText();
        PatternsUtils.testPattern(value, true, this);
    }//GEN-LAST:event_testCapturePatternActionPerformed

    private void testCaptureMatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testCaptureMatchActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            PatternsUtils.testPattern(capturePatternDefinition.getText(), true, capturePatternTestText.getText(), (Integer) capturePatternSpinner.getValue(), capturePatternFixedValue.getText(), this);
        }
    }//GEN-LAST:event_testCaptureMatchActionPerformed

    private void searchTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTableActionPerformed
    }//GEN-LAST:event_searchTableActionPerformed

    private void removeTableFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeTableFilterActionPerformed
        GuiUtils.filterTable(table, null, 0);
    }//GEN-LAST:event_removeTableFilterActionPerformed

    private void tableDeleteRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableDeleteRecordActionPerformed
        TablesUtils.deleteTableRecord(this);
    }//GEN-LAST:event_tableDeleteRecordActionPerformed

    private void tableAddRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableAddRecordActionPerformed
        TablesUtils.addTableRecord(this);
    }//GEN-LAST:event_tableAddRecordActionPerformed

    private void tableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyTyped
    }//GEN-LAST:event_tableKeyTyped

    private void searchTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchTableKeyTyped
    }//GEN-LAST:event_searchTableKeyTyped

    private void saveModelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveModelActionPerformed
        GuiUtils.modelActionPerformed(true, this);
    }//GEN-LAST:event_saveModelActionPerformed

    private void compileModelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compileModelActionPerformed
        GuiUtils.modelActionPerformed(false, this);
    }//GEN-LAST:event_compileModelActionPerformed

    private void resetModelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetModelActionPerformed
        if (SE.init(getSegmentsPath())) {
            modelTree.setModel(SE.getVisualStructure());
            DefaultTreeModel model = (DefaultTreeModel) (modelTree.getModel());
            model.reload();
        }
    }//GEN-LAST:event_resetModelActionPerformed

    private void expotTableFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expotTableFileChooserActionPerformed
        TablesUtils.exportTable(evt, this);
    }//GEN-LAST:event_expotTableFileChooserActionPerformed

    private void importTableFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTableFileChooserActionPerformed
        TablesUtils.importTable(evt, this);
    }//GEN-LAST:event_importTableFileChooserActionPerformed

    private void tableExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableExportActionPerformed
        setLastFolder(expotTableFileChooser);
        selectExportTable.setVisible(true);
    }//GEN-LAST:event_tableExportActionPerformed

    private void learningFactorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_learningFactorActionPerformed
    }//GEN-LAST:event_learningFactorActionPerformed

    private void fattoreKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fattoreKActionPerformed
    }//GEN-LAST:event_fattoreKActionPerformed

    private void sogliaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sogliaActionPerformed
    }//GEN-LAST:event_sogliaActionPerformed

    private void segmentPatternDefinitionFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_segmentPatternDefinitionFocusGained
        segmentPatternSuggestor.setDictionary(me.getSuggestions((DefaultMutableTreeNode) modelTree.getModel().getRoot()));
        confirmSegmentPattern.setEnabled(true);
    }//GEN-LAST:event_segmentPatternDefinitionFocusGained

    private void capturePatternDefinitionFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_capturePatternDefinitionFocusGained
        capturePatternSuggestor.setDictionary(me.getSuggestions((DefaultMutableTreeNode) modelTree.getModel().getRoot()));
        confirmCapturePattern.setEnabled(true);
    }//GEN-LAST:event_capturePatternDefinitionFocusGained

    private void excelCorpusChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_excelCorpusChooserActionPerformed
        FilesAndSegmentsUtils.doFilesTableImportExcel(evt, this);
    }//GEN-LAST:event_excelCorpusChooserActionPerformed


    private void renameDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameDefinitionActionPerformed
        DictionaryUtils.renameDefinition(this);
    }//GEN-LAST:event_renameDefinitionActionPerformed

    private void filterFileKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_filterFileKeyReleased
        if (isClassify) {
            return;
        }
        GuiUtils.filterTable(filesTable, filterFile.getText(), 8);
    }//GEN-LAST:event_filterFileKeyReleased

    private void searchTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchTableKeyReleased
        GuiUtils.filterTable(table, searchTable.getText(), 0);        // TODO add your handling code here:
    }//GEN-LAST:event_searchTableKeyReleased

    private void segmenta1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmenta1ActionPerformed
        GuiUtils.modelActionPerformed(false, this);
        GuiUtils.doSegment(this);
    }//GEN-LAST:event_segmenta1ActionPerformed

    private void capturePatternFixedValueFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_capturePatternFixedValueFocusGained
        confirmCapturePattern.setEnabled(true);
    }//GEN-LAST:event_capturePatternFixedValueFocusGained

    private void capturePatternSpinnerFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_capturePatternSpinnerFocusGained
        confirmCapturePattern.setEnabled(true);        // TODO add your handling code here:
    }//GEN-LAST:event_capturePatternSpinnerFocusGained

    private void modelEditorFrameWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_modelEditorFrameWindowClosed
    }//GEN-LAST:event_modelEditorFrameWindowClosed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        if (jButton8.getToolTipText().equals("UnDock")) {
            filesTab.remove(modelEditorContainer);
            modelEditorFrame.getContentPane().add(modelEditorContainer, java.awt.BorderLayout.CENTER);
            modelEditorFrame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
            modelEditorFrame.setVisible(true);
            jButton8.setToolTipText("Dock");
        } else {
            doDockModelEditor();
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void patternsImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_patternsImportActionPerformed
        setLastFolder(importPatternsFileChooser);
        selectImportPatterns.setVisible(true);
    }//GEN-LAST:event_patternsImportActionPerformed

    private void patternsExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_patternsExportActionPerformed
        setLastFolder(expotPatternsFileChooser);
        selectExportPatterns.setVisible(true);
    }//GEN-LAST:event_patternsExportActionPerformed

    private void expotPatternsFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expotPatternsFileChooserActionPerformed
        PatternsUtils.exportPatterns(evt, this);
    }//GEN-LAST:event_expotPatternsFileChooserActionPerformed

    private void importPatternsFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPatternsFileChooserActionPerformed
        PatternsUtils.importPatterns(evt, this);
    }//GEN-LAST:event_importPatternsFileChooserActionPerformed

    private void saveRelationshipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRelationshipActionPerformed
        CapturesUtils.saveRelationship(this);
    }//GEN-LAST:event_saveRelationshipActionPerformed

    private void openSegmentRelationshipPanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSegmentRelationshipPanelActionPerformed
        CapturesUtils.openSegmentRelationship(this);
    }//GEN-LAST:event_openSegmentRelationshipPanelActionPerformed

    private void saveRelationship1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRelationship1ActionPerformed
        CapturesUtils.saveRelationShip(this);
    }//GEN-LAST:event_saveRelationship1ActionPerformed

    private void classificationTree1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_classificationTree1MouseClicked
        if (evt.getClickCount() == 2 && !evt.isConsumed()) {
            evt.consume();
            classTree1MouseEventManagement(evt);
        }
    }//GEN-LAST:event_classificationTree1MouseClicked

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        classTree1Find();
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField2KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyTyped
    }//GEN-LAST:event_jTextField2KeyTyped

    private void classifyPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classifyPatternActionPerformed
        PatternsUtils.classifyPattern(evt, this);
    }//GEN-LAST:event_classifyPatternActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        catClass.setText("");
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jTextField2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyReleased
        jTextField2ActionPerformed(null);
    }//GEN-LAST:event_jTextField2KeyReleased

    private void exportToExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportToExcelActionPerformed
        if (isClassify) {
            return;
        }
        setLastFolder(expotExcelFileChooser);
        selectExportExcel.setVisible(true);
    }//GEN-LAST:event_exportToExcelActionPerformed

    private void expotExcelFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expotExcelFileChooserActionPerformed
        FilesAndSegmentsUtils.doExportToExcel(evt, this);
    }//GEN-LAST:event_expotExcelFileChooserActionPerformed

    private void removeSearchFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSearchFilterActionPerformed
        GuiUtils.filterTable(capturePatternTable, null, 3);
    }//GEN-LAST:event_removeSearchFilterActionPerformed

    private void searchNormalizationKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchNormalizationKeyTyped
    }//GEN-LAST:event_searchNormalizationKeyTyped

    private void searchDefinitionKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchDefinitionKeyReleased
        GuiUtils.filterTable(dictionaryTable, searchDefinition.getText(), 0);
    }//GEN-LAST:event_searchDefinitionKeyReleased

    private void searchNormalizationKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchNormalizationKeyReleased
        GuiUtils.filterTable(capturePatternTable, searchNormalization.getText(), 3);
    }//GEN-LAST:event_searchNormalizationKeyReleased

    private void searchNormalizationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchNormalizationActionPerformed
    }//GEN-LAST:event_searchNormalizationActionPerformed

    private void tempCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tempCaptureActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setTemporary(tempCapture.isSelected());
        }
    }//GEN-LAST:event_tempCaptureActionPerformed

    private void captureTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureTargetActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setScope((String) captureTarget.getSelectedItem());
        }
    }//GEN-LAST:event_captureTargetActionPerformed

    private void captureFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureFormatActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setCaptureFormat(captureFormat.getText());
        }
    }//GEN-LAST:event_captureFormatActionPerformed

    private void captureTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureTypeActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            String type = (String) captureType.getSelectedItem();
            node.setCaptureType(type);
            GuiUtils.enableTimeLimits(type, this);
        }
    }//GEN-LAST:event_captureTypeActionPerformed

    private void linguaAnalizzatoreIstruzioneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linguaAnalizzatoreIstruzioneActionPerformed
        StopWordsUtils.populateStopWords(this);
        LuceneIndexUtils.populateIndex(this);
    }//GEN-LAST:event_linguaAnalizzatoreIstruzioneActionPerformed

    private void configurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configurationActionPerformed
        configurationDialog.setVisible(true);
    }//GEN-LAST:event_configurationActionPerformed

    private void addStopWordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStopWordActionPerformed
        StopWordsUtils.addStopWord(this);
    }//GEN-LAST:event_addStopWordActionPerformed

    private void deleteStopWordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteStopWordActionPerformed
        StopWordsUtils.deleteStopWord(this);
    }//GEN-LAST:event_deleteStopWordActionPerformed

    private void removeStopWordFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeStopWordFilterActionPerformed
        GuiUtils.filterTable(stopWordsTable, null, 0);
    }//GEN-LAST:event_removeStopWordFilterActionPerformed

    private void searchStopWordsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchStopWordsActionPerformed
    }//GEN-LAST:event_searchStopWordsActionPerformed

    private void searchStopWordsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchStopWordsKeyReleased
        GuiUtils.filterTable(stopWordsTable, searchStopWords.getText(), 0);        // TODO add your handling code here:
    }//GEN-LAST:event_searchStopWordsKeyReleased

    private void searchStopWordsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchStopWordsKeyTyped
    }//GEN-LAST:event_searchStopWordsKeyTyped

    private void stopWordsTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_stopWordsTableKeyTyped
    }//GEN-LAST:event_stopWordsTableKeyTyped

    private void deleteDocumentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDocumentActionPerformed
        LuceneIndexUtils.deleteDocument(this);
    }//GEN-LAST:event_deleteDocumentActionPerformed

    private void removeDocumentFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDocumentFilterActionPerformed
        GuiUtils.filterTable(documentsTable, null, 1);
        serachDocumentBody.setText("");
    }//GEN-LAST:event_removeDocumentFilterActionPerformed

    private void serachDocumentBodyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serachDocumentBodyActionPerformed
    }//GEN-LAST:event_serachDocumentBodyActionPerformed

    private void serachDocumentBodyKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_serachDocumentBodyKeyReleased
        LuceneIndexUtils.searchDocumentBody(this);
    }//GEN-LAST:event_serachDocumentBodyKeyReleased

    private void serachDocumentBodyKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_serachDocumentBodyKeyTyped
    }//GEN-LAST:event_serachDocumentBodyKeyTyped

    private void documentsTableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_documentsTableKeyTyped
    }//GEN-LAST:event_documentsTableKeyTyped

    private void gestioneIndiceTabbedPanelFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_gestioneIndiceTabbedPanelFocusGained
    }//GEN-LAST:event_gestioneIndiceTabbedPanelFocusGained

    private void manageDocumentsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_manageDocumentsFocusGained
    }//GEN-LAST:event_manageDocumentsFocusGained

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        LuceneIndexUtils.populateIndex(this);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void manageClassificationTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_manageClassificationTreeMouseClicked
        LuceneIndexUtils.manageClassificationTree(evt, this);
    }//GEN-LAST:event_manageClassificationTreeMouseClicked

    private void searchManageClassificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchManageClassificationActionPerformed
        String searched = searchManageClassification.getText();
        if (searched.length() > 0) {
            List<TreePath> paths = GuiUtils.find((DefaultMutableTreeNode) manageClassificationTree.getModel().getRoot(), searched, false);
            GuiUtils.scrollToPath(manageClassificationTree, paths);
        }
    }//GEN-LAST:event_searchManageClassificationActionPerformed

    private void exportTreeFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportTreeFileChooserActionPerformed
        GuiUtils.exportTree(evt, this);
    }//GEN-LAST:event_exportTreeFileChooserActionPerformed

    private void importTreeFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTreeFileChooserActionPerformed
        GuiUtils.importTree(evt, this);
    }//GEN-LAST:event_importTreeFileChooserActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        setLastFolder(importTreeFileChooser);
        selectImportTree.setVisible(true);
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        setLastFolder(exportTreeFileChooser);
        selectExportTree.setVisible(true);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void menuCaricaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCaricaActionPerformed
    }//GEN-LAST:event_menuCaricaActionPerformed

    private void importFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importFileChooserActionPerformed
        selectFileToImport.setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            final String sourceDir = importFileChooser.getSelectedFile().getParent();
            FilesAndSegmentsUtils.filesTableReadFolder(sourceDir, importFileChooser.getSelectedFiles(), this);
        }
    }//GEN-LAST:event_importFileChooserActionPerformed

    private void interrompiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_interrompiActionPerformed
        if (isClassify) {
            stopSegmentAndClassify.setValue(true);
        }
        if (rtt != null) {
            rtt.interrupt();
        }
        interrompi.setEnabled(false);
    }//GEN-LAST:event_interrompiActionPerformed

    private void segmentaEBastaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_segmentaEBastaActionPerformed
        FilesAndSegmentsUtils.doFilesTableSegment(false, this);
    }//GEN-LAST:event_segmentaEBastaActionPerformed

    private void endTimeIntervalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endTimeIntervalActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setAsEndPeriod(endTimeInterval.isSelected());
        }
    }//GEN-LAST:event_endTimeIntervalActionPerformed

    private void startTimeIntervalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startTimeIntervalActionPerformed
        CaptureTreeNode node = (CaptureTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setAsStartPeriod(startTimeInterval.isSelected());
        }
    }//GEN-LAST:event_startTimeIntervalActionPerformed

    private void processori2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processori2ActionPerformed
    }//GEN-LAST:event_processori2ActionPerformed

    private void tagCloudActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagCloudActionPerformed
        FilesAndSegmentsUtils.doFilesTableTagCloud(this);
    }//GEN-LAST:event_tagCloudActionPerformed

    private void globalTagCloudComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_globalTagCloudComponentResized
        if (tcp != null) {
            tcp.repaint();
        }
    }//GEN-LAST:event_globalTagCloudComponentResized

    private void nuvolettaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nuvolettaActionPerformed
        GuiUtils.doTagCloud(this);
    }//GEN-LAST:event_nuvolettaActionPerformed

    private void deleteDpFieldsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDpFieldsActionPerformed
        DataProvidersUtils.deleteDataProvider(this);
    }//GEN-LAST:event_deleteDpFieldsActionPerformed

    private void dpFieldsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dpFieldsTableMouseClicked
        DataProvidersUtils.clearDpFieldsDetails(this);
        DataProvidersUtils.dpFieldsTableAction(this);
    }//GEN-LAST:event_dpFieldsTableMouseClicked

    private void dpFileNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpFileNameActionPerformed
    }//GEN-LAST:event_dpFileNameActionPerformed

    private void addDpFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDpFieldActionPerformed
        DataProvidersUtils.clearDpFieldsDetails(this);
        DataProvidersUtils.enableDpFieldsDetails(this);
        dpFieldPosition.setValue(dpFieldsTable.getModel().getRowCount() + 1);
    }//GEN-LAST:event_addDpFieldActionPerformed

    private void confirmDpFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmDpFieldActionPerformed
        DataProvidersUtils.confirmDpFiled(this);
    }//GEN-LAST:event_confirmDpFieldActionPerformed

    private void dprTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dprTableMouseClicked
        DataProvidersUtils.dprTableMouseClick(this);
    }//GEN-LAST:event_dprTableMouseClicked

    private void dprPriorityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dprPriorityActionPerformed
        DataProviderTreeRelationshipNode node = (DataProviderTreeRelationshipNode) me.getCurrentNode();
        if (node != null) {
            node.setPriority(dprPriority.isSelected());
        }
    }//GEN-LAST:event_dprPriorityActionPerformed

    private void dprSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dprSaveActionPerformed
        DataProvidersUtils.dprSave(this);
    }//GEN-LAST:event_dprSaveActionPerformed

    private void dpDelimitatoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpDelimitatoreActionPerformed
    }//GEN-LAST:event_dpDelimitatoreActionPerformed

    private void dpTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpTypeActionPerformed
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setType((String) dpType.getSelectedItem());
        }
    }//GEN-LAST:event_dpTypeActionPerformed

    private void dpQuoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpQuoteActionPerformed
    }//GEN-LAST:event_dpQuoteActionPerformed

    private void dpLineSepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpLineSepActionPerformed
    }//GEN-LAST:event_dpLineSepActionPerformed

    private void dpEscapeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpEscapeActionPerformed
    }//GEN-LAST:event_dpEscapeActionPerformed

    private void dpFieldTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpFieldTypeActionPerformed
    }//GEN-LAST:event_dpFieldTypeActionPerformed

    private void dpFileNameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpFileNameKeyReleased
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setFileName(dpFileName.getText());
        }
    }//GEN-LAST:event_dpFileNameKeyReleased

    private void dpDelimitatoreKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpDelimitatoreKeyReleased
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setDelimiter(dpDelimitatore.getText());
        }
    }//GEN-LAST:event_dpDelimitatoreKeyReleased

    private void dpEscapeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpEscapeKeyReleased
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setEscape(dpEscape.getText());
        }
    }//GEN-LAST:event_dpEscapeKeyReleased

    private void dpQuoteKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpQuoteKeyReleased
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setQuote(dpQuote.getText());
        }
    }//GEN-LAST:event_dpQuoteKeyReleased

    private void dpLineSepKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpLineSepKeyTyped
    }//GEN-LAST:event_dpLineSepKeyTyped

    private void dpLineSepKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dpLineSepKeyReleased
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setLineSeparator(dpLineSep.getText());
        }
    }//GEN-LAST:event_dpLineSepKeyReleased

    private void csvdpchooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_csvdpchooserActionPerformed
        DataProvidersUtils.csvDataProvider(evt, this);
    }//GEN-LAST:event_csvdpchooserActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        setLastFolder(csvdpchooser);
        selectCSVDataProvider.setVisible(true);
    }//GEN-LAST:event_jButton7ActionPerformed

    private void importDpFieldsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importDpFieldsActionPerformed
        DataProvidersUtils.importDpFields(this);
    }//GEN-LAST:event_importDpFieldsActionPerformed

    private void dprSegmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dprSegmentActionPerformed
        DataProviderTreeRelationshipNode node = (DataProviderTreeRelationshipNode) me.getCurrentNode();
        if (node != null) {
            node.setSegmentName((String) dprSegment.getSelectedItem());
            DataProvidersUtils.populateDataProviderRelationship(node, this);
        }
    }//GEN-LAST:event_dprSegmentActionPerformed

    private void dprCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dprCaptureActionPerformed
    }//GEN-LAST:event_dprCaptureActionPerformed

    private void dprTableMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dprTableMouseEntered
    }//GEN-LAST:event_dprTableMouseEntered

    private void dprSegmentItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dprSegmentItemStateChanged

    }//GEN-LAST:event_dprSegmentItemStateChanged

    private void dprSegmentMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dprSegmentMouseClicked
    }//GEN-LAST:event_dprSegmentMouseClicked

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        DataProviderTreeNode node = (DataProviderTreeNode) me.getCurrentNode();
        if (node != null) {
            node.skipFirstRow(jCheckBox1.isSelected());
        }
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void onlySegmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onlySegmentActionPerformed
    }//GEN-LAST:event_onlySegmentActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        if (isClassify) {
            return;
        }
        if (evt.getActionCommand().equals("Inizializza")) {
            Thread t = new Thread(this::initializeModel);
            t.setDaemon(true);
            t.start();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void selezionaIndiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selezionaIndiceActionPerformed
        p1IndexFileChooser.setCurrentDirectory(new File(cc.getIndexFolder()));
        selectIndexFolder.setVisible(true);
        jButton3.setEnabled(false);
        classificaTesto.setEnabled(false);
    }//GEN-LAST:event_selezionaIndiceActionPerformed

    private void percorsoIndiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_percorsoIndiceActionPerformed
    }//GEN-LAST:event_percorsoIndiceActionPerformed

    private void burnToStorageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_burnToStorageActionPerformed
        BurnToStorageDialog ts = new BurnToStorageDialog("Burn", this);
        ts.actionPerformed(evt);
    }//GEN-LAST:event_burnToStorageActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        LogGui.printSystemInfo();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        GuiUtils.runGarbageCollection();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        jButton12ActionPerformed(evt);
    }//GEN-LAST:event_jButton13ActionPerformed

    private void dpFieldTableRelationshipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpFieldTableRelationshipActionPerformed
    }//GEN-LAST:event_dpFieldTableRelationshipActionPerformed

    private void fromDataProviderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromDataProviderActionPerformed
        TableTreeNode node = (TableTreeNode) me.getCurrentNode();
        if (node != null) {
            node.setPopulateFromDp(fromDataProvider.isSelected());
            TablesUtils.populateTablePanel(node, this);
        }
    }//GEN-LAST:event_fromDataProviderActionPerformed

    private void rebuildIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rebuildIndexActionPerformed
    }//GEN-LAST:event_rebuildIndexActionPerformed

    private void moveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpActionPerformed
        GuiUtils.moveUp(capturePatternTable);
        ((CaptureTreeNode) me.getCurrentNode()).updatePatternsFromTable(capturePatternTable);
    }//GEN-LAST:event_moveUpActionPerformed

    private void moveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownActionPerformed
        GuiUtils.moveDown(capturePatternTable);
        ((CaptureTreeNode) me.getCurrentNode()).updatePatternsFromTable(capturePatternTable);
    }//GEN-LAST:event_moveDownActionPerformed

    private void moveTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveTopActionPerformed
        GuiUtils.moveTop(capturePatternTable);
        ((CaptureTreeNode) me.getCurrentNode()).updatePatternsFromTable(capturePatternTable);
    }//GEN-LAST:event_moveTopActionPerformed

    private void moveBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveBottomActionPerformed
        GuiUtils.moveBottom(capturePatternTable);
        ((CaptureTreeNode) me.getCurrentNode()).updatePatternsFromTable(capturePatternTable);
    }//GEN-LAST:event_moveBottomActionPerformed

    private void moveUp1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUp1ActionPerformed
        GuiUtils.moveUp(segmentPatternsTable);
        ((SegmentTreeNode) me.getCurrentNode()).updatePatternsFromTable(segmentPatternsTable);
    }//GEN-LAST:event_moveUp1ActionPerformed

    private void moveDown1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDown1ActionPerformed
        GuiUtils.moveDown(segmentPatternsTable);
        ((SegmentTreeNode) me.getCurrentNode()).updatePatternsFromTable(segmentPatternsTable);
    }//GEN-LAST:event_moveDown1ActionPerformed

    private void moveTop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveTop1ActionPerformed
        GuiUtils.moveTop(segmentPatternsTable);
        ((SegmentTreeNode) me.getCurrentNode()).updatePatternsFromTable(segmentPatternsTable);
    }//GEN-LAST:event_moveTop1ActionPerformed

    private void moveBottom1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveBottom1ActionPerformed
        GuiUtils.moveBottom(segmentPatternsTable);
        ((SegmentTreeNode) me.getCurrentNode()).updatePatternsFromTable(segmentPatternsTable);
    }//GEN-LAST:event_moveBottom1ActionPerformed

    private void classStartLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classStartLevelActionPerformed
        ME.getRoot().setStartLevel(classStartLevel.getSelectedIndex() + 1);
        ME.storeXml(NodeData.getDocument(ME.getRoot()));
        needUpdate = true;
    }//GEN-LAST:event_classStartLevelActionPerformed

    private void selezionaOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selezionaOCRActionPerformed
        String path = cc.getOcrPath();
        if (path != null && path.length() != 0) {
            ocrFileChooser.setCurrentDirectory(new File(path));
        }
        selectOCRFolder.setVisible(true);
    }//GEN-LAST:event_selezionaOCRActionPerformed

    private void percorsoOCRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_percorsoOCRActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_percorsoOCRActionPerformed

    private void ocrFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ocrFileChooserActionPerformed
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            percorsoOCR.setText(ocrFileChooser.getSelectedFile().getAbsolutePath());
        }
        selectOCRFolder.setVisible(false);

    }//GEN-LAST:event_ocrFileChooserActionPerformed

    private void exportIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportIndexActionPerformed
        setLastFolder(expotExcelIndexFileChooser);
        selectExportExcelIndex.setVisible(true);
    }//GEN-LAST:event_exportIndexActionPerformed

    private void expotExcelIndexFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expotExcelIndexFileChooserActionPerformed
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            String fileToExport = expotExcelIndexFileChooser.getSelectedFile().getAbsolutePath();
            LuceneIndexUtils.exportExcelFile(fileToExport, this);
        }
        selectExportExcelIndex.setVisible(false);
    }//GEN-LAST:event_expotExcelIndexFileChooserActionPerformed

    /**
     * Metodo di start del software SemGUI
     *
     * @param args the command line arguments
     * @throws java.lang.ClassNotFoundException classe non trovata
     */
    public static void main(String args[]) throws ClassNotFoundException {
        try {
            Properties props = new Properties();
            props.put("logoString", "openSem");
            AeroLookAndFeel.setCurrentTheme(props);
            UIManager.setLookAndFeel("com.jtattoo.plaf.aero.AeroLookAndFeel");
            GuiUtils.adjustFontSize();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        java.awt.EventQueue.invokeLater(() -> {
            SemGui sem = new SemGui();
            sem.setVisible(true);
            ME.closeAllReaders();
            SE.closeAllReaders();
        });
    }

    // Variabili
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isSaving = false;
    private final Object lockSync = new Object();
    private Map<Integer, SemDocument> tableData = null;
    private ReadFolderToTable rtt;
    private CapturesCoverage capcov = null;
    private static final MulticlassEngine ME = new MulticlassEngine();
    private static final SegmentEngine SE = new SegmentEngine();
    private static final DocumentParser DP = new DocumentParser();
    private final SemConfiguration cc;
    private final ModelEditor me;
    private final AutoSuggestor segmentPatternSuggestor;
    private final AutoSuggestor capturePatternSuggestor;
    private String lastFolder = null;
    private boolean isInit = false;
    private TagCloudPanel tcp;
    private Map<String, Integer> classes = new HashMap<>();
    private boolean needUpdate = false;
    private boolean isClassify = false;

    private final FinalBoolean stopSegmentAndClassify = new FinalBoolean(false);
    private final FinalBoolean stopTagCloud = new FinalBoolean(false);
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addCapturePattern;
    private javax.swing.JButton addDpField;
    private javax.swing.JButton addStopWord;
    private javax.swing.JButton alert;
    private javax.swing.JButton batch;
    private javax.swing.JButton burnToStorage;
    private javax.swing.JDialog captureClassificationRelationship;
    private javax.swing.JPanel captureConfigurationPanel;
    private javax.swing.JPanel captureConfigurationPanel5;
    private javax.swing.JSplitPane captureConfigurationSplitPanel;
    private javax.swing.JSplitPane captureConfigurationSplitPanel3;
    private javax.swing.JPanel captureConfigurationSuperPanel;
    private javax.swing.JPanel captureConfigurationSuperPanel3;
    private javax.swing.JTextField captureFormat;
    private javax.swing.JLabel captureName;
    private javax.swing.JPanel capturePanel;
    private javax.swing.JScrollPane capturePatternContentScrollPanel;
    private javax.swing.JPanel capturePatternContentTable;
    private javax.swing.JPanel capturePatternContentTable1;
    private javax.swing.JTextArea capturePatternDefinition;
    private javax.swing.JPanel capturePatternEditPanel;
    private javax.swing.JPanel capturePatternEditPanel1;
    private javax.swing.JPanel capturePatternEditPanel3;
    private javax.swing.JTextField capturePatternFixedValue;
    private javax.swing.JPanel capturePatternPanel;
    private javax.swing.JSpinner capturePatternSpinner;
    private javax.swing.JLabel capturePatternStatus;
    private javax.swing.JLabel capturePatternStatus1;
    private javax.swing.JLabel capturePatternStatus3;
    private javax.swing.JTable capturePatternTable;
    private javax.swing.JTextArea capturePatternTestText;
    private javax.swing.JToolBar capturePatternToolbar;
    private javax.swing.JToolBar capturePatternToolbar1;
    private javax.swing.JToolBar capturePatternToolbar3;
    private javax.swing.JPanel capturePatternsPanel;
    private javax.swing.JPanel capturePatternsPanel3;
    private javax.swing.JScrollPane capturePatternsScrollPanel;
    private javax.swing.JScrollPane capturePatternsScrollPanel3;
    private javax.swing.JToolBar capturePatternsToolbar;
    private javax.swing.JTable captureRelationshipTable;
    private javax.swing.JSplitPane captureSplit;
    private javax.swing.JComboBox<String> captureTarget;
    private javax.swing.JComboBox<String> captureType;
    private javax.swing.JTable captureValues;
    private javax.swing.JTextField catClass;
    private javax.swing.JTree categorieSegmentsPanel;
    private javax.swing.JTextField cercaCategoriaSegmentsPanel;
    private javax.swing.JButton changed;
    private javax.swing.JTree changedFilterTree;
    private javax.swing.JTable changedTable;
    private javax.swing.JSplitPane changes;
    private javax.swing.JScrollPane changesTableScrollPanel;
    private javax.swing.JScrollPane changesTreeScrollPanel;
    private javax.swing.JComboBox<String> classStartLevel;
    private javax.swing.JButton classificaTesto;
    private javax.swing.JTree classificationResult;
    private javax.swing.JLabel classificationStatus;
    private javax.swing.JTree classificationTree;
    private javax.swing.JTree classificationTree1;
    private javax.swing.JPanel classificationTreePanel;
    private javax.swing.JPanel classificazione;
    private javax.swing.JButton classifyPattern;
    private javax.swing.JComboBox<String> classifyYN;
    private javax.swing.JComboBox<String> colonnaDescrizione;
    private javax.swing.JButton compileModel;
    private javax.swing.JButton configuration;
    private javax.swing.JDialog configurationDialog;
    private javax.swing.JButton confirmCapturePattern;
    private javax.swing.JButton confirmDefinitionPattern;
    private javax.swing.JButton confirmDpField;
    private javax.swing.JButton confirmSegmentPattern;
    private javax.swing.JToolBar consolleToolbar;
    private javax.swing.JSplitPane coverage;
    private javax.swing.JTable coverageDocumentsTable;
    private javax.swing.JSplitPane coverageSplitPanel;
    private javax.swing.JTable coverageTable;
    private javax.swing.JScrollPane coverageTableScrollPanel;
    private javax.swing.JPanel createIndexPanel;
    private javax.swing.JFileChooser csvdpchooser;
    private javax.swing.JPanel dataproviderFieldsPanel;
    private javax.swing.JPanel dataproviderPanel;
    private javax.swing.JSplitPane dataproviderRelationship;
    private javax.swing.JSplitPane dataproviderSplit;
    private javax.swing.JSplitPane dataproviderSplitPanel;
    private javax.swing.JScrollPane dbFieldsScrollPanel;
    private javax.swing.JComboBox<String> defaultYN;
    private javax.swing.JPanel definitionDefinitionPanel;
    private javax.swing.JTextField definitionName;
    private javax.swing.JPanel definitionPanel;
    private javax.swing.JTextArea definitionPattern;
    private javax.swing.JPanel definitionPatternEditPanel;
    private javax.swing.JScrollPane definitionPatternScrollPanel;
    private javax.swing.JTextArea definitionPatternTest;
    private javax.swing.JScrollPane definitionPatternTestScrollPanel;
    private javax.swing.JToolBar definitionPatternToolbar;
    private javax.swing.JLabel definitionStatus;
    private javax.swing.JButton deleteCapturePattern;
    private javax.swing.JButton deleteDefinition;
    private javax.swing.JButton deleteDocument;
    private javax.swing.JButton deleteDpFields;
    private javax.swing.JButton deleteStopWord;
    private javax.swing.JPanel dictionaryPanel;
    private javax.swing.JSplitPane dictionarySplit;
    private javax.swing.JLabel dictionaryStatus;
    private javax.swing.JTable dictionaryTable;
    private javax.swing.JScrollPane dictionaryTableScrollPanel;
    private javax.swing.JToolBar dictionaryToolbar;
    private javax.swing.JTable documentsTable;
    private javax.swing.JScrollPane documentsTableScrollPanel;
    private javax.swing.JPanel dpConfigurationPanel;
    private javax.swing.JPanel dpDefinition;
    private javax.swing.JTextField dpDelimitatore;
    private javax.swing.JTextField dpEscape;
    private javax.swing.JLabel dpFieldId;
    private javax.swing.JTextField dpFieldName;
    private javax.swing.JSpinner dpFieldPosition;
    private javax.swing.JComboBox<String> dpFieldTableRelationship;
    private javax.swing.JComboBox<String> dpFieldType;
    private javax.swing.JPanel dpFieldsPanel;
    private javax.swing.JTable dpFieldsTable;
    private javax.swing.JToolBar dpFieldsToolbar;
    private javax.swing.JTextField dpFileName;
    private javax.swing.JTextField dpLineSep;
    private javax.swing.JLabel dpName;
    private javax.swing.JTextField dpQuote;
    private javax.swing.JComboBox<String> dpType;
    private javax.swing.JComboBox<String> dprCapture;
    private javax.swing.JCheckBox dprEnrich;
    private javax.swing.JLabel dprFieldName;
    private javax.swing.JCheckBox dprKey;
    private javax.swing.JLabel dprName;
    private javax.swing.JPanel dprPanel;
    private javax.swing.JCheckBox dprPriority;
    private javax.swing.JPanel dprRelationshipPanel;
    private javax.swing.JButton dprSave;
    private javax.swing.JComboBox<String> dprSegment;
    private javax.swing.JTable dprTable;
    private javax.swing.JCheckBox endTimeInterval;
    private javax.swing.JLabel etichettaAlberoSegmenti;
    private javax.swing.JFileChooser excelCorpusChooser;
    private javax.swing.JFileChooser excelFileChooser;
    private javax.swing.JFileChooser excelFileChooserClass;
    private javax.swing.JButton exportIndex;
    private javax.swing.JButton exportToExcel;
    private javax.swing.JFileChooser exportTreeFileChooser;
    private javax.swing.JFileChooser expotExcelFileChooser;
    private javax.swing.JFileChooser expotExcelIndexFileChooser;
    private javax.swing.JFileChooser expotPatternsFileChooser;
    private javax.swing.JFileChooser expotTableFileChooser;
    private javax.swing.JTextField fattoreK;
    private javax.swing.JTextField fileExcel;
    private javax.swing.JTextArea fileText;
    private javax.swing.JTextArea fileText1;
    private javax.swing.JPanel files;
    private javax.swing.JPanel filesDx;
    private javax.swing.JPanel filesDx1;
    private javax.swing.JLabel filesInfoLabel;
    private javax.swing.JTextPane filesPanelHtml;
    private javax.swing.JTextPane filesPanelHtml1;
    private javax.swing.JTextPane filesPanelHtmlFormatted;
    private javax.swing.JTextPane filesPanelHtmlFormatted1;
    private javax.swing.JTree filesPanelSegmentTree;
    private javax.swing.JButton filesPanelSegmenta;
    private javax.swing.JTable filesPanleCapturesTable;
    private javax.swing.JSplitPane filesSplitPanel;
    private javax.swing.JPanel filesSx;
    private javax.swing.JTabbedPane filesTab;
    private javax.swing.JTable filesTable;
    private javax.swing.JToolBar filesToolbar;
    private javax.swing.JTextField filterFile;
    private javax.swing.JTextField filterSegments;
    private javax.swing.JButton firstLevelOnly;
    private javax.swing.JFileChooser folderChooser;
    private javax.swing.JFileChooser folderToLoadChooser;
    private javax.swing.JCheckBox fromDataProvider;
    private javax.swing.JPanel gestioneIndice;
    private javax.swing.JTabbedPane gestioneIndiceTabbedPanel;
    private javax.swing.JDialog globalCapturesSegmentsRelationship;
    private javax.swing.JFrame globalTagCloud;
    private javax.swing.JEditorPane help;
    private javax.swing.JEditorPane htmlFormatted;
    private javax.swing.JEditorPane htmlResult;
    private javax.swing.JEditorPane htmlTimeline;
    private javax.swing.JPanel imagesPanel;
    private javax.swing.JScrollPane imagesScrollPanel;
    private javax.swing.JButton importDpFields;
    private javax.swing.JFileChooser importFileChooser;
    private javax.swing.JFileChooser importPatternsFileChooser;
    private javax.swing.JFileChooser importTableFileChooser;
    private javax.swing.JFileChooser importTreeFileChooser;
    private javax.swing.JLabel initLabel;
    private javax.swing.JButton interrompi;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane16;
    private javax.swing.JScrollPane jScrollPane17;
    private javax.swing.JScrollPane jScrollPane18;
    private javax.swing.JScrollPane jScrollPane19;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane20;
    private javax.swing.JScrollPane jScrollPane21;
    private javax.swing.JScrollPane jScrollPane22;
    private javax.swing.JScrollPane jScrollPane23;
    private javax.swing.JScrollPane jScrollPane24;
    private javax.swing.JScrollPane jScrollPane25;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JScrollPane jScrollPane27;
    private javax.swing.JScrollPane jScrollPane28;
    private javax.swing.JScrollPane jScrollPane29;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane30;
    private javax.swing.JScrollPane jScrollPane31;
    private javax.swing.JScrollPane jScrollPane35;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator15;
    private javax.swing.JToolBar.Separator jSeparator16;
    private javax.swing.JToolBar.Separator jSeparator17;
    private javax.swing.JToolBar.Separator jSeparator18;
    private javax.swing.JToolBar.Separator jSeparator19;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator20;
    private javax.swing.JToolBar.Separator jSeparator21;
    private javax.swing.JToolBar.Separator jSeparator22;
    private javax.swing.JToolBar.Separator jSeparator23;
    private javax.swing.JToolBar.Separator jSeparator24;
    private javax.swing.JToolBar.Separator jSeparator25;
    private javax.swing.JToolBar.Separator jSeparator26;
    private javax.swing.JToolBar.Separator jSeparator27;
    private javax.swing.JToolBar.Separator jSeparator28;
    private javax.swing.JToolBar.Separator jSeparator29;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator30;
    private javax.swing.JToolBar.Separator jSeparator31;
    private javax.swing.JToolBar.Separator jSeparator32;
    private javax.swing.JToolBar.Separator jSeparator33;
    private javax.swing.JToolBar.Separator jSeparator34;
    private javax.swing.JToolBar.Separator jSeparator35;
    private javax.swing.JToolBar.Separator jSeparator36;
    private javax.swing.JToolBar.Separator jSeparator37;
    private javax.swing.JToolBar.Separator jSeparator38;
    private javax.swing.JToolBar.Separator jSeparator39;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator40;
    private javax.swing.JToolBar.Separator jSeparator41;
    private javax.swing.JToolBar.Separator jSeparator42;
    private javax.swing.JToolBar.Separator jSeparator43;
    private javax.swing.JToolBar.Separator jSeparator44;
    private javax.swing.JSeparator jSeparator45;
    private javax.swing.JToolBar.Separator jSeparator46;
    private javax.swing.JToolBar.Separator jSeparator47;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JSplitPane jSplitPane4;
    private javax.swing.JSplitPane jSplitPane5;
    private javax.swing.JSplitPane jSplitPane6;
    private javax.swing.JSplitPane jSplitPane7;
    private javax.swing.JSplitPane jSplitPane8;
    private javax.swing.JSplitPane jSplitPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTabbedPane jTabbedPane3;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTabbedPane jTabbedPane5;
    private javax.swing.JTabbedPane jTabbedPane6;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar10;
    private javax.swing.JToolBar jToolBar11;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar5;
    private javax.swing.JToolBar jToolBar6;
    private javax.swing.JToolBar jToolBar7;
    private javax.swing.JToolBar jToolBar8;
    private javax.swing.JToolBar jToolBar9;
    private javax.swing.JLabel labelTesto;
    private javax.swing.JTextField learningFactor;
    private javax.swing.JComboBox<String> linguaAnalizzatoreIstruzione;
    private javax.swing.JTextArea logInizializzazione;
    private javax.swing.JTextArea logIstruzione;
    private javax.swing.JScrollPane logPanel;
    private javax.swing.JTree manageClassificationTree;
    private javax.swing.JPanel manageClassificationTreePanel;
    private javax.swing.JPanel manageDocuments;
    private javax.swing.JLabel manageDocumentsStatus;
    private javax.swing.JPanel manageStopWordsPanel;
    private javax.swing.JLabel manageStopWrodsStatus;
    private javax.swing.JButton matchSegmentPattern;
    private javax.swing.JLabel memInfo;
    private javax.swing.JButton menuCarica;
    private javax.swing.JPanel modeEditorInfo;
    private javax.swing.JSplitPane modelEditor;
    private javax.swing.JSplitPane modelEditorContainer;
    private javax.swing.JFrame modelEditorFrame;
    private javax.swing.JTabbedPane modelElements;
    private javax.swing.JTree modelTree;
    private javax.swing.JScrollPane modelTreeScrollPanel;
    private javax.swing.JPanel modelTreeSplitPanel;
    private javax.swing.JButton moveBottom;
    private javax.swing.JButton moveBottom1;
    private javax.swing.JButton moveDown;
    private javax.swing.JButton moveDown1;
    private javax.swing.JButton moveTop;
    private javax.swing.JButton moveTop1;
    private javax.swing.JButton moveUp;
    private javax.swing.JButton moveUp1;
    private javax.swing.JComboBox<String> multipleYN;
    private javax.swing.JButton newDefinition;
    private javax.swing.JButton notMarked;
    private javax.swing.JButton nuvoletta;
    private javax.swing.JFileChooser ocrFileChooser;
    private javax.swing.JCheckBox onlySegment;
    private javax.swing.JFileChooser openFileChooser;
    private javax.swing.JButton openSegmentRelationshipPanel;
    private javax.swing.JFileChooser p1IndexFileChooser;
    private javax.swing.JFileChooser p2IndexFileChooser;
    private javax.swing.JPanel pannelloAlbero;
    private javax.swing.JPanel pannelloAlbero1;
    private javax.swing.JPanel pannelloClassificazione;
    private javax.swing.JPanel pannelloTesto;
    private javax.swing.JPanel pannelloTokenizzazione;
    private javax.swing.JButton patternsExport;
    private javax.swing.JButton patternsImport;
    private javax.swing.JTextField percorsoIndice;
    private javax.swing.JTextField percorsoIndice1;
    private javax.swing.JTextField percorsoOCR;
    private javax.swing.JComboBox<String> processori;
    private javax.swing.JComboBox<String> processori1;
    private javax.swing.JComboBox<String> processori2;
    private javax.swing.JCheckBox rebuildIndex;
    private javax.swing.JButton removeDefinitionFilters;
    private javax.swing.JButton removeDocumentFilter;
    private javax.swing.JButton removeFilters;
    private javax.swing.JButton removeSearchFilter;
    private javax.swing.JButton removeStopWordFilter;
    private javax.swing.JButton removeTableFilter;
    private javax.swing.JButton renameDefinition;
    private javax.swing.JButton resetModel;
    private javax.swing.JButton resetSegmenta;
    private javax.swing.JComboBox<String> salvaHTML;
    private javax.swing.JButton salvaStorage;
    private javax.swing.JFileChooser saveAsFileChooser;
    private javax.swing.JButton saveModel;
    private javax.swing.JButton saveRelationship;
    private javax.swing.JButton saveRelationship1;
    private javax.swing.JTextField searchDefinition;
    private javax.swing.JTextField searchManageClassification;
    private javax.swing.JTextField searchNormalization;
    private javax.swing.JTextField searchStopWords;
    private javax.swing.JTextField searchTable;
    private javax.swing.JTree segmentClassificationResult;
    private javax.swing.JPanel segmentConfigurationPanel;
    private javax.swing.JFileChooser segmentFileChooser1;
    private javax.swing.JLabel segmentName;
    private javax.swing.JPanel segmentPanel;
    private javax.swing.JButton segmentPatternAdd;
    private javax.swing.JPanel segmentPatternConfigurationPanel;
    private javax.swing.JTextArea segmentPatternDefinition;
    private javax.swing.JPanel segmentPatternDefinitionPanel;
    private javax.swing.JScrollPane segmentPatternDefinitionScrollPanel;
    private javax.swing.JButton segmentPatternDelete;
    private javax.swing.JPanel segmentPatternPanel;
    private javax.swing.JScrollPane segmentPatternScrollPanelTestArea;
    private javax.swing.JLabel segmentPatternStatus;
    private javax.swing.JTextArea segmentPatternTestArea;
    private javax.swing.JToolBar segmentPatternToolbar;
    private javax.swing.JPanel segmentPatternsPanel;
    private javax.swing.JScrollPane segmentPatternsScrollPanel;
    private javax.swing.JTable segmentPatternsTable;
    private javax.swing.JToolBar segmentPatternsToolbar;
    private javax.swing.JTextArea segmentText;
    private javax.swing.JTextArea segmentTextArea;
    private javax.swing.JTextArea segmentTokens;
    private javax.swing.JTree segmentTree;
    private javax.swing.JButton segmenta;
    private javax.swing.JButton segmenta1;
    private javax.swing.JButton segmentaCartella;
    private javax.swing.JButton segmentaEBasta;
    private javax.swing.JButton segmentaEClassifica;
    private javax.swing.JButton segmentaFile;
    private javax.swing.JPanel segmentazione;
    private javax.swing.JSplitPane segments;
    private javax.swing.JSplitPane segmentsConsolleSplitPanel;
    private javax.swing.JSplitPane segmentsSplit;
    private javax.swing.JSplitPane segmentsSplitPanel;
    private javax.swing.JTable segmentsTable;
    private javax.swing.JDialog selectCSVDataProvider;
    private javax.swing.JDialog selectExcelFile;
    private javax.swing.JDialog selectExcelFileClass;
    private javax.swing.JDialog selectExcelFileSer;
    private javax.swing.JDialog selectExportExcel;
    private javax.swing.JDialog selectExportExcelIndex;
    private javax.swing.JDialog selectExportPatterns;
    private javax.swing.JDialog selectExportTable;
    private javax.swing.JDialog selectExportTree;
    private javax.swing.JDialog selectFileToImport;
    private javax.swing.JDialog selectFileToSegment;
    private javax.swing.JDialog selectFolderToLoad;
    private javax.swing.JDialog selectFolderToProcess;
    private javax.swing.JDialog selectImportPatterns;
    private javax.swing.JDialog selectImportTable;
    private javax.swing.JDialog selectImportTree;
    private javax.swing.JDialog selectIndexFoderIstruzione;
    private javax.swing.JDialog selectIndexFolder;
    private javax.swing.JDialog selectOCRFolder;
    private javax.swing.JDialog selectOpenStorage;
    private javax.swing.JDialog selectSaveStorageAs;
    private javax.swing.JDialog selectStopWords2;
    private javax.swing.JButton selezionaExcel;
    private javax.swing.JButton selezionaIndice;
    private javax.swing.JButton selezionaIndiceIstruzione;
    private javax.swing.JButton selezionaOCR;
    private javax.swing.JButton selezionaStop;
    private javax.swing.JTextField serachDocumentBody;
    private javax.swing.JTextField soglia;
    private javax.swing.JButton startBuildIndex;
    private javax.swing.JCheckBox startTimeInterval;
    private javax.swing.JLabel statusGestioneIndice;
    private javax.swing.JLabel statusSegments;
    private javax.swing.JTextField stopWords2;
    private javax.swing.JFileChooser stopWordsFileChooser2;
    private javax.swing.JScrollPane stopWordsScrollPanel;
    private javax.swing.JTable stopWordsTable;
    private javax.swing.JPanel systemPanel;
    private javax.swing.JTable table;
    private javax.swing.JButton tableAddRecord;
    private javax.swing.JButton tableDeleteRecord;
    private javax.swing.JButton tableExport;
    private javax.swing.JButton tableImport;
    private javax.swing.JPanel tablePanel;
    private javax.swing.JScrollPane tableScrollpanel;
    private javax.swing.JToolBar tableToolbar;
    private javax.swing.JToolBar tableToolbar1;
    private javax.swing.JToolBar tableToolbar2;
    private javax.swing.JButton tagCloud;
    private javax.swing.JCheckBox tempCapture;
    private javax.swing.JButton testCaptureMatch;
    private javax.swing.JButton testCapturePattern;
    private javax.swing.JButton testDefinitionMatch;
    private javax.swing.JButton testDefinitionRegex;
    private javax.swing.JButton testSegmentPattern;
    private javax.swing.JTextArea testo;
    private javax.swing.JTextArea testoDaSegmentare;
    private javax.swing.JTextArea token;
    private javax.swing.JButton underTreshold;
    private javax.swing.JCheckBox usaCategorie;
    // End of variables declaration//GEN-END:variables

    // Get variabili
    /**
     *
     * @return albero delle categorie nel pannello segmenti
     */
    public JTree getCategorieSegmentsPanel() {
        return categorieSegmentsPanel;
    }

    /**
     *
     * @return albero dei cambiati
     */
    public JTree getChangedFilterTree() {
        return changedFilterTree;
    }

    /**
     *
     * @return tabella dei cambiati
     */
    public JTable getChangedTable() {
        return changedTable;
    }

    /**
     *
     * @return albero di classificazione
     */
    public JTree getManageClassificationTree() {
        return manageClassificationTree;
    }

    /**
     *
     * @return true se il sistema sta classificando
     */
    public boolean isIsClassify() {
        return isClassify;
    }

    /**
     *
     * @return JDialog con il file excel selezionato
     */
    public JDialog getSelectExcelFile() {
        return selectExcelFile;
    }

    /**
     *
     * @return JDialog con il file excel con la struttura di classificazione
     */
    public JDialog getSelectExcelFileClass() {
        return selectExcelFileClass;
    }

    /**
     *
     * @return JDialog con il file .ser dove sono serializzati i documenti
     */
    public JDialog getSelectExcelFileSer() {
        return selectExcelFileSer;
    }

    /**
     *
     * @return JDialog per selezionare dove esportare l'excel
     */
    public JDialog getSelectExportExcel() {
        return selectExportExcel;
    }

    /**
     *
     * @return JDialog per esportare i pattern
     */
    public JDialog getSelectExportPatterns() {
        return selectExportPatterns;
    }

    /**
     *
     * @return JDialog per selezionare dove esportare una tabella
     */
    public JDialog getSelectExportTable() {
        return selectExportTable;
    }

    /**
     *
     * @return JDialog per selezionare dove esportare l'albero
     */
    public JDialog getSelectExportTree() {
        return selectExportTree;
    }

    /**
     *
     * @return JDialog per selezionare il file da importare
     */
    public JDialog getSelectFileToImport() {
        return selectFileToImport;
    }

    /**
     *
     * @return JDialog per selezionare il file da segmentare
     */
    public JDialog getSelectFileToSegment() {
        return selectFileToSegment;
    }

    /**
     *
     * @return JDialog per selezionare la cartella da caricare
     */
    public JDialog getSelectFolderToLoad() {
        return selectFolderToLoad;
    }

    /**
     *
     * @return JDialog per selezionare la cartella da processare
     */
    public JDialog getSelectFolderToProcess() {
        return selectFolderToProcess;
    }

    /**
     *
     * @return JDialog per selezionare i file dei pattern da importare
     */
    public JDialog getSelectImportPatterns() {
        return selectImportPatterns;
    }

    /**
     *
     * @return Label per l'info del files panel
     */
    public JLabel getFilesInfoLabel() {
        return filesInfoLabel;
    }

    /**
     *
     * @return JDialog per selezionare la tabella da importare
     */
    public JDialog getSelectImportTable() {
        return selectImportTable;
    }

    /**
     *
     * @return JDialog per selezionare l'albero da importare
     */
    public JDialog getSelectImportTree() {
        return selectImportTree;
    }

    /**
     *
     * @return JDialog per selezionare la cartella dell'indice che si vuole
     * creare
     */
    public JDialog getSelectIndexFoderIstruzione() {
        return selectIndexFoderIstruzione;
    }

    /**
     *
     * @return JDialog per selezionare la cartella dell'indice che si vuole
     * aprire
     */
    public JDialog getSelectIndexFolder() {
        return selectIndexFolder;
    }

    /**
     *
     * @return JDialog per selezionare l'apertura dello storage
     */
    public JDialog getSelectOpenStorage() {
        return selectOpenStorage;
    }

    /**
     *
     * @return JDialog per selezionare dove salvare con nome
     */
    public JDialog getSelectSaveStorageAs() {
        return selectSaveStorageAs;
    }

    /**
     *
     * @return JDialog per selezionare le stopwords di istruzione
     */
    public JDialog getSelectStopWords2() {
        return selectStopWords2;
    }

    /**
     *
     * @return Processo per leggere una cartella in una tabella.
     */
    public ReadFolderToTable getRtt() {
        return rtt;
    }

    /**
     *
     * @return seleziona cartella
     */
    public JFileChooser getFolderChooser() {
        return folderChooser;
    }

    /**
     *
     * @return seleziona cartella
     */
    public JFileChooser getFolderToLoadChooser() {
        return folderToLoadChooser;
    }

    /**
     *
     * @return seleziona file da aprire
     */
    public JFileChooser getOpenFileChooser() {
        return openFileChooser;
    }

    /**
     *
     * @return classificatore
     */
    public MulticlassEngine getME() {
        return ME;
    }

    /**
     *
     * @return segmentatore
     */
    public SegmentEngine getSE() {
        return SE;
    }

    /**
     *
     * @return parser
     */
    public DocumentParser getDP() {
        return DP;
    }

    /**
     *
     * @return definizione della cattura
     */
    public JTextArea getCapturePatternDefinition() {
        return capturePatternDefinition;
    }

    /**
     *
     * @param chooser imposta l'ultima cartella aperta
     */
    public void setLastFolder(JFileChooser chooser) {
        if (lastFolder == null) {
            lastFolder = cc.getLastFolder();
        }
        if (!lastFolder.isEmpty()) {
            File f = new File(lastFolder);
            if (!f.isDirectory()) {
                f = f.getParentFile();
            }
            chooser.setCurrentDirectory(f);
        }
    }

    /**
     * azione sulla tabella delle catture
     */
    public void capturePatternTableAction() {
        CapturesUtils.capturePatternTableAction(this);
    }

    /**
     *
     * @return pulsate per importare i fields dataprovider
     */
    public JButton getImportDpFields() {
        return importDpFields;
    }

    /**
     *
     * @return file chooser per importazione
     */
    public JFileChooser getImportFileChooser() {
        return importFileChooser;
    }

    /**
     *
     * @return file chooser per importare i pattern
     */
    public JFileChooser getImportPatternsFileChooser() {
        return importPatternsFileChooser;
    }

    /**
     *
     * @return file chooser per importare la tabella
     */
    public JFileChooser getImportTableFileChooser() {
        return importTableFileChooser;
    }

    /**
     *
     * @return file chooser per importare l'albero
     */
    public JFileChooser getImportTreeFileChooser() {
        return importTreeFileChooser;
    }

    /**
     *
     * @return filechooser per importare une excel
     */
    public JFileChooser getExcelCorpusChooser() {
        return excelCorpusChooser;
    }

    /**
     *
     * @return file chooser per importare un excel
     */
    public JFileChooser getExcelFileChooser() {
        return excelFileChooser;
    }

    /**
     *
     * @return file chooser per importare un excel di nodi
     */
    public JFileChooser getExcelFileChooserClass() {
        return excelFileChooserClass;
    }

    /**
     *
     * @return pulsante per aprire il menu di caricamento
     */
    public JButton getMenuCarica() {
        return menuCarica;
    }

    /**
     *
     * @return pulsante per interrompere
     */
    public JButton getInterrompi() {
        return interrompi;
    }

    /**
     *
     * @return valore fisso per la cattura
     */
    public JTextField getCapturePatternFixedValue() {
        return capturePatternFixedValue;
    }

    /**
     *
     * @return tabella dei pattern di cattura
     */
    public JTable getCapturePatternTable() {
        return capturePatternTable;
    }

    /**
     *
     * @return textrea di test
     */
    public JTextArea getCapturePatternTestText() {
        return capturePatternTestText;
    }

    /**
     *
     * @return textfield
     */
    public JTextField getjTextField1() {
        return jTextField1;
    }

    /**
     *
     * @return getjTextField2
     */
    public JTextField getjTextField2() {
        return jTextField2;
    }

    /**
     *
     * @return tabella di relazione cattura
     */
    public JTable getCaptureRelationshipTable() {
        return captureRelationshipTable;
    }

    /**
     *
     * @return valore cattura
     */
    public JTable getCaptureValues() {
        return captureValues;
    }

    /**
     *
     * @return risultati classificazione
     */
    public JTree getClassificationResult() {
        return classificationResult;
    }

    /**
     *
     * @return albero di classificazione
     */
    public JTree getClassificationTree() {
        return classificationTree;
    }

    /**
     *
     * @return albero di classificazione
     */
    public JTree getClassificationTree1() {
        return classificationTree1;
    }

    /**
     *
     * @return tabella di coverage sui documenti
     */
    public JTable getCoverageDocumentsTable() {
        return coverageDocumentsTable;
    }

    /**
     *
     * @return tabella di coverage
     */
    public JTable getCoverageTable() {
        return coverageTable;
    }

    /**
     *
     * @return nome della definizione
     */
    public JTextField getDefinitionName() {
        return definitionName;
    }

    /**
     *
     * @return pattern di definizione
     */
    public JTextArea getDefinitionPattern() {
        return definitionPattern;
    }

    /**
     *
     * @return testo di test
     */
    public JTextArea getDefinitionPatternTest() {
        return definitionPatternTest;
    }

    /**
     *
     * @return fattore k
     */
    public JTextField getFattoreK() {
        return fattoreK;
    }

    /**
     *
     * @return file excel
     */
    public JTextField getFileExcel() {
        return fileExcel;
    }

    /**
     *
     * @return file di testo
     */
    public JTextArea getFileText() {
        return fileText;
    }

    /**
     *
     * @return fileText1
     */
    public JTextArea getFileText1() {
        return fileText1;
    }

    /**
     *
     * @return filePanelHtml
     */
    public JTextPane getFilesPanelHtml() {
        return filesPanelHtml;
    }

    /**
     *
     * @return filesPanelHtml1
     */
    public JTextPane getFilesPanelHtml1() {
        return filesPanelHtml1;
    }

    /**
     *
     * @return filesPanelHtmlFormatted
     */
    public JTextPane getFilesPanelHtmlFormatted() {
        return filesPanelHtmlFormatted;
    }

    /**
     *
     * @return filesPanelHtmlFormatted1
     */
    public JTextPane getFilesPanelHtmlFormatted1() {
        return filesPanelHtmlFormatted1;
    }

    /**
     *
     * @return filesPanelSegmentTree
     */
    public JTree getFilesPanelSegmentTree() {
        return filesPanelSegmentTree;
    }

    /**
     *
     * @return filesPanleCapturesTable
     */
    public JTable getFilesPanleCapturesTable() {
        return filesPanleCapturesTable;
    }

    /**
     *
     * @return model editor
     */
    public ModelEditor getModelEditor() {
        return me;
    }

    /**
     *
     * @return tabella files
     */
    public JTable getFilesTable() {
        return filesTable;
    }

    /**
     *
     * @return filtro su tabella files
     */
    public JTextField getFilterFile() {
        return filterFile;
    }

    /**
     *
     * @return filtro sui segmenti
     */
    public JTextField getFilterSegments() {
        return filterSegments;
    }

    /**
     *
     * @return finestra di log
     */
    public JTextArea getLogInizializzazione() {
        return logInizializzazione;
    }

    /**
     *
     * @return finestra di log
     */
    public JTextArea getLogIstruzione() {
        return logIstruzione;
    }

    /**
     *
     * @return albero del modello di segmentazione
     */
    public JTree getModelTree() {
        return modelTree;
    }

    /**
     *
     * @return percorso indice di classificazione
     */
    public JTextField getPercorsoIndice() {
        return percorsoIndice;
    }

    /**
     *
     * @return percorso indice
     */
    public JTextField getPercorsoIndice1() {
        return percorsoIndice1;
    }

    /**
     *
     * @return filtro di ricerca su definizioni
     */
    public JTextField getSearchDefinition() {
        return searchDefinition;
    }

    /**
     *
     * @return filtro di ricerca sulla classificazione
     */
    public JTextField getSearchManageClassification() {
        return searchManageClassification;
    }

    /**
     *
     * @return true se il sistema sta salvado
     */
    public boolean isIsSaving() {
        return isSaving;
    }

    /**
     *
     * @return filtro di ricerca sulle normalizzate
     */
    public JTextField getSearchNormalization() {
        return searchNormalization;
    }

    /**
     *
     * @return filtro di ricerca sulle stopwords
     */
    public JTextField getSearchStopWords() {
        return searchStopWords;
    }

    /**
     *
     * @return filtro di ricerca nella tabella
     */
    public JTextField getSearchTable() {
        return searchTable;
    }

    /**
     *
     * @return albero dei risultati della segmentazione
     */
    public JTree getSegmentClassificationResult() {
        return segmentClassificationResult;
    }

    /**
     *
     * @return deifnizione del pattern di segmentazione
     */
    public JTextArea getSegmentPatternDefinition() {
        return segmentPatternDefinition;
    }

    /**
     *
     * @return area di test
     */
    public JTextArea getSegmentPatternTestArea() {
        return segmentPatternTestArea;
    }

    /**
     *
     * @return testo da segmentare
     */
    public JTextArea getSegmentText() {
        return segmentText;
    }

    /**
     *
     * @return text area di segmentazione
     */
    public JTextArea getSegmentTextArea() {
        return segmentTextArea;
    }

    /**
     *
     * @return texarea con i token
     */
    public JTextArea getSegmentTokens() {
        return segmentTokens;
    }

    /**
     *
     * @return albero dei segmenti
     */
    public JTree getSegmentTree() {
        return segmentTree;
    }

    /**
     *
     * @return ricerca nel testo del documento
     */
    public JTextField getSerachDocumentBody() {
        return serachDocumentBody;
    }

    /**
     *
     * @return soglia
     */
    public JTextField getSoglia() {
        return soglia;
    }

    /**
     *
     * @return stopwords
     */
    public JTextField getStopWords2() {
        return stopWords2;
    }

    /**
     *
     * @return testo
     */
    public JTextArea getTesto() {
        return testo;
    }

    /**
     *
     * @return testo da segmentare
     */
    public JTextArea getTestoDaSegmentare() {
        return testoDaSegmentare;
    }

    /**
     *
     * @return vista tokenizzata
     */
    public JTextArea getToken() {
        return token;
    }

    /**
     *
     * @return true se il sistema si sta inizializzando
     */
    public boolean isIsInit() {
        return isInit;
    }

    /**
     *
     * @return learning factor per il classificatore
     */
    public JTextField getLearningFactor() {
        return learningFactor;
    }

    /**
     *
     * @return scelta della lingua
     */
    public JComboBox<String> getLinguaAnalizzatoreIstruzione() {
        return linguaAnalizzatoreIstruzione;
    }

    /**
     *
     * @return status documenti
     */
    public JLabel getManageDocumentsStatus() {
        return manageDocumentsStatus;
    }

    /**
     *
     * @return status stop words
     */
    public JLabel getManageStopWrodsStatus() {
        return manageStopWrodsStatus;
    }

    /**
     *
     * @return true se è necessario re-inizializzare
     */
    public boolean isNeedUpdate() {
        return needUpdate;
    }

    /**
     *
     * @param needUpdate identifica se il sistema deve essere re-inizializzato
     */
    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    /**
     *
     * @return status del dizionario
     */
    public JLabel getDictionaryStatus() {
        return dictionaryStatus;
    }

    /**
     *
     * @return tabella del dizionario
     */
    public JTable getDictionaryTable() {
        return dictionaryTable;
    }

    /**
     *
     * @return tabella dei documenti
     */
    public JTable getDocumentsTable() {
        return documentsTable;
    }

    /**
     *
     * @return se a false il processo di tagcolud si ferma
     */
    public FinalBoolean getStopTagCloud() {
        return stopTagCloud;
    }

    /**
     *
     * @return file chooser
     */
    public JFileChooser getStopWordsFileChooser2() {
        return stopWordsFileChooser2;
    }

    /**
     *
     * @return tabella di stop word
     */
    public JTable getStopWordsTable() {
        return stopWordsTable;
    }

    /**
     *
     * @return tabella
     */
    public JTable getTable() {
        return table;
    }

    /**
     *
     * @return pulsante per aggiungere un pattern di cattura
     */
    public JButton getAddCapturePattern() {
        return addCapturePattern;
    }

    /**
     *
     * @return pulsante per aggiungere un field
     */
    public JButton getAddDpField() {
        return addDpField;
    }

    /**
     *
     * @return pulsante per aggiungere una stop word
     */
    public JButton getAddStopWord() {
        return addStopWord;
    }

    /**
     *
     * @return pulsante per filtrare i segmenti in stato alert
     */
    public JButton getAlert() {
        return alert;
    }

    /**
     *
     * @return pulsante per processare in modo batch i dati
     */
    public JButton getBatch() {
        return batch;
    }

    /**
     *
     * @return pulsante per caricare il dataprovider
     */
    public JButton getBurnToStorage() {
        return burnToStorage;
    }

    /**
     *
     * @return dialog per la relazione della cattura
     */
    public JDialog getCaptureClassificationRelationship() {
        return captureClassificationRelationship;
    }

    /**
     *
     * @return classe di categoria
     */
    public JTextField getCatClass() {
        return catClass;
    }

    /**
     *
     * @return field per la ricarca
     */
    public JTextField getCercaCategoriaSegmentsPanel() {
        return cercaCategoriaSegmentsPanel;
    }

    /**
     *
     * @return pulsante per identificare i cambiati
     */
    public JButton getChanged() {
        return changed;
    }

    /**
     *
     * @return pulsante per classificare un testo
     */
    public JButton getClassificaTesto() {
        return classificaTesto;
    }

    /**
     *
     * @return tabbed panel
     */
    public JTabbedPane getFilesTab() {
        return filesTab;
    }

    /**
     *
     * @param isClassify true se sta classificando
     */
    public void setIsClassify(boolean isClassify) {
        this.isClassify = isClassify;
    }

    /**
     *
     * @return status di classificaione
     */
    public JLabel getClassificationStatus() {
        return classificationStatus;
    }

    /**
     *
     * @return combo per dire se una cattura classifica
     */
    public JComboBox<String> getClassifyYN() {
        return classifyYN;
    }

    /**
     *
     * @return combo per selezionare la colonna descrizione
     */
    public JComboBox<String> getColonnaDescrizione() {
        return colonnaDescrizione;
    }

    /**
     *
     * @return pulsante per compilare il modello
     */
    public JButton getCompileModel() {
        return compileModel;
    }

    /**
     *
     * @return pulsante per aprire la configurazione
     */
    public JButton getConfiguration() {
        return configuration;
    }

    /**
     *
     * @return filechooser per selezionare un csv
     */
    public JFileChooser getCsvdpchooser() {
        return csvdpchooser;
    }

    /**
     *
     * @return combo per definire se un segmento è di default
     */
    public JComboBox<String> getDefaultYN() {
        return defaultYN;
    }

    /**
     *
     * @return dialog per la configurazione
     */
    public JDialog getConfigurationDialog() {
        return configurationDialog;
    }

    /**
     *
     * @return compo per definire i processori
     */
    public JComboBox<String> getProcessori() {
        return processori;
    }

    /**
     *
     * @return combo per definire i processori
     */
    public JComboBox<String> getProcessori1() {
        return processori1;
    }

    /**
     *
     * @return combo per definire i processori da utilizzare
     */
    public JComboBox<String> getProcessori2() {
        return processori2;
    }

    /**
     *
     * @return pulsante per cancellare il pattern di cattura
     */
    public JButton getDeleteCapturePattern() {
        return deleteCapturePattern;
    }

    /**
     *
     * @return pulsante per cancellare una definizione
     */
    public JButton getDeleteDefinition() {
        return deleteDefinition;
    }

    /**
     *
     * @return pulsante per cancellare documenti
     */
    public JButton getDeleteDocument() {
        return deleteDocument;
    }

    /**
     *
     * @return pulsante per cancellare i fields del dataprovider
     */
    public JButton getDeleteDpFields() {
        return deleteDpFields;
    }

    /**
     *
     * @return pulsante per cancellare una stopword
     */
    public JButton getDeleteStopWord() {
        return deleteStopWord;
    }

    /**
     *
     * @return etichetta albero
     */
    public JLabel getEtichettaAlberoSegmenti() {
        return etichettaAlberoSegmenti;
    }

    /**
     *
     * @return pulsante per esportare in excel
     */
    public JButton getExportToExcel() {
        return exportToExcel;
    }

    /**
     *
     * @return file chooser per esportare l'albero
     */
    public JFileChooser getExportTreeFileChooser() {
        return exportTreeFileChooser;
    }

    /**
     *
     * @return file chooser excel
     */
    public JFileChooser getExpotExcelFileChooser() {
        return expotExcelFileChooser;
    }

    /**
     *
     * @return mappa classi
     */
    public Map<String, Integer> getClasses() {
        return classes;
    }

    /**
     *
     * @return file chooser per esportare i pattern
     */
    public JFileChooser getExpotPatternsFileChooser() {
        return expotPatternsFileChooser;
    }

    /**
     *
     * @return file chooser per esportare una tabella
     */
    public JFileChooser getExpotTableFileChooser() {
        return expotTableFileChooser;
    }

    /**
     *
     * @return pulsante per segmentare tutti i files
     */
    public JButton getFilesPanelSegmenta() {
        return filesPanelSegmenta;
    }

    /**
     *
     * @return pulsante per filtrare i documenti classificati solo al primo
     * livello
     */
    public JButton getFirstLevelOnly() {
        return firstLevelOnly;
    }

    /**
     *
     * @return check box
     */
    public JCheckBox getjCheckBox1() {
        return jCheckBox1;
    }

    /**
     *
     * @return flag per dire se un segmento è multiplo
     */
    public JComboBox<String> getMultipleYN() {
        return multipleYN;
    }

    /**
     *
     * @return pulsante per aggiungere una definizione
     */
    public JButton getNewDefinition() {
        return newDefinition;
    }

    /**
     *
     * @return pulsante per filtrare i non marcati
     */
    public JButton getNotMarked() {
        return notMarked;
    }

    /**
     *
     * @return pulante per il tag cloud
     */
    public JButton getNuvoletta() {
        return nuvoletta;
    }

    /**
     *
     * @return checkbox per l'inizializzazione dei soli segmenti
     */
    public JCheckBox getOnlySegment() {
        return onlySegment;
    }

    /**
     *
     * @return file chooser
     */
    public JFileChooser getP1IndexFileChooser() {
        return p1IndexFileChooser;
    }

    /**
     *
     * @return file chooser per l'indice
     */
    public JFileChooser getP2IndexFileChooser() {
        return p2IndexFileChooser;
    }

    /**
     *
     * @return nome del segmento
     */
    public JLabel getSegmentName() {
        return segmentName;
    }

    /**
     *
     * @return pulsante per aprire il tag cloud
     */
    public JButton getTagCloud() {
        return tagCloud;
    }

    /**
     *
     * @return pulsante per testare il pattern di cattura
     */
    public JButton getTestCapturePattern() {
        return testCapturePattern;
    }

    /**
     *
     * @return pulsante per verificare se la definizione fa match
     */
    public JButton getTestDefinitionMatch() {
        return testDefinitionMatch;
    }

    /**
     *
     * @return pulsante per testare la regex di definizione
     */
    public JButton getTestDefinitionRegex() {
        return testDefinitionRegex;
    }

    /**
     *
     * @return pulsante per testare un pattern
     */
    public JButton getTestSegmentPattern() {
        return testSegmentPattern;
    }

    /**
     *
     * @return pulsante per filtrare i documenti classificati con uno score
     * sottosoglia
     */
    public JButton getUnderTreshold() {
        return underTreshold;
    }

    /**
     *
     * @return se flaggato usa i nomi delle categorie come istruzione
     */
    public JCheckBox getUsaCategorie() {
        return usaCategorie;
    }

    /**
     *
     * @return pulsante segmenta
     */
    public JButton getSegmenta() {
        return segmenta;
    }

    /**
     *
     * @return pulsante segmenta
     */
    public JButton getSegmenta1() {
        return segmenta1;
    }

    /**
     *
     * @return pulsante segmenta cartella
     */
    public JButton getSegmentaCartella() {
        return segmentaCartella;
    }

    /**
     *
     * @return pulsante segmenta
     */
    public JButton getSegmentaEBasta() {
        return segmentaEBasta;
    }

    /**
     *
     * @return pulsante segmenta e classifica
     */
    public JButton getSegmentaEClassifica() {
        return segmentaEClassifica;
    }

    /**
     *
     * @return tabella dei files
     */
    public JButton getSegmentaFile() {
        return segmentaFile;
    }

    /**
     *
     * @return tabella dei segmenti
     */
    public JTable getSegmentsTable() {
        return segmentsTable;
    }

    /**
     *
     * @return dialog per la selezione del csv
     */
    public JDialog getSelectCSVDataProvider() {
        return selectCSVDataProvider;
    }

    /**
     *
     * @return boolean che dice se il processo si deve fermare
     */
    public FinalBoolean getStopSegmentAndClassify() {
        return stopSegmentAndClassify;
    }

    /**
     *
     * @return status della tabella di gestione indice
     */
    public JLabel getStatusGestioneIndice() {
        return statusGestioneIndice;
    }

    /**
     *
     * @return status della tabella segmenti
     */
    public JLabel getStatusSegments() {
        return statusSegments;
    }

    /**
     *
     * @return formato cattura
     */
    public JTextField getCaptureFormat() {
        return captureFormat;
    }

    /**
     *
     * @return target cattura
     */
    public JComboBox<String> getCaptureTarget() {
        return captureTarget;
    }

    /**
     *
     * @return tipo cattura
     */
    public JComboBox<String> getCaptureType() {
        return captureType;
    }

    /**
     *
     * @return nome della cattura
     */
    public JLabel getCaptureName() {
        return captureName;
    }

    /**
     *
     * @return pulsante per identificare un pattern di classificazione
     */
    public JButton getClassifyPattern() {
        return classifyPattern;
    }

    /**
     *
     * @return check box che identifica se una cattura è temporanea
     */
    public JCheckBox getTempCapture() {
        return tempCapture;
    }

    /**
     *
     * @return pulsante per aprire la relazione con il segmento
     */
    public JButton getOpenSegmentRelationshipPanel() {
        return openSegmentRelationshipPanel;
    }

    /**
     *
     * @return tabella cache dei SemDocument
     */
    public Map<Integer, SemDocument> getTableData() {
        return tableData;
    }

    /**
     *
     * @param tableMap imposta la cache dei documenti
     */
    public void setTableData(Map<Integer, SemDocument> tableMap) {
        this.tableData = tableMap;
    }

    /**
     *
     * @return delimitatore del csv del dataprovider
     */
    public JTextField getDpDelimitatore() {
        return dpDelimitatore;
    }

    /**
     *
     * @return carattere di escaping del csv del dataprovider
     */
    public JTextField getDpEscape() {
        return dpEscape;
    }

    /**
     *
     * @return nome del dataprovider
     */
    public JLabel getDpName() {
        return dpName;
    }

    /**
     *
     * @return carattere di quoting del csv
     *
     */
    public JTextField getDpQuote() {
        return dpQuote;
    }

    /**
     *
     * @return tipo del dataprovider
     */
    public JComboBox<String> getDpType() {
        return dpType;
    }

    /**
     *
     * @return combo box per associare una cattura ad un field nella relazione
     * di dataprovider
     */
    public JComboBox<String> getDprCapture() {
        return dprCapture;
    }

    /**
     *
     * @return check box che identifica che un field deve essere importato se la
     * chiave matcha
     */
    public JCheckBox getDprEnrich() {
        return dprEnrich;
    }

    /**
     *
     * @return nome del field della relazione del dataprovider
     */
    public JLabel getDprFieldName() {
        return dprFieldName;
    }

    /**
     *
     * @return check box identifica se un field del dataprovider è chiave nella
     * relazione
     */
    public JCheckBox getDprKey() {
        return dprKey;
    }

    /**
     *
     * @return nome della relazione del dataprovider
     */
    public JLabel getDprName() {
        return dprName;
    }

    /**
     *
     * @return check box per identificare se una relazione ha la priorità sulle
     * catture già valorizzate
     */
    public JCheckBox getDprPriority() {
        return dprPriority;
    }

    /**
     *
     * @return pulsante salva relazione
     */
    public JButton getDprSave() {
        return dprSave;
    }

    /**
     *
     * @return combo box per scegliere il segmento legato ad un dataprovider
     */
    public JComboBox<String> getDprSegment() {
        return dprSegment;
    }

    /**
     *
     * @return tabella di relazione del dataprovider
     */
    public JTable getDprTable() {
        return dprTable;
    }

    /**
     *
     * @return check box per dire che una tabella è alimentata da dataprovider
     */
    public JCheckBox getFromDataProvider() {
        return fromDataProvider;
    }

    /**
     *
     * @return pulsante per aggiungere un record ad una tabella
     */
    public JButton getTableAddRecord() {
        return tableAddRecord;
    }

    /**
     *
     * @return pulsante per cancellare un record
     */
    public JButton getTableDeleteRecord() {
        return tableDeleteRecord;
    }

    /**
     *
     * @return pulsante per esportare una tabella
     */
    public JButton getTableExport() {
        return tableExport;
    }

    /**
     *
     * @return pulsate per importare una tabella
     */
    public JButton getTableImport() {
        return tableImport;
    }

    /**
     *
     * @return pulsante per rimuovere il filtro su una tabella
     */
    public JButton getRemoveTableFilter() {
        return removeTableFilter;
    }

    /**
     *
     * @return label dello status delle definizioni
     */
    public JLabel getDefinitionStatus() {
        return definitionStatus;
    }

    /**
     * resetta il filtro sulla tabella dei files
     */
    public void resetFilesFilters() {
        GuiUtils.filterTable(filesTable, null, 4);
    }

    private void segmentPatternsTableAction() {
        SegmentsUtils.segmentPatternsTableAction(this);
    }

    private void changeDocument(TableCellListener tcl) {
        LuceneIndexUtils.changeIndexDocument(tcl, this);
    }

    /**
     *
     * @return ritorna il contenuto della label di status del pattern
     */
    public String getSegmentPatternStatusText() {
        return segmentPatternStatus.getText();
    }

    /**
     *
     * @return ritorna la label di status del segment pattern
     */
    public JLabel getSegmentPatternStatus() {
        return segmentPatternStatus;
    }

    /**
     *
     * @return checkbox che identifica una cattura come end period
     */
    public JCheckBox getEndTimeInterval() {
        return endTimeInterval;
    }

    /**
     *
     * @return checkbox che identifica una cattura come start period
     */
    public JCheckBox getStartTimeInterval() {
        return startTimeInterval;
    }

    /**
     *
     * @return pulsante per confermare il field per il dataprovider
     */
    public JButton getConfirmDpField() {
        return confirmDpField;
    }

    /**
     *
     * @return ritorna la label dove viene inserito l'id del field del
     * dataprovider
     */
    public JLabel getDpFieldId() {
        return dpFieldId;
    }

    /**
     *
     * @return textfield con il nome del dataprovider
     */
    public JTextField getDpFieldName() {
        return dpFieldName;
    }

    /**
     *
     * @return Spinner del dataprovider field
     */
    public JSpinner getDpFieldPosition() {
        return dpFieldPosition;
    }

    /**
     *
     * @return combo box dove si definisce la relazione tra field e tabella
     */
    public JComboBox<String> getDpFieldTableRelationship() {
        return dpFieldTableRelationship;
    }

    /**
     *
     * @return combo box dove si definiscono i tipi di field del dataprovider
     */
    public JComboBox<String> getDpFieldType() {
        return dpFieldType;
    }

    /**
     *
     * @return pannello dei fields
     */
    public JPanel getDpFieldsPanel() {
        return dpFieldsPanel;
    }

    /**
     *
     * @return tabella dei fields del dataprovider
     */
    public JTable getDpFieldsTable() {
        return dpFieldsTable;
    }

    /**
     *
     * @return toolbar del dataprovider
     */
    public JToolBar getDpFieldsToolbar() {
        return dpFieldsToolbar;
    }

    /**
     *
     * @return casella contenente il nome del file con cui si vuole alimentare
     * il dataprovider
     */
    public JTextField getDpFileName() {
        return dpFileName;
    }

    /**
     *
     * @return textfield per la configurazione del separatore di linea nei data
     * provider
     */
    public JTextField getDpLineSep() {
        return dpLineSep;
    }

    /**
     *
     * @return pulsante per salvare un pattern di segmento
     */
    public JButton getConfirmSegmentPattern() {
        return confirmSegmentPattern;
    }

    /**
     *
     * @return pulsante per selezionare un excel
     */
    public JButton getSelezionaExcel() {
        return selezionaExcel;
    }

    /**
     *
     * @return pulsante per selezionare la cartella della struttura
     */
    public JButton getSelezionaIndice() {
        return selezionaIndice;
    }

    /**
     *
     * @return pulsante per selezionare la cartella dell'indice di istruzione
     */
    public JButton getSelezionaIndiceIstruzione() {
        return selezionaIndiceIstruzione;
    }

    /**
     *
     * @return pulsante per selezionare le stopwords
     */
    public JButton getSelezionaStop() {
        return selezionaStop;
    }

    /**
     *
     * @return pulsante per far partire la costruzione dell'indice
     */
    public JButton getStartBuildIndex() {
        return startBuildIndex;
    }

    /**
     *
     * @return Finestra di dialogo per la definizione delle relazioni tra
     * cattura (globale) e segmento
     */
    public JDialog getGlobalCapturesSegmentsRelationship() {
        return globalCapturesSegmentsRelationship;
    }

    /**
     *
     * @return pannello immagini
     */
    public JPanel getImagesPanel() {
        return imagesPanel;
    }

    /**
     *
     * @param rtt Processo che legge da una cartella in tabella
     */
    public void setRtt(ReadFolderToTable rtt) {
        this.rtt = rtt;
    }

    /**
     *
     * @return Tabella dei segmenti
     */
    public JTable getSegmentPatternsTable() {
        return segmentPatternsTable;
    }

    /**
     *
     * @return Spinner per la cattura
     */
    public JSpinner getCapturePatternSpinner() {
        return capturePatternSpinner;
    }

    /**
     *
     * @return label che contiene lo stato di un pattern di cattura
     */
    public JLabel getCapturePatternStatus() {
        return capturePatternStatus;
    }

    /**
     *
     * @return pulsante per confermare un pattern di cattura
     */
    public JButton getConfirmCapturePattern() {
        return confirmCapturePattern;
    }

    /**
     *
     * @return pulsante per confermare la definizione di un pattern
     */
    public JButton getConfirmDefinitionPattern() {
        return confirmDefinitionPattern;
    }

    /**
     *
     * @return Pulsante per testare se la cattura matcha
     */
    public JButton getTestCaptureMatch() {
        return testCaptureMatch;
    }

    //Metodi specifici
    /**
     * Inizializza il modello
     */
    public void initializeModel() {
        isInit = false;
        cc.updateConfiguration(percorsoIndice.getText(), soglia.getText(), fattoreK.getText(), getSegmentsPath(), lastFolder, String.valueOf(linguaAnalizzatoreIstruzione.getSelectedItem()), learningFactor.getText(), percorsoOCR.getText());
        //LogGui.setjTextArea(logInizializzazione);
        logInizializzazione.setEditable(false);
        logInizializzazione.setEnabled(true);
        logInizializzazione.setText("");
        jButton2.setEnabled(false);
        //Abilita i testi...
        boolean ok = false;
        try {
            LogGui.printMemorySummary();
            if (!onlySegment.isSelected()) {
                LogGui.info("INIT MULTICLASSIFIER...");
                initLabel.setText("Init multiclassifier...");
                if (ME.init(percorsoIndice.getText(), Integer.parseInt(fattoreK.getText()), rebuildIndex.isSelected())) {
                    StopWordsUtils.populateStopWords(this);
                    LuceneIndexUtils.populateIndex(this);
                    etichettaAlberoSegmenti.setText("Albero - Categorie: " + ME.getCats().size());
                    ok = true;
                    classificaTesto.setEnabled(true);
                    jButton3.setEnabled(true);
                    batch.setEnabled(true);

                }
            }
            LogGui.printMemorySummary();
            initLabel.setText("Init segmenter...");
            LogGui.info("INIT SEGMENTER...");
            if (SE.init(getSegmentsPath())) {
                ok = ok && true;
                segmenta.setEnabled(true);
                nuvoletta.setEnabled(true);
                segmentaEClassifica.setEnabled(true);
                tagCloud.setEnabled(true);
                filesPanelSegmenta.setEnabled(true);
                segmentaFile.setEnabled(true);
                segmentaCartella.setEnabled(true);
                segmentaEBasta.setEnabled(true);
                resetSegmenta.setEnabled(true);
                modelTree.setModel(SE.getVisualStructure());
                saveModel.setEnabled(true);
                dprSegment.removeAllItems();
                List<String> segNames = me.getSegmentsNames((DefaultMutableTreeNode) modelTree.getModel().getRoot());
                for (String segment : segNames) {
                    dprSegment.addItem(segment);
                }
            }
            initLabel.setText("Build classification tree...");
            LogGui.info("Build classification tree...");
            NodeData root = ME.getRoot(); // Disegna l'albero
            javax.swing.tree.DefaultMutableTreeNode clResults = new javax.swing.tree.DefaultMutableTreeNode("Classificazione");
            GuiUtils.paintTree(root, clResults);
            classificationTree.setModel(new javax.swing.tree.DefaultTreeModel(clResults));
            classificationTree1.setModel(new javax.swing.tree.DefaultTreeModel(clResults));
            manageClassificationTree.setModel(new javax.swing.tree.DefaultTreeModel(clResults));
            categorieSegmentsPanel.setModel(new javax.swing.tree.DefaultTreeModel(clResults));
            initLabel.setText("");
            GuiUtils.runGarbageCollection();
            LogGui.info("END INIT!!!");
            configurationDialog.setVisible(false);
            isInit = true;
            needUpdate = false;
            classStartLevel.setModel(getClassTreeDepth());
            classStartLevel.setSelectedIndex(root.getStartLevel() - 1);

        } catch (Exception e) {
            LogGui.printException(e);
        }
        jButton2.setEnabled(true);
        LogGui.printMemorySummary();
    }

    /**
     * Aggiorna l'ultima cartella aperta
     *
     * @param absolutePath percorso assoluto dell'ultima cartella usata
     */
    public void updateLastSelectFolder(String absolutePath) {
        lastFolder = absolutePath;
        cc.updateConfiguration(percorsoIndice.getText(), soglia.getText(), fattoreK.getText(), getSegmentsPath(), lastFolder, String.valueOf(linguaAnalizzatoreIstruzione.getSelectedItem()), learningFactor.getText(), percorsoOCR.getText());
    }

    /**
     * Aggiorna le statistiche
     */
    public void updateStats() {
        filesTab.setTitleAt(0, "Storage (" + filesTable.getRowCount() + ")");
        filesTab.setTitleAt(1, "Segmenti (" + segmentsTable.getRowCount() + ")");
        statusSegments.setText("Totale elementi: " + segmentsTable.getRowCount());
        filesTab.setTitleAt(2, "Cambiamenti (" + changedTable.getRowCount() + ")");
    }

    private void segmentTreeSetText() {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) segmentTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        if (node.getUserObject() instanceof SegmentationUtils.TextNode) {
            SegmentationUtils.TextNode txt = (SegmentationUtils.TextNode) node.getUserObject();
            segmentTextArea.setText(txt.getText());
            segmentTextArea.setCaretPosition(0);
            Enumeration en = node.getParent().children();
            while (en.hasMoreElements()) {
                DefaultMutableTreeNode nx = (DefaultMutableTreeNode) en.nextElement();
                if ("Classificazione".equals(nx.getUserObject())) {
                    if (!testo.getText().equals(txt.getText())) {
                        testo.setText(txt.getText());
                        testo.setCaretPosition(0);
                        segmentaEClassificaActionPerformed(null);
                    }
                    break;
                }
            }

        }
    }

    private void doDockModelEditor() {
        jButton8.setToolTipText("UnDock");
        modelEditorFrame.setVisible(false);
        modelEditorFrame.getContentPane().remove(modelEditorContainer);
        filesTab.add(modelEditorContainer, 6);
        filesTab.setTitleAt(6, "Model Editor");
        modelEditorContainer.requestFocus();
    }

    /**
     * Evento quando si cerca nel documento
     */
    public void serachDocumentBodyKeyReleased() {
        serachDocumentBodyKeyReleased(null);
    }

    private void doReadClassifyWriteExcel(ActionEvent evt) {
        selectExcelFileClass.setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            batch.setEnabled(false);
            final String fileName = excelFileChooserClass.getSelectedFile().getAbsolutePath();
            updateLastSelectFolder(fileName);
            //LogGui.setjTextArea(logInizializzazione);
            logInizializzazione.setEditable(false);
            logInizializzazione.setEnabled(true);
            logInizializzazione.setText("");
            logInizializzazione.requestFocus();
            Thread t = new Thread(() -> {
                ReadClassifyWrite rpw = new ReadClassifyWrite(Integer.parseInt((String) processori.getSelectedItem()));
                rpw.process(fileName, colonnaDescrizione.getSelectedIndex(), ME, DP, Double.parseDouble(soglia.getText()));
                batch.setEnabled(true);
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private void doFileSegmentation(ActionEvent evt) {
        selectFileToSegment.setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            final File file = segmentFileChooser1.getSelectedFile();
            updateLastSelectFolder(file.getAbsolutePath());
            Thread t = new Thread(() -> {
                try {
                    testoDaSegmentare.setText("");
                    htmlResult.setContentType("text/html");
                    htmlResult.setText("Elaborazione in corso...");
                    htmlTimeline.setContentType("text/html");
                    htmlTimeline.setText("");
                    htmlResult.setText("");
                    segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(new DefaultMutableTreeNode("Segmentazione in corso...")));
                    segmentTextArea.setText("");
                    imagesPanel.removeAll();
                    String text = DP.getTextFromFile(file, percorsoOCR.getText());
                    String html = DP.getHtmlFromFile(file);
                    BufferedImage image = DP.getLargestImageFromFile(file);
                    if (image != null) {
                        ImagePanel pl = new ImagePanel(image);
                        imagesPanel.add(pl);
                        Dimension size = new Dimension(image.getWidth(), image.getHeight());
                        imagesPanel.setPreferredSize(size);
                        imagesPanel.setMinimumSize(size);
                        imagesPanel.setMaximumSize(size);
                        imagesPanel.setSize(size);
                    }

                    String language = DP.getLanguageFromText(text);
                    imagesPanel.repaint();
                    testoDaSegmentare.setText(text);
                    htmlFormatted.setText(html);
                    Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = SE.getSegments(text, ME, Double.parseDouble(soglia.getText()), language);
                    htmlResult.setText(SegmentationUtils.getHtml(identifiedSegments, language));
                    htmlTimeline.setText(SegmentationUtils.getHtmlDurations(identifiedSegments));
                    segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(SegmentationUtils.getJTree(new DefaultMutableTreeNode("Segmentazione"), identifiedSegments, language)));
                    htmlResult.setCaretPosition(0);
                    htmlTimeline.setCaretPosition(0);
                    testoDaSegmentare.setCaretPosition(0);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            });
            t.setDaemon(true);
            t.start();

        }
    }

    /**
     * Fa la segmentazione di un documento della tabella Files
     *
     * @param text testo da segmentare
     * @param id id nella cache dei SemDocument
     * @param currentFilesPosition posizione nella JTable
     */
    public void doDocumentSegmentation(String text, int id, int currentFilesPosition) {
        testoDaSegmentare.setText(text);
        Thread t = new Thread(() -> {
            synchronized (lockSync) {
                htmlResult.setContentType("text/html");
                filesPanelHtml.setContentType("text/html");
                htmlResult.setText("Elaborazione in corso...");
                filesPanelHtml.setText("Elaborazione in corso...");
                htmlTimeline.setText("");
                segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(new DefaultMutableTreeNode("Segmentazione in corso...")));
                filesPanelSegmentTree.setModel(new javax.swing.tree.DefaultTreeModel(new DefaultMutableTreeNode("Segmentazione in corso...")));
                segmentTextArea.setText("");
                imagesPanel.removeAll();
                try {
                    Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments;
                    SemDocument dto = tableData.get(id);
                    SemDocument old = dto.clone();
                    String language = dto.getLanguage();
                    DefaultTableModel model = (DefaultTableModel) segmentsTable.getModel();
                    if (dto.getIdentifiedSegments() != null) {
                        Set<String> segToDelete = new HashSet<>();
                        List<Object[]> sr = dto.getSegmentRows();
                        sr.stream().forEach((s) -> {
                            segToDelete.add((String) s[0]);
                        });
                        int rc = model.getRowCount();
                        for (int i = (rc - 1); i >= 0; i--) {
                            if (segToDelete.contains(model.getValueAt(i, 0))) {
                                model.removeRow(i);
                            }
                        }
                    }
                    identifiedSegments = SE.getSegments(text, ME, Double.parseDouble(soglia.getText()), language);
                    dto.setIdentifiedSegments(identifiedSegments);

                    List<Object[]> rows = dto.getSegmentRows();
                    rows.stream().forEach((row) -> {
                        model.addRow(row);
                    });
                    filesTab.setTitleAt(1, "Segmenti (" + segmentsTable.getRowCount() + ")");
                    String html = SegmentationUtils.getHtml(identifiedSegments, language);
                    filesPanelHtml.setContentType("text/html");
                    htmlResult.setContentType("text/html");
                    htmlResult.setText(html);
                    htmlTimeline.setContentType("text/html");
                    htmlTimeline.setText(SegmentationUtils.getHtmlDurations(identifiedSegments));
                    filesPanelHtml.setText(html);
                    DefaultMutableTreeNode tree = SegmentationUtils.getJTree(new DefaultMutableTreeNode("Segmentazione"), identifiedSegments, language);
                    segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(tree));
                    filesPanelSegmentTree.setModel(new javax.swing.tree.DefaultTreeModel(tree));
                    htmlResult.setCaretPosition(0);
                    htmlTimeline.setCaretPosition(0);
                    filesPanelHtml.setCaretPosition(0);
                    testoDaSegmentare.setCaretPosition(0);
                    Map<String, Integer> stats = new HashMap<>();
                    stats = SegmentationUtils.getFileStats(stats, identifiedSegments, "");
                    FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 3, stats.get("Segments"), this);
                    FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 4, stats.get("ClassSegments"), this);
                    FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 5, stats.get("Captures"), this);
                    FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 6, stats.get("Sentencies"), this);
                    FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 7, stats.get("Classifications"), this);
                    DefaultTableModel dm = (DefaultTableModel) filesPanleCapturesTable.getModel();
                    int rowCount = dm.getRowCount();
                    for (int i = rowCount - 1; i >= 0; i--) {
                        dm.removeRow(i);
                    }
                    dto.getCapturesRows().stream().forEach((row) -> {
                        dm.addRow(row);
                    });
                    ChangedUtils.updateChangedTable(dto, old, this);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
            ChangedUtils.updateChangedTree(this);
        });
        t.setDaemon(true);
        t.start();
    }

    private void doTextClassification() {
        Thread t = new Thread(
                () -> {
                    if (needUpdate) {
                        initializeModel();
                        needUpdate = false;
                    }
                    String text = testo.getText();
                    String language = DP.getLanguageFromText(text);
                    if (text.length() > 0) {
                        token.setText(ME.tokenize(text, language));
                        try {
                            long startBayes = System.currentTimeMillis();
                            List<ClassificationPath> bayes = ME.bayesClassify(text, Double.parseDouble(soglia.getText()), language);
                            long endBayes = System.currentTimeMillis();
                            ClassificationPath knn = ME.knnClassify(text, Double.parseDouble(soglia.getText()), language);
                            long endKnn = System.currentTimeMillis();
                            classificationStatus.setText("");
                            javax.swing.tree.DefaultMutableTreeNode clResults = new javax.swing.tree.DefaultMutableTreeNode("Classificazione");
                            bayes.stream().forEach((cp) -> {
                                javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode(cp.getTechnology());
                                javax.swing.tree.DefaultMutableTreeNode currentNode = treeNode1;
                                for (int i = 0; i < 4; i++) {
                                    String node = cp.getPath()[i];
                                    if (node != null) {
                                        String label = node + "(" + ClassificationPath.df.format(cp.getScore()[i]) + ")";
                                        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode(label);
                                        currentNode.add(treeNode2);
                                        currentNode = treeNode2;
                                    }
                                }
                                clResults.add(treeNode1);
                            });
                            if (knn != null) {
                                javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode(knn.getTechnology());
                                javax.swing.tree.DefaultMutableTreeNode currentNode = treeNode1;
                                for (int i = 0; i < 4; i++) {
                                    String node = knn.getPath()[i];
                                    if (node != null) {
                                        String label = node + "(" + ClassificationPath.df.format(knn.getScore()[i]) + ")";
                                        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode(label);
                                        currentNode.add(treeNode2);
                                        currentNode = treeNode2;
                                    }
                                }
                                clResults.add(treeNode1);
                            }
                            classificationStatus.setText("Language: " + language + " - Bayes time: " + (endBayes - startBayes) + "ms. - KNN time: " + (endKnn - endBayes) + "ms.");
                            classificationResult.setModel(new javax.swing.tree.DefaultTreeModel(clResults));
                            GuiUtils.expandAll(classificationResult);
                        } catch (Exception e) {
                            LogGui.printException(e);
                        }
                    }
                });
        t.setDaemon(true);
        t.start();
    }

    private void changeStopWord(TableCellListener tcl) { //OK
        StopWordsUtils.changeStopWord(tcl, this);
    }

    /**
     * Apre la finestra con il tagcloud
     *
     * @param result risultato del tag clouding
     * @param wordsCount conteggio delle parole
     */
    public void openCloudFrame(TagCloudResults result, int wordsCount) {
        tcp = new TagCloudPanel();
        tcp.setResult(result, wordsCount);
        globalTagCloud.getContentPane().removeAll();
        globalTagCloud.getContentPane().add(tcp, java.awt.BorderLayout.CENTER);
        globalTagCloud.setSize(800, 600);
        tcp.repaint();
        globalTagCloud.setVisible(true);
    }

    /**
     * Azione sul dizionario
     */
    public void dictionaryTableAction() {
        DictionaryUtils.dictionaryTableAction(this);
    }

    /**
     * Ritorna il percorso del file di segment
     *
     * @return percorso del file di segment
     */
    public String getSegmentsPath() {
        return new File(percorsoIndice.getText() + "/segments.xml").getAbsolutePath();
    }

    /**
     * Ricerca sul classTree
     */
    public void classTree1Find() {
        String searched = jTextField2.getText();
        if (searched.length() > 0) {
            List<TreePath> paths = GuiUtils.find((DefaultMutableTreeNode) classificationTree1.getModel().getRoot(), searched, false);
            List<TreePath> p1 = new ArrayList<>();
            if (paths.size() > 0) {
                p1.add(paths.get(0));
            }
            GuiUtils.scrollToPath(classificationTree1, p1);
        }
    }

    /**
     * Aggiorna le statistiche di copertura delle catture
     */
    public void captureCoverageUpdate() {
        capcov = new CapturesCoverage(tableData);
        GuiUtils.clearTable(coverageTable);
        DefaultTableModel model = (DefaultTableModel) coverageTable.getModel();
        capcov.getTableRows().stream().forEach((row) -> {
            model.addRow(row);
        });
    }

    /**
     * Ritorna il percorso dove è installato OCR
     *
     * @return percorso fisico OCR
     */
    public JTextField getPercorsoOCR() {
        return percorsoOCR;

    }

    private void doReadTagWrite() {
        File sourceDir = folderChooser.getSelectedFile();
        updateLastSelectFolder(sourceDir.getAbsolutePath());
        selectFolderToProcess.setVisible(false);
        int processors = processori1.getSelectedIndex() + 1;
        boolean html = salvaHTML.getSelectedIndex() == 0;
        logInizializzazione.setEditable(false);
        logInizializzazione.setEnabled(true);
        logInizializzazione.setText("");
        logInizializzazione.requestFocus();
        Thread t = new Thread(() -> {
            ReadSegmentWrite rtw = new ReadSegmentWrite(processors);
            rtw.process(sourceDir.getAbsolutePath(), DP, SE, ME, Double.parseDouble(soglia.getText()), html, percorsoOCR.getText());
        });
        t.setDaemon(true);
        t.start();
    }

    private void doTextSegmentation() {
        Thread t = new Thread(() -> {
            htmlResult.setContentType("text/html");
            htmlResult.setText("Elaborazione in corso...");
            htmlTimeline.setContentType("text/html");
            htmlTimeline.setText("");
            segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(new DefaultMutableTreeNode("Segmentazione in corso...")));
            segmentTextArea.setText("");
            imagesPanel.removeAll();
            try {
                String text = testoDaSegmentare.getText();
                String language = DP.getLanguageFromText(text);
                Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = SE.getSegments(text, ME, Double.parseDouble(soglia.getText()), language);
                htmlResult.setText(SegmentationUtils.getHtml(identifiedSegments, language));
                htmlTimeline.setText(SegmentationUtils.getHtmlDurations(identifiedSegments));
                segmentTree.setModel(new javax.swing.tree.DefaultTreeModel(SegmentationUtils.getJTree(new DefaultMutableTreeNode("Segmentazione"), identifiedSegments, language)));
                htmlResult.setCaretPosition(0);
                htmlTimeline.setCaretPosition(0);
                testoDaSegmentare.setCaretPosition(0);
            } catch (Exception e) {
                LogGui.printException(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void classTree1MouseEventManagement(MouseEvent evt) {
        int selRow = classificationTree1.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = classificationTree1.getPathForLocation(evt.getX(), evt.getY());
        classificationTree1.setSelectionPath(selPath);
        if (selRow != -1) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) classificationTree1.getLastSelectedPathComponent();
            TreeNode[] path = node.getPath();
            String x = "";
            for (TreeNode t : path) {
                String v = t.toString();
                if (!v.equals("Classificazione")) {
                    x = x.length() == 0 ? v : x + ">" + v;
                }
            }
            catClass.setText(x);
        }
    }

    private void filesTableDelete() {
        FilesAndSegmentsUtils.filesTableDelete(this);
    }

    private void segmentsTableSelectedRow() {
        segmentsTableMouseClicked(null);
    }

    private void fileTableSelectedRow() {
        filesTableMouseClicked(null);
    }

    private void hilightSegment() {
        FilesAndSegmentsUtils.segmentsTableHilightSegment(this);
    }

    private void filesTableSelectedRow() {
        filesTableMouseClicked(null);
    }

    private javax.swing.DefaultComboBoxModel getClassTreeDepth() {
        if (isInit) {
            int depth = getClassDepth();
            String[] lev = new String[depth - 1];
            for (int i = 0; i < lev.length; i++) {
                lev[i] = String.valueOf((i + 1));
            }
            return new javax.swing.DefaultComboBoxModel<>(lev);

        }
        return new javax.swing.DefaultComboBoxModel<>(new String[]{});
    }

    private int getClassDepth() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) manageClassificationTree.getModel().getRoot();
        if (node != null) {
            return node.getDepth();

        }
        return 1;
    }

}
