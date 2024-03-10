module com.example.conecta_4_god {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.conecta_4_god to javafx.fxml;
    exports com.example.conecta_4_god;
}