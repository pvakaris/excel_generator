package uab.kopi.services;

import javafx.scene.control.Alert;

public class Alerter {

    public static void displayError(String message) {
        showAlert(Alert.AlertType.ERROR, "Klaida", message);
    }

    public static void displayResult(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Rezultatas", message);
    }

    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.getDialogPane().setStyle("-fx-font-family: 'serif'");
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
