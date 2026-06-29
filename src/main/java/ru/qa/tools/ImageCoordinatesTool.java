package ru.qa.tools;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class ImageCoordinatesTool extends Application {

    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;

    private final ImageView imageView = new ImageView();
    private final Rectangle selection = new Rectangle();
    private final Pane imagePane = new Pane(imageView, selection);
    private ScrollPane scrollPane;

    private final Label fileLabel = new Label("Файл: -");
    private final Label imageSizeLabel = new Label("Изображение: -");
    private final Label zoomLabel = new Label("Масштаб: 100%");
    private final Label cursorLabel = new Label("Курсор: -");
    private final Label leftTopLabel = new Label("Левый верхний угол: -");
    private final Label rightBottomLabel = new Label("Правый нижний угол: -");
    private final Label sizeLabel = new Label("Размер: -");
    private final Label scenarioHeaderLabel = new Label("| minX | minY | maxX | maxY |");
    private final Label scenarioValueLabel = new Label("| - | - | - | - |");
    private final Label emptyStateLabel = new Label("Откройте изображение PNG или JPG");

    private double imageWidth;
    private double imageHeight;
    private double zoom = 1.0;
    private int startX;
    private int startY;
    private boolean panning;
    private ResizeMode activeResizeMode = ResizeMode.NONE;
    private double panStartSceneX;
    private double panStartSceneY;
    private double panStartOffsetX;
    private double panStartOffsetY;
    private Coordinates lastSelection;
    private Coordinates resizeStartSelection;

    @Override
    public void start(Stage stage) {
        configureImageView();
        configureSelection();
        configureImagePane();

        MenuBar menuBar = createMenuBar(stage);
        ToolBar toolBar = createToolBar(stage);
        StackPane imageArea = createImageArea();
        VBox sidePanel = createSidePanel();

        BorderPane content = new BorderPane();
        content.setTop(toolBar);
        content.setCenter(imageArea);
        content.setRight(sidePanel);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(content);

        Scene scene = new Scene(root, 1200, 800);

        stage.setTitle("Координаты на изображении");
        stage.setScene(scene);
        stage.show();

        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            loadImage(new File(args.get(0)));
        }
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuItem openItem = new MenuItem("Открыть изображение...");
        openItem.setOnAction(event -> openImage(stage));

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.setOnAction(event -> stage.close());

        Menu fileMenu = new Menu("Файл");
        fileMenu.getItems().addAll(openItem, exitItem);

        return new MenuBar(fileMenu);
    }

    private ToolBar createToolBar(Stage stage) {
        Button openButton = new Button("Открыть");
        openButton.setOnAction(event -> openImage(stage));

        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(event -> setZoom(zoom / 1.25));

        Button zoomResetButton = new Button("100%");
        zoomResetButton.setOnAction(event -> setZoom(1.0));

        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(event -> setZoom(zoom * 1.25));

        return new ToolBar(openButton, new Separator(), zoomOutButton, zoomResetButton, zoomInButton, new Separator(), zoomLabel);
    }

    private StackPane createImageArea() {
        scrollPane = new ScrollPane(imagePane);
        scrollPane.setPannable(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown() && imageView.getImage() != null) {
                zoomAt(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? zoom * 1.1 : zoom / 1.1);
                event.consume();
            }
        });

        StackPane imageArea = new StackPane(scrollPane, emptyStateLabel);
        imageArea.setStyle("-fx-background-color: #202124;");
        emptyStateLabel.setTextFill(Color.web("#d8dee9"));
        emptyStateLabel.setStyle("-fx-font-size: 18;");
        return imageArea;
    }

    private VBox createSidePanel() {
        Button copyButton = new Button("Скопировать строку");
        copyButton.setMaxWidth(Double.MAX_VALUE);
        copyButton.setOnAction(event -> copyCoordinates());

        VBox sidePanel = new VBox(10,
                fileLabel,
                imageSizeLabel,
                zoomLabel,
                new Separator(),
                cursorLabel,
                leftTopLabel,
                rightBottomLabel,
                sizeLabel,
                new Separator(),
                scenarioHeaderLabel,
                scenarioValueLabel,
                new Separator(),
                copyButton,
                new Separator(),
                new Label("ЛКМ + перетаскивание: выделить область"),
                new Label("ЛКМ по краю/углу области: изменить размер"),
                new Label("Ctrl + ЛКМ + перетаскивание: двигать изображение"),
                new Label("Колесо мыши + перетаскивание: двигать изображение"),
                new Label("Ctrl + колесо мыши: масштаб")
        );
        sidePanel.setPrefWidth(280);
        sidePanel.setPadding(new Insets(12));
        sidePanel.setStyle("-fx-background-color: #f6f8fa;");
        return sidePanel;
    }

    private void configureImageView() {
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);
    }

    private void configureImagePane() {
        imagePane.setStyle("-fx-background-color: #2b2b2b;");
        imagePane.setOnMouseMoved(event -> {
            if (imageView.getImage() == null) {
                return;
            }

            Point point = toImagePoint(event.getX(), event.getY());
            cursorLabel.setText("Курсор: x=%d, y=%d".formatted(point.x(), point.y()));
            updateMouseCursor(point, event.isControlDown());
        });

        imagePane.setOnMousePressed(event -> {
            if (imageView.getImage() == null) {
                return;
            }
            if (isPanStart(event.getButton(), event.isControlDown())) {
                startPanning(event.getSceneX(), event.getSceneY());
                event.consume();
                return;
            }
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            Point point = toImagePoint(event.getX(), event.getY());
            activeResizeMode = resizeModeAt(point);
            if (activeResizeMode != ResizeMode.NONE) {
                resizeStartSelection = lastSelection;
                resizeSelection(point);
                event.consume();
                return;
            }

            startX = point.x();
            startY = point.y();
            lastSelection = null;
            updateSelection(startX, startY, startX, startY);
            event.consume();
        });

        imagePane.setOnMouseDragged(event -> {
            if (imageView.getImage() == null) {
                return;
            }
            if (panning || event.isMiddleButtonDown() || event.isControlDown()) {
                if (!panning) {
                    startPanning(event.getSceneX(), event.getSceneY());
                }
                panTo(event.getSceneX(), event.getSceneY());
                event.consume();
                return;
            }
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            Point current = toImagePoint(event.getX(), event.getY());
            if (activeResizeMode != ResizeMode.NONE) {
                resizeSelection(current);
            } else {
                updateSelection(startX, startY, current.x(), current.y());
            }
            event.consume();
        });

        imagePane.setOnMouseReleased(event -> {
            if (panning) {
                panning = false;
                event.consume();
            }
            activeResizeMode = ResizeMode.NONE;
            resizeStartSelection = null;
        });
    }

    private boolean isPanStart(MouseButton button, boolean controlDown) {
        return button == MouseButton.MIDDLE || (button == MouseButton.PRIMARY && controlDown);
    }

    private void configureSelection() {
        selection.setFill(Color.color(0.1, 0.45, 1.0, 0.22));
        selection.setStroke(Color.DODGERBLUE);
        selection.setStrokeWidth(1.5);
        selection.setVisible(false);
        selection.setMouseTransparent(true);
    }

    private void openImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Открыть изображение");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg"));

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadImage(file);
        }
    }

    private void loadImage(File file) {
        Image image = new Image(file.toURI().toString(), false);
        if (image.isError()) {
            fileLabel.setText("Файл: не удалось открыть");
            return;
        }

        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        imageView.setImage(image);
        emptyStateLabel.setVisible(false);
        fileLabel.setText("Файл: " + file.getName());
        imageSizeLabel.setText("Изображение: %d x %d".formatted((int) imageWidth, (int) imageHeight));

        resetSelection();
        setZoom(1.0);
    }

    private void setZoom(double requestedZoom) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, requestedZoom));
        imageView.setFitWidth(imageWidth * zoom);
        imageView.setFitHeight(imageHeight * zoom);
        imagePane.setMinSize(imageWidth * zoom, imageHeight * zoom);
        imagePane.setPrefSize(imageWidth * zoom, imageHeight * zoom);
        zoomLabel.setText("Масштаб: %d%%".formatted(Math.round(zoom * 100)));

        if (lastSelection != null) {
            drawSelection(lastSelection);
        }
    }

    private void zoomAt(double sceneX, double sceneY, double requestedZoom) {
        double oldZoom = zoom;
        Bounds viewport = scrollPane.getViewportBounds();
        double oldContentWidth = imageWidth * oldZoom;
        double oldContentHeight = imageHeight * oldZoom;
        double oldMaxOffsetX = Math.max(0, oldContentWidth - viewport.getWidth());
        double oldMaxOffsetY = Math.max(0, oldContentHeight - viewport.getHeight());
        double oldOffsetX = scrollPane.getHvalue() * oldMaxOffsetX;
        double oldOffsetY = scrollPane.getVvalue() * oldMaxOffsetY;

        Point2D mouseInImagePane = imagePane.sceneToLocal(sceneX, sceneY);
        double mouseViewportX = mouseInImagePane.getX() - oldOffsetX;
        double mouseViewportY = mouseInImagePane.getY() - oldOffsetY;
        double imageX = clamp(mouseInImagePane.getX() / oldZoom, 0, imageWidth);
        double imageY = clamp(mouseInImagePane.getY() / oldZoom, 0, imageHeight);

        setZoom(requestedZoom);

        double newContentWidth = imageWidth * zoom;
        double newContentHeight = imageHeight * zoom;
        double newMaxOffsetX = Math.max(0, newContentWidth - viewport.getWidth());
        double newMaxOffsetY = Math.max(0, newContentHeight - viewport.getHeight());
        double newOffsetX = imageX * zoom - mouseViewportX;
        double newOffsetY = imageY * zoom - mouseViewportY;

        scrollPane.setHvalue(newMaxOffsetX == 0 ? 0 : clamp(newOffsetX / newMaxOffsetX, 0, 1));
        scrollPane.setVvalue(newMaxOffsetY == 0 ? 0 : clamp(newOffsetY / newMaxOffsetY, 0, 1));
    }

    private void startPanning(double sceneX, double sceneY) {
        Bounds viewport = scrollPane.getViewportBounds();
        panning = true;
        panStartSceneX = sceneX;
        panStartSceneY = sceneY;
        panStartOffsetX = scrollPane.getHvalue() * maxHorizontalOffset(viewport);
        panStartOffsetY = scrollPane.getVvalue() * maxVerticalOffset(viewport);
    }

    private void panTo(double sceneX, double sceneY) {
        Bounds viewport = scrollPane.getViewportBounds();
        double maxOffsetX = maxHorizontalOffset(viewport);
        double maxOffsetY = maxVerticalOffset(viewport);
        double newOffsetX = panStartOffsetX - (sceneX - panStartSceneX);
        double newOffsetY = panStartOffsetY - (sceneY - panStartSceneY);

        scrollPane.setHvalue(maxOffsetX == 0 ? 0 : clamp(newOffsetX / maxOffsetX, 0, 1));
        scrollPane.setVvalue(maxOffsetY == 0 ? 0 : clamp(newOffsetY / maxOffsetY, 0, 1));
    }

    private double maxHorizontalOffset(Bounds viewport) {
        return Math.max(0, imageWidth * zoom - viewport.getWidth());
    }

    private double maxVerticalOffset(Bounds viewport) {
        return Math.max(0, imageHeight * zoom - viewport.getHeight());
    }

    private Point toImagePoint(double paneX, double paneY) {
        int imageX = clamp((int) Math.floor(paneX / zoom), 0, (int) imageWidth - 1);
        int imageY = clamp((int) Math.floor(paneY / zoom), 0, (int) imageHeight - 1);
        return new Point(imageX, imageY);
    }

    private void updateSelection(int firstX, int firstY, int secondX, int secondY) {
        Coordinates coordinates = new Coordinates(
                Math.min(firstX, secondX),
                Math.min(firstY, secondY),
                Math.max(firstX, secondX),
                Math.max(firstY, secondY)
        );
        lastSelection = coordinates;
        drawSelection(coordinates);
        updateSelectionInfo(coordinates);
    }

    private void resizeSelection(Point point) {
        if (resizeStartSelection == null) {
            return;
        }

        int left = resizeStartSelection.left();
        int top = resizeStartSelection.top();
        int right = resizeStartSelection.right();
        int bottom = resizeStartSelection.bottom();

        switch (activeResizeMode) {
            case NORTH -> updateSelection(left, bottom, right, point.y());
            case SOUTH -> updateSelection(left, top, right, point.y());
            case WEST -> updateSelection(right, top, point.x(), bottom);
            case EAST -> updateSelection(left, top, point.x(), bottom);
            case NORTH_WEST -> updateSelection(right, bottom, point.x(), point.y());
            case NORTH_EAST -> updateSelection(left, bottom, point.x(), point.y());
            case SOUTH_WEST -> updateSelection(right, top, point.x(), point.y());
            case SOUTH_EAST -> updateSelection(left, top, point.x(), point.y());
            case NONE -> {
            }
        }
    }

    private void updateMouseCursor(Point point, boolean controlDown) {
        if (controlDown) {
            imagePane.setCursor(Cursor.MOVE);
            return;
        }

        imagePane.setCursor(cursorFor(resizeModeAt(point)));
    }

    private ResizeMode resizeModeAt(Point point) {
        if (lastSelection == null || !selection.isVisible()) {
            return ResizeMode.NONE;
        }

        int tolerance = Math.max(1, (int) Math.ceil(8 / zoom));
        boolean nearLeft = Math.abs(point.x() - lastSelection.left()) <= tolerance;
        boolean nearRight = Math.abs(point.x() - lastSelection.right()) <= tolerance;
        boolean nearTop = Math.abs(point.y() - lastSelection.top()) <= tolerance;
        boolean nearBottom = Math.abs(point.y() - lastSelection.bottom()) <= tolerance;
        boolean insideX = point.x() >= lastSelection.left() - tolerance && point.x() <= lastSelection.right() + tolerance;
        boolean insideY = point.y() >= lastSelection.top() - tolerance && point.y() <= lastSelection.bottom() + tolerance;

        if (nearLeft && nearTop) {
            return ResizeMode.NORTH_WEST;
        }
        if (nearRight && nearTop) {
            return ResizeMode.NORTH_EAST;
        }
        if (nearLeft && nearBottom) {
            return ResizeMode.SOUTH_WEST;
        }
        if (nearRight && nearBottom) {
            return ResizeMode.SOUTH_EAST;
        }
        if (nearTop && insideX) {
            return ResizeMode.NORTH;
        }
        if (nearBottom && insideX) {
            return ResizeMode.SOUTH;
        }
        if (nearLeft && insideY) {
            return ResizeMode.WEST;
        }
        if (nearRight && insideY) {
            return ResizeMode.EAST;
        }

        return ResizeMode.NONE;
    }

    private Cursor cursorFor(ResizeMode resizeMode) {
        return switch (resizeMode) {
            case NORTH, SOUTH -> Cursor.V_RESIZE;
            case WEST, EAST -> Cursor.H_RESIZE;
            case NORTH_WEST, SOUTH_EAST -> Cursor.NW_RESIZE;
            case NORTH_EAST, SOUTH_WEST -> Cursor.NE_RESIZE;
            case NONE -> Cursor.CROSSHAIR;
        };
    }

    private void drawSelection(Coordinates coordinates) {
        selection.setX(coordinates.left() * zoom);
        selection.setY(coordinates.top() * zoom);
        selection.setWidth((coordinates.right() - coordinates.left() + 1) * zoom);
        selection.setHeight((coordinates.bottom() - coordinates.top() + 1) * zoom);
        selection.setVisible(true);
    }

    private void updateSelectionInfo(Coordinates coordinates) {
        leftTopLabel.setText("Левый верхний угол: x=%d, y=%d".formatted(coordinates.left(), coordinates.top()));
        rightBottomLabel.setText("Правый нижний угол: x=%d, y=%d".formatted(coordinates.right(), coordinates.bottom()));
        sizeLabel.setText("Размер: ширина=%d, высота=%d".formatted(coordinates.width(), coordinates.height()));
        scenarioValueLabel.setText(formatScenarioRow(coordinates));
    }

    private void resetSelection() {
        selection.setVisible(false);
        lastSelection = null;
        cursorLabel.setText("Курсор: -");
        leftTopLabel.setText("Левый верхний угол: -");
        rightBottomLabel.setText("Правый нижний угол: -");
        sizeLabel.setText("Размер: -");
        scenarioValueLabel.setText("| - | - | - | - |");
    }

    private void copyCoordinates() {
        if (lastSelection == null) {
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(formatScenarioRow(lastSelection));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String formatScenarioRow(Coordinates coordinates) {
        return "| %d | %d | %d | %d |".formatted(
                coordinates.left(),
                coordinates.top(),
                coordinates.right(),
                coordinates.bottom()
        );
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Point(int x, int y) {
    }

    private record Coordinates(int left, int top, int right, int bottom) {
        int width() {
            return right - left + 1;
        }

        int height() {
            return bottom - top + 1;
        }
    }

    private enum ResizeMode {
        NONE,
        NORTH,
        SOUTH,
        WEST,
        EAST,
        NORTH_WEST,
        NORTH_EAST,
        SOUTH_WEST,
        SOUTH_EAST
    }

    public static void main(String[] args) {
        launch(args);
    }
}
