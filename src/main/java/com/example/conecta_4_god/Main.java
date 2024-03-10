package com.example.conecta_4_god;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        // Cargar el archivo FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Conecta4.fxml"));
        Parent root = loader.load();

        // Obtener el controlador asociado al archivo FXML
        GameController controller = loader.getController();

        // Configurar el controlador (si es necesario)

        // Configurar la escena
        primaryStage.setTitle("Conecta 4");
        primaryStage.setScene(new Scene(root, 900, 600));

        // Mostrar la ventana principal
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
