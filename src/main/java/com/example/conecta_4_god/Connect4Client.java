package com.example.conecta_4_god;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

public class Connect4Client extends Application {

    private static final int PORT = 8888;
    private static final String SERVER_IP = "localhost";

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private BorderPane root;
    private GridPane gameGrid;
    private Circle[][] circles;

    private int[][] board = new int[7][6]; // Connect 4 board
    private String playerName;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Connect 4 - Client");
        dialog.setHeaderText("Escribe tu nombre:");
        dialog.setContentText("Nombre:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            playerName = result.get();
        } else {
            Platform.exit();
            return;
        }

        root = new BorderPane();
        gameGrid = new GridPane();
        circles = new Circle[7][6];

        initializeBoard();

        root.setCenter(gameGrid);

        Scene scene = new Scene(root, 427, 370);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Connect 4 - Client");
        primaryStage.setOnCloseRequest(event -> closeConnection());
        primaryStage.show();
        connectToServer();
    }

    private void initializeBoard() {
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                Circle circle = new Circle(30);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);

                int columnIndex = col;
                circle.setOnMouseClicked(event -> handleCellClick(columnIndex));

                gameGrid.add(circle, col, row);
                circles[col][row] = circle;
            }
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server.");

            // Envía el nombre del jugador al servidor
            writer.println("PLAYER_NAME:" + playerName);

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> processMessage(finalMessage));
            }

            // En el método receiveMessages()
            if (message.startsWith("PLAYER_COLOR:")) {
                String playerColor = message.substring("PLAYER_COLOR:".length());
                if (playerColor.equals("RED")) {
                    // Asignar color rojo al jugador
                } else if (playerColor.equals("YELLOW")) {
                    // Asignar color amarillo al jugador
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void processMessage(String message) {
        if (message.startsWith("GAME_STATE:")) {
            String gameState = message.substring("GAME_STATE:".length());
            Platform.runLater(() -> updateBoard(gameState));
        } else if (message.startsWith("GAME_OVER:")) {
            String result = message.substring("GAME_OVER:".length());
            handleGameOver(result);
        }
    }

    private void updateBoard(String gameState) {
        int index = 0;
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                board[col][row] = gameState.charAt(index++) - '0';
                circles[col][row].setFill(board[col][row] == 0 ? Color.WHITE :
                        (board[col][row] == 1 ? Color.RED : Color.YELLOW));
            }
        }
    }

    private void handleCellClick(int columnIndex) {
        writer.println("MOVE:" + columnIndex);
    }

    private void handleGameOver(String result) {
        if (result.startsWith("Winner")) {
            int winnerIndex = Integer.parseInt(result.substring("Winner".length()));
            if (winnerIndex == 0 || winnerIndex == 1) {
                String winnerColor = (winnerIndex == 0) ? "ROJO" : "AMARILLO";
                Platform.runLater(() -> {
                    showAlert("¡El jugador " + winnerColor + " ha ganado! ¡Fin del juego!");
                    writer.println("RESET_BOARD");
                    initializeBoard(); // Reinicia el tablero
                });
            }
        } else if (result.equals("Draw")) {
            Platform.runLater(() -> {
                showAlert("¡El juego ha terminado en empate!");
                writer.println("RESET_BOARD");
                initializeBoard(); // Reinicia el tablero
            });
        }
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fin del juego");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
