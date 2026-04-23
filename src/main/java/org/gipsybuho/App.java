package org.gipsybuho;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.ui.MainView;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.initialize();

        MainView mainView = new MainView(primaryStage);
        Scene scene = new Scene(mainView, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());

        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);

        try {
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")));
            primaryStage.getIcons().add(icon);
        } catch (Exception ignored) {}

        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.closeConnection();
    }
}
