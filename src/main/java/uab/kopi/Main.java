package uab.kopi;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uab.kopi.services.ExcelProcessor;

import java.io.File;

import static uab.kopi.services.Alerter.displayError;

/**
 * The main class for the data processing application. It provides a graphical user interface for randomly selecting data
 * from an Excel file and then saving the selected data to a new Excel file. Additionally, it generates an explanatory
 * text file that lists the records taken from the source file.
 */
public class Main extends Application {

    private File selectedFile;
    private File selectedFolder;
    private Stage primaryStage;
    private Label importedFileLabel;
    private Label selectedFolderLabel;
    private ToggleGroup choiceGroup;
    private TextField valueTextField;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes and displays the primary stage of the JavaFX application.
     *
     * @param stage The primary stage of the application.
     */
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Generuoklis");
        Scene scene = createMainScene();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Creates the main scene of the application, containing UI elements for importing files, setting processing options,
     * and generating processed data.
     *
     * @return The main scene of the application.
     */
    private Scene createMainScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label logoLabel = createLogoLabel();
        Button importButton = createImportButton();
        importedFileLabel = new Label("Importuotas failas: Nepasirinkta");
        HBox choiceBox = createChoiceBox();
        valueTextField = createValueTextField();
        Button selectDestinationButton = createSelectDestinationButton();
        selectedFolderLabel = new Label("Išsaugojimo vieta: Nepasirinkta");
        Button processButton = createProcessButton();

        root.getChildren().addAll(
                logoLabel,
                importButton,
                importedFileLabel,
                choiceBox,
                valueTextField,
                selectDestinationButton,
                selectedFolderLabel,
                processButton
        );

        Scene scene = new Scene(root, 400, 500);
        scene.getRoot().setStyle("-fx-font-family: 'serif'");
        return scene;
    }

    /**
     * Creates a label displaying the application logo.
     *
     * @return The label displaying the application logo.
     */
    private Label createLogoLabel() {
        Label logoLabel = new Label("Generuoklis");
        logoLabel.setStyle("-fx-font-size: 30px;");
        return logoLabel;
    }

    /**
     * Creates a button for importing Excel files and sets its action to open a FileChooser dialog.
     *
     * @return The button for importing Excel files.
     */
    private Button createImportButton() {
        Button importButton = new Button("Importuoti Excel failą");
        importButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Pasirinkti Excel failą");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx")
            );
            selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                importedFileLabel.setText("Importuotas failas: " + selectedFile.getName());
            }
        });
        return importButton;
    }

    /**
     * Creates a horizontal box containing radio buttons for choosing the processing option: percentage or quantity.
     *
     * @return The horizontal box with choice radio buttons.
     */
    private HBox createChoiceBox() {
        choiceGroup = new ToggleGroup();
        RadioButton proportionRadioButton = new RadioButton("Procentai");
        proportionRadioButton.setToggleGroup(choiceGroup);
        RadioButton amountRadioButton = new RadioButton("Kiekis");
        amountRadioButton.setToggleGroup(choiceGroup);
        HBox choiceBox = new HBox(10, proportionRadioButton, amountRadioButton);
        choiceBox.setAlignment(Pos.CENTER);
        return choiceBox;
    }

    /**
     * Creates a text field for entering the value for processing.
     *
     * @return The text field for entering the processing value.
     */
    private TextField createValueTextField() {
        TextField valueTextField = new TextField();
        valueTextField.setPromptText("Įveskite reikšmę...");
        valueTextField.setMaxWidth(200); // Limit width
        return valueTextField;
    }

    /**
     * Creates a button for selecting the destination folder and sets its action to open a DirectoryChooser dialog.
     *
     * @return The button for selecting the destination folder.
     */
    private Button createSelectDestinationButton() {
        Button selectDestinationButton = new Button("Pasirinkti išsaugojimo vietą");
        selectDestinationButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Pasirinkti išsaugojimo vietą");
            selectedFolder = directoryChooser.showDialog(primaryStage);
            if (selectedFolder != null) {
                selectedFolderLabel.setText("Išsaugojimo vieta: " + selectedFolder.getAbsolutePath());
            }
        });
        return selectDestinationButton;
    }

    /**
     * Creates a button for initiating the data processing based on the chosen options.
     *
     * @return The button for processing data.
     */
    private Button createProcessButton() {
        Button processButton = new Button("Generuoti");
        processButton.setStyle("-fx-font-weight: bold;");

        // Set the action to be executed when the button is clicked
        processButton.setOnAction(e -> {
            // Retrieve the selected toggle (choice) from the choiceGroup
            Toggle selectedToggle = choiceGroup.getSelectedToggle();
            String selectedChoice = null;

            // Check if a toggle is selected
            if (selectedToggle != null) {
                // Cast the selected toggle to a RadioButton to get the chosen processing option
                RadioButton selectedRadioButton = (RadioButton) selectedToggle;
                selectedChoice = selectedRadioButton.getText();
            }

            // Validate whether necessary selections and inputs are made
            if (selectedFile == null || selectedFolder == null || selectedChoice == null) {
                displayError("Pasirinkite failą, išsaugojimo vietą ir būtinai pasirinkite vieną iš pasirinkimo punktų.");
            } else {
                String value = valueTextField.getText();
                if (selectedChoice.equals("Procentai")) {
                    try {
                        double proportionValue = Double.parseDouble(value);
                        if (proportionValue >= 0 && proportionValue <= 100) {
                            // Initiate data processing with the selected options
                            ExcelProcessor.processFile(selectedFile, selectedFolder, proportionValue, true);
                        } else {
                            displayError("Procentai turi būti nuo 0 iki 100.");
                        }
                    } catch (NumberFormatException ex) {
                        displayError("Netinkama skaitinė reikšmė procentams.");
                    }
                } else if (selectedChoice.equals("Kiekis")) {
                    try {
                        int amountValue = Integer.parseInt(value);
                        if (amountValue >= 0) {
                            // Initiate data processing with the selected options
                            ExcelProcessor.processFile(selectedFile, selectedFolder, amountValue, false);
                        } else {
                            displayError("Kiekis turi būti neneigiamas sveikasis skaičius.");
                        }
                    } catch (NumberFormatException ex) {
                        displayError("Netinkama skaitinė reikšmė kiekiui.");
                    }
                }
            }
        });

        return processButton;
    }
}

