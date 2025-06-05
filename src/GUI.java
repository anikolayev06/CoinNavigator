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
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class GUI extends Application {

    private VBox tabBar;

    private final Controller controller = new Controller();
    private BorderPane rootPane;
    private Scene mainScene;

    // The currently selected list (table) name; default to first one
    private String currentList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        rootPane = new BorderPane();

        // On startup, pick the first available list (should at least be “owned”)
        List<String> allLists = controller.getAllListNames();
        if (allLists.isEmpty()) {
            // In case metadata was empty (unlikely), create defaults
            controller.createList("Owned");
            controller.createList("Wishlist");
            allLists = controller.getAllListNames();
        }
        currentList = allLists.get(0);

        showListPage();

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Coin Collection");
        primaryStage.show();
    }

    private void showListPage() {
        // 1) Fetch all coins in currentList
        List<Coin> coinList = controller.listCoins(currentList);
        ObservableList<Coin> data = FXCollections.observableArrayList(coinList);

        // --- SEARCH CONTROLS at top ---
        Label attrLabel = new Label("Attribute:");
        ComboBox<String> attrBox = new ComboBox<>();
        attrBox.getItems().addAll(controller.getSearchableAttributes());
        attrBox.setValue("name"); // default

        Label valueLabel = new Label("Value:");
        TextField valueField = new TextField();
        Button searchBtn = new Button("Search");

        // --- TABLE SETUP ---
        final TableView<Coin> tableView = new TableView<>(data);
        tableView.setPrefWidth(600);
        tableView.setPrefHeight(400);

        // “Reset” button to clear filter
        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
            valueField.clear();
            attrBox.setValue("name");
        });

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(10));
        searchBar.getChildren().addAll(attrLabel, attrBox, valueLabel, valueField, searchBtn, resetBtn);

        // Define columns: name, date, grade, thickness, diameter, weight, edge, denomination, composition
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

        // Context menu: Edit / Delete
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
                        controller.deleteCoin(currentList, selected);
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

        // Backspace key → prompt delete
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
                            controller.deleteCoin(currentList, selected);
                            tableView.getItems().remove(selected);
                        }
                    });
                }
            }
        });

        // Search button: filter in‐place
        searchBtn.setOnAction(e -> {
            String attr = attrBox.getValue();
            String val = valueField.getText().trim();
            if (attr == null || val.isEmpty()) {
                tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
            } else {
                List<Coin> matches = controller.searchCoins(currentList, attr, val);
                tableView.setItems(FXCollections.observableArrayList(matches));
            }
        });

        // “Add Coin” button below the table
        Button addCoinBelowBtn = new Button("Add Coin");
        addCoinBelowBtn.setOnAction(e -> showAddPage());

        // “Delete Database” button next to Add Coin
        Button deleteListBtn = new Button("Delete Database");
        // Message label for deletion feedback
        Label deleteMsg = new Label();
        deleteMsg.setTextFill(Color.RED);

        VBox combined = new VBox(10);
        combined.setPadding(new Insets(10));
        combined.getChildren().addAll(searchBar, tableView, addCoinBelowBtn, deleteListBtn, deleteMsg);

        // --- TAB BAR on the left for switching among lists ---
        tabBar = new VBox(10);
        tabBar.setPadding(new Insets(10));

        // 1) Generate a ToggleButton for every existing list
        ToggleGroup tg = new ToggleGroup();
        for (String listName : controller.getAllListNames()) {
            ToggleButton tb = new ToggleButton(listName);
            tb.setToggleGroup(tg);
            if (listName.equals(currentList)) {
                tb.setSelected(true);
                tb.setStyle("-fx-background-color: lightgray;");
            }
            tb.setOnAction(evt -> {
                currentList = listName;
                // reset styling on all buttons
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                tb.setStyle("-fx-background-color: lightgray;");
                reloadTable(tableView);
            });
            tabBar.getChildren().add(tb);
        }

        // 2) The “+” button to create a brand‐new list
        ToggleButton addBtn = new ToggleButton("+");
        addBtn.setToggleGroup(tg);
        addBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New List");
            dialog.setHeaderText("Create a new list");
            dialog.setContentText("List name:");
            dialog.showAndWait().ifPresent(name -> {
                if (name == null || name.trim().isEmpty()) return;
                // 2a) Persist the new list
                controller.createList(name);
                // 2b) Create a ToggleButton for it
                ToggleButton newTb = new ToggleButton(name);
                newTb.setToggleGroup(tg);
                newTb.setOnAction(evt2 -> {
                    currentList = name;
                    for (javafx.scene.Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tbn) {
                            tbn.setStyle(null);
                        }
                    }
                    newTb.setStyle("-fx-background-color: lightgray;");
                    reloadTable(tableView);
                });
                // 2c) Insert it just before the “+” button
                tabBar.getChildren().add(tabBar.getChildren().size() - 1, newTb);
                // select it
                for (javafx.scene.Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                newTb.setStyle("-fx-background-color: lightgray;");
                currentList = name;
                reloadTable(tableView);
            });
        });
        tabBar.getChildren().add(addBtn);

        // Now that tabBar is initialized, set up deleteListBtn's handler
        deleteListBtn.setOnAction(ev -> {
            if ("owned".equals(currentList) || "wishlist".equals(currentList)) {
                deleteMsg.setText("Cannot delete Owned or Wishlist");
            } else {
                boolean success = controller.deleteList(currentList);
                if (!success) {
                    deleteMsg.setText("Cannot delete Owned or Wishlist");
                } else {
                    deleteMsg.setText("");
                    // Remove toggle button from tabBar
                    for (Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tb && tb.getText().equals(currentList)) {
                            tabBar.getChildren().remove(node);
                            break;
                        }
                    }
                    // Select first available list (skip the "+" button)
                    for (Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tb && !"+".equals(tb.getText())) {
                            currentList = tb.getText();
                            tb.setSelected(true);
                            tb.setStyle("-fx-background-color: lightgray;");
                            break;
                        }
                    }
                    reloadTable(tableView);
                }
            }
        });

        // 3) Assemble final layout
        BorderPane listPane = new BorderPane();
        listPane.setLeft(tabBar);
        listPane.setCenter(combined);
        rootPane.setCenter(listPane);
    }

    private void showAddPage() {
        // Build exactly the same “Add Coin” form as before, but at save time call createCoinInList(...)
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
        // Swapped row placements as requested:
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

        VBox errorBox = new VBox(5);
        Label messageLabel = new Label();

        saveBtn.setOnAction(e -> {
            errorBox.getChildren().clear();
            messageLabel.setText("");

            // Collect all raw fields into a Map:
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

            Controller.ValidationResult vr = controller.createCoinInList(currentList, rawFields);
            if (!vr.isValid()) {
                for (Controller.FieldError fe : vr.getErrors()) {
                    errorBox.getChildren().add(makeError(fe.getField(), fe.getExpectedType()));
                }
            } else {
                messageLabel.setText("Coin added with ID: " + vr.getCreatedId());
                // clear inputs
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
        layout.getChildren().addAll(topRow, additionalGrid, buttonRow, errorBox, messageLabel);

        rootPane.setCenter(layout);
    }

    private void showEditForCoin(Coin coin) {
        // Same as before, but call saveCoin(currentList, coin) on “Save Changes”
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
     * Renders a single error message:
     * “Invalid input for {field}; a(n) {type} is required” with {field}/{type} in red.
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

    /**
     * Helper to reload the table whenever we switch currentList.
     */
    private void reloadTable(TableView<Coin> tableView) {
        tableView.setItems(FXCollections.observableArrayList(controller.listCoins(currentList)));
    }
}