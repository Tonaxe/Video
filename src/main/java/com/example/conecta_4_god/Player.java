package com.example.conecta_4_god;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Player {
    private String name;
    private Socket socket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    public Player(String name, Socket socket, BufferedReader inputReader, PrintWriter outputWriter) {
        this.name = name;
        this.socket = socket;
        this.inputReader = inputReader;
        this.outputWriter = outputWriter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public BufferedReader getInputReader() {
        return inputReader;
    }

    public void setInputReader(BufferedReader inputReader) {
        this.inputReader = inputReader;
    }

    public PrintWriter getOutputWriter() {
        return outputWriter;
    }

    public void setOutputWriter(PrintWriter outputWriter) {
        this.outputWriter = outputWriter;
    }
}
