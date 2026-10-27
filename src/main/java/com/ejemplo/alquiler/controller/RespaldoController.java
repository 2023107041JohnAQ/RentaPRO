package com.ejemplo.alquiler.controller;

import com.ejemplo.alquiler.service.RespaldoService;
import com.ejemplo.alquiler.service.RespaldoService.Respaldo;
import com.ejemplo.alquiler.util.Abrir;
import com.ejemplo.alquiler.util.Dialogos;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Component
public class RespaldoController {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RespaldoService servicio;

    public RespaldoController(RespaldoService servicio) {
        this.servicio = servicio;
    }

    @FXML private TableView<Respaldo> tabla;
    @FXML private TableColumn<Respaldo, String> colFecha, colTipo, colTamano, colArchivo;
    @FXML private Label lblMensaje, lblCarpeta, lblUltimo;
    @FXML private Button btnCrear, btnCopia, btnRestaurar;

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(FECHA_HORA.format(c.getValue().fecha())));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().manual() ? "MANUAL" : "AUTO"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                if (vacio || texto == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(texto);
                insignia.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase(Locale.ROOT));
                setText(null);
                setGraphic(insignia);
            }
        });
        colTamano.setCellValueFactory(c -> new SimpleStringProperty(tamano(c.getValue().bytes())));
        colArchivo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().archivo().getFileName().toString()));
        lblCarpeta.setText(servicio.carpeta().toString());
        btnRestaurar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        refrescar();
    }

    private static String tamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public void refrescar() {
        if (tabla == null) return;
        List<Respaldo> lista = servicio.listar();
        tabla.setItems(FXCollections.observableArrayList(lista));
        lblUltimo.setText(lista.isEmpty()
                ? "Todavía no hay respaldos. El primero se crea solo al abrir la aplicación o con el botón de abajo."
                : "Último respaldo: " + FECHA_HORA.format(lista.get(0).fecha()) + " · " + lista.size() + " guardado(s).");
    }

    private void mostrar(boolean ok, String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        lblMensaje.getStyleClass().removeIf(c -> c.startsWith("banner-"));
        lblMensaje.getStyleClass().add(ok ? "banner-ok" : "banner-alerta");
    }

    private void ocupado(boolean si) {
        btnCrear.setDisable(si);
        btnCopia.setDisable(si);
    }

    @FXML
    private void crearAhora() {
        ocupado(true);
        mostrar(true, "Creando respaldo...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return servicio.crear(true);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).whenComplete((ruta, ex) -> Platform.runLater(() -> {
            ocupado(false);
            if (ex != null) mostrar(false, "No se pudo crear el respaldo: " + mensajeDe(ex));
            else mostrar(true, "Respaldo creado: " + ruta.getFileName());
            refrescar();
        }));
    }

    @FXML
    private void guardarCopia() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar copia de la base de datos");
        fc.setInitialFileName("rentapro-copia-" + LocalDate.now() + ".db");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos RentaPro (*.db)", "*.db"));
        File destino = fc.showSaveDialog(tabla.getScene().getWindow());
        if (destino == null) return;
        ocupado(true);
        mostrar(true, "Guardando copia...");
        CompletableFuture.runAsync(() -> {
            try {
                servicio.exportar(destino.toPath());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).whenComplete((v, ex) -> Platform.runLater(() -> {
            ocupado(false);
            if (ex != null) mostrar(false, "No se pudo guardar la copia: " + mensajeDe(ex));
            else mostrar(true, "Copia guardada en: " + destino.getAbsolutePath());
        }));
    }

    @FXML
    private void restaurarSeleccionado() {
        Respaldo r = tabla.getSelectionModel().getSelectedItem();
        if (r == null) return;
        restaurar(r.archivo(), FECHA_HORA.format(r.fecha()));
    }

    @FXML
    private void restaurarDesdeArchivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Elegir un respaldo (.db) para restaurar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos RentaPro (*.db)", "*.db"));
        File f = fc.showOpenDialog(tabla.getScene().getWindow());
        if (f != null) restaurar(f.toPath(), f.getName());
    }

    private void restaurar(Path archivo, String descripcion) {
        if (!Files.isRegularFile(archivo)) {
            mostrar(false, "El archivo ya no existe: " + archivo);
            return;
        }
        if (!Dialogos.confirmar("Restaurar el respaldo de " + descripcion,
                "Los datos actuales (clientes, reservas, pagos...) serán reemplazados por los de ese respaldo.\n\n"
                        + "La base de datos de ahora se conserva en la carpeta de respaldos con el nombre «rentapro-antes-de-restaurar-...», "
                        + "por si necesitas volver atrás.\n\nLa restauración se aplica al volver a abrir RentaPro.",
                "Preparar restauración")) return;
        try {
            servicio.programarRestauracion(archivo);
        } catch (Exception e) {
            mostrar(false, e.getMessage());
            return;
        }
        mostrar(true, "Restauración preparada. Cierra RentaPro y vuelve a abrirla para aplicarla.");
        if (Dialogos.confirmar("Restauración preparada", "¿Quieres cerrar RentaPro ahora para aplicarla? Después ábrela de nuevo.", "Cerrar RentaPro")) {
            Platform.exit();
        }
    }

    @FXML
    private void abrirCarpeta() {
        try {
            Files.createDirectories(servicio.carpeta());
        } catch (Exception ignorada) {
        }
        if (!Abrir.ruta(servicio.carpeta())) mostrar(false, "No se pudo abrir la carpeta. Búscala en: " + servicio.carpeta());
    }

    private static String mensajeDe(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
