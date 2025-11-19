package ui;

import client.GameClient;
import client.GameClientStateListener;
import common.NetworkConstants;
import common.dto.PublicGameInfoDTO;
import common.dto.RoomDescriptionDTO;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import common.commands.UpdateTaskStateCommand;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import server.ServerMain;
import singleplayer.SinglePlayerMain;
import ui.util.GameOutputParser;
import ui.util.RoomView;
import ui.util.TextAreaOutputStream;
import ui.windows.ChatWindow;
import ui.windows.JournalWindow;
import ui.windows.TasksWindow;
import ui.windows.HelpWindow;

public class MainController implements GameClientStateListener {

    private enum UIState {
        MENU,
        CHOOSING_CASE,
        CHOOSING_LANGUAGE,
        CASE_INVITATION,
        GAME_SINGLE,
        GAME_MULTI,
        MULTIPLAYER_MENU,
        ADDING_CASE_TERMINAL,
        PROMPT_JOIN_HOST,
        PROMPT_JOIN_PORT
    }

    private enum UIMultiplayerSubState {
        NONE,
        CONNECTING,
        MAIN_MENU,
        HOST_OPTIONS,
        CASE_SELECTION,
        LANGUAGE_SELECTION,
        HOSTING_LOBBY,
        JOIN_OPTIONS,
        PUBLIC_GAMES_LIST,
        PRIVATE_GAME_ENTRY,
        IN_LOBBY,
        IN_GAME,
        DISCONNECTED
    }

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Button tasksButton;
    @FXML
    private Button journalButton;
    @FXML
    private Button chatButton;
    @FXML
    private Button helpButton;
    @FXML
    private Button exitButton;
    @FXML
    private Label unreadChatLabel;
    @FXML
    private StackPane roomPane;
    @FXML
    private VBox rightInfoPanel;
    @FXML
    private VBox neighboringRoomsContainer;
    @FXML
    private TextArea terminalTextArea;
    @FXML
    private TextField terminalInputField;
    @FXML
    private Label statusLabel;
    @FXML
    private SplitPane bottomSplitPane;

    private GameClient gameClient;
    private Thread gameClientThread;
    private JournalWindow journalWindow;
    private ChatWindow chatWindow;
    private TasksWindow tasksWindow;
    private HelpWindow helpWindow;
    private RoomView roomView;
    private int unreadChatCount = 0;

    private VBox mainMenuVBox;
    private List<String> launchArgs;
    private HostServices hostServices;
    private UIState currentState = UIState.MENU;

    private TextAreaOutputStream taos;
    private SinglePlayerMain singlePlayerGame;
    private Thread singlePlayerGameThread;
    private JsonDTO.CaseFile selectedCaseFile; // Temporarily store the case for language selection
    private UIMultiplayerSubState currentMultiplayerSubState = UIMultiplayerSubState.NONE;
    private boolean isSinglePlayer;
    private boolean isHostPlayer;
    private java.util.Map<String, Boolean> taskStates;

    // --- Embedded Server & Multiplayer Config ---
    private server.GameServer embeddedServer;
    private Thread embeddedServerThread;
    private boolean embeddedServerRunning = false;
    private String configuredServerHost;
    private int configuredServerPort;

    @FXML
    public void initialize() {
        this.taskStates = new java.util.HashMap<>();
        terminalTextArea.setEditable(false);
        terminalTextArea.setWrapText(true);

        // Auto-scrolling is now handled by the TextAreaOutputStream

        terminalInputField.setOnAction(event -> handleTerminalInput());
        tasksButton.setOnAction(event -> {
            playSound("click.wav");
            openTasksWindow();
        });
        journalButton.setOnAction(event -> {
            playSound("pageflip.mp3");
            openJournalWindow();
        });
        chatButton.setOnAction(event -> {
            playSound("click.wav");
            openChatWindow();
        });

        helpButton.setOnAction(event -> {
            playSound("click.wav");
            openHelpWindow();
        });

        exitButton.setOnAction(event -> {
            playSound("click.wav");
            sendCommand("exit");
        });

        updateStatus("GUI Ready. Please select a game mode.");
        unreadChatLabel.setVisible(false);
        bottomSplitPane.setDividerPositions(0.7);

        roomView = new RoomView(this);
        roomPane.getChildren().add(roomView);

        // Redirect System.out and System.in
        this.taos = new TextAreaOutputStream(terminalTextArea);
        GameOutputParser parser = new GameOutputParser(this);
        taos.setParser(parser);
        System.setOut(new PrintStream(taos, true));

        createMainMenu();
        setupButtonIcons();
        updateUIVisibility();
    }

    private void showCaseInvitation(String invitationText, boolean isHost) {
        VBox invitationBox = new VBox(20);
        invitationBox.setAlignment(Pos.CENTER);
        invitationBox.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLabel = new Label("Case Invitation");
        titleLabel.setStyle("-fx-font-size: 24; -fx-text-fill: #d4af37;");

        TextArea invitationTextArea = new TextArea(invitationText);
        invitationTextArea.setEditable(false);
        invitationTextArea.setWrapText(true);
        invitationTextArea.setStyle(
                "-fx-control-inner-background: #2b2b2b; " +
                "-fx-text-fill: #e0e0e0; " +
                "-fx-font-family: 'Georgia'; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #d4af37; " +
                "-fx-border-width: 1;"
        );
        invitationTextArea.setPrefWidth(600);
        invitationTextArea.setPrefHeight(400);

        Button startButton = new Button("Start Case");
        startButton.setOnAction(event -> handleStartCase());

        invitationBox.getChildren().addAll(titleLabel, invitationTextArea, startButton);

        if (!isHost) {
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            invitationBox.getChildren().add(cancelButton);
        }

        Platform.runLater(() -> {
            roomPane.getChildren().clear();
            roomPane.getChildren().add(invitationBox);
            currentState = UIState.CASE_INVITATION;
            updateUIVisibility();
        });
    }

    private void handleStartCase() {
        playSound("click.wav");
        if (isSinglePlayer) {
            // Run in a background thread to avoid freezing the UI
            new Thread(() -> {
                singlePlayerGame.processCommand("start case");
                Platform.runLater(() -> {
                    currentState = UIState.GAME_SINGLE;
                    updateUIVisibility();
                });
            }).start();
        } else {
            sendCommand("start case");
            // The UI will be updated by the server's response
        }
    }

    public void setLaunchArgs(List<String> args) {
        this.launchArgs = args;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupButtonIcons() {
        setButtonIcon(tasksButton, "/icons/tasks.png");
        setButtonIcon(journalButton, "/icons/journal.png");
        setButtonIcon(chatButton, "/icons/chat.png");
    }

    private void setButtonIcon(Button button, String iconPath) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitHeight(20);
            iconView.setFitWidth(20);
            button.setGraphic(iconView);
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath);
        }
    }

    private void playSound(String soundFile) {
        try {
            String soundPath = getClass().getResource("/sounds/" + soundFile).toExternalForm();
            Media sound = new Media(soundPath);
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play sound: " + soundFile);
        }
    }

    private void createMainMenu() {
        mainMenuVBox = new VBox(15);
        mainMenuVBox.setAlignment(Pos.CENTER);
        mainMenuVBox.getStyleClass().add("main-menu-container");

        Button singlePlayerButton = new Button("Single Player");
        singlePlayerButton.getStyleClass().add("main-menu-button");
        singlePlayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("1");
        });

        Button hostMultiplayerButton = new Button("Host Multiplayer Game");
        hostMultiplayerButton.getStyleClass().add("main-menu-button");
        hostMultiplayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("2");
        });

        Button joinMultiplayerButton = new Button("Join Multiplayer Game");
        joinMultiplayerButton.getStyleClass().add("main-menu-button");
        joinMultiplayerButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("3");
        });

        Button addCaseButton = new Button("Add Custom Case");
        addCaseButton.getStyleClass().add("main-menu-button");
        addCaseButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("4");
        });

        Button quitButton = new Button("Quit");
        quitButton.getStyleClass().add("main-menu-button");
        quitButton.setOnAction(event -> {
            playSound("click.wav");
            handleMainMenuInput("5");
        });

        mainMenuVBox
                .getChildren()
                .addAll(singlePlayerButton, hostMultiplayerButton, joinMultiplayerButton, addCaseButton, quitButton);
    }

    private void handleMainMenuInput(String input) {
        switch (input) {
            case "1":
                startSinglePlayer();
                break;
            case "2":
                startHostMultiplayer();
                break;
            case "3":
                startJoinMultiplayer();
                break;
            case "4":
                // For GUI, open the window. For terminal, prompt.
                if (System.getProperty("java.class.path").contains("openjfx")) { // A bit of a hack to detect GUI mode
                    new ui.windows.AddCaseWindow(this).show();
                } else {
                    currentState = UIState.ADDING_CASE_TERMINAL;
                    terminalTextArea.appendText("\nPlease enter the full file path to the case JSON file and press Enter:\n");
                }
                break;
            case "5":
                shutdown();
                break;
            default:
                terminalTextArea.appendText("Invalid selection. Please enter a number from 1 to 5.\n");
                break;
        }
    }

    private void updateUIVisibility() {
        Platform.runLater(() -> {
            Node currentView = roomPane.getChildren().isEmpty() ? null : roomPane.getChildren().get(0);
            Node nextView = null;

            switch (currentState) {
                case CHOOSING_CASE:
                case CHOOSING_LANGUAGE:
                    // These states manage their own views, but we need to ensure game buttons are off
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    return; // Return early as the view is handled by show... methods
                case CASE_INVITATION:
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(isHostPlayer); // Only host can exit at this stage
                    rightInfoPanel.setVisible(false);
                    return; // Return early to prevent view transition logic from running
                case MENU:
                    nextView = mainMenuVBox;
                    terminalTextArea.clear();
                    terminalTextArea.appendText("Welcome to Detective Game! Please select a mode to begin.\n");
                    terminalTextArea.appendText("\n--- Main Menu ---\n");
                    terminalTextArea.appendText("1. Single Player\n");
                    terminalTextArea.appendText("2. Host Multiplayer Game\n");
                    terminalTextArea.appendText("3. Join Multiplayer Game\n");
                    terminalTextArea.appendText("4. Add Custom Case\n");
                    terminalTextArea.appendText("5. Quit\n");
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    break;
                case MULTIPLAYER_MENU:
                    tasksButton.setVisible(false);
                    journalButton.setVisible(false);
                    chatButton.setVisible(false);
                    helpButton.setVisible(false);
                    exitButton.setVisible(false);
                    rightInfoPanel.setVisible(false);
                    return; // Return early to prevent view transition logic from running
                case GAME_SINGLE:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    chatButton.setVisible(false);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
                case GAME_MULTI:
                    nextView = roomView;
                    tasksButton.setVisible(true);
                    journalButton.setVisible(true);
                    chatButton.setVisible(true);
                    helpButton.setVisible(true);
                    exitButton.setVisible(true);
                    rightInfoPanel.setVisible(true);
                    break;
            }

            if (currentView != nextView) {
                final Node viewToDisplay = nextView;
                FadeTransition ft = new FadeTransition(Duration.millis(500), currentView);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.setOnFinished(event -> {
                    roomPane.getChildren().clear();
                    roomPane.getChildren().add(viewToDisplay);
                    FadeTransition ft2 = new FadeTransition(Duration.millis(500), viewToDisplay);
                    ft2.setFromValue(0.0);
                    ft2.setToValue(1.0);
                    ft2.play();
                });
                ft.play();
            }
        });
    }

    private void startSinglePlayer() {
        isSinglePlayer = true;
        isHostPlayer = true;
        taskStates.clear();
        // Clear the room view to prevent state bleeding from multiplayer
        updateRoomView(null);
        updateStatus("Starting Single Player...");
        currentState = UIState.CHOOSING_CASE;
        singlePlayerGame = new SinglePlayerMain();
        showSinglePlayerCaseSelection();
    }

    public void showSinglePlayerCaseSelection() {
        List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
        VBox caseSelectionBox = new VBox(15);
        caseSelectionBox.setAlignment(Pos.CENTER);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Case ---\n");
        for (int i = 0; i < cases.size(); i++) {
            JsonDTO.CaseFile caseFile = cases.get(i);
            terminalTextArea.appendText((i + 1) + ". " + caseFile.getUniversalTitle() + "\n");
            Button caseButton = new Button(caseFile.getUniversalTitle());
            caseButton.setOnAction(event -> showSinglePlayerLanguageSelection(caseFile));
            caseSelectionBox.getChildren().add(caseButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("---------------------\n");

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(event -> {
            currentState = UIState.MENU;
            updateUIVisibility();
        });

        caseSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(caseSelectionBox);
    }

    public void showCaseSelectionMenu() {
        Platform.runLater(() -> {
            if (taskStates != null) {
                taskStates.clear();
            }
            currentState = UIState.CHOOSING_CASE;
            updateUIVisibility();
            showSinglePlayerCaseSelection();
        });
    }

    public void returnToMultiplayerMenu() {
        Platform.runLater(() -> {
            // This will be called by the parser when the host cancels
            onMainMenu();
        });
    }

    private void handleCaseSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.MENU;
            updateUIVisibility();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<JsonDTO.CaseFile> cases = singlePlayerGame.getAvailableCases();
            if (choice > 0 && choice <= cases.size()) {
                showSinglePlayerLanguageSelection(cases.get(choice - 1));
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid case number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void showSinglePlayerLanguageSelection(JsonDTO.CaseFile caseFile) {
        this.selectedCaseFile = caseFile; // Store the selected case
        currentState = UIState.CHOOSING_LANGUAGE;
        VBox langSelectionBox = new VBox(15);
        langSelectionBox.setAlignment(Pos.CENTER);
        List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
        java.util.Collections.sort(langCodes);

        terminalTextArea.clear();
        terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
        for (int i = 0; i < langCodes.size(); i++) {
            String langCode = langCodes.get(i);
            String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
            terminalTextArea.appendText((i + 1) + ". " + langName + "\n");
            Button langButton = new Button(langName);
            langButton.setOnAction(event -> {
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(caseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            });
            langSelectionBox.getChildren().add(langButton);
        }
        terminalTextArea.appendText("0. Back\n");
        terminalTextArea.appendText("-------------------------------------\n");
        Button backButton = new Button("Back to Case Selection");
        backButton.setOnAction(event -> {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
        });
        langSelectionBox.getChildren().add(backButton);
        roomPane.getChildren().clear();
        roomPane.getChildren().add(langSelectionBox);
    }

    private void handleLanguageSelectionInput(String input) {
        if (input.equals("0") || input.equalsIgnoreCase("back")) {
            currentState = UIState.CHOOSING_CASE;
            showSinglePlayerCaseSelection();
            return;
        }
        try {
            int choice = Integer.parseInt(input);
            List<String> langCodes = new java.util.ArrayList<>(selectedCaseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);

            if (choice > 0 && choice <= langCodes.size()) {
                String langCode = langCodes.get(choice - 1);
                JsonDTO.LocalizedCaseFile localizedCase = singlePlayerGame.selectCaseAndLanguage(selectedCaseFile, langCode);
                singlePlayerGame.initializeCase(localizedCase);
                showCaseInvitation(localizedCase.getInvitation(), true);
            } else {
                terminalTextArea.appendText("Invalid selection. Please choose a valid language number.\n");
            }
        } catch (NumberFormatException e) {
            terminalTextArea.appendText("Invalid command. Please enter a number or '0' to go back.\n");
        }
    }

    private void startHostMultiplayer() {
        if (embeddedServerRunning) {
            terminalTextArea.appendText("\n[ERROR] An embedded server is already running.\n");
            return;
        }

        updateStatus("Starting embedded game server...");
        terminalTextArea.appendText("\nStarting embedded game server for hosted multiplayer game...\n");

        embeddedServerThread = new Thread(() -> {
            try {
                // We instantiate GameServer directly, not ServerMain
                embeddedServer = new server.GameServer(NetworkConstants.DEFAULT_PORT);
                embeddedServer.startServer(); // This blocks until the server is ready
                embeddedServer.run(); // This starts the server's main loop
            } catch (Exception e) {
                Platform.runLater(() -> {
                    terminalTextArea.appendText("[ERROR] Failed to start embedded server: " + e.getMessage() + "\n");
                });
                e.printStackTrace();
            }
        }, "Embedded-GameServer-Thread");

        embeddedServerThread.setDaemon(true);
        embeddedServerThread.start();
        embeddedServerRunning = true;

        // Give the server a moment to start up before connecting.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Configure client to connect to the new embedded server
        this.configuredServerHost = "localhost";
        this.configuredServerPort = NetworkConstants.DEFAULT_PORT;
        startMultiplayer();
    }

    private void startJoinMultiplayer() {
        // A simple way to detect if we're in a GUI environment.
        // The property is set by the JavaFX launcher.
        boolean isGuiMode = System.getProperty("java.class.path").contains("openjfx");

        if (isGuiMode) {
            promptForServerAddressAndStartMultiplayer();
        } else {
            // Terminal-based flow
            currentState = UIState.PROMPT_JOIN_HOST;
            terminalTextArea.appendText("\nEnter server IP (blank for localhost): \n");
        }
    }

    private void promptForServerAddressAndStartMultiplayer() {
        javafx.scene.control.Dialog<javafx.util.Pair<String, String>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Join Multiplayer Game");
        dialog.setHeaderText("Enter the host's server address and port.");

        javafx.scene.control.ButtonType okButtonType = new javafx.scene.control.ButtonType("Join", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField hostField = new TextField();
        hostField.setPromptText("Host IP or name");
        hostField.setText("localhost");

        TextField portField = new TextField();
        portField.setPromptText("Port");
        portField.setText(String.valueOf(NetworkConstants.DEFAULT_PORT));

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(hostField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new javafx.util.Pair<>(hostField.getText(), portField.getText());
            }
            return null;
        });

        java.util.Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(hostPort -> {
            String host = hostPort.getKey().trim();
            String portStr = hostPort.getValue().trim();

            if (host.isEmpty()) {
                host = "localhost";
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                port = NetworkConstants.DEFAULT_PORT;
                terminalTextArea.appendText("\n[WARN] Invalid port entered, using default: " + port + "\n");
            }

            this.configuredServerHost = host;
            this.configuredServerPort = port;
            startMultiplayer();
        });
    }

    private void startMultiplayer() {
        isSinglePlayer = false;
        isHostPlayer = false; // Guest by default, updated by server
        taskStates.clear();
        updateStatus("Starting Multiplayer Client...");

        // Use the configured host and port instead of launch args
        gameClient = new GameClient(configuredServerHost, configuredServerPort, this.taos);
        gameClient.setListener(this);

        gameClientThread = new Thread(() -> {
            try {
                gameClient.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                currentState = UIState.MENU;
                updateUIVisibility();
            }
        }, "GameClient-Thread");
        gameClientThread.setDaemon(true);
        gameClientThread.start();
        currentState = UIState.GAME_MULTI;
        // Don't call updateUIVisibility here, the listener will do it.
    }


    private String getLaunchArg(int index, String defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            return launchArgs.get(index);
        }
        return defaultValue;
    }

    private int getLaunchArg(int index, int defaultValue) {
        if (launchArgs != null && launchArgs.size() > index) {
            try {
                return Integer.parseInt(launchArgs.get(index));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    public void shutdownEmbeddedServer() {
        if (embeddedServerRunning && embeddedServer != null) {
            terminalTextArea.appendText("\nShutting down embedded server...\n");
            embeddedServer.stopServer(); // Signal the server to stop
            if (embeddedServerThread != null && embeddedServerThread.isAlive()) {
                try {
                    // Give the server a moment to close connections
                    embeddedServerThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    terminalTextArea.appendText("\n[WARN] Interrupted while waiting for server thread to shut down.\n");
                }
                if (embeddedServerThread.isAlive()) {
                    embeddedServerThread.interrupt(); // Forcefully interrupt if it's stuck
                }
            }
            embeddedServerRunning = false;
            embeddedServer = null;
            embeddedServerThread = null;
            terminalTextArea.appendText("Embedded server shut down.\n");
        }
    }

    public void shutdown() {
        shutdownEmbeddedServer(); // Ensure server is stopped on app exit
        System.out.println("\nShutting down application...");
        if (gameClient != null) {
            gameClient.stopClient();
        }
        if (gameClientThread != null && gameClientThread.isAlive()) {
            try {
                gameClientThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleTerminalInput() {
        String input = terminalInputField.getText().trim();
        if (!input.isEmpty()) {
            if (currentState == UIState.GAME_SINGLE) {
                singlePlayerGame.processCommand(input);
            } else if ((currentState == UIState.GAME_MULTI || currentState == UIState.MULTIPLAYER_MENU) && gameClient != null) {
                if (currentMultiplayerSubState == UIMultiplayerSubState.MAIN_MENU && input.equals("3")) {
                    gameClient.stopClient();
                    if (gameClientThread != null) {
                        gameClientThread.interrupt(); // Interrupt the thread to unblock it
                    }
                } else {
                    gameClient.enqueueUserInput(input);
                }
            } else if (currentState == UIState.MENU) {
                handleMainMenuInput(input);
            } else if (currentState == UIState.CHOOSING_CASE) {
                handleCaseSelectionInput(input);
            } else if (currentState == UIState.CHOOSING_LANGUAGE) {
                handleLanguageSelectionInput(input);
            } else if (currentState == UIState.ADDING_CASE_TERMINAL) {
                String result = singleplayer.util.CaseFileUtil.addCaseFile(input);
                terminalTextArea.appendText(result + "\n");
                currentState = UIState.MENU;
                updateUIVisibility(); // To reprint the main menu
            } else if (currentState == UIState.PROMPT_JOIN_HOST) {
                this.configuredServerHost = input.isEmpty() ? "localhost" : input;
                currentState = UIState.PROMPT_JOIN_PORT;
                terminalTextArea.appendText("Enter server port (blank for " + NetworkConstants.DEFAULT_PORT + "): \n");
            } else if (currentState == UIState.PROMPT_JOIN_PORT) {
                try {
                    this.configuredServerPort = input.isEmpty() ? NetworkConstants.DEFAULT_PORT : Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    this.configuredServerPort = NetworkConstants.DEFAULT_PORT;
                    terminalTextArea.appendText("\n[WARN] Invalid port, using default: " + this.configuredServerPort + "\n");
                }
                startMultiplayer();
            } else if (currentState == UIState.CASE_INVITATION && !isSinglePlayer) {
                gameClient.enqueueUserInput(input);
            } else if (currentState == UIState.CASE_INVITATION && isSinglePlayer) {
                if (input.equalsIgnoreCase("start case")) {
                    handleStartCase();
                }
            }
            terminalInputField.clear();
        }
    }

    private void openTasksWindow() {
        if (tasksWindow == null) {
            tasksWindow = new TasksWindow(this);
        }

        // Dynamically load tasks based on the current game context
        if (isSinglePlayer && singlePlayerGame != null) {
            List<String> tasks = singlePlayerGame.getCurrentCaseTasks();
            tasksWindow.loadTasks(tasks, taskStates);
        } else if (!isSinglePlayer && gameClient != null) {
            List<String> tasks = gameClient.getCurrentCaseTasks();
            tasksWindow.loadTasks(tasks, taskStates);
        }

        tasksWindow.show();
    }

    public void updateTaskState(String task, boolean isCompleted) {
        if (isSinglePlayer) {
            // For single player, we just update the local map directly.
            taskStates.put(task, isCompleted);
        } else if (gameClient != null) {
            // For multiplayer, we need to find the task index and send a command.
            List<String> tasks = gameClient.getCurrentCaseTasks();
            if (tasks != null) {
                int taskIndex = tasks.indexOf(task);
                if (taskIndex != -1) {
                    // Note: We are NOT updating the local map here directly.
                    // The UI will only update when the server broadcasts the change back to us,
                    // ensuring a single source of truth and synchronization.
                    UpdateTaskStateCommand command = new UpdateTaskStateCommand(taskIndex, isCompleted);
                    gameClient.sendDirectCommand(command);
                }
            }
        }
    }

    private void openJournalWindow() {
        if (journalWindow == null) {
            journalWindow = new JournalWindow(this);
        }

        // Dynamically load journal entries based on the current game context
        if (isSinglePlayer && singlePlayerGame != null) {
            List<common.dto.JournalEntryDTO> entries = singlePlayerGame.getGameContext().getJournalEntries(null);
            if (entries != null) {
                journalWindow.setEntries(entries);
            }
        } else if (!isSinglePlayer && gameClient != null) {
            List<common.dto.JournalEntryDTO> entries = gameClient.getJournalEntries();
            if (entries != null) {
                journalWindow.setEntries(entries);
            }
        }

        journalWindow.show();
    }

    private void openChatWindow() {
        if (chatWindow == null) {
            chatWindow = new ChatWindow(this);
        }

        // Dynamically load chat history
        if (!isSinglePlayer && gameClient != null) {
            List<common.dto.ChatMessage> history = gameClient.getChatHistory();
            if (history != null) {
                chatWindow.loadHistory(history);
            }
        }

        chatWindow.show();
        unreadChatCount = 0;
        updateUnreadChatLabel();
    }

    private void openHelpWindow() {
        if (helpWindow == null) {
            helpWindow = new HelpWindow();
        }
        helpWindow.show();
    }

    public void incrementUnreadChat() {
        unreadChatCount++;
        updateUnreadChatLabel();
    }

    private void updateUnreadChatLabel() {
        if (unreadChatCount > 0) {
            unreadChatLabel.setText(String.valueOf(unreadChatCount));
            unreadChatLabel.setVisible(true);
        } else {
            unreadChatLabel.setVisible(false);
        }
    }

    public void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    public TextArea getTerminalTextArea() {
        return terminalTextArea;
    }

    public StackPane getRoomPane() {
        return roomPane;
    }

    public VBox getRightInfoPanel() {
        return rightInfoPanel;
    }

    public void sendCommand(String command) {
        if ((currentState == UIState.GAME_MULTI
                || currentState == UIState.MULTIPLAYER_MENU
                || (currentState == UIState.CASE_INVITATION && !isSinglePlayer))
                && gameClient != null) {
            gameClient.enqueueUserInput(command);
        } else if ((currentState == UIState.GAME_SINGLE || (currentState == UIState.CASE_INVITATION && isSinglePlayer)) && singlePlayerGame != null) {
            singlePlayerGame.processCommand(command);
        }
    }

    public GameClient getGameClient() {
        return gameClient;
    }

    public void updateRoomView(RoomDescriptionDTO roomDescription) {
        if (roomView != null) {
            Platform.runLater(() -> {
                if (roomDescription != null) {
                    roomView.loadRoom(roomDescription);
                    updateRightPanel(roomDescription);
                    updateStatus("Current room: " + roomDescription.getName());
                } else {
                    roomView.clear();
                    updateRightPanel(null);
                    updateStatus("No active game.");
                }
            });
        }
    }

    public void refreshRoomView() {
        if (isSinglePlayer && singlePlayerGame != null && singlePlayerGame.getGameContext() != null) {
            singleplayer.GameContextSinglePlayer context = singlePlayerGame.getGameContext();
            Core.Room currentRoom = context.getCurrentRoomForPlayer(null);
            if (currentRoom != null) {
                RoomDescriptionDTO dto = context.createRoomDescriptionDTO(currentRoom, null);
                updateRoomView(dto);
            }
        }
    }

    public void refreshJournalWindow() {
        if (journalWindow != null) {
            Platform.runLater(this::openJournalWindow);
        }
    }

    private void updateRightPanel(RoomDescriptionDTO roomDescription) {
        neighboringRoomsContainer.getChildren().clear();

        if (roomDescription == null || roomDescription.getExits() == null) {
            return;
        }

        for (java.util.Map.Entry<String, String> entry : roomDescription.getExits().entrySet()) {
            String direction = entry.getKey();
            String roomName = entry.getValue();
            String buttonText = direction + ": " + roomName;

            Button roomButton = new Button(buttonText);
            roomButton.setPrefWidth(Double.MAX_VALUE); // Make buttons fill the width

            roomButton.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    // Command is the direction, e.g., "north"
                    String command = "move " + direction;
                    sendCommand(command);
                }
            });

            Tooltip tooltip = new Tooltip("Double-click to move to the " + roomName);
            roomButton.setTooltip(tooltip);

            neighboringRoomsContainer.getChildren().add(roomButton);
        }
    }

    public void showRoomResponse(String targetName, String response) {
        if (roomView != null) {
            Platform.runLater(() -> {
                roomView.showResponseBubble(targetName, response);
            });
        }
    }

    public void addJournalEntry(String entry) {
        if (journalWindow != null) {
            Platform.runLater(() -> {
                journalWindow.addEntry(entry);
            });
        }
    }

    public void addChatMessage(String sender, String message) {
        if (chatWindow != null) {
            Platform.runLater(() -> {
                chatWindow.addChatMessage(sender, message);
            });
        }
    }

    @Override
    public void onDisconnected() {
        shutdownEmbeddedServer(); // Shut down server if we were hosting
        currentMultiplayerSubState = UIMultiplayerSubState.DISCONNECTED;
        Platform.runLater(() -> {
            VBox disconnectedBox = new VBox(15);
            disconnectedBox.setAlignment(Pos.CENTER);
            Label label = new Label("Disconnected from server.");
            Button reconnectButton = new Button("Reconnect");
            reconnectButton.setOnAction(event -> sendCommand("connect"));
            disconnectedBox.getChildren().addAll(label, reconnectButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(disconnectedBox);
        });
    }

    @Override
    public void onConnecting() {
        currentMultiplayerSubState = UIMultiplayerSubState.CONNECTING;
        Platform.runLater(() -> {
            VBox connectingBox = new VBox(15);
            connectingBox.setAlignment(Pos.CENTER);
            Label label = new Label("Connecting to server...");
            connectingBox.getChildren().add(label);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(connectingBox);
        });
    }

    @Override
    public void onConnected() {
        // This will shortly be followed by onMainMenu
    }

    @Override
    public void onMainMenu() {
        if (taskStates != null) {
            taskStates.clear();
        }
        currentMultiplayerSubState = UIMultiplayerSubState.MAIN_MENU;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Multiplayer Menu ---\n");
            terminalTextArea.appendText("1. Host Game\n");
            terminalTextArea.appendText("2. Join Game\n");
            terminalTextArea.appendText("3. Back to Main Menu\n");
            terminalTextArea.appendText("----------------------\n");
            VBox menuBox = new VBox(15);
            menuBox.setAlignment(Pos.CENTER);
            Button hostButton = new Button("Host Game");
            hostButton.setOnAction(event -> sendCommand("1"));
            Button joinButton = new Button("Join Game");
            joinButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back to Main Menu");
            backButton.setOnAction(event -> {
                shutdownEmbeddedServer(); // Shut down server if we were the host
                gameClient.stopClient();
                if (gameClientThread != null) {
                    gameClientThread.interrupt();
                }
                // Transition back to the main application menu
                currentState = UIState.MENU;
                updateUIVisibility();
            });
            menuBox.getChildren().addAll(hostButton, joinButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(menuBox);
        });
    }

    @Override
    public void onHostGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.HOST_OPTIONS;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Host Game Options ---\n");
            terminalTextArea.appendText("1. Host Public Game\n");
            terminalTextArea.appendText("2. Host Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-------------------------\n");
            VBox hostOptionsBox = new VBox(15);
            hostOptionsBox.setAlignment(Pos.CENTER);
            Button publicButton = new Button("Host Public Game");
            publicButton.setOnAction(event -> sendCommand("1"));
            Button privateButton = new Button("Host Private Game");
            privateButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("3"));
            hostOptionsBox.getChildren().addAll(publicButton, privateButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(hostOptionsBox);
        });
    }

    @Override
    public void onCaseSelection(List<JsonDTO.CaseFile> cases) {
        currentMultiplayerSubState = UIMultiplayerSubState.CASE_SELECTION;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Case ---\n");
            VBox caseSelectionBox = new VBox(15);
            caseSelectionBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < cases.size(); i++) {
                final int caseNum = i + 1;
                String caseTitle = cases.get(i).getUniversalTitle();
                terminalTextArea.appendText(caseNum + ". " + caseTitle + "\n");
                Button caseButton = new Button(caseTitle);
                caseButton.setOnAction(event -> sendCommand(String.valueOf(caseNum)));
                caseSelectionBox.getChildren().add(caseButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("---------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            caseSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(caseSelectionBox);
        });
    }

    @Override
    public void onLanguageSelection(JsonDTO.CaseFile caseFile) {
        currentMultiplayerSubState = UIMultiplayerSubState.LANGUAGE_SELECTION;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Select a Language for " + caseFile.getUniversalTitle() + " ---\n");
            VBox langSelectionBox = new VBox(15);
            langSelectionBox.setAlignment(Pos.CENTER);
            List<String> langCodes = new java.util.ArrayList<>(caseFile.getLocalizations().keySet());
            java.util.Collections.sort(langCodes);
            for (int i = 0; i < langCodes.size(); i++) {
                final int langNum = i + 1;
                String langCode = langCodes.get(i);
                String langName = caseFile.getLocalizations().get(langCode).getLanguageName();
                terminalTextArea.appendText(langNum + ". " + langName + "\n");
                Button langButton = new Button(langName);
                langButton.setOnAction(event -> sendCommand(String.valueOf(langNum)));
                langSelectionBox.getChildren().add(langButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("-------------------------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            langSelectionBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(langSelectionBox);
        });
    }

    @Override
    public void onHostingLobby(String gameCode) {
        currentMultiplayerSubState = UIMultiplayerSubState.HOSTING_LOBBY;
        Platform.runLater(() -> {
            VBox lobbyBox = new VBox(15);
            lobbyBox.setAlignment(Pos.CENTER);
            Label label = new Label("Waiting for another player to join...");
            if (gameCode != null) {
                Label codeLabel = new Label("Private Game Code: " + gameCode);
                lobbyBox.getChildren().add(codeLabel);
            }
            Button cancelButton = new Button("Cancel");
            cancelButton.setOnAction(event -> sendCommand("cancel"));
            lobbyBox.getChildren().addAll(label, cancelButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(lobbyBox);
        });
    }

    @Override
    public void onJoinGameOptions() {
        currentMultiplayerSubState = UIMultiplayerSubState.JOIN_OPTIONS;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Join Game Options ---\n");
            terminalTextArea.appendText("1. Join Public Game\n");
            terminalTextArea.appendText("2. Join Private Game\n");
            terminalTextArea.appendText("3. Back\n");
            terminalTextArea.appendText("-----------------------\n");
            VBox joinOptionsBox = new VBox(15);
            joinOptionsBox.setAlignment(Pos.CENTER);
            Button publicButton = new Button("Join Public Game");
            publicButton.setOnAction(event -> sendCommand("1"));
            Button privateButton = new Button("Join Private Game");
            privateButton.setOnAction(event -> sendCommand("2"));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("3"));
            joinOptionsBox.getChildren().addAll(publicButton, privateButton, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(joinOptionsBox);
        });
    }

    @Override
    public void onPublicGamesList(List<PublicGameInfoDTO> games) {
        currentMultiplayerSubState = UIMultiplayerSubState.PUBLIC_GAMES_LIST;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            terminalTextArea.clear();
            terminalTextArea.appendText("--- Public Games ---\n");
            VBox gamesBox = new VBox(15);
            gamesBox.setAlignment(Pos.CENTER);
            for (int i = 0; i < games.size(); i++) {
                final int gameNum = i + 1;
                PublicGameInfoDTO game = games.get(i);
                String gameInfo = game.getCaseTitle() + " hosted by " + game.getHostPlayerDisplayId();
                terminalTextArea.appendText(gameNum + ". " + gameInfo + "\n");
                Button gameButton = new Button(gameInfo);
                gameButton.setOnAction(event -> sendCommand(String.valueOf(gameNum)));
                gamesBox.getChildren().add(gameButton);
            }
            terminalTextArea.appendText("0. Back\n");
            terminalTextArea.appendText("--------------------\n");
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("0"));
            gamesBox.getChildren().add(backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(gamesBox);
        });
    }

    @Override
    public void onPrivateGameEntry() {
        currentMultiplayerSubState = UIMultiplayerSubState.PRIVATE_GAME_ENTRY;
        currentState = UIState.MULTIPLAYER_MENU;
        updateUIVisibility();
        Platform.runLater(() -> {
            VBox privateGameBox = new VBox(15);
            privateGameBox.setAlignment(Pos.CENTER);
            Label label = new Label("Enter Private Game Code:");
            TextField codeField = new TextField();
            codeField.setOnAction(event -> sendCommand(codeField.getText()));
            Button backButton = new Button("Back");
            backButton.setOnAction(event -> sendCommand("cancel"));
            privateGameBox.getChildren().addAll(label, codeField, backButton);
            roomPane.getChildren().clear();
            roomPane.getChildren().add(privateGameBox);
        });
    }

    @Override
    public void onLobby() {
        currentMultiplayerSubState = UIMultiplayerSubState.IN_LOBBY;
    }

    @Override
    public void onEnterGame(RoomDescriptionDTO initialRoom) {
        isSinglePlayer = false;
        currentMultiplayerSubState = UIMultiplayerSubState.IN_GAME;
        currentState = UIState.GAME_MULTI;
        Platform.runLater(() -> {
            roomPane.getChildren().clear();
            roomPane.getChildren().add(roomView);
            tasksButton.setVisible(true);
            journalButton.setVisible(true);
            chatButton.setVisible(true);
            helpButton.setVisible(true);
            exitButton.setVisible(true);
            rightInfoPanel.setVisible(true);
            updateRoomView(initialRoom);
        });
    }

    @Override
    public void onUpdateRoom(RoomDescriptionDTO newRoom) {
        updateRoomView(newRoom);
    }

    @Override
    public void onReceiveCaseInvitation(String invitation, boolean isHost) {
        this.isHostPlayer = isHost;
        showCaseInvitation(invitation, isHost);
    }

    @Override
    public void onJournalUpdated() {
        refreshJournalWindow();
    }

    @Override
    public void onChatMessageReceived(common.dto.ChatMessage message) {
        if (chatWindow != null) {
            Platform.runLater(() -> {
                chatWindow.addChatMessage(message);
            });
        }
    }

    @Override
    public void onTaskStateUpdate(int taskIndex, boolean isCompleted) {
        // This is a multiplayer-only feature, as single player state is local.
        if (isSinglePlayer || gameClient == null) {
            return;
        }

        Platform.runLater(() -> {
            List<String> tasks = gameClient.getCurrentCaseTasks();

            if (tasks != null && taskIndex >= 0 && taskIndex < tasks.size()) {
                String task = tasks.get(taskIndex);
                this.taskStates.put(task, isCompleted); // Update the local state map

                // If the tasks window is open, refresh its view to reflect the change
                if (tasksWindow != null && tasksWindow.isShowing()) {
                    tasksWindow.loadTasks(tasks, this.taskStates);
                }
            }
        });
    }
}
