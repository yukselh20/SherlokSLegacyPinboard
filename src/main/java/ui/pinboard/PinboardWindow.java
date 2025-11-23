package ui.pinboard;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PinboardWindow extends Stage {
    private final PinboardModel model;
    private final PinboardCanvas canvas;

    public PinboardWindow(PinboardModel model) {
        this.model = model;
        this.canvas = new PinboardCanvas(model);

        initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Detective Pinboard");

        BorderPane root = new BorderPane();

        // Toolbar
        ToolBar toolbar = new ToolBar();
        Button addNoteBtn = new Button("Add Note");
        addNoteBtn.setOnAction(e -> {
            // Add note at center of current view (approx) or random pos
            model.addItem(new PinboardNote(100, 100, "New Note", "#FFFFA5"));
        });

        Button clearBtn = new Button("Clear Board");
        clearBtn.setOnAction(e -> model.clear());

        toolbar.getItems().addAll(addNoteBtn, clearBtn);
        root.setTop(toolbar);

        // Center Canvas
        root.setCenter(canvas);

        // Right Panel
        ExamTemplatePanel examPanel = new ExamTemplatePanel();
        root.setRight(examPanel);

        Scene scene = new Scene(root, 1000, 700);
        this.setScene(scene);
    }
}
