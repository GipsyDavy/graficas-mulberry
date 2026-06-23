package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static org.gipsybuho.service.LanguageManager.t;

/**
 * Módulo "ASISTENTES": agrupa el chat de Asistente IA y la configuración del Asistente Visual,
 * antes repartidos entre el módulo "Asistente" y la pestaña Configuración → Asistente.
 */
public class AsistentesView extends VBox {

    public AsistentesView(VisualAssistantView visualAssistant) {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabIA = new Tab("🤖  " + t("asistentes.tab.ia"), new IAView());

        ScrollPane scrollVisual = new ScrollPane(new VisualAssistantConfigView(visualAssistant));
        scrollVisual.setFitToWidth(true);
        scrollVisual.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        Tab tabVisual = new Tab("🎭  " + t("asistentes.tab.visual"), scrollVisual);

        tabs.getTabs().addAll(tabIA, tabVisual);
        getChildren().add(tabs);
    }
}
