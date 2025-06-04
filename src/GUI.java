import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUI extends Application {

    private final Controller controller = new Controller();
    private BorderPane rootPane;
    // private VBox sideBar; // Removed, no longer needed
    private Scene mainScene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        rootPane = new BorderPane();
        // Initialize GUI: directly show the list view as default (no sidebar)
        showListPage();

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Coin Collection");
        primaryStage.show();
    }

    private void showHomePage() {
        Label welcomeLabel = new Label("Welcome to Coin Collection!");
        rootPane.setCenter(wrapInVBox(welcomeLabel));
    }

    private void showListPage() {
        // Fetch all coins initially
        List<Coin> coinList = controller.listCoins();
        ObservableList<Coin> data = FXCollections.observableArrayList(coinList);

        // --- SEARCH CONTROLS at top ---
        Label attrLabel = new Label("Attribute:");
        ComboBox<String> attrBox = new ComboBox<>();
        attrBox.getItems().addAll(controller.getSearchableAttributes());
        attrBox.setValue("name"); // default selection

        Label valueLabel = new Label("Value:");
        TextField valueField = new TextField();
        Button searchBtn = new Button("Search");

        // --- TABLE SETUP BELOW ---
        TableView<Coin> tableView = new TableView<>(data);
        tableView.setPrefWidth(600);
        tableView.setPrefHeight(400);

        // Now add Reset button and its handler, referencing tableView safely
        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            // Clear filters and show all coins
            tableView.setItems(FXCollections.observableArrayList(controller.listCoins()));
            valueField.clear();
            attrBox.setValue("name");
        });

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(10));
        searchBar.getChildren().addAll(attrLabel, attrBox, valueLabel, valueField, searchBtn, resetBtn);

        TableColumn<Coin, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<Coin, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDate())));

        TableColumn<Coin, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getGrade()));

        TableColumn<Coin, String> thicknessCol = new TableColumn<>("Thickness");
        thicknessCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getThickness())));

        TableColumn<Coin, String> diameterCol = new TableColumn<>("Diameter");
        diameterCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDiameter())));

        TableColumn<Coin, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getWeight())));

        TableColumn<Coin, String> edgeCol = new TableColumn<>("Edge");
        edgeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEdge()));

        TableColumn<Coin, String> denomCol = new TableColumn<>("Denomination");
        denomCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDenomination()));

        TableColumn<Coin, String> compositionCol = new TableColumn<>("Composition");
        compositionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getComposition()));

        tableView.getColumns().addAll(
            nameCol,
            dateCol,
            gradeCol,
            thicknessCol,
            diameterCol,
            weightCol,
            edgeCol,
            denomCol,
            compositionCol
        );

        // Reuse existing context menu (Edit/Delete) setup:
        MenuItem editItem = new MenuItem("Edit Coin");
        editItem.setOnAction(e -> {
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditForCoin(selected);
            }
        });
        MenuItem deleteItem = new MenuItem("Delete Coin");
        deleteItem.setOnAction(e -> {
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Delete");
                alert.setHeaderText("Delete Coin");
                alert.setContentText("Are you sure you want to delete \"" +
                        selected.getName() + "\" (ID: " + selected.getId() + ")?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        controller.deleteCoin(selected);
                        data.remove(selected);
                    }
                });
            }
        });
        ContextMenu contextMenu = new ContextMenu(editItem, deleteItem);

        tableView.setRowFactory(tv -> {
            TableRow<Coin> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getIndex());
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Coin selected = row.getItem();

                    StringBuilder sb = new StringBuilder();
                    sb.append("ID: ").append(selected.getId()).append("\n");
                    sb.append("Name: ").append(selected.getName()).append("\n");
                    sb.append("Date: ").append(selected.getDate()).append("\n");
                    sb.append("Thickness: ").append(selected.getThickness()).append("\n");
                    sb.append("Diameter: ").append(selected.getDiameter()).append("\n");
                    sb.append("Grade: ").append(selected.getGrade()).append("\n");
                    sb.append("Composition: ").append(selected.getComposition()).append("\n");
                    sb.append("Denomination: ").append(selected.getDenomination()).append("\n");
                    sb.append("Edge: ").append(selected.getEdge()).append("\n");
                    sb.append("Weight: ").append(selected.getWeight());

                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("Coin Details");
                    infoAlert.setHeaderText("Details for " + selected.getName());
                    infoAlert.setContentText(sb.toString());
                    infoAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    infoAlert.showAndWait();
                }
            });
            return row;
        });

        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                Coin selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText("Delete Coin");
                    alert.setContentText("Are you sure you want to delete \"" +
                            selected.getName() + "\" (ID: " + selected.getId() + ")?");
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteCoin(selected);
                            tableView.getItems().remove(selected);
                        }
                    });
                }
            }
        });

        // Wire the Search button to filter the table data in-place:
        searchBtn.setOnAction(e -> {
            String attr = attrBox.getValue();
            String value = valueField.getText().trim();
            if (attr == null || value.isEmpty()) {
                // If no search criteria, show all coins
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins()));
            } else {
                List<Coin> matches = controller.searchCoins(attr, value);
                tableView.setItems(FXCollections.observableArrayList(matches));
            }
        });

        // Combine searchBar above the tableView in a VBox:
        VBox combined = new VBox(10);
        combined.setPadding(new Insets(10));
        combined.getChildren().addAll(searchBar, tableView);
        // Add Coin button below table
        Button addCoinBelowBtn = new Button("Add Coin");
        addCoinBelowBtn.setOnAction(e -> showAddPage());
        combined.getChildren().add(addCoinBelowBtn);

        rootPane.setCenter(combined);
    }

    private void showAddPage() {
        // Labels & fields for Name, Date, Grade
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        nameField.setMaxWidth(150);

        Label dateLabel = new Label("Date:");
        TextField dateField = new TextField();
        dateField.setMaxWidth(100);

        Label gradeLabel = new Label("Grade:");
        ComboBox<String> gradeBox = new ComboBox<>();
        gradeBox.getItems().addAll("MS", "PF", "AU", "XF", "VF", "F", "VG", "G", "AG", "FR", "PO", "N/A");
        gradeBox.setValue("N/A");
        gradeBox.setMaxWidth(80);

        TextField customGradeField = new TextField();
        customGradeField.setMaxWidth(80);
        customGradeField.setVisible(false);

        StackPane gradeContainer = new StackPane();
        gradeContainer.getChildren().addAll(gradeBox, customGradeField);

        CheckBox advancedGradeCheck = new CheckBox("Advanced Grade");
        advancedGradeCheck.setOnAction(e -> {
            boolean advanced = advancedGradeCheck.isSelected();
            gradeBox.setVisible(!advanced);
            customGradeField.setVisible(advanced);
        });

        HBox topRow = new HBox(10);
        topRow.setPadding(new Insets(20, 20, 0, 20));
        topRow.getChildren().addAll(
                nameLabel, nameField,
                dateLabel, dateField,
                gradeLabel, gradeContainer,
                advancedGradeCheck
        );

        // Additional attributes: thickness, diameter, composition, denomination, edge, weight
        Label thicknessLabel = new Label("Thickness:");
        TextField thicknessField = new TextField();
        thicknessField.setMaxWidth(100);

        Label diameterLabel = new Label("Diameter:");
        TextField diameterField = new TextField();
        diameterField.setMaxWidth(100);

        Label compositionLabel = new Label("Composition:");
        TextField compositionField = new TextField();
        compositionField.setMaxWidth(120);

        Label denominationLabel = new Label("Denomination:");
        TextField denominationField = new TextField();
        denominationField.setMaxWidth(120);

        Label edgeLabel = new Label("Edge:");
        TextField edgeField = new TextField();
        edgeField.setMaxWidth(100);

        Label weightLabel = new Label("Weight:");
        TextField weightField = new TextField();
        weightField.setMaxWidth(100);

        GridPane additionalGrid = new GridPane();
        additionalGrid.setPadding(new Insets(10, 20, 0, 20));
        additionalGrid.setHgap(10);
        additionalGrid.setVgap(10);

        additionalGrid.add(thicknessLabel, 0, 0);
        additionalGrid.add(thicknessField, 1, 0);
        additionalGrid.add(diameterLabel, 2, 0);
        additionalGrid.add(diameterField, 3, 0);

        // Swapped: edge now at row 1, composition at row 2
        additionalGrid.add(edgeLabel, 0, 1);
        additionalGrid.add(edgeField, 1, 1);
        // Swapped: weight now at row 1, denomination at row 2
        additionalGrid.add(weightLabel, 2, 1);
        additionalGrid.add(weightField, 3, 1);
        additionalGrid.add(compositionLabel, 0, 2);
        additionalGrid.add(compositionField, 1, 2);
        additionalGrid.add(denominationLabel, 2, 2);
        additionalGrid.add(denominationField, 3, 2);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> {
            // Return to the list view
            showListPage();
        });
        VBox errorBox = new VBox(5);
        Label messageLabel = new Label();

        saveBtn.setOnAction(e -> {
            errorBox.getChildren().clear();
            messageLabel.setText("");

            // Pack raw inputs into a Map<String,String>
            Map<String, String> rawFields = new HashMap<>();
            rawFields.put("name", nameField.getText().trim());
            rawFields.put("date", dateField.getText().trim());
            rawFields.put("grade", advancedGradeCheck.isSelected()
                    ? customGradeField.getText().trim()
                    : gradeBox.getValue());
            rawFields.put("thickness", thicknessField.getText().trim());
            rawFields.put("diameter", diameterField.getText().trim());
            rawFields.put("composition", compositionField.getText().trim());
            rawFields.put("denomination", denominationField.getText().trim());
            rawFields.put("edge", edgeField.getText().trim());
            rawFields.put("weight", weightField.getText().trim());

            // Delegate validation & creation to controller
            Controller.ValidationResult vr = controller.createCoin(rawFields);
            if (!vr.isValid()) {
                // Display each validation error
                for (Controller.FieldError fe : vr.getErrors()) {
                    errorBox.getChildren().add(makeError(fe.getField(), fe.getExpectedType()));
                }
            } else {
                // Creation succeeded
                messageLabel.setText("Coin added with ID: " + vr.getCreatedId());

                // Clear all inputs
                nameField.clear();
                dateField.clear();
                gradeBox.setValue("N/A");
                customGradeField.clear();
                advancedGradeCheck.setSelected(false);
                gradeBox.setVisible(true);
                customGradeField.setVisible(false);
                thicknessField.clear();
                diameterField.clear();
                compositionField.clear();
                denominationField.clear();
                edgeField.clear();
                weightField.clear();
            }
        });

        HBox buttonRow = new HBox(10);
        buttonRow.getChildren().addAll(saveBtn, cancelBtn);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().clear();
        layout.getChildren().addAll(topRow, additionalGrid, buttonRow, errorBox, messageLabel);

        rootPane.setCenter(layout);
    }


    // wrapInVBox is still used by showHomePage, so keep it
    private VBox wrapInVBox(javafx.scene.Node... nodes) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(nodes);
        return vbox;
    }

    private void showEditForCoin(Coin coin) {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField(coin.getName());
        Label dateLabel = new Label("Date:");
        TextField dateField = new TextField(String.valueOf(coin.getDate()));
        Label thicknessLabel = new Label("Thickness:");
        TextField thicknessField = new TextField(String.valueOf(coin.getThickness()));
        Label diameterLabel = new Label("Diameter:");
        TextField diameterField = new TextField(String.valueOf(coin.getDiameter()));
        Label gradeLabel = new Label("Grade:");
        TextField gradeField = new TextField(String.valueOf(coin.getGrade()));
        Label compositionLabel = new Label("Composition:");
        TextField compositionField = new TextField(coin.getComposition());
        Label denominationLabel = new Label("Denomination:");
        TextField denominationField = new TextField(coin.getDenomination());
        Label edgeLabel = new Label("Edge:");
        TextField edgeField = new TextField(coin.getEdge());
        Label weightLabel = new Label("Weight:");
        TextField weightField = new TextField(String.valueOf(coin.getWeight()));

        Button saveBtn = new Button("Save Changes");
        Button cancelBtn = new Button("Cancel");

        form.add(nameLabel, 0, 0);
        form.add(nameField, 1, 0);
        form.add(dateLabel, 0, 1);
        form.add(dateField, 1, 1);
        form.add(thicknessLabel, 0, 2);
        form.add(thicknessField, 1, 2);
        form.add(diameterLabel, 0, 3);
        form.add(diameterField, 1, 3);
        form.add(gradeLabel, 0, 4);
        form.add(gradeField, 1, 4);
        form.add(compositionLabel, 0, 5);
        form.add(compositionField, 1, 5);
        form.add(denominationLabel, 0, 6);
        form.add(denominationField, 1, 6);
        form.add(edgeLabel, 0, 7);
        form.add(edgeField, 1, 7);
        form.add(weightLabel, 0, 8);
        form.add(weightField, 1, 8);
        form.add(saveBtn, 0, 9);
        form.add(cancelBtn, 1, 9);

        saveBtn.setOnAction(e -> {
            try {
                coin.setName(nameField.getText().trim());
                coin.setDate(Integer.parseInt(dateField.getText().trim()));
                coin.setThickness(Double.parseDouble(thicknessField.getText().trim()));
                coin.setDiameter(Double.parseDouble(diameterField.getText().trim()));
                coin.setGrade(gradeField.getText().trim());
                coin.setComposition(compositionField.getText().trim());
                coin.setDenomination(denominationField.getText().trim());
                coin.setEdge(edgeField.getText().trim());
                coin.setWeight(Double.parseDouble(weightField.getText().trim()));
                controller.saveCoin(coin);
                showListPage();
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Invalid Input");
                err.setHeaderText("Parse Error");
                err.setContentText("One or more numeric fields are invalid.");
                err.showAndWait();
            }
        });

        cancelBtn.setOnAction(e -> showListPage());
        rootPane.setCenter(form);
    }

    /**
     * Renders a single-line TextFlow: “Invalid input for {field}; a(n) {type} is required”
     * with {field} and {type} colored red.
     */
    private TextFlow makeError(String field, String type) {
        return new TextFlow(
                new Text("Invalid input for "),
                createRedText(field),
                new Text("; a(n) "),
                createRedText(type),
                new Text(" is required")
        );
    }

    private Text createRedText(String content) {
        Text t = new Text(content);
        t.setFill(javafx.scene.paint.Color.RED);
        return t;
    }
}