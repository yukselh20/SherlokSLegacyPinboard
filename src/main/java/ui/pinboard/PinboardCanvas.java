package ui.pinboard;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import java.util.HashMap;
import java.util.Map;

public class PinboardCanvas extends Pane {
    private final PinboardModel model;
    private final Map<String, PinboardCard> cardMap = new HashMap<>();
    private final Map<PinboardConnection, Line> connectionMap = new HashMap<>();

    // Connection drawing state
    private PinboardCard connectionStartCard;
    private Line tempLine;

    public PinboardCanvas(PinboardModel model) {
        this.model = model;
        this.setStyle("-fx-background-color: #f0e68c; -fx-background-image: url('/images/corkboard_bg.png');");
        // Fallback color if image missing.

        setupModelListeners();
        setupInteraction();
    }

    private void setupModelListeners() {
        model.getItems().addListener((ListChangeListener<PinboardItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PinboardItem item : change.getAddedSubList()) {
                        addCard(item);
                    }
                }
                if (change.wasRemoved()) {
                    for (PinboardItem item : change.getRemoved()) {
                        removeCard(item);
                    }
                }
            }
        });

        model.getConnections().addListener((ListChangeListener<PinboardConnection>) change -> {
             while (change.next()) {
                if (change.wasAdded()) {
                    for (PinboardConnection conn : change.getAddedSubList()) {
                        drawConnection(conn);
                    }
                }
                if (change.wasRemoved()) {
                     for (PinboardConnection conn : change.getRemoved()) {
                        removeConnection(conn);
                    }
                }
            }
        });

        // Initial population
        model.getItems().forEach(this::addCard);
        model.getConnections().forEach(this::drawConnection);
    }

    private void addCard(PinboardItem item) {
        PinboardCard card = new PinboardCard(item);

        // Add connection drawing logic to card
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                handleConnectionStart(card);
                event.consume();
            } else if (event.getButton() == MouseButton.PRIMARY && connectionStartCard != null) {
                handleConnectionEnd(card);
                event.consume();
            }
        });

        cardMap.put(item.getId(), card);
        this.getChildren().add(card);
    }

    private void removeCard(PinboardItem item) {
        PinboardCard card = cardMap.remove(item.getId());
        if (card != null) {
            this.getChildren().remove(card);
        }
    }

    private void drawConnection(PinboardConnection conn) {
        PinboardCard startCard = cardMap.get(conn.getStartItem().getId());
        PinboardCard endCard = cardMap.get(conn.getEndItem().getId());

        if (startCard != null && endCard != null) {
            Line line = new Line();
            line.setStrokeWidth(3);
            line.setStroke(Color.RED); // Red string style

            // Bind line ends to card centers
            line.startXProperty().bind(startCard.layoutXProperty().add(startCard.widthProperty().divide(2)));
            line.startYProperty().bind(startCard.layoutYProperty().add(startCard.heightProperty().divide(2)));
            line.endXProperty().bind(endCard.layoutXProperty().add(endCard.widthProperty().divide(2)));
            line.endYProperty().bind(endCard.layoutYProperty().add(endCard.heightProperty().divide(2)));

            connectionMap.put(conn, line);
            this.getChildren().add(0, line); // Add to back
        }
    }

    private void removeConnection(PinboardConnection conn) {
        Line line = connectionMap.remove(conn);
        if (line != null) {
            this.getChildren().remove(line);
        }
    }

    // --- Interaction for creating connections ---

    private void handleConnectionStart(PinboardCard card) {
        connectionStartCard = card;

        // Create temp line following mouse
        tempLine = new Line();
        tempLine.setStroke(Color.RED);
        tempLine.setStrokeWidth(2);
        tempLine.getStrokeDashArray().addAll(5d, 5d);

        tempLine.startXProperty().bind(card.layoutXProperty().add(card.widthProperty().divide(2)));
        tempLine.startYProperty().bind(card.layoutYProperty().add(card.heightProperty().divide(2)));
        tempLine.setEndX(card.getLayoutX() + card.getWidth()/2);
        tempLine.setEndY(card.getLayoutY() + card.getHeight()/2);

        this.getChildren().add(tempLine);

        this.setOnMouseMoved(event -> {
            if (tempLine != null) {
                tempLine.setEndX(event.getX());
                tempLine.setEndY(event.getY());
            }
        });
    }

    private void handleConnectionEnd(PinboardCard card) {
        if (connectionStartCard != null && connectionStartCard != card) {
            model.addConnection(connectionStartCard.getItem(), card.getItem());
        }
        cancelConnectionMode();
    }

    public void cancelConnectionMode() {
        if (tempLine != null) {
            this.getChildren().remove(tempLine);
            tempLine = null;
        }
        connectionStartCard = null;
        this.setOnMouseMoved(null);
    }

    private void setupInteraction() {
        // Click on canvas to cancel connection mode
        this.setOnMouseClicked(event -> {
            if (connectionStartCard != null) {
                cancelConnectionMode();
            }
        });
    }
}
