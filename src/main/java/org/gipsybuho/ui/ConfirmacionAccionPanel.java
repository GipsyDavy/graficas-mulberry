package org.gipsybuho.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.gipsybuho.model.AccionERP;
import org.gipsybuho.service.AccionDispatcherService;
import org.gipsybuho.service.JsonInterceptorService;

import java.util.Optional;
import java.util.function.Consumer;

public class ConfirmacionAccionPanel extends VBox {

    private static final String COLOR_VERDE = "#27AE60";
    private static final String COLOR_GRIS  = "#7F8C8D";

    // Colores derivados dinámicamente de la acción
    private final String colorAccion;
    private final String colorFondo;

    private final AccionERP accion;
    private final AccionDispatcherService dispatcher = new AccionDispatcherService();

    private TextField txtTotal;
    private TextArea  txtObservaciones;

    public ConfirmacionAccionPanel(AccionERP accion,
                                   Consumer<String> onConfirmado,
                                   Runnable onCancelado) {
        this.accion      = accion;
        this.colorAccion = accion.colorHex();
        this.colorFondo  = accion.colorFondoHex();

        setStyle(String.format(
            "-fx-background-color:#FDFBFC; -fx-border-color:%s; -fx-border-radius:10; " +
            "-fx-background-radius:10; -fx-border-width:1.5;", colorFondo));
        setPadding(new Insets(0));
        setSpacing(0);
        setMaxWidth(700);

        getChildren().addAll(
            buildHeader(),
            buildSummary(),
            buildSeparador(),
            buildDetalles(),
            buildCamposEditables(),
            buildSeparador(),
            buildBotones(onConfirmado, onCancelado)
        );
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Label icono = new Label(accion.accionIcono());
        icono.setStyle("-fx-font-size:20;");

        Label titulo = new Label(accion.accionLabel().toUpperCase());
        titulo.setStyle(String.format(
            "-fx-font-weight:bold; -fx-font-size:13; -fx-text-fill:%s;", colorAccion));

        Label badge = new Label("ACCIÓN PENDIENTE DE CONFIRMACIÓN");
        badge.setStyle(String.format(
            "-fx-background-color:%s; -fx-text-fill:%s; -fx-font-size:9; " +
            "-fx-font-weight:bold; -fx-padding:2 8; -fx-background-radius:10;",
            colorFondo, colorAccion));

        Button btnEditarJson = new Button("{ } JSON");
        btnEditarJson.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#888; -fx-font-size:10; " +
            "-fx-padding:3 8; -fx-border-color:#CCC; -fx-border-radius:4; " +
            "-fx-background-radius:4; -fx-cursor:hand;");
        btnEditarJson.setOnAction(e -> abrirEditorJson());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox header = new HBox(10, icono, titulo, sp, badge, btnEditarJson);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:" + colorFondo + "; -fx-background-radius:10 10 0 0;");
        return header;
    }

    // ── Preview summary ───────────────────────────────────────────────────────

    private VBox buildSummary() {
        String texto = accion.previewSummary != null && !accion.previewSummary.isBlank()
            ? accion.previewSummary
            : "Revisa los detalles antes de confirmar.";

        Text t = new Text(texto);
        t.setFill(Color.web("#2C1A28"));
        t.setFont(Font.font("System", FontWeight.NORMAL, 13));
        t.setWrappingWidth(640);

        TextFlow tf = new TextFlow(t);
        tf.setPadding(new Insets(12, 16, 12, 16));
        tf.setStyle("-fx-background-color:" + COLOR_SUMMARY_BG + ";");

        VBox box = new VBox(tf);
        box.setStyle(String.format(
            "-fx-border-color:%s; -fx-border-width:0 0 0 4; -fx-background-color:#FEF9FD;",
            colorAccion));
        return box;
    }

    // ── Detalles (cliente, fecha, items) ──────────────────────────────────────

    private VBox buildDetalles() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 8, 16));

        if (accion.data == null) return box;

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        int row = 0;

        if (accion.data.clienteNombre != null && !accion.data.clienteNombre.isBlank()) {
            grid.add(etiqueta("Cliente:"), 0, row);
            grid.add(valor(accion.data.clienteNombre), 1, row++);
        }
        if (accion.data.fecha != null && !accion.data.fecha.isBlank()) {
            grid.add(etiqueta("Fecha:"), 0, row);
            grid.add(valor(accion.data.fecha), 1, row++);
        }

        box.getChildren().add(grid);

        // Tabla de items
        if (accion.data.items != null && !accion.data.items.isEmpty()) {
            box.getChildren().add(buildTablaItems());
        }

        // Total estimado (lectura, el editable va abajo)
        if (accion.data.totalEstimado > 0) {
            HBox totalRow = new HBox(8);
            totalRow.setAlignment(Pos.CENTER_LEFT);
            totalRow.setPadding(new Insets(6, 0, 0, 0));
            Label lblT = etiqueta("Total estimado:");
            Label valT = new Label(String.format("%.2f €", accion.data.totalEstimado));
            valT.setStyle("-fx-font-weight:bold; -fx-font-size:14; -fx-text-fill:" + colorAccion + ";");
            totalRow.getChildren().addAll(lblT, valT);
            box.getChildren().add(totalRow);
        }

        return box;
    }

    private VBox buildTablaItems() {
        VBox tabla = new VBox(0);
        tabla.setStyle(
            "-fx-background-color:#F9F4F8; -fx-background-radius:6; " +
            "-fx-border-color:#E8D4E4; -fx-border-radius:6; -fx-border-width:1;");
        tabla.setPadding(new Insets(0));

        // Cabecera
        HBox cabecera = buildFilaTabla(
            "Descripción", "Cant.", "Técnica", "Precio u.", "Total",
            true, colorFondo);
        tabla.getChildren().add(cabecera);

        // Separador bajo cabecera
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#D4B8D0;");
        tabla.getChildren().add(sep);

        // Filas de items
        boolean alterno = false;
        for (AccionERP.Item item : accion.data.items) {
            String desc = item.descripcion != null ? item.descripcion : "—";
            if (item.soporte != null && !item.soporte.isBlank())
                desc += "  ·  " + item.soporte;
            if (item.gramaje > 0) desc += " " + (int) item.gramaje + "g";
            if (item.colores > 0) desc += "  ·  " + item.colores + " col.";

            double totalLinea = item.precioUnitario > 0
                ? item.cantidad * item.precioUnitario * (1 - item.mermaPorcentaje / 100.0) : 0;

            HBox fila = buildFilaTabla(
                desc,
                String.valueOf(item.cantidad),
                item.tecnica != null ? item.tecnica : "—",
                item.precioUnitario > 0 ? String.format("%.2f €", item.precioUnitario) : "—",
                totalLinea > 0 ? String.format("%.2f €", totalLinea) : "—",
                false, alterno ? "#FDFAFC" : "white");
            tabla.getChildren().add(fila);

            // Añadir pantones si los hay
            if (item.pantones != null && !item.pantones.isEmpty()) {
                Label pantonesLbl = new Label("  Pantones: " + String.join(", ", item.pantones));
                pantonesLbl.setStyle("-fx-font-size:10; -fx-text-fill:#888; -fx-padding:0 0 4 12;");
                tabla.getChildren().add(pantonesLbl);
            }
            alterno = !alterno;
        }

        return tabla;
    }

    private HBox buildFilaTabla(String desc, String cant, String tec,
                                 String precio, String total,
                                 boolean esCabecera, String bgColor) {
        String estilo = esCabecera
            ? "-fx-font-weight:bold; -fx-font-size:11; -fx-text-fill:" + colorAccion + ";"
            : "-fx-font-size:11; -fx-text-fill:#333;";

        Label lDesc   = new Label(desc);   lDesc.setStyle(estilo);   lDesc.setWrapText(true);
        Label lCant   = new Label(cant);   lCant.setStyle(estilo);
        Label lTec    = new Label(tec);    lTec.setStyle(estilo);
        Label lPrecio = new Label(precio); lPrecio.setStyle(estilo);
        Label lTotal  = new Label(total);  lTotal.setStyle(estilo + "-fx-font-weight:bold;");

        HBox.setHgrow(lDesc, Priority.ALWAYS);
        lCant.setMinWidth(40);   lCant.setMaxWidth(40);
        lTec.setMinWidth(90);    lTec.setMaxWidth(90);
        lPrecio.setMinWidth(65); lPrecio.setMaxWidth(65);
        lTotal.setMinWidth(65);  lTotal.setMaxWidth(65);

        HBox fila = new HBox(8, lDesc, lCant, lTec, lPrecio, lTotal);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(6, 10, 6, 10));
        fila.setStyle("-fx-background-color:" + bgColor + ";");
        return fila;
    }

    // ── Campos editables ──────────────────────────────────────────────────────

    private VBox buildCamposEditables() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 16, 14, 16));

        // Total editable
        HBox rowTotal = new HBox(10);
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        Label lblTotal = etiqueta("Ajustar total (€):");
        txtTotal = new TextField(accion.data != null && accion.data.totalEstimado > 0
            ? String.format("%.2f", accion.data.totalEstimado) : "");
        txtTotal.setPromptText("0.00");
        txtTotal.setPrefWidth(120);
        rowTotal.getChildren().addAll(lblTotal, txtTotal);

        // Observaciones editables
        HBox rowObs = new HBox(10);
        rowObs.setAlignment(Pos.TOP_LEFT);
        Label lblObs = etiqueta("Observaciones:");
        txtObservaciones = new TextArea(accion.data != null
            && accion.data.observaciones != null ? accion.data.observaciones : "");
        txtObservaciones.setPromptText("Notas adicionales...");
        txtObservaciones.setPrefRowCount(2);
        txtObservaciones.setWrapText(true);
        HBox.setHgrow(txtObservaciones, Priority.ALWAYS);
        rowObs.getChildren().addAll(lblObs, txtObservaciones);

        box.getChildren().addAll(rowTotal, rowObs);
        return box;
    }

    // ── Botones ───────────────────────────────────────────────────────────────

    private HBox buildBotones(Consumer<String> onConfirmado, Runnable onCancelado) {
        Button btnConfirmar = new Button("✓  Confirmar Acción");
        btnConfirmar.setStyle(String.format(
            "-fx-background-color:%s; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-font-size:13; -fx-padding:10 24; -fx-background-radius:6; -fx-cursor:hand;",
            COLOR_VERDE));

        Button btnCancelar = new Button("✗  Cancelar");
        btnCancelar.setStyle(String.format(
            "-fx-background-color:%s; -fx-text-fill:white; -fx-font-size:13; " +
            "-fx-padding:10 20; -fx-background-radius:6; -fx-cursor:hand;",
            COLOR_GRIS));

        btnConfirmar.setOnAction(e -> {
            aplicarEdiciones();
            setDisable(true);
            Thread.ofVirtual().start(() -> {
                AccionDispatcherService.ResultadoAccion resultado = dispatcher.ejecutar(accion);
                javafx.application.Platform.runLater(() -> {
                    String feedback = resultado.exito()
                        ? resultado.mensaje() + (resultado.detalle().isBlank()
                            ? "" : "\n" + resultado.detalle())
                        : "❌ " + resultado.mensaje() + "\n" + resultado.detalle();
                    marcarEjecutado(resultado.exito());
                    onConfirmado.accept(feedback);
                });
            });
        });

        btnCancelar.setOnAction(e -> {
            marcarCancelado();
            onCancelado.run();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox row = new HBox(12, sp, btnConfirmar, btnCancelar);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(12, 16, 16, 16));
        row.setStyle("-fx-background-color:" + colorFondo + "; -fx-background-radius:0 0 10 10;");
        return row;
    }

    // ── Editor JSON avanzado ──────────────────────────────────────────────────

    private void abrirEditorJson() {
        String jsonActual = serializarAccion();

        TextArea editor = new TextArea(jsonActual);
        editor.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size:12;");
        editor.setPrefRowCount(22);
        editor.setPrefWidth(560);
        editor.setWrapText(false);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11;");

        VBox contenido = new VBox(8, new Label("Edita el JSON de la acción directamente:"),
            editor, lblError);
        contenido.setPadding(new Insets(4));

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Editor JSON avanzado");
        dlg.setHeaderText(accion.accionIcono() + "  " + accion.accionLabel());
        dlg.getDialogPane().setContent(contenido);
        dlg.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Aplicar cambios", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(600);

        // Validar antes de cerrar
        Button btnAplicar = (Button) dlg.getDialogPane()
            .lookupButton(dlg.getDialogPane().getButtonTypes().get(0));
        btnAplicar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String jsonEditado = editor.getText().trim();
            JsonInterceptorService.Resultado resultado =
                JsonInterceptorService.interceptar("```json\n" + jsonEditado + "\n```");
            if (!resultado.tieneAccion()) {
                // Intentar parseo directo
                resultado = JsonInterceptorService.interceptar(jsonEditado);
            }
            if (!resultado.tieneAccion()) {
                lblError.setText("⚠ JSON inválido o sin campo 'action'. Revisa la sintaxis.");
                event.consume();
            }
        });

        Optional<ButtonType> resp = dlg.showAndWait();
        if (resp.isPresent() && resp.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String jsonEditado = editor.getText().trim();
            JsonInterceptorService.Resultado resultado =
                JsonInterceptorService.interceptar("```json\n" + jsonEditado + "\n```");
            resultado.accion().ifPresent(nueva -> {
                accion.action       = nueva.action;
                accion.data         = nueva.data;
                accion.previewSummary = nueva.previewSummary;
            });
        }
    }

    private String serializarAccion() {
        try {
            ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(accion);
        } catch (Exception e) {
            return "{ \"error\": \"No se pudo serializar\" }";
        }
    }

    // ── Helpers visuales ──────────────────────────────────────────────────────

    private void aplicarEdiciones() {
        if (accion.data == null) return;
        try {
            if (txtTotal != null && !txtTotal.getText().isBlank())
                accion.data.totalEstimado = Double.parseDouble(
                    txtTotal.getText().replace(",", "."));
        } catch (NumberFormatException ignored) {}
        if (txtObservaciones != null)
            accion.data.observaciones = txtObservaciones.getText().trim();
    }

    private void marcarEjecutado(boolean exito) {
        setStyle(getStyle() + (exito
            ? "; -fx-border-color:#27AE60;"
            : "; -fx-border-color:#E74C3C;"));
        setOpacity(0.75);
    }

    private void marcarCancelado() {
        setStyle(getStyle() + "; -fx-border-color:#BDC3C7;");
        setOpacity(0.55);
        setDisable(true);
    }

    private static Region buildSeparador() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#E8D4E4;");
        return sep;
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:11; -fx-text-fill:#777; -fx-font-weight:bold; -fx-min-width:110;");
        return l;
    }

    private static Label valor(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#2C1A28;");
        return l;
    }

    private static Label celda(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-size:11; -fx-text-fill:#333;");
        l.setWrapText(true);
        return l;
    }
}
