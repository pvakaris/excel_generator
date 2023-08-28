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

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Generuoklis");
        Scene scene = createMainScene();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

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

    private Label createLogoLabel() {
        Label logoLabel = new Label("Generuoklis");
        logoLabel.setStyle("-fx-font-size: 30px;");
        return logoLabel;
    }

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

    private TextField createValueTextField() {
        TextField valueTextField = new TextField();
        valueTextField.setPromptText("Įveskite reikšmę...");
        valueTextField.setMaxWidth(200); // Limit width
        return valueTextField;
    }

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

    private Button createProcessButton() {
        Button processButton = new Button("Generuoti");
        processButton.setStyle("-fx-font-weight: bold;");
        processButton.setOnAction(e -> {
            Toggle selectedToggle = choiceGroup.getSelectedToggle();
            String selectedChoice = null;
            if (selectedToggle != null) {
                RadioButton selectedRadioButton = (RadioButton) selectedToggle;
                selectedChoice = selectedRadioButton.getText();
            }

            if (selectedFile == null || selectedFolder == null || selectedChoice == null) {
                displayError("Pasirinkite failą, išsaugojimo vietą ir būtinai pasirinkite vieną iš pasirinkimo punktų.");
            } else {
                String value = valueTextField.getText();
                if (selectedChoice.equals("Procentai")) {
                    try {
                        double proportionValue = Double.parseDouble(value);
                        if (proportionValue >= 0 && proportionValue <= 100) {
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

