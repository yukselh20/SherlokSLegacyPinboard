package ui.pinboard;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.scene.effect.DropShadow;
import javafx.scene.Cursor;

public class PinboardCard extends StackPane {
    private final PinboardItem item;
    private double mouseAnchorX;
    private double mouseAnchorY;

    public PinboardCard(PinboardItem item) {
        this.item = item;
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        this.setLayoutX(item.getX());
        this.setLayoutY(item.getY());

        // Visual Background
        Rectangle bg = new Rectangle(item.getWidth(), item.getHeight());
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setEffect(new DropShadow(5, Color.GRAY));

        VBox contentBox = new VBox(5);
        contentBox.setPadding(new Insets(10));
        contentBox.setMaxSize(item.getWidth(), item.getHeight());

        if (item instanceof PinboardNote) {
            PinboardNote note = (PinboardNote) item;
            bg.setFill(Color.web(note.getColorHex()));

            TextArea textArea = new TextArea(note.getText());
            textArea.setWrapText(true);
            textArea.setStyle("-fx-control-inner-background: " + note.getColorHex() + "; -fx-background-color: transparent;");
            textArea.setPrefHeight(item.getHeight() - 20);
            // Simple binding to update model
            textArea.textProperty().addListener((obs, old, newVal) -> note.setText(newVal));

            contentBox.getChildren().add(textArea);

        } else if (item instanceof PinboardClue) {
            PinboardClue clue = (PinboardClue) item;
            bg.setFill(Color.FLORALWHITE);
            bg.setStroke(Color.LIGHTGRAY);

            Label titleLabel = new Label(clue.getTitle());
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            titleLabel.setWrapText(true);

            Label bodyLabel = new Label(clue.getSummary());
            bodyLabel.setFont(Font.font("Arial", 10));
            bodyLabel.setWrapText(true);
            bodyLabel.setMaxHeight(Double.MAX_VALUE);

            contentBox.getChildren().addAll(titleLabel, bodyLabel);
        }

        this.getChildren().addAll(bg, contentBox);
    }

    private void setupEventHandlers() {
        this.setOnMousePressed(event -> {
            mouseAnchorX = event.getSceneX() - getLayoutX();
            mouseAnchorY = event.getSceneY() - getLayoutY();
            this.toFront(); // Bring to top
            this.setCursor(Cursor.MOVE);
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - mouseAnchorX;
            double newY = event.getSceneY() - mouseAnchorY;

            this.setLayoutX(newX);
            this.setLayoutY(newY);

            // Update model
            item.setX(newX);
            item.setY(newY);

            event.consume();
        });

        this.setOnMouseReleased(event -> {
            this.setCursor(Cursor.DEFAULT);
            event.consume();
        });
    }

    public PinboardItem getItem() {
        return item;
    }
}
