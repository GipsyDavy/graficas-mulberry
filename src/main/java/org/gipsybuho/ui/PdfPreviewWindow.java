package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfPreviewWindow {

    public static void mostrar(byte[] pdfBytes, String titulo) {
        if (pdfBytes == null || pdfBytes.length == 0) return;

        List<javafx.scene.image.Image> paginas = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int nPags = doc.getNumberOfPages();
            for (int i = 0; i < nPags; i++) {
                BufferedImage bi = renderer.renderImageWithDPI(i, 150);
                paginas.add(SwingFXUtils.toFXImage(bi, null));
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo renderizar el PDF:\n" + e.getMessage());
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        if (paginas.isEmpty()) return;

        int[] paginaActual = {0};

        ImageView imgView = new ImageView(paginas.get(0));
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(780);

        ScrollPane scroll = new ScrollPane(imgView);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background-color: #888;");

        Label lblPagina = new Label("Página 1 / " + paginas.size());
        lblPagina.setStyle("-fx-font-size:12px; -fx-font-weight:bold;");

        Button btnAnterior  = new Button("◀ Anterior");
        Button btnSiguiente = new Button("Siguiente ▶");
        Button btnZoomIn    = new Button("🔍+");
        Button btnZoomOut   = new Button("🔍−");

        for (Button b : new Button[]{btnAnterior, btnSiguiente, btnZoomIn, btnZoomOut}) {
            b.setStyle("-fx-padding:4 12; -fx-background-radius:4;");
        }

        btnAnterior.setDisable(true);
        btnSiguiente.setDisable(paginas.size() <= 1);

        btnAnterior.setOnAction(e -> {
            if (paginaActual[0] > 0) {
                paginaActual[0]--;
                imgView.setImage(paginas.get(paginaActual[0]));
                lblPagina.setText("Página " + (paginaActual[0] + 1) + " / " + paginas.size());
                btnAnterior.setDisable(paginaActual[0] == 0);
                btnSiguiente.setDisable(false);
                scroll.setVvalue(0);
            }
        });

        btnSiguiente.setOnAction(e -> {
            if (paginaActual[0] < paginas.size() - 1) {
                paginaActual[0]++;
                imgView.setImage(paginas.get(paginaActual[0]));
                lblPagina.setText("Página " + (paginaActual[0] + 1) + " / " + paginas.size());
                btnSiguiente.setDisable(paginaActual[0] == paginas.size() - 1);
                btnAnterior.setDisable(false);
                scroll.setVvalue(0);
            }
        });

        double[] zoom = {780.0};
        btnZoomIn.setOnAction(e -> {
            zoom[0] = Math.min(zoom[0] * 1.25, 2400);
            imgView.setFitWidth(zoom[0]);
        });
        btnZoomOut.setOnAction(e -> {
            zoom[0] = Math.max(zoom[0] / 1.25, 300);
            imgView.setFitWidth(zoom[0]);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controles = new HBox(8, btnAnterior, lblPagina, btnSiguiente, spacer, btnZoomOut, btnZoomIn);
        controles.setAlignment(Pos.CENTER_LEFT);
        controles.setPadding(new Insets(8, 12, 8, 12));
        controles.setStyle("-fx-background-color:#f5f5f5; -fx-border-color:#dddddd; -fx-border-width:0 0 1 0;");

        BorderPane root = new BorderPane();
        root.setTop(controles);
        root.setCenter(scroll);

        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.initModality(Modality.NONE);
        stage.setScene(new Scene(root, 850, 720));
        stage.show();
    }
}
