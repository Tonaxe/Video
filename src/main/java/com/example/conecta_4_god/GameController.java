package com.example.conecta_4_god;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class GameController {

    private Socket socket;
    private BufferedReader serverInput;
    private PrintWriter serverOutput;

    @FXML
    private GridPane gameGrid;

    @FXML
    private CheckBox vsAI;

    @FXML
    private CheckBox vsPlayer;

    @FXML
    private Button playButton;

    @FXML
    private Circle turnIndicator;

    @FXML
    private TextField playerName;

    @FXML
    private TextField opponentName;

    @FXML
    private Label turnLabel;

    @FXML
    private MenuItem creadorMenuItem;

    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private BorderPane gameBorderPane;

    @FXML
    private Pane bottomPane;

    @FXML
    private Pane leftPane;

    @FXML
    private Pane rightPane;

    private boolean isVsAI;
    private boolean isPlayerOnTurn = true;  // true: player1, false: player2
    private int[][] board = new int[7][6];  // Connect 4 board

    private int currentPlayer = 1;  // Inicializa con el jugador 1

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    @FXML
    private void initialize() {
        // Configurar la inicialización del controlador
        resetBoard();
        updatePlayerTurnLabel();
        turnIndicator.setFill(Color.WHITE);
        creadorMenuItem.setOnAction(event -> showCreatorInfo());
        exitMenuItem.setOnAction(event -> {
            // Cierra la aplicación
            Platform.exit();
            System.exit(0);
        });

        // Agregar un listener al checkbox vsPlayer para manejar la lógica cuando se hace clic
        vsPlayer.setOnAction(event -> handleVsPlayerCheckbox());
    }

    @FXML
    private void handleVsPlayerCheckbox() {
        if (vsPlayer.isSelected()) {
            // El cliente ha seleccionado jugar contra otro jugador
            playerName.setEditable(true); // Permitir al cliente ingresar su nombre

            // Envía un mensaje al servidor para indicar que el cliente está listo para jugar
            sendReadyToPlayMessage();
        } else {
            // El cliente ha deseleccionado jugar contra otro jugador
            playerName.setEditable(false); // Deshabilitar la edición del nombre
        }
    }

    private void sendReadyToPlayMessage() {
        try {
            serverOutput.println("READY_TO_PLAY:" + playerName.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void resetBoard() {
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                board[col][row] = 0;
            }
        }
    }

    @FXML
    private void handlePlayButton() {
        // Verifica si vsAI está seleccionado
        if (vsAI.isSelected() && !vsPlayer.isSelected()) {
            isVsAI = true;
            // Inicia el juego contra la IA
            startGameAgainstAI();
        } else if (vsPlayer.isSelected() && !vsAI.isSelected()) {
            isVsAI = false;
            // Inicia el juego contra otro jugador
            startGameAgainstPlayer();
        } else {
            // Ambos o ninguno están seleccionados, muestra una alerta de error
            showError("Selecciona solo una opción: 1 vs 1 con IA o 1 vs 1 con PJ");
            return; // Detén la ejecución para evitar iniciar el juego con opciones incorrectas
        }

        // Actualiza el color del indicador de turno antes de iniciar el juego
        updateTurnIndicatorColor();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateTurnIndicatorColor() {
        // Actualiza el color del indicador de turno según el jugador actual
        turnIndicator.setFill(currentPlayer == 1 ? Color.RED : Color.YELLOW);
    }

    private void startGameAgainstAI() {
        // Lógica para iniciar el juego contra la IA
        resetBoard();
        initializeBoard();

        new Thread(() -> {
            Random random = new Random();

            while (!isGameOver()) {
                try {
                    Thread.sleep(2000); // Espera de 1 segundo antes de que la IA haga su movimiento
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Platform.runLater(() -> {
                    if (!isGameOver() && !isPlayerOnTurn) {
                        int column = random.nextInt(7); // Elige una columna aleatoria (de 0 a 6)
                        handleCellClick(column);
                    }
                });
            }
        }).start();
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setServerInput(BufferedReader serverInput) {
        this.serverInput = serverInput;
    }

    public void setServerOutput(PrintWriter serverOutput) {
        this.serverOutput = serverOutput;
    }

    private void startGameAgainstPlayer() {
        // Lógica para iniciar el juego contra otro jugador
        resetBoard();
        initializeBoard();
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(8888);
            clientSocket = serverSocket.accept();
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        int column = (int) objectInputStream.readObject();
                        handleCellClick(column);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeBoard() {
        gameGrid.getChildren().clear();

        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                Circle circle = new Circle(30);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);

                int columnIndex = col;
                circle.setOnMouseClicked(event -> handleCellClick(columnIndex));

                gameGrid.add(circle, col, row);
            }
        }
    }

    private boolean checkWinner(int columnIndex, int rowIndex, int currentPlayer) {

        if (checkVertical(columnIndex, currentPlayer)) return true;

        if (checkHorizontal(rowIndex, currentPlayer)) return true;

        if (checkDiagonal(currentPlayer)) return true;

        return false;
    }

    private boolean checkVertical(int columnIndex, int currentPlayer) {
        if (columnIndex < 0 || columnIndex >= board.length) {
            return false; // Índice de columna fuera de los límites
        }

        int count = 0;
        for (int row = 0; row < board[columnIndex].length; row++) {
            if (row < 0 || row >= board[columnIndex].length) {
                continue; // Índice de fila fuera de los límites, continuar con la siguiente iteración
            }

            if (board[columnIndex][row] == currentPlayer) {
                count++;
                if (count == 4) return true; // Conecta 4 en la columna
            } else {
                count = 0; // Reinicia la cuenta si no hay una ficha del jugador actual en la posición actual
            }
        }
        return false;
    }

    private boolean checkHorizontal(int rowIndex, int currentPlayer) {
        if (rowIndex < 0 || rowIndex >= board[0].length) {
            return false; // Índice de fila fuera de los límites
        }

        int count = 0;
        for (int col = 0; col < board.length; col++) {
            if (col < 0 || col >= board.length) {
                continue; // Índice de columna fuera de los límites, continuar con la siguiente iteración
            }

            if (board[col][rowIndex] == currentPlayer) {
                count++;
                if (count == 4) return true; // Conecta 4 en la fila
            } else {
                count = 0; // Reinicia la cuenta si no hay una ficha del jugador actual en la posición actual
            }
        }
        return false;
    }
    private boolean checkDiagonal(int currentPlayer) {
        // Verificar diagonales ascendentes
        for (int col = 0; col <= board.length - 4; col++) {
            for (int row = 0; row <= board[col].length - 4; row++) {
                if (checkDiagonalAscendente(col, row, currentPlayer)) {
                    return true;
                }
            }
        }

        // Verificar diagonales descendentes
        for (int col = 0; col <= board.length - 4; col++) {
            for (int row = 3; row < board[col].length; row++) {
                if (checkDiagonalDescendente(col, row, currentPlayer)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkDiagonalAscendente(int startCol, int startRow, int currentPlayer) {
        for (int i = 0; i < 4; i++) {
            if (board[startCol + i][startRow + i] != currentPlayer) {
                return false;
            }
        }
        return true; // Conecta 4 en diagonal ascendente
    }

    private boolean checkDiagonalDescendente(int startCol, int startRow, int currentPlayer) {
        for (int i = 0; i < 4; i++) {
            if (board[startCol + i][startRow - i] != currentPlayer) {
                return false;
            }
        }
        return true; // Conecta 4 en diagonal descendente
    }

    @FXML
    private void handleCellClick(int columnIndex) {
        if (isGameOver()) {
            // El juego ha terminado, puedes mostrar un mensaje o realizar alguna acción
            return;
        }

        int row = findEmptyRow(columnIndex);
        if (row != -1) {
            int currentPlayer = getCurrentPlayer();
            placePiece(columnIndex, row, currentPlayer);
            sendColumnToOpponent(columnIndex);

            if (checkWinner(columnIndex, row, currentPlayer)) {
                // El jugador actual ha ganado, realiza las acciones necesarias
                handleGameEnd(currentPlayer);
            } else if (isBoardFull()) {
                // El tablero está lleno, pero no hay un ganador (empate)
                handleGameEnd(-1);
            } else {
                // Cambia al siguiente jugador
                switchPlayer();
            }
        }
    }

    private void sendColumnToOpponent(int columnIndex) {
        try {
            objectOutputStream.writeObject(columnIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isGameOver() {
        return isBoardFull() || checkWinner(-1, -1, -1); // Modifica según tu lógica de finalización del juego
    }

    private int getCurrentPlayer() {
        return currentPlayer;
    }

    private void placePiece(int columnIndex, int rowIndex, int currentPlayer) {
        // Actualizar la matriz board
        board[columnIndex][rowIndex] = currentPlayer;

        // Crear un nuevo círculo para representar la ficha
        Circle piece = new Circle(33.0);
        piece.setStroke(Color.BLACK);

        // Establecer el color de la ficha según el jugador actual
        piece.setFill(currentPlayer == 1 ? Color.RED : Color.YELLOW);

        // Añadir la ficha a la posición correspondiente en el GridPane
        gameGrid.add(piece, columnIndex, rowIndex);
    }

    private void switchPlayer() {
        // Cambia el jugador actual
        currentPlayer = (currentPlayer == 1) ? 2 : 1;

        // Actualiza la interfaz gráfica para mostrar qué jugador tiene el turno
        updatePlayerTurnLabel();

        // Actualiza el estado del jugador que tiene el turno
        isPlayerOnTurn = (currentPlayer == 1);
    }

    private void updatePlayerTurnLabel() {
        turnLabel.setText("Turno del Jugador: " + currentPlayer);

        // Cambia el color del círculo según el jugador actual
        Paint color = (currentPlayer == 1) ? Color.RED : Color.YELLOW;
        turnIndicator.setFill(color);
    }

    private void handleGameEnd(int winner) {
        if (winner != -1) {
            String winnerName = (winner == 1) ? playerName.getText() : opponentName.getText();
            String message = "¡Jugador " + winnerName + " ha ganado!";
            showAlert("Partida Finalizada", message);
        } else {
            showAlert("Partida Finalizada", "Empate. No hay un ganador.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private int findEmptyRow(int columnIndex) {
        for (int row = board[columnIndex].length - 1; row >= 0; row--) {
            if (board[columnIndex][row] == 0) {
                return row; // Encuentra la fila vacía más baja en la columna
            }
        }
        return -1; // Retorna -1 si la columna está llena
    }

    private boolean isBoardFull() {
        for (int col = 0; col < board.length; col++) {
            if (board[col][0] == 0) {
                return false; // Si hay al menos una columna con espacio, el tablero no está lleno
            }
        }
        return true; // Si no hay ninguna columna con espacio, el tablero está lleno
    }

    private void showCreatorInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información del Creador");
        alert.setHeaderText(null);
        alert.setContentText("Creador: Yassin Mennana Belkas");
        alert.showAndWait();
    }

    public void changeThemeToBlanco() {
        // Limpiar las clases de estilo existentes
        bottomPane.getStyleClass().clear();
        leftPane.getStyleClass().clear();
        rightPane.getStyleClass().clear();
        gameGrid.getStyleClass().clear();

        // Aplicar la clase de estilo al Pane de abajo
        bottomPane.getStyleClass().add("fondo-blanco");

        // Aplicar la clase de estilo al Pane de la izquierda
        leftPane.getStyleClass().add("fondo-blanco");

        // Aplicar la clase de estilo al Pane de la derecha
        rightPane.getStyleClass().add("fondo-blanco");

        // Aplicar la clase de estilo al GridPane
        gameGrid.getStyleClass().add("fondo-blanco");
    }

    public void changeThemeToOscuro() {
        // Limpiar las clases de estilo existentes
        bottomPane.getStyleClass().clear();
        leftPane.getStyleClass().clear();
        rightPane.getStyleClass().clear();
        gameGrid.getStyleClass().clear();

        // Aplicar la clase de estilo al Pane de abajo
        bottomPane.getStyleClass().add("fondo-oscuro");

        // Aplicar la clase de estilo al Pane de la izquierda
        leftPane.getStyleClass().add("fondo-oscuro");

        // Aplicar la clase de estilo al Pane de la derecha
        rightPane.getStyleClass().add("fondo-oscuro");

        // Aplicar la clase de estilo al GridPane
        gameGrid.getStyleClass().add("fondo-oscuro");
    }

}
