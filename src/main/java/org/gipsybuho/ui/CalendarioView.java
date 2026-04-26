package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.gipsybuho.dao.NotaCalendarioDAO;
import org.gipsybuho.model.NotaCalendario;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarioView extends VBox {

    private YearMonth mesActual = YearMonth.now();
    private final GridPane gridDias = new GridPane();
    private final Label lblMesAnio = new Label();
    private final NotaCalendarioDAO dao = new NotaCalendarioDAO();
    private final Locale esES = new Locale("es", "ES");
    private Set<LocalDate> fechasConNotas = new HashSet<>();

    public CalendarioView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(16);

        Label titulo = new Label("Calendario");
        titulo.getStyleClass().add("view-title");

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setPercentWidth(100.0 / 7);
            gridDias.getColumnConstraints().add(cc);
        }
        gridDias.setHgap(6);
        gridDias.setVgap(6);

        getChildren().addAll(titulo, buildNavegador(), buildCabeceraDias(), gridDias);
        cargarMes();
    }

    private HBox buildNavegador() {
        Button btnAnt = new Button("◀");
        Button btnSig = new Button("▶");
        Button btnHoy = new Button("Hoy");
        btnAnt.getStyleClass().add("btn-cal-nav");
        btnSig.getStyleClass().add("btn-cal-nav");
        btnHoy.getStyleClass().add("btn-cal-hoy");

        btnAnt.setOnAction(e -> { mesActual = mesActual.minusMonths(1); cargarMes(); });
        btnSig.setOnAction(e -> { mesActual = mesActual.plusMonths(1); cargarMes(); });
        btnHoy.setOnAction(e -> { mesActual = YearMonth.now(); cargarMes(); });

        lblMesAnio.getStyleClass().add("cal-mes-label");
        HBox.setHgrow(lblMesAnio, Priority.ALWAYS);

        HBox nav = new HBox(10, btnAnt, lblMesAnio, btnSig, btnHoy);
        nav.setAlignment(Pos.CENTER_LEFT);
        return nav;
    }

    private GridPane buildCabeceraDias() {
        String[] nombres = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        GridPane header = new GridPane();
        header.setHgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setPercentWidth(100.0 / 7);
            header.getColumnConstraints().add(cc);

            Label lbl = new Label(nombres[i]);
            lbl.getStyleClass().add("cal-header-dia");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            header.add(lbl, i, 0);
        }
        return header;
    }

    private void cargarMes() {
        String nombre = mesActual.getMonth().getDisplayName(TextStyle.FULL, esES);
        lblMesAnio.setText(cap(nombre) + " " + mesActual.getYear());

        try {
            fechasConNotas = new HashSet<>(dao.fechasConNotas(mesActual.getYear(), mesActual.getMonthValue()));
        } catch (Exception e) {
            fechasConNotas = new HashSet<>();
        }

        gridDias.getChildren().clear();

        LocalDate primerDia = mesActual.atDay(1);
        int offset    = primerDia.getDayOfWeek().getValue() - 1; // Lun=0
        int totalDias = mesActual.lengthOfMonth();
        LocalDate hoy = LocalDate.now();

        for (int i = 0; i < offset; i++) {
            StackPane vacio = new StackPane();
            vacio.getStyleClass().add("cal-dia-vacio");
            vacio.setMinHeight(78);
            gridDias.add(vacio, i, 0);
        }

        int col = offset, row = 0;
        for (int dia = 1; dia <= totalDias; dia++) {
            LocalDate fecha = mesActual.atDay(dia);
            VBox cell = buildCeldaDia(dia, fecha,
                fechasConNotas.contains(fecha),
                fecha.equals(hoy),
                fecha.isBefore(hoy));
            gridDias.add(cell, col, row);
            if (++col == 7) { col = 0; row++; }
        }
    }

    private VBox buildCeldaDia(int dia, LocalDate fecha,
                                boolean tieneNota, boolean esHoy, boolean esPasado) {
        VBox cell = new VBox(4);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPadding(new Insets(6));
        cell.setMinHeight(78);
        cell.setPrefHeight(78);
        cell.setCursor(Cursor.HAND);
        cell.getStyleClass().add("cal-dia-cell");

        if (esHoy)        cell.getStyleClass().add("cal-dia-hoy");
        else if (esPasado) cell.getStyleClass().add("cal-dia-pasado");
        if (tieneNota)    cell.getStyleClass().add("cal-dia-con-nota");

        Label lblNum = new Label(String.valueOf(dia));
        lblNum.getStyleClass().add(esHoy ? "cal-dia-num-hoy" : "cal-dia-num");
        cell.getChildren().add(lblNum);

        if (tieneNota) {
            Label punto = new Label("●");
            punto.getStyleClass().add("cal-nota-punto");
            cell.getChildren().add(punto);
        }

        cell.setOnMouseClicked(e -> abrirDialogoNota(fecha));
        return cell;
    }

    private void abrirDialogoNota(LocalDate fecha) {
        List<NotaCalendario> notas;
        try {
            notas = dao.findByFecha(fecha);
        } catch (Exception e) {
            notas = new ArrayList<>();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", esES);
        dialog.setTitle("📅  " + cap(fecha.format(fmt)));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Guardar");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cerrar");

        VBox cuerpo = new VBox(10);
        cuerpo.setPadding(new Insets(6, 2, 6, 2));
        cuerpo.setMinWidth(440);

        if (!notas.isEmpty()) {
            Label lblExist = new Label("Notas guardadas:");
            lblExist.setStyle("-fx-font-weight: bold; -fx-text-fill: #2D1A28;");
            cuerpo.getChildren().add(lblExist);

            for (NotaCalendario nota : notas) {
                HBox fila = new HBox(10);
                fila.setAlignment(Pos.CENTER_LEFT);
                fila.setStyle("-fx-background-color: #F8EEF6; -fx-padding: 10; " +
                    "-fx-background-radius: 5; -fx-border-color: #DEC8D8; " +
                    "-fx-border-radius: 5; -fx-border-width: 1;");

                VBox textos = new VBox(3);
                HBox.setHgrow(textos, Priority.ALWAYS);

                Label lblTit = new Label(nota.getTitulo());
                lblTit.setStyle("-fx-font-weight: bold; -fx-text-fill: #2D1A28;");
                textos.getChildren().add(lblTit);

                if (nota.getNota() != null && !nota.getNota().isBlank()) {
                    Label lblTxt = new Label(nota.getNota());
                    lblTxt.setWrapText(true);
                    lblTxt.setStyle("-fx-text-fill: #7A5A72; -fx-font-size: 12px;");
                    textos.getChildren().add(lblTxt);
                }

                Button btnBorrar = new Button("✕");
                btnBorrar.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                    "-fx-font-size: 10px; -fx-cursor: hand; " +
                    "-fx-background-radius: 3; -fx-padding: 3 7;");
                final int nid = nota.getId();
                btnBorrar.setOnAction(ev -> {
                    try {
                        dao.eliminar(nid);
                        fila.setVisible(false);
                        fila.setManaged(false);
                        cargarMes();
                    } catch (Exception ex) {
                        System.err.println("Error al eliminar nota: " + ex.getMessage());
                    }
                });

                fila.getChildren().addAll(textos, btnBorrar);
                cuerpo.getChildren().add(fila);
            }

            Separator sep = new Separator();
            VBox.setMargin(sep, new Insets(4, 0, 0, 0));
            cuerpo.getChildren().add(sep);
        }

        Label lblNueva = new Label(notas.isEmpty() ? "Añadir nota:" : "Nueva nota:");
        lblNueva.setStyle("-fx-font-weight: bold; -fx-text-fill: #2D1A28;");

        TextField tfTitulo = new TextField();
        tfTitulo.setPromptText("Título  (ej: Reunión con cliente, entrega pedido…)");

        TextArea taDetalle = new TextArea();
        taDetalle.setPromptText("Detalles opcionales…");
        taDetalle.setPrefRowCount(3);
        taDetalle.setWrapText(true);

        cuerpo.getChildren().addAll(lblNueva, tfTitulo, taDetalle);

        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(520);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dialog.getDialogPane().setContent(scroll);

        if (getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String tit = tfTitulo.getText().trim();
                if (!tit.isEmpty()) {
                    try {
                        dao.guardar(new NotaCalendario(fecha, tit, taDetalle.getText().trim()));
                        cargarMes();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR,
                            "No se pudo guardar la nota:\n" + ex.getMessage()).showAndWait();
                    }
                }
            }
        });
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
