package com.example.conecta_4_god;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Connect4Server {

    private static final int PORT = 8888;
    private static final int MAX_CLIENTS = 2;

    private List<ClientHandler> clients;
    private ServerSocket serverSocket;
    private int currentPlayerIndex = 0;
    private int[][] board = new int[7][6];

    public Connect4Server() {
        clients = new ArrayList<>();
        initializeBoard();
        startServer();
    }

    private void initializeBoard() {
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                board[col][row] = 0;
            }
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                if (clients.size() < MAX_CLIENTS) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket);
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();

                    // Verificar si hay al menos dos clientes antes de enviar mensajes
                    if (clients.size() == 2) {
                        clients.get(0).sendMessage("PLAYER_COLOR:RED");
                        clients.get(1).sendMessage("PLAYER_COLOR:YELLOW");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private synchronized void handleMove(int columnIndex) {
        // Actualizar el tablero con el movimiento del jugador actual
        for (int row = 5; row >= 0; row--) {
            if (board[columnIndex][row] == 0) {
                board[columnIndex][row] = currentPlayerIndex + 1; // jugador 1: 1, jugador 2: 2
                break;
            }
        }

        // Verificar si hay un ganador después del movimiento
        if (checkWinner(columnIndex)) {
            broadcast("GAME_OVER:Winner" + currentPlayerIndex); // Enviar mensaje de ganador
            return; // Salir del método después de enviar el mensaje de fin de juego
        } else if (isBoardFull()) {
            broadcast("GAME_OVER:Draw"); // Enviar mensaje de empate
            return; // Salir del método después de enviar el mensaje de fin de juego
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % MAX_CLIENTS;
        broadcastGameState();
    }

    private synchronized boolean checkWinner(int columnIndex) {
        int currentPlayer = currentPlayerIndex + 1; // jugador 1: 1, jugador 2: 2

        // Verificar vertical
        int row = findRow(columnIndex);
        if (row != -1 && checkVertical(columnIndex, row, currentPlayer)) {
            return true;
        }

        // Verificar horizontal
        if (checkHorizontal(columnIndex, currentPlayer)) {
            return true;
        }

        // Verificar diagonal ascendente y descendente
        if (row != -1 && checkDiagonal(columnIndex, row, currentPlayer)) {
            return true;
        }

        return false;
    }

    private synchronized int findRow(int columnIndex) {
        for (int row = 0; row < 6; row++) {
            if (board[columnIndex][row] == 0) {
                return row;
            }
        }
        return -1; // Columna llena
    }

    private synchronized boolean checkVertical(int columnIndex, int rowIndex, int currentPlayer) {
        int count = 1; // Contar la ficha actual
        // Verificar hacia abajo
        for (int row = rowIndex + 1; row < 6; row++) {
            if (board[columnIndex][row] == currentPlayer) {
                count++;
                if (count == 4) {
                    return true; // Conecta 4 en vertical
                }
            } else {
                break; // Interrupción de la secuencia
            }
        }
        // Verificar hacia arriba
        for (int row = rowIndex - 1; row >= 0; row--) {
            if (board[columnIndex][row] == currentPlayer) {
                count++;
                if (count == 4) {
                    return true; // Conecta 4 en vertical
                }
            } else {
                break; // Interrupción de la secuencia
            }
        }
        return false;
    }

    private synchronized boolean checkHorizontal(int columnIndex, int currentPlayer) {
        int count = 0;
        for (int row = 0; row < 6; row++) {
            if (board[columnIndex][row] == currentPlayer) {
                count++;
                if (count == 4) {
                    return true; // Conecta 4 en horizontal
                }
            } else {
                count = 0; // Reiniciar contador si la secuencia se rompe
            }
        }
        return false;
    }

    private synchronized boolean checkDiagonal(int columnIndex, int rowIndex, int currentPlayer) {
        // Verificar diagonal ascendente
        int count = 1; // Contar la ficha actual
        int col = columnIndex - 1;
        int row = rowIndex - 1;
        while (col >= 0 && row >= 0 && board[col][row] == currentPlayer) {
            count++;
            col--;
            row--;
        }
        col = columnIndex + 1;
        row = rowIndex + 1;
        while (col < 7 && row < 6 && board[col][row] == currentPlayer) {
            count++;
            col++;
            row++;
        }
        if (count >= 4) {
            return true; // Conecta 4 en diagonal ascendente
        }

        // Verificar diagonal descendente
        count = 1; // Reiniciar contador
        col = columnIndex - 1;
        row = rowIndex + 1;
        while (col >= 0 && row < 6 && board[col][row] == currentPlayer) {
            count++;
            col--;
            row++;
        }
        col = columnIndex + 1;
        row = rowIndex - 1;
        while (col < 7 && row >= 0 && board[col][row] == currentPlayer) {
            count++;
            col++;
            row--;
        }
        return count >= 4; // Conecta 4 en diagonal descendente
    }

    private synchronized boolean isBoardFull() {
        for (int col = 0; col < 7; col++) {
            if (board[col][5] == 0) {
                return false; // Todavía hay espacio en al menos una columna
            }
        }
        return true; // Tablero lleno
    }

    private synchronized void broadcastGameState() {
        StringBuilder gameState = new StringBuilder("GAME_STATE:");
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 6; row++) {
                gameState.append(board[col][row]);
            }
        }
        String gameStateMessage = gameState.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(gameStateMessage);
        }
    }

    private class ClientHandler implements Runnable {

        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            try {
                clientSocket = socket;
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // Recibe el nombre del jugador
                String playerName = reader.readLine();
                System.out.println("Player connected: " + playerName);

                String input;
                while ((input = reader.readLine()) != null) {
                    if (input.startsWith("MOVE:")) {
                        int columnIndex = Integer.parseInt(input.substring("MOVE:".length()));
                        handleMove(columnIndex);
                    } else if (input.equals("RESET_BOARD")) { // Manejar el mensaje para reiniciar el tablero
                        handleResetBoard();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Cliente desconectado, eliminarlo de la lista de clientes y cerrar el socket
                clients.remove(this);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }
    }

    private synchronized void handleResetBoard() {
        initializeBoard(); // Reiniciar el tablero
        broadcastGameState(); // Enviar el estado del tablero reiniciado a todos los clientes
    }

    public static void main(String[] args) {
        new Connect4Server();
    }
}
