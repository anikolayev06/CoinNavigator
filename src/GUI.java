import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main JavaFX application.
 * Shows a left‐hand “tab bar” of all lists, with “+” at the bottom to add new lists.
 * The center shows Search controls, table of coins, and Add/Delete buttons.
 * Errors from Controller appear at the top in red.
 */
public class GUI extends Application {

    private VBox tabBar;
    private final Controller controller = new Controller();
    private BorderPane rootPane;
    private Scene mainScene;
    private String currentList; // the name of the currently selected list

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        rootPane = new BorderPane();

        // Attempt to fetch all list names. If the database is corrupted (or empty),
        // delete coins.db and recreate defaults.
        List<String> allLists = new ArrayList<>();
        try {
            allLists = controller.getAllListNames();
        } catch (Exception e) {
            // If something goes wrong (e.g. corrupted file), delete and start fresh
            new File("coins.db").delete();
            allLists = new ArrayList<>();
        }

        // If still empty, create our two defaults
        if (allLists.isEmpty()) {
            controller.createList("Owned");
            controller.createList("Wishlist");
            allLists = controller.getAllListNames();
        }

        // If for any reason it’s still empty (shouldn’t be), default to “Owned”
        currentList = allLists.isEmpty() ? "Owned" : allLists.get(0);

        showListPage();

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Coin Collection");
        primaryStage.show();
    }

    private void showListPage() {
        // Clear any previously stored errors
        controller.clearErrorBox();

        // 1) Fetch all coins in currentList (may be empty)
        List<Coin> coinList = controller.listCoins(currentList);
        ObservableList<Coin> data = FXCollections.observableArrayList(coinList);

        // — Create an “error display” box at the very top —
        VBox errorDisplay = new VBox(5);
        errorDisplay.setPadding(new Insets(10));

        // A helper to refresh the error display whenever controller.errorBox is updated:
        Runnable updateErrorDisplay = () -> {
            errorDisplay.getChildren().clear();
            for (String msg : controller.getErrorBox()) {
                Label lbl = new Label(msg);
                lbl.setTextFill(Color.RED);
                errorDisplay.getChildren().add(lbl);
            }
        };

        // — SEARCH CONTROLS at top —
        Label attrLabel = new Label("Attribute:");
        ComboBox<String> attrBox = new ComboBox<>();
        attrBox.getItems().addAll(controller.getSearchableAttributes());
        attrBox.setValue("name"); // default

        Label valueLabel = new Label("Value:");
        TextField valueField = new TextField();
        Button searchBtn = new Button("Search");

        // “Reset” button to clear filter
        Button resetBtn = new Button("Reset");

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(10));
        searchBar.getChildren().addAll(attrLabel, attrBox, valueLabel, valueField, searchBtn, resetBtn);

        // — TABLE SETUP —
        final TableView<Coin> tableView = new TableView<>(data);
        tableView.setPrefWidth(600);
        tableView.setPrefHeight(400);

        TableColumn<Coin, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Coin, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDate())));

        TableColumn<Coin, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGrade()));

        TableColumn<Coin, String> thicknessCol = new TableColumn<>("Thickness");
        thicknessCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getThickness())));

        TableColumn<Coin, String> diameterCol = new TableColumn<>("Diameter");
        diameterCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDiameter())));

        TableColumn<Coin, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getWeight())));

        TableColumn<Coin, String> edgeCol = new TableColumn<>("Edge");
        edgeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEdge()));

        TableColumn<Coin, String> denomCol = new TableColumn<>("Denomination");
        denomCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDenomination()));

        TableColumn<Coin, String> compositionCol = new TableColumn<>("Composition");
        compositionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getComposition()));

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

        // — Context menu: Edit / Delete —
        MenuItem editItem = new MenuItem("Edit Coin");
        editItem.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditForCoin(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete Coin");
        deleteItem.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Delete");
                alert.setHeaderText("Delete Coin");
                alert.setContentText("Are you sure you want to delete \"" +
                        selected.getName() + "\" (ID: " + selected.getId() + ")?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        controller.deleteCoin(currentList, selected);
                        data.remove(selected);
                        updateErrorDisplay.run();
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
                    Coin sel = row.getItem();
                    StringBuilder sb = new StringBuilder();
                    sb.append("ID: ").append(sel.getId()).append("\n");
                    sb.append("Name: ").append(sel.getName()).append("\n");
                    sb.append("Date: ").append(sel.getDate()).append("\n");
                    sb.append("Thickness: ").append(sel.getThickness()).append("\n");
                    sb.append("Diameter: ").append(sel.getDiameter()).append("\n");
                    sb.append("Grade: ").append(sel.getGrade()).append("\n");
                    sb.append("Composition: ").append(sel.getComposition()).append("\n");
                    sb.append("Denomination: ").append(sel.getDenomination()).append("\n");
                    sb.append("Edge: ").append(sel.getEdge()).append("\n");
                    sb.append("Weight: ").append(sel.getWeight());

                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Coin Details");
                    info.setHeaderText("Details for " + sel.getName());
                    info.setContentText(sb.toString());
                    info.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    info.showAndWait();
                }
            });
            return row;
        });

        // — Backspace key → prompt delete —
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                controller.clearErrorBox();
                updateErrorDisplay.run();
                Coin selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText("Delete Coin");
                    alert.setContentText("Are you sure you want to delete \"" +
                            selected.getName() + "\" (ID: " + selected.getId() + ")?");
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteCoin(currentList, selected);
                            tableView.getItems().remove(selected);
                            updateErrorDisplay.run();
                        }
                    });
                }
            }
        });

        // — Search button: filter in‐place —
        searchBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            String attr = attrBox.getValue();
            String val = valueField.getText().trim();
            if (attr == null || val.isEmpty()) {
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
            } else {
                List<Coin> matches = controller.searchCoins(currentList, attr, val);
                tableView.setItems(FXCollections.observableArrayList(matches));
            }
            updateErrorDisplay.run();
        });

        // — Reset button: clear filter —
        resetBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
            valueField.clear();
            attrBox.setValue("name");
        });

        // — “Add Coin” button below the table —
        Button addCoinBelowBtn = new Button("Add Coin");
        addCoinBelowBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            showAddPage();
        });

        // — “Delete Database” button next to Add Coin —
        Button deleteListBtn = new Button("Delete Database");
        deleteListBtn.setOnAction(ev -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            boolean success = controller.deleteList(currentList);
            if (!success) {
                // errorBox was populated by controller.deleteList
                updateErrorDisplay.run();
            } else {
                // Remove that toggle button from the tab bar
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tb && tb.getText().equals(currentList)) {
                        tabBar.getChildren().remove(node);
                        break;
                    }
                }
                // Select first available list (skip the “+” button)
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tb && !"+".equals(tb.getText())) {
                        currentList = tb.getText();
                        tb.setSelected(true);
                        tb.setStyle("-fx-background-color: lightgray;");
                        break;
                    }
                }
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
                updateErrorDisplay.run();
            }
        });

        // — Combine searchBar, tableView, buttons & errorDisplay in a VBox —
        VBox combined = new VBox(10);
        combined.setPadding(new Insets(10));
        combined.getChildren().addAll(
                errorDisplay,
                searchBar,
                tableView
        );

        HBox buttonRow = new HBox();
        buttonRow.setPadding(new Insets(10, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().addAll(addCoinBelowBtn, spacer, deleteListBtn);

        combined.getChildren().add(buttonRow);

        // — TAB BAR on the left for switching among lists —
        tabBar = new VBox(10);
        tabBar.setPadding(new Insets(10));

        // spacer to push list buttons down to align with table top
        Region topSpacer = new Region();
        topSpacer.prefHeightProperty().bind(
                errorDisplay.heightProperty().add(searchBar.heightProperty())
        );
        tabBar.getChildren().add(topSpacer);

        ToggleGroup tg = new ToggleGroup();
        for (String listName : controller.getAllListNames()) {
            ToggleButton tb = new ToggleButton(listName);
            tb.setToggleGroup(tg);
            if (listName.equals(currentList)) {
                tb.setSelected(true);
                tb.setStyle("-fx-background-color: lightgray;");
            }
            tb.setOnAction(evt -> {
                controller.clearErrorBox();
                updateErrorDisplay.run();
                currentList = listName;
                // reset styling on all buttons
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                tb.setStyle("-fx-background-color: lightgray;");
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
                updateErrorDisplay.run();
            });
            tabBar.getChildren().add(tb);
        }

        // “+” button to create a brand-new list
        ToggleButton addBtn = new ToggleButton("+");
        addBtn.setToggleGroup(tg);
        addBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New List");
            dialog.setHeaderText("Create a new list");
            dialog.setContentText("List name:");
            dialog.showAndWait().ifPresent(name -> {
                if (name == null || name.trim().isEmpty()) return;
                controller.createList(name);
                // Create a ToggleButton for it
                ToggleButton newTb = new ToggleButton(name);
                newTb.setToggleGroup(tg);
                newTb.setOnAction(evt2 -> {
                    controller.clearErrorBox();
                    updateErrorDisplay.run();
                    currentList = name;
                    for (javafx.scene.Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tbn) {
                            tbn.setStyle(null);
                        }
                    }
                    newTb.setStyle("-fx-background-color: lightgray;");
                    tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
                    updateErrorDisplay.run();
                });
                // Insert it just before the “+” button
                tabBar.getChildren().add(tabBar.getChildren().size() - 1, newTb);
                // Select it
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                newTb.setStyle("-fx-background-color: lightgray;");
                currentList = name;
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
                updateErrorDisplay.run();
            });
        });
        tabBar.getChildren().add(addBtn);

        // — Assemble final layout —
        BorderPane listPane = new BorderPane();
        listPane.setLeft(tabBar);
        listPane.setCenter(combined);
        rootPane.setCenter(listPane);

        // Initially render any errors (there should be none on first load)
        updateErrorDisplay.run();
    }

    private void showAddPage() {
        // Build the “Add Coin” form. Save time delegates to controller.createCoinInList(...)
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
            boolean adv = advancedGradeCheck.isSelected();
            gradeBox.setVisible(!adv);
            customGradeField.setVisible(adv);
        });

        HBox topRow = new HBox(10);
        topRow.setPadding(new Insets(20, 20, 0, 20));
        topRow.getChildren().addAll(
                nameLabel, nameField,
                dateLabel, dateField,
                gradeLabel, gradeContainer,
                advancedGradeCheck
        );

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

        // Swapped placements: edge at row 1, weight at row 1; composition at row 2; denomination at row 2
        additionalGrid.add(edgeLabel, 0, 1);
        additionalGrid.add(edgeField, 1, 1);
        additionalGrid.add(weightLabel, 2, 1);
        additionalGrid.add(weightField, 3, 1);
        additionalGrid.add(compositionLabel, 0, 2);
        additionalGrid.add(compositionField, 1, 2);
        additionalGrid.add(denominationLabel, 2, 2);
        additionalGrid.add(denominationField, 3, 2);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> showListPage());

        // This errorBox is local to the “Add” form only. (Separate from the controller’s errorBox.)
        VBox errorBox = new VBox(5);
        Label messageLabel = new Label();

        saveBtn.setOnAction(e -> {
            errorBox.getChildren().clear();
            messageLabel.setText("");

            // 1) Build a map of raw inputs
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

            // 2) Delegate validation & creation to controller
            Controller.ValidationResult vr = controller.createCoinInList(currentList, rawFields);
            if (!vr.isValid()) {
                // Display each FieldError in this local form’s errorBox
                for (Controller.FieldError fe : vr.getErrors()) {
                    TextFlow tf = new TextFlow(
                            new Text("Invalid input for "),
                            createRedText(fe.getField()),
                            new Text("; a(n) "),
                            createRedText(fe.getExpectedType()),
                            new Text(" is required")
                    );
                    errorBox.getChildren().add(tf);
                }
            } else {
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

        HBox buttonRow = new HBox(10, saveBtn, cancelBtn);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(topRow, additionalGrid, buttonRow, errorBox, messageLabel);

        rootPane.setCenter(layout);
    }

    private void showEditForCoin(Coin coin) {
        // Similar to “Add” form, except we pre‐fill and call saveCoin on “Save Changes”
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
        TextField gradeField = new TextField(coin.getGrade());
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
                controller.saveCoin(currentList, coin);
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
     * Renders a single red Text node.
     */
    private Text createRedText(String content) {
        Text t = new Text(content);
        t.setFill(Color.RED);
        return t;
    }
}