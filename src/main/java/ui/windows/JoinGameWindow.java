package ui.windows;

import client.discovery.DiscoveredGame;
import client.discovery.LanGameDiscoveryService;
import client.discovery.StubLanGameDiscoveryService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.MainController;

public class JoinGameWindow extends Stage {

    private final MainController mainController;
    private final LanGameDiscoveryService discoveryService;
    private final ListView<DiscoveredGame> publicGamesListView;

    public JoinGameWindow(MainController mainController) {
        this.mainController = mainController;
        this.discoveryService = new StubLanGameDiscoveryService(); // Using the stub for now

        setTitle("Join Multiplayer Game");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top section: List of public games
        VBox topVBox = new VBox(5);
        Label publicGamesLabel = new Label("Public Games on LAN:");
        publicGamesListView = new ListView<>();
        topVBox.getChildren().addAll(publicGamesLabel, publicGamesListView);
        root.setCenter(topVBox);

        // Bottom section: Join by code and actions
        VBox bottomVBox = new VBox(10);

        HBox joinByCodeBox = new HBox(5);
        TextField codeField = new TextField();
        codeField.setPromptText("Enter Join Code");
        Button joinByCodeButton = new Button("Join by Code");
        joinByCodeBox.getChildren().addAll(codeField, joinByCodeButton);

        HBox actionButtonsBox = new HBox(10);
        Button joinSelectedButton = new Button("Join Selected Game");
        Button refreshButton = new Button("Refresh");
        Button closeButton = new Button("Close");
        actionButtonsBox.getChildren().addAll(joinSelectedButton, refreshButton, closeButton);

        bottomVBox.getChildren().addAll(joinByCodeBox, actionButtonsBox);
        root.setBottom(bottomVBox);

        // Event Handlers
        joinSelectedButton.setOnAction(event -> joinSelectedGame());
        joinByCodeButton.setOnAction(event -> joinGameByCode(codeField.getText()));
        refreshButton.setOnAction(event -> refreshGamesList());
        closeButton.setOnAction(event -> this.close());

        // Initial population of the list
        refreshGamesList();

        Scene scene = new Scene(root, 400, 300);
        setScene(scene);
    }

    private void refreshGamesList() {
        discoveryService.refreshAsync(); // Trigger a refresh
        publicGamesListView.setItems(FXCollections.observableArrayList(discoveryService.getCurrentGames()));
    }

    private void joinSelectedGame() {
        DiscoveredGame selectedGame = publicGamesListView.getSelectionModel().getSelectedItem();
        if (selectedGame != null) {
            mainController.joinGameByDiscovery(selectedGame);
            this.close();
        } else {
            // TODO: Show an alert
            System.out.println("No game selected.");
        }
    }

    private void joinGameByCode(String code) {
        if (code != null && !code.trim().isEmpty()) {
            mainController.joinGameByCode(code.trim().toUpperCase());
            this.close();
        } else {
            // TODO: Show an alert
            System.out.println("Please enter a code.");
        }
    }
}
