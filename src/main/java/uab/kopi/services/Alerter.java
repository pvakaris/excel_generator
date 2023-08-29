package uab.kopi.services;

import javafx.scene.control.Alert;

/**
 * A utility class for displaying alert messages with different types (error or result) using JavaFX's Alert dialog.
 */
public class Alerter {

    /**
     * Displays an error alert with the provided error message.
     *
     * @param message The error message to be displayed.
     */
    public static void displayError(String message) {
        showAlert(Alert.AlertType.ERROR, "Klaida", message);
    }

    /**
     * Displays a result alert with the provided result message.
     *
     * @param message The result message to be displayed.
     */
    public static void displayResult(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Rezultatas", message);
    }

    /**
     * Displays an alert with the specified type, title, and message.
     *
     * @param type    The type of alert (error, information, etc.).
     * @param title   The title of the alert.
     * @param message The message content of the alert.
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.getDialogPane().setStyle("-fx-font-family: 'serif'");
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

