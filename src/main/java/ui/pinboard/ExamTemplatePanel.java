package ui.pinboard;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class ExamTemplatePanel extends VBox {

    public ExamTemplatePanel() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #2c2c2c;");
        this.setPrefWidth(300);

        Label header = new Label("Case Summary Template");
        header.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 16; -fx-font-weight: bold;");
        header.setWrapText(true);

        Label subHeader = new Label("(Use this to organize your thoughts. This is NOT the final exam.)");
        subHeader.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10;");
        subHeader.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #2c2c2c; -fx-border-color: transparent;");

        VBox fieldsBox = new VBox(10);
        fieldsBox.setPadding(new Insets(5));

        fieldsBox.getChildren().addAll(
            createSection("Main Suspect(s)"),
            createSection("Primary Motive(s)"),
            createSection("Opportunity / Means"),
            createSection("Weapon (if identifiable)"),
            createSection("Key Contradictions"),
            createSection("Supporting Evidence"),
            createSection("Suspicious Behavior/Lies"),
            createSection("Alibi Verification"),
            createSection("Remaining Questions")
        );

        scrollPane.setContent(fieldsBox);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        this.getChildren().addAll(header, subHeader, scrollPane);
    }

    private TitledPane createSection(String title) {
        VBox content = new VBox(5);
        TextArea area = new TextArea();
        area.setPromptText("Enter notes here...");
        area.setPrefRowCount(3);
        area.setWrapText(true);
        // Styling to match dark theme
        area.setStyle("-fx-control-inner-background: #404040; -fx-text-fill: white;");

        content.getChildren().add(area);

        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false); // Start collapsed to save space
        pane.setStyle("-fx-text-fill: black;"); // Default JavaFX TitledPane usually needs styling tweaks

        return pane;
    }
}
