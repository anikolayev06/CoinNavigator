import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.Optional;

/**
 * Main JavaFX application.
 * Left: a “tab bar” of all lists (toggle buttons + a “+” at the bottom).
 * Center: error‐box, search bar, TableView, and Add/Edit/Delete buttons.
 * All coin columns & forms are generated dynamically from Controller.getCoinAttributeNames().
 */
public class GUI extends Application {

    private VBox tabBar;
    private final Controller controller = new Controller();
    private BorderPane rootPane;
    private Scene mainScene;
    private String currentList;        // name of the currently selected list
    private TableView<Coin> tableView; // reference for the central TableView

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        rootPane = new BorderPane();

        // 1) Attempt to fetch all list names; if DB is missing/corrupt, delete coins.db and recreate
        List<String> allLists = new ArrayList<>();
        try {
            allLists = controller.getAllListNames();
        } catch (Exception e) {
            new File(System.getProperty("user.home") + File.separator + "coins.db").delete();
            allLists = new ArrayList<>();
        }
        if (allLists.isEmpty()) {
            controller.createList("Owned");
            controller.createList("Wishlist");
            allLists = controller.getAllListNames();
        }
        currentList = allLists.isEmpty() ? "Owned" : allLists.get(0);

        showListPage();

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Coin Collection");
        primaryStage.show();
    }

    private void showListPage() {
        controller.clearErrorBox();

        // 2) Fetch all coins in currentList
        List<Coin> coinList = controller.listCoins(currentList);
        ObservableList<Coin> data = FXCollections.observableArrayList(coinList);

        // ─── Build “errorDisplay” at the top ────────────────────────────────────────
        VBox errorDisplay = new VBox(5);
        errorDisplay.setPadding(new Insets(10));
        Runnable updateErrorDisplay = () -> {
            errorDisplay.getChildren().clear();
            for (String msg : controller.getErrorBox()) {
                Label lbl = new Label(msg);
                lbl.setTextFill(Color.RED);
                errorDisplay.getChildren().add(lbl);
            }
        };

        // ─── Build “searchBar” ─────────────────────────────────────────────────────
        Label attrLabel = new Label("Attribute:");
        ComboBox<String> attrBox = new ComboBox<>();
        attrBox.getItems().addAll(controller.getCoinAttributeNames());
        attrBox.setValue("name"); // default

        Label valueLabel = new Label("Value:");
        TextField valueField = new TextField();
        Button searchBtn = new Button("Search");
        Button resetBtn = new Button("Reset");

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(10));
        searchBar.getChildren().addAll(attrLabel, attrBox, valueLabel, valueField, searchBtn, resetBtn);

        // ─── Build “TableView” dynamically ────────────────────────────────────────
        tableView = new TableView<>(data);
        tableView.setPrefWidth(600);
        tableView.setPrefHeight(400);

        for (String attr : controller.getCoinAttributeNames()) {
            TableColumn<Coin, String> col = new TableColumn<>(
                    Character.toUpperCase(attr.charAt(0)) + attr.substring(1)
            );
            col.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getAttributeValue(attr))
            );
            tableView.getColumns().add(col);
        }

        // ─── Context menu: Edit / Move / Delete ──────────────────────────────────
        MenuItem editItem = new MenuItem("Edit Coin");
        editItem.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCoinForm(selected);
            }
        });

        // Declare "Move Coin" submenu (items will be dynamically populated)
        Menu moveMenu = new Menu("Move Coin");

        MenuItem deleteItem = new MenuItem("Delete Coin");
        deleteItem.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirm Delete");
                alert.setHeaderText("Delete Coin");
                alert.setContentText("Delete \"" + selected.getName() + "\"?");
                alert.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.OK) {
                        controller.deleteCoin(currentList, selected);
                        data.remove(selected);
                        updateErrorDisplay.run();
                    }
                });
            }
        });

        ContextMenu contextMenu = new ContextMenu(editItem, moveMenu, deleteItem);

        tableView.setRowFactory(tv -> {
            TableRow<Coin> row = new TableRow<>();
            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getIndex());
                    // Dynamically rebuild moveMenu items
                    moveMenu.getItems().clear();
                    for (String listName : controller.getAllListNames()) {
                        if (!listName.equals(currentList)) {
                            MenuItem targetItem = new MenuItem(listName);
                            Coin selected = row.getItem();
                            targetItem.setOnAction(ev -> {
                                controller.clearErrorBox();
                                controller.moveCoin(currentList, listName, selected);
                                // Refresh table data
                                tableView.setItems(FXCollections.observableArrayList(
                                    controller.listCoins(currentList)
                                ));
                            });
                            moveMenu.getItems().add(targetItem);
                        }
                    }
                    contextMenu.show(row, e.getScreenX(), e.getScreenY());
                }
            });
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Coin sel = row.getItem();
                    showCoinForm(sel);
                }
            });
            return row;
        });

        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                controller.clearErrorBox();
                updateErrorDisplay.run();
                Coin selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirm Delete");
                    alert.setHeaderText("Delete Coin");
                    alert.setContentText("Delete \"" + selected.getName() + "\"?");
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.OK) {
                            controller.deleteCoin(currentList, selected);
                            tableView.getItems().remove(selected);
                            updateErrorDisplay.run();
                        }
                    });
                }
            }
        });


        // ─── Wire Search / Reset ───────────────────────────────────────────────────
        searchBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            String attr = attrBox.getValue();
            String val = valueField.getText().trim();
            if (attr == null || val.isEmpty()) {
                tableView.setItems(FXCollections.observableArrayList(
                        controller.listCoins(currentList)
                ));
            } else {
                List<Coin> matches = controller.searchCoins(currentList, attr, val);
                tableView.setItems(FXCollections.observableArrayList(matches));
            }
            updateErrorDisplay.run();
        });

        resetBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            tableView.setItems(FXCollections.observableArrayList(
                    controller.listCoins(currentList)
            ));
            valueField.clear();
            attrBox.setValue("name");
        });

        // ─── “Add Coin”, “Edit Coin”, and “Delete Database” buttons ────────────────
        Button addCoinBtn = new Button("Add Coin");
        addCoinBtn.setOnAction(e -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            showCoinForm(null);
        });

        Button editCoinBtn = new Button("Edit Coin");
        editCoinBtn.setDisable(true);
        editCoinBtn.setOnAction(e -> {
            Coin selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.clearErrorBox();
                updateErrorDisplay.run();
                showCoinForm(selected);
            }
        });

        // Listen to selection changes to enable/disable "Edit Coin"
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            editCoinBtn.setDisable(newSel == null);
        });

        Button deleteListBtn = new Button("Delete Database");
        deleteListBtn.setOnAction(ev -> {
            controller.clearErrorBox();
            updateErrorDisplay.run();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete Database");
            confirm.setContentText("Are you sure you want to delete the database \"" + currentList + "\"?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            boolean success = controller.deleteList(currentList);
            if (!success) {
                updateErrorDisplay.run();
            } else {
                // Remove from tabBar
                for (Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tb && tb.getText().equals(currentList)) {
                        tabBar.getChildren().remove(node);
                        break;
                    }
                }
                // Switch to first remaining list (skip '+')
                for (Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tb && !"+".equals(tb.getText())) {
                        currentList = tb.getText();
                        tb.setSelected(true);
                        tb.setStyle("-fx-background-color: lightgray;");
                        break;
                    }
                }
                tableView.setItems(FXCollections.observableArrayList(
                        controller.listCoins(currentList)
                ));
                updateErrorDisplay.run();
            }
        });

        // Layout: Add Coin | Edit Coin on left, Delete Database on right
        HBox buttonRow = new HBox(10);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonRow.getChildren().addAll(addCoinBtn, editCoinBtn, spacer, deleteListBtn);

        // ─── Combine errorDisplay, searchBar, tableView, buttonRow ────────────
        VBox combined = new VBox(10);
        combined.setPadding(new Insets(10));
        combined.getChildren().addAll(
                searchBar,
                tableView,
                buttonRow,
                errorDisplay
        );
        combined.setOnMouseClicked(e -> {
            Node target = (Node) e.getTarget();
            boolean insideTable = false;
            while (target != null) {
                if (target == tableView) {
                    insideTable = true;
                    break;
                }
                target = target.getParent();
            }
            if (!insideTable) {
                tableView.getSelectionModel().clearSelection();
            }
        });

        // ─── Build the “tabBar” on the left ─────────────────────────────────────────
        tabBar = new VBox(10);
        tabBar.setPadding(new Insets(10));
        Region topSpacer = new Region();
        topSpacer.prefHeightProperty().bind(searchBar.heightProperty());
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
                // Reset styling on all buttons
                for (Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                tb.setStyle("-fx-background-color: lightgray;");
                tableView.setItems(FXCollections.observableArrayList(
                        controller.listCoins(currentList)
                ));
                updateErrorDisplay.run();
            });
            tb.setOnContextMenuRequested((ContextMenuEvent event) -> {
                ContextMenu menu = new ContextMenu();
                MenuItem deleteDbItem = new MenuItem("Delete Database");
                deleteDbItem.setOnAction(ev -> {
                    controller.clearErrorBox();
                    updateErrorDisplay.run();
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Delete");
                    confirm.setHeaderText("Delete Database");
                    confirm.setContentText("Are you sure you want to delete the database \"" + listName + "\"?");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return;
                    }
                    boolean success = controller.deleteList(listName);
                    if (!success) {
                        updateErrorDisplay.run();
                        return;
                    }
                    // Remove from tabBar
                    for (Iterator<Node> it = tabBar.getChildren().iterator(); it.hasNext(); ) {
                        Node node = it.next();
                        if (node instanceof ToggleButton tbn && tbn.getText().equals(listName)) {
                            it.remove();
                            break;
                        }
                    }
                    // Switch to first remaining list (skip '+')
                    String newCurrent = null;
                    for (Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tbn && !"+".equals(tbn.getText())) {
                            newCurrent = tbn.getText();
                            tbn.setSelected(true);
                            tbn.setStyle("-fx-background-color: lightgray;");
                            break;
                        }
                    }
                    if (newCurrent != null) {
                        currentList = newCurrent;
                        tableView.setItems(FXCollections.observableArrayList(
                                controller.listCoins(currentList)
                        ));
                    } else {
                        tableView.setItems(FXCollections.observableArrayList());
                        currentList = null;
                    }
                    updateErrorDisplay.run();
                });
                menu.getItems().add(deleteDbItem);
                menu.show(tb, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            tabBar.getChildren().add(tb);
        }

        // The “+” button to create a brand‐new list
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
                ToggleButton newTb = new ToggleButton(name);
                newTb.setToggleGroup(tg);
                newTb.setOnAction(evt2 -> {
                    controller.clearErrorBox();
                    updateErrorDisplay.run();
                    currentList = name;
                    for (Node node : tabBar.getChildren()) {
                        if (node instanceof ToggleButton tbn) {
                            tbn.setStyle(null);
                        }
                    }
                    newTb.setStyle("-fx-background-color: lightgray;");
                    tableView.setItems(FXCollections.observableArrayList(
                            controller.listCoins(currentList)
                    ));
                    updateErrorDisplay.run();
                });
                newTb.setOnContextMenuRequested((ContextMenuEvent event) -> {
                    ContextMenu menu = new ContextMenu();
                    MenuItem deleteDbItem = new MenuItem("Delete Database");
                    deleteDbItem.setOnAction(ev -> {
                        controller.clearErrorBox();
                        updateErrorDisplay.run();
                        Alert confirmDb = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmDb.setTitle("Confirm Delete");
                        confirmDb.setHeaderText("Delete Database");
                        confirmDb.setContentText("Are you sure you want to delete the database \"" + name + "\"?");
                        Optional<ButtonType> dbResult = confirmDb.showAndWait();
                        if (dbResult.isEmpty() || dbResult.get() != ButtonType.OK) {
                            return;
                        }
                        boolean successDb = controller.deleteList(name);
                        if (!successDb) {
                            updateErrorDisplay.run();
                            return;
                        }
                        // Remove from tabBar
                        for (Iterator<Node> it = tabBar.getChildren().iterator(); it.hasNext(); ) {
                            Node node = it.next();
                            if (node instanceof ToggleButton tbn && tbn.getText().equals(name)) {
                                it.remove();
                                break;
                            }
                        }
                        // Switch to first remaining list (skip '+')
                        String newCurrentDb = null;
                        for (Node node : tabBar.getChildren()) {
                            if (node instanceof ToggleButton tbn && !"+".equals(tbn.getText())) {
                                newCurrentDb = tbn.getText();
                                tbn.setSelected(true);
                                tbn.setStyle("-fx-background-color: lightgray;");
                                break;
                            }
                        }
                        if (newCurrentDb != null) {
                            currentList = newCurrentDb;
                            tableView.setItems(FXCollections.observableArrayList(
                                    controller.listCoins(currentList)
                            ));
                        } else {
                            tableView.setItems(FXCollections.observableArrayList());
                            currentList = null;
                        }
                        updateErrorDisplay.run();
                    });
                    menu.getItems().add(deleteDbItem);
                    menu.show(newTb, event.getScreenX(), event.getScreenY());
                    event.consume();
                });
                tabBar.getChildren().add(tabBar.getChildren().size() - 1, newTb);
                for (Node node : tabBar.getChildren()) {
                    if (node instanceof ToggleButton tbn) {
                        tbn.setStyle(null);
                    }
                }
                newTb.setStyle("-fx-background-color: lightgray;");
                currentList = name;
                tableView.setItems(FXCollections.observableArrayList(
                        controller.listCoins(currentList)
                ));
                updateErrorDisplay.run();
            });
        });
        tabBar.getChildren().add(addBtn);

        // ─── Assemble final layout ──────────────────────────────────────────────────
        BorderPane listPane = new BorderPane();
        listPane.setLeft(tabBar);
        listPane.setCenter(combined);
        rootPane.setCenter(listPane);

        updateErrorDisplay.run();
    }

    /**
     * Unified form for creating a new coin or editing an existing one.
     * If coinToEdit is null, we are adding; otherwise, we are editing.
     */
    private void showCoinForm(Coin coinToEdit) {
        controller.clearErrorBox();

        // Build a GridPane with one row per attribute
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(20));

        Map<String, Node> inputMap = new LinkedHashMap<>();
        List<String> attrs = controller.getCoinAttributeNames();

        // Prefill values if editing
        for (int row = 0; row < attrs.size(); row++) {
            String attr = attrs.get(row);
            Label lbl = new Label(Character.toUpperCase(attr.charAt(0)) + attr.substring(1) + ":");
            Node inputControl;

            if (attr.equals("grade")) {
                ComboBox<String> gradeBox = new ComboBox<>();
                gradeBox.getItems().addAll("MS","PF","AU","XF","VF","F","VG","G","AG","FR","PO","N/A");
                TextField customGradeField = new TextField();
                CheckBox advCheck = new CheckBox("Advanced");

                if (coinToEdit != null) {
                    String existing = coinToEdit.getAttributeValue(attr);
                    if (gradeBox.getItems().contains(existing)) {
                        gradeBox.setValue(existing);
                        customGradeField.setVisible(false);
                        advCheck.setSelected(false);
                    } else {
                        customGradeField.setText(existing);
                        gradeBox.setVisible(false);
                        advCheck.setSelected(true);
                        customGradeField.setVisible(true);
                    }
                } else {
                    gradeBox.setValue("N/A");
                    customGradeField.setVisible(false);
                }

                gradeBox.setMaxWidth(120);
                customGradeField.setMaxWidth(120);
                StackPane stack = new StackPane(gradeBox, customGradeField);
                advCheck.setOnAction(e -> {
                    boolean adv = advCheck.isSelected();
                    gradeBox.setVisible(!adv);
                    customGradeField.setVisible(adv);
                });

                HBox gradeRow = new HBox(10, stack, advCheck);
                inputControl = gradeRow;

                inputMap.put(attr + "_combo", gradeBox);
                inputMap.put(attr + "_custom", customGradeField);
                inputMap.put(attr + "_check", advCheck);
            } else {
                TextField tf = new TextField();
                if (coinToEdit != null) {
                    tf.setText(coinToEdit.getAttributeValue(attr));
                }
                inputControl = tf;
                inputMap.put(attr, tf);
            }

            formGrid.add(lbl, 0, row);
            formGrid.add(inputControl, 1, row);
        }

        Button saveBtn = new Button(coinToEdit == null ? "Save" : "Save Changes");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> showListPage());

        VBox localErrorBox = new VBox(5);
        Label messageLabel = new Label();

        saveBtn.setOnAction(e -> {
            localErrorBox.getChildren().clear();
            messageLabel.setText("");

            // Build rawFields map from inputs
            Map<String, String> rawFields = new HashMap<>();
            for (String attr : attrs) {
                if (attr.equals("grade")) {
                    CheckBox adv = (CheckBox) inputMap.get(attr + "_check");
                    ComboBox<String> cb = (ComboBox<String>) inputMap.get(attr + "_combo");
                    TextField custom = (TextField) inputMap.get(attr + "_custom");
                    String val = adv.isSelected() ? custom.getText().trim() : cb.getValue();
                    rawFields.put("grade", val);
                } else {
                    TextField tf = (TextField) inputMap.get(attr);
                    rawFields.put(attr, tf.getText().trim());
                }
            }

            // Unified validation for both new/edit
            boolean hasError = false;
            String[] requiredFields = {"name", "date", "grade"};
            for (String req : requiredFields) {
                if (rawFields.get(req) == null || rawFields.get(req).isEmpty()) {
                    TextFlow tfErr = new TextFlow(
                            createRedText(Character.toUpperCase(req.charAt(0)) + req.substring(1) + " is required")
                    );
                    localErrorBox.getChildren().add(tfErr);
                    hasError = true;
                }
            }
            // Parse numeric fields
            String[] numericFields = {"date", "diameter", "thickness", "weight"};
            for (String nf : numericFields) {
                String val = rawFields.get(nf);
                if (val != null && !val.isEmpty()) {
                    try {
                        if (nf.equals("date")) {
                            Integer.parseInt(val);
                        } else {
                            Double.parseDouble(val);
                        }
                    } catch (NumberFormatException ex) {
                        TextFlow tfErr = new TextFlow(
                                createRedText("Incorrect data type for " + nf + ", " +
                                        (nf.equals("date") ? "integer" : "decimal") + " required")
                        );
                        localErrorBox.getChildren().add(tfErr);
                        hasError = true;
                    }
                }
            }
            if (hasError) {
                return;
            }

            if (coinToEdit == null) {
                // Create new coin
                Controller.ValidationResult vr = controller.createCoinInList(currentList, rawFields);
                messageLabel.setText("Coin added (ID: " + vr.getCreatedId() + ")");
                // Clear inputs
                for (String attr : attrs) {
                    if (attr.equals("grade")) {
                        ComboBox<String> cb = (ComboBox<String>) inputMap.get(attr + "_combo");
                        TextField custom = (TextField) inputMap.get(attr + "_custom");
                        CheckBox adv = (CheckBox) inputMap.get(attr + "_check");
                        adv.setSelected(false);
                        cb.setValue("N/A");
                        custom.clear();
                    } else {
                        TextField tf = (TextField) inputMap.get(attr);
                        tf.clear();
                    }
                }
            } else {
                // Edit existing coin: set attributes then save
                for (String attr : attrs) {
                    String val = rawFields.get(attr);
                    coinToEdit.setAttributeValue(attr, val);
                }
                controller.saveCoin(currentList, coinToEdit);
                showListPage();
            }
        });

        HBox buttonRow = new HBox(10, saveBtn, cancelBtn);
        VBox layout = new VBox(10, formGrid, localErrorBox, messageLabel, buttonRow);
        layout.setPadding(new Insets(20));

        rootPane.setCenter(layout);
    }

    /** Renders a single red Text node. */
    private Text createRedText(String content) {
        Text t = new Text(content);
        t.setFill(Color.RED);
        return t;
    }

    // showMovePanel method removed: moving is now handled via context menu submenu.
}