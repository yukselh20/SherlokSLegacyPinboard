package ui.pinboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.JournalEntryDTO;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PinboardController {

    private final Stage stage;
    private final Pane canvas;
    private final List<PinboardItemModel> items = new ArrayList<>();
    private final List<PinboardLinkModel> links = new ArrayList<>();
    private final Map<String, Node> itemNodeMap = new HashMap<>();
    private final Map<PinboardLinkModel, Line> linkNodeMap = new HashMap<>();
    private final Map<String, TextArea> templateNotesMap = new HashMap<>();
    private final Map<String, VBox> templateDropTargetsMap = new HashMap<>();

    // State
    private boolean isLinkMode = false;
    private String linkStartId = null;
    private PinboardItemModel draggedItem = null;
    private double dragDeltaX, dragDeltaY;

    // UI Components
    private ScrollPane canvasScrollPane;
    private VBox evidenceListVBox;
    private VBox templateVBox;

    private final Set<String> addedJournalEntryIds = new HashSet<>();

    public PinboardController() {
        this.stage = new Stage();
        this.canvas = new Pane();
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStylesheets().add(getClass().getResource("/css/pinboard.css").toExternalForm());
        root.getStyleClass().add("pinboard-root");

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("pinboard-toolbar");

        Button addNoteBtn = new Button("Add Note");
        addNoteBtn.setOnAction(e -> createNoteAtCenter());

        ToggleButton linkModeBtn = new ToggleButton("Link Mode");
        linkModeBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isLinkMode = newVal;
            if (!isLinkMode) {
                linkStartId = null;
            }
        });

        Button clearBtn = new Button("Clear Board");
        clearBtn.setOnAction(e -> clearBoard());

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> savePinboard());

        Button loadBtn = new Button("Load");
        loadBtn.setOnAction(e -> loadPinboard());

        toolBar.getItems().addAll(addNoteBtn, linkModeBtn, new Separator(), clearBtn, new Separator(), saveBtn, loadBtn);
        root.setTop(toolBar);

        // --- Left Panel: Evidence List ---
        evidenceListVBox = new VBox(5);
        evidenceListVBox.setPadding(new Insets(10));
        ScrollPane leftScroll = new ScrollPane(evidenceListVBox);
        leftScroll.setFitToWidth(true);
        leftScroll.setPrefWidth(200);
        leftScroll.getStyleClass().add("pinboard-sidebar");

        VBox leftPanel = new VBox(new Label("Evidence (Drag to Board)"), leftScroll);
        leftPanel.setPadding(new Insets(5));
        root.setLeft(leftPanel);

        // --- Center: Canvas ---
        canvas.getStyleClass().add("pinboard-canvas");
        canvas.setPrefSize(2000, 2000); // Large canvas
        canvas.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });
        canvas.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                // Check if it's from the evidence list or template
                String content = db.getString();
                // Simple format: "TYPE|TITLE|CONTENT|REF_ID"
                String[] parts = content.split("\\|", 4);
                if (parts.length >= 3) {
                    double x = e.getX();
                    double y = e.getY();
                    PinboardItemModel newItem = new PinboardItemModel();
                    newItem.setType(PinboardItemModel.ItemType.valueOf(parts[0]));
                    newItem.setTitle(parts[1]);
                    newItem.setContent(parts[2]);
                    if (parts.length > 3) newItem.setRelatedJournalEntryId(parts[3]);
                    newItem.setX(x);
                    newItem.setY(y);

                    addItemToBoard(newItem);
                    success = true;
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        canvasScrollPane = new ScrollPane(canvas);
        canvasScrollPane.setPannable(true); // Allow panning with mouse drag when not on an item
        root.setCenter(canvasScrollPane);

        // --- Right Panel: Final Exam Template ---
        templateVBox = new VBox(10);
        templateVBox.setPadding(new Insets(10));

        addTemplateSection("Main Suspect(s)");
        addTemplateSection("Primary Motive(s)");
        addTemplateSection("Opportunity / Means");
        addTemplateSection("Weapon");
        addTemplateSection("Key Contradictions");
        addTemplateSection("Supporting Evidence");
        addTemplateSection("Suspicious Behavior");
        addTemplateSection("Alibi Verification");
        addTemplateSection("Remaining Questions");

        ScrollPane rightScroll = new ScrollPane(templateVBox);
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(250);
        rightScroll.getStyleClass().add("pinboard-sidebar");

        VBox rightPanel = new VBox(new Label("Case Summary Template"), rightScroll);
        rightPanel.setPadding(new Insets(5));
        root.setRight(rightPanel);

        Scene scene = new Scene(root, 1200, 900);
        stage.setScene(scene);
        stage.setTitle("Detective Pinboard");

        // Try to load auto-save on startup
        File autoSave = getSaveFile();
        if (autoSave.exists()) {
            loadPinboardFromFile(autoSave);
        }
    }

    private File getSaveFile() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, ".detective_game");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "pinboard_autosave.json");
    }

    private void addTemplateSection(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Type notes here...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        templateNotesMap.put(title, notesArea);

        // Drop target for evidence
        VBox dropTarget = new VBox(5);
        dropTarget.setStyle("-fx-border-color: #555; -fx-border-style: dashed; -fx-padding: 5; -fx-min-height: 40;");
        Label dropLabel = new Label("Drop Evidence Here");
        dropLabel.setTextFill(Color.GRAY);
        dropTarget.getChildren().add(dropLabel);
        dropTarget.setAlignment(javafx.geometry.Pos.CENTER);

        templateDropTargetsMap.put(title, dropTarget);

        dropTarget.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            e.consume();
        });

        dropTarget.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                String[] parts = db.getString().split("\\|", 3);
                if (parts.length >= 2) {
                    Label itemLabel = new Label("• " + parts[1]);
                    itemLabel.setTooltip(new Tooltip(parts.length > 2 ? parts[2] : ""));
                    itemLabel.setTextFill(Color.LIGHTGRAY);
                    dropTarget.getChildren().add(itemLabel);
                    dropLabel.setVisible(false);
                    e.setDropCompleted(true);
                }
            }
            e.consume();
        });

        VBox sectionBox = new VBox(2, header, notesArea, dropTarget);
        templateVBox.getChildren().add(sectionBox);
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void reset() {
        clearBoard();
        evidenceListVBox.getChildren().clear();
        addedJournalEntryIds.clear();
        // Clear template notes manually if we want to reset them too?
        // For now, clearBoard only clears the canvas items.
        // A full reset might need to clear the right panel text areas too.
    }

    private void clearBoard() {
        items.clear();
        links.clear();
        canvas.getChildren().clear();
        itemNodeMap.clear();
        linkNodeMap.clear();
    }

    public void addJournalEntry(JournalEntryDTO entry) {
        // Prevent duplicates
        // Use hash of text + timestamp as ID
        String refId = String.valueOf(Objects.hash(entry.getText(), entry.getTimestamp()));
        if (addedJournalEntryIds.contains(refId)) {
            return;
        }
        addedJournalEntryIds.add(refId);

        // Smart Title Generation
        String text = entry.getText();
        String smartTitle = "Journal Entry";
        if (text.toLowerCase().contains("ask") || text.toLowerCase().contains("question")) {
            smartTitle = "Questioning";
            // Try to extract name? For now just generic type.
        } else if (text.toLowerCase().contains("deduce") || text.toLowerCase().contains("deduction")) {
            smartTitle = "Deduction";
        } else if (text.toLowerCase().contains("found") || text.toLowerCase().contains("examine")) {
            smartTitle = "Evidence";
        }

        // Create a draggable card in the left sidebar
        HBox card = new HBox();
        card.getStyleClass().add("pinboard-item");
        card.setStyle("-fx-background-color: #e0e0e0; -fx-cursor: hand;");

        VBox contentBox = new VBox(2);
        Label title = new Label(smartTitle);
        title.getStyleClass().add("pinboard-item-title");
        Label content = new Label(text);
        content.getStyleClass().add("pinboard-item-content");
        content.setMaxWidth(160);
        content.setWrapText(true);

        contentBox.getChildren().addAll(title, content);
        card.getChildren().add(contentBox);

        // Drag source
        final String finalSmartTitle = smartTitle;
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            // Format: TYPE|TITLE|CONTENT|REF_ID
            cc.putString("EVIDENCE|" + finalSmartTitle + "|" + text + "|" + refId);
            db.setContent(cc);
            e.consume();
        });

        evidenceListVBox.getChildren().add(0, card); // Add to top
    }

    private void createNoteAtCenter() {
        double x = Math.abs(canvasScrollPane.getViewportBounds().getMinX()) + 100;
        double y = Math.abs(canvasScrollPane.getViewportBounds().getMinY()) + 100;

        PinboardItemModel note = new PinboardItemModel();
        note.setType(PinboardItemModel.ItemType.NOTE);
        note.setTitle("New Note");
        note.setContent("Double click to edit");
        note.setX(x);
        note.setY(y);
        note.setColor("#fdfd96");

        addItemToBoard(note);
    }

    private void addItemToBoard(PinboardItemModel item) {
        items.add(item);
        Node node = createItemNode(item);
        itemNodeMap.put(item.getId(), node);
        canvas.getChildren().add(node);
        node.setLayoutX(item.getX());
        node.setLayoutY(item.getY());
    }

    private Node createItemNode(PinboardItemModel item) {
        VBox box = new VBox(2);
        box.setPrefSize(item.getWidth(), item.getHeight());
        box.setMinSize(100, 80); // Minimum size
        box.getStyleClass().add("pinboard-item");
        box.setStyle("-fx-background-color: " + item.getColor() + ";");

        TextField titleField = new TextField(item.getTitle());
        titleField.getStyleClass().add("pinboard-item-title");
        titleField.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        TextArea contentArea = new TextArea(item.getContent());
        contentArea.getStyleClass().add("pinboard-item-content");
        contentArea.setWrapText(true);
        contentArea.setEditable(true);
        contentArea.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-text-fill: black;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Resize Handle
        Label resizeHandle = new Label("◢");
        resizeHandle.setTextFill(Color.GRAY);
        resizeHandle.setStyle("-fx-cursor: se-resize; -fx-font-size: 10px;");
        resizeHandle.setAlignment(Pos.BOTTOM_RIGHT);
        resizeHandle.setMaxWidth(Double.MAX_VALUE);

        resizeHandle.setOnMouseDragged(e -> {
            double newWidth = Math.max(100, e.getX() + resizeHandle.getBoundsInParent().getMinX()); // Simplified
            // Better approach: calculate delta from mouse
            // But since handle is inside, let's use scene coordinates
        });

        // Re-implement resize correctly using wrapper or event filter
        // A simpler way for VBox:
        HBox bottomBar = new HBox(resizeHandle);
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBar.setPadding(new Insets(0, 2, 0, 0));

        resizeHandle.setOnMousePressed(e -> {
            e.consume(); // Prevent drag of parent
        });
        resizeHandle.setOnMouseDragged(e -> {
            // This logic is tricky because handle moves.
            // Let's use the box's layout
            double newW = Math.max(100, e.getSceneX() - box.localToScene(0,0).getX());
            double newH = Math.max(80, e.getSceneY() - box.localToScene(0,0).getY());
            box.setPrefSize(newW, newH);
            item.setWidth(newW);
            item.setHeight(newH);
            updateLinks(item); // Force redraw of links
            e.consume();
        });

        // Update model on change
        titleField.textProperty().addListener((obs, o, n) -> item.setTitle(n));
        contentArea.textProperty().addListener((obs, o, n) -> item.setContent(n));

        box.getChildren().addAll(titleField, contentArea, bottomBar);

        // Dragging logic on the canvas
        makeDraggable(box, item);

        // Link logic
        box.setOnMouseClicked(e -> {
            if (isLinkMode) {
                handleLinkClick(item);
            } else if (e.getClickCount() == 2 && e.getButton() == MouseButton.SECONDARY) {
                // Remove item?
                removeItem(item);
            }
        });

        return box;
    }

    private void makeDraggable(Node node, PinboardItemModel item) {
        node.setOnMousePressed(e -> {
            if (!isLinkMode) {
                dragDeltaX = node.getLayoutX() - e.getSceneX();
                dragDeltaY = node.getLayoutY() - e.getSceneY();
                node.toFront();
                e.consume();
            }
        });

        node.setOnMouseDragged(e -> {
            if (!isLinkMode) {
                double newX = e.getSceneX() + dragDeltaX;
                double newY = e.getSceneY() + dragDeltaY;
                node.setLayoutX(newX);
                node.setLayoutY(newY);
                item.setX(newX);
                item.setY(newY);
                updateLinks(item);
                e.consume();
            }
        });
    }

    private void handleLinkClick(PinboardItemModel item) {
        if (linkStartId == null) {
            linkStartId = item.getId();
            // Highlight start node (optional)
        } else {
            if (!linkStartId.equals(item.getId())) {
                createLink(linkStartId, item.getId());
            }
            linkStartId = null;
        }
    }

    private void createLink(String startId, String endId) {
        // Check if link exists
        for (PinboardLinkModel link : links) {
            if ((link.getStartItemId().equals(startId) && link.getEndItemId().equals(endId)) ||
                (link.getStartItemId().equals(endId) && link.getEndItemId().equals(startId))) {
                return; // Link exists
            }
        }

        PinboardLinkModel link = new PinboardLinkModel(startId, endId);
        links.add(link);
        drawLink(link);
    }

    private void drawLink(PinboardLinkModel link) {
        Node startNode = itemNodeMap.get(link.getStartItemId());
        Node endNode = itemNodeMap.get(link.getEndItemId());

        if (startNode instanceof Region && endNode instanceof Region) {
            Region startRegion = (Region) startNode;
            Region endRegion = (Region) endNode;

            Line line = new Line();
            line.setStroke(Color.RED);
            line.setStrokeWidth(2);
            // Bind coordinates
            line.startXProperty().bind(startRegion.layoutXProperty().add(startRegion.widthProperty().divide(2)));
            line.startYProperty().bind(startRegion.layoutYProperty().add(startRegion.heightProperty().divide(2)));
            line.endXProperty().bind(endRegion.layoutXProperty().add(endRegion.widthProperty().divide(2)));
            line.endYProperty().bind(endRegion.layoutYProperty().add(endRegion.heightProperty().divide(2)));

            // Allow right click to remove link
            line.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    removeLink(link);
                }
            });

            linkNodeMap.put(link, line);
            canvas.getChildren().add(0, line); // Add behind items
        }
    }

    private void updateLinks(PinboardItemModel item) {
        // JavaFX bindings handle this automatically!
    }

    private void removeItem(PinboardItemModel item) {
        items.remove(item);
        Node node = itemNodeMap.remove(item.getId());
        canvas.getChildren().remove(node);

        // Remove associated links
        List<PinboardLinkModel> toRemove = links.stream()
                .filter(l -> l.getStartItemId().equals(item.getId()) || l.getEndItemId().equals(item.getId()))
                .collect(Collectors.toList());

        toRemove.forEach(this::removeLink);
    }

    private void removeLink(PinboardLinkModel link) {
        links.remove(link);
        Line line = linkNodeMap.remove(link);
        canvas.getChildren().remove(line);
    }

    private void savePinboard() {
        PinboardModel model = new PinboardModel();
        model.setItems(items);
        model.setLinks(links);

        // Save template notes
        Map<String, String> tData = new HashMap<>();
        for (Map.Entry<String, TextArea> entry : templateNotesMap.entrySet()) {
            tData.put(entry.getKey(), entry.getValue().getText());
        }
        model.setTemplateData(tData);

        // Save template dropped items
        Map<String, List<String>> droppedItemsMap = new HashMap<>();
        for (Map.Entry<String, VBox> entry : templateDropTargetsMap.entrySet()) {
            List<String> items = new ArrayList<>();
            for (Node child : entry.getValue().getChildren()) {
                if (child instanceof Label && ((Label) child).getText().startsWith("• ")) {
                    items.add(((Label) child).getText().substring(2) + "|" + ((Label) child).getTooltip().getText());
                }
            }
            droppedItemsMap.put(entry.getKey(), items);
        }
        model.setTemplateDroppedItems(droppedItemsMap);

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(getSaveFile(), model);
            System.out.println("Pinboard saved to " + getSaveFile().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPinboard() {
        loadPinboardFromFile(getSaveFile());
    }

    private void loadPinboardFromFile(File file) {
        if (!file.exists()) return;

        ObjectMapper mapper = new ObjectMapper();
        try {
            PinboardModel model = mapper.readValue(file, PinboardModel.class);
            clearBoard();

            for (PinboardItemModel item : model.getItems()) {
                addItemToBoard(item);
            }

            // Re-create links after all items are added
            for (PinboardLinkModel link : model.getLinks()) {
                links.add(link);
                drawLink(link);
            }

            // Restore template notes
            if (model.getTemplateData() != null) {
                for (Map.Entry<String, String> entry : model.getTemplateData().entrySet()) {
                    TextArea area = templateNotesMap.get(entry.getKey());
                    if (area != null) {
                        area.setText(entry.getValue());
                    }
                }
            }

            // Restore dropped items
            if (model.getTemplateDroppedItems() != null) {
                for (Map.Entry<String, List<String>> entry : model.getTemplateDroppedItems().entrySet()) {
                    VBox target = templateDropTargetsMap.get(entry.getKey());
                    if (target != null) {
                        // Hide placeholder
                        target.getChildren().forEach(n -> {
                            if (n instanceof Label && "Drop Evidence Here".equals(((Label) n).getText())) {
                                n.setVisible(false);
                            }
                        });

                        for (String itemStr : entry.getValue()) {
                            String[] parts = itemStr.split("\\|", 2);
                            String text = parts[0];
                            String tooltip = parts.length > 1 ? parts[1] : "";

                            Label itemLabel = new Label("• " + text);
                            itemLabel.setTooltip(new Tooltip(tooltip));
                            itemLabel.setTextFill(Color.LIGHTGRAY);
                            target.getChildren().add(itemLabel);
                        }
                    }
                }
            }

            System.out.println("Pinboard loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
