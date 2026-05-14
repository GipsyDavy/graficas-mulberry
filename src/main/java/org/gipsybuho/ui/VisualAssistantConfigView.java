package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.TextToSpeechService;

public class VisualAssistantConfigView extends VBox {

    private final VisualAssistantView assistant;

    public VisualAssistantConfigView(VisualAssistantView assistant) {
        this.assistant = assistant;
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(14);

        Label titulo = new Label("Configuración asistente visual");
        titulo.getStyleClass().add("view-title");
        Label subtitulo = new Label("Ajusta el personaje, la voz y el tamaño del ayudante flotante.");
        subtitulo.getStyleClass().add("view-subtitle");

        VBox panel = new VBox(14);
        panel.getStyleClass().add("config-panel");
        panel.getChildren().add(buildForm());

        getChildren().addAll(titulo, subtitulo, panel);
    }

    private GridPane buildForm() {
        CheckBox chkActivo = new CheckBox("Mostrar asistente visual");
        chkActivo.setSelected(assistant.isActivo());
        chkActivo.selectedProperty().addListener((obs, old, selected) -> {
            SoundService.play(SoundService.Sound.CLICK);
            assistant.setActivo(selected);
        });

        CheckBox chkVoz = new CheckBox("Leer ayudas por voz");
        chkVoz.setSelected(assistant.isVozActiva());
        chkVoz.selectedProperty().addListener((obs, old, selected) -> {
            SoundService.play(SoundService.Sound.CLICK);
            assistant.setVozActiva(selected);
        });

        ComboBox<String> cbVozTts = new ComboBox<>();
        cbVozTts.getItems().setAll(TextToSpeechService.nombresVoces());
        cbVozTts.setValue(TextToSpeechService.getVozSeleccionada());
        cbVozTts.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;
            SoundService.play(SoundService.Sound.CLICK);
            TextToSpeechService.setVozSeleccionada(selected);
        });

        ComboBox<String> cbPersonaje = new ComboBox<>();
        cbPersonaje.getItems().setAll(VisualAssistantView.nombresPersonajes());
        cbPersonaje.setValue(assistant.getPersonajeActual());
        cbPersonaje.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) return;
            SoundService.play(SoundService.Sound.CLICK);
            assistant.setPersonajeActual(selected);
        });

        Slider slTamano = new Slider(44, 88, assistant.getTamanoActual());
        slTamano.setShowTickMarks(true);
        slTamano.setShowTickLabels(true);
        slTamano.setMajorTickUnit(11);
        slTamano.setBlockIncrement(4);

        Label lblTamano = new Label(assistant.getTamanoActual() + " px");
        slTamano.valueProperty().addListener((obs, old, value) -> {
            int tamano = value.intValue();
            lblTamano.setText(tamano + " px");
            assistant.setTamanoActual(tamano);
        });

        Button btnRestablecer = new Button("Restablecer posición");
        btnRestablecer.setOnAction(e -> {
            SoundService.play(SoundService.Sound.CLICK);
            assistant.restablecerPosicion();
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setAlignment(Pos.TOP_LEFT);

        grid.add(label("Estado:"), 0, 0);
        grid.add(chkActivo, 1, 0);
        grid.add(label("Voz:"), 0, 1);
        grid.add(new HBox(10, chkVoz, cbVozTts), 1, 1);
        grid.add(label("Personaje:"), 0, 2);
        grid.add(cbPersonaje, 1, 2);
        grid.add(label("Tamaño:"), 0, 3);
        grid.add(new HBox(10, slTamano, lblTamano), 1, 3);
        grid.add(label("Posición:"), 0, 4);
        grid.add(btnRestablecer, 1, 4);

        return grid;
    }

    private Label label(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("config-form-label");
        return label;
    }
}
