package com.ejemplo.alquiler.controller;

import com.ejemplo.alquiler.model.Mantenimiento;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.service.AlquilerService;
import com.ejemplo.alquiler.service.MantenimientoService;
import com.ejemplo.alquiler.service.MantenimientoService.Registro;
import com.ejemplo.alquiler.util.Dialogos;
import com.ejemplo.alquiler.util.Formato;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Component
public class MantenimientoController {

    private final MantenimientoService servicio;
    private final AlquilerService alquiler;

    private Runnable alCambiar = () -> { };

    private Mantenimiento editando;

    public MantenimientoController(MantenimientoService servicio, AlquilerService alquiler) {
        this.servicio = servicio;
        this.alquiler = alquiler;
    }

    public void setAlCambiar(Runnable r) {
        alCambiar = r == null ? () -> { } : r;
    }

    @FXML private Label tituloForm, lblAlertas;
    @FXML private ComboBox<Vehiculo> cbVehiculo;
    @FXML private ComboBox<String> cbTipo;
    @FXML private DatePicker dpFecha, dpVencimiento;
    @FXML private TextField txtKm, txtCosto, txtNotas;
    @FXML private CheckBox chkSoloAlertas;
    @FXML private TableView<Registro> tabla;
    @FXML private TableColumn<Registro, String> colVehiculo, colTipo, colFecha, colVence, colEstado, colDias, colKm, colCosto, colNotas;

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList(Mantenimiento.TIPOS));
        dpFecha.setValue(LocalDate.now());

        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mantenimiento().getVehiculo().toString()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mantenimiento().getTipo()));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(Formato.fecha(c.getValue().mantenimiento().getFecha())));
        colVence.setCellValueFactory(c -> new SimpleStringProperty(Formato.fecha(c.getValue().mantenimiento().getVencimiento())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                if (vacio || texto == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(texto);
                insignia.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase(Locale.ROOT).replace(' ', '-'));
                setText(null);
                setGraphic(insignia);
            }
        });
        colDias.setCellValueFactory(c -> new SimpleStringProperty(textoDias(c.getValue())));
        colKm.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mantenimiento().getKilometraje() == null
                ? "" : String.format(Locale.US, "%,d km", c.getValue().mantenimiento().getKilometraje())));
        colCosto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mantenimiento().getCosto() > 0
                ? Formato.moneda(c.getValue().mantenimiento().getCosto()) : ""));
        colNotas.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().mantenimiento().getNotas() == null ? "" : c.getValue().mantenimiento().getNotas()));

        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> {
            if (r != null) cargar(r.mantenimiento());
        });
        chkSoloAlertas.selectedProperty().addListener((o, a, b) -> refrescar());
        refrescar();
    }

    private static String textoDias(Registro r) {
        if (r.dias() == null) return "—";
        long d = r.dias();
        if (MantenimientoService.RENOVADO.equals(r.estado())) return "Reemplazado por uno más nuevo";
        if (d < 0) return "Venció hace " + (-d) + " día(s)";
        if (d == 0) return "Vence hoy";
        return "Faltan " + d + " día(s)";
    }

    public void refrescar() {
        if (tabla == null) return;
        Vehiculo previo = cbVehiculo.getValue();
        List<Vehiculo> vs = alquiler.vehiculos();
        cbVehiculo.setItems(FXCollections.observableArrayList(vs));
        if (previo != null) {
            int pos = vs.indexOf(previo);
            cbVehiculo.setValue(pos >= 0 ? vs.get(pos) : null);
        }

        List<Registro> todos = servicio.registros();
        List<Registro> alertas = todos.stream().filter(Registro::esAlerta)
                .filter(r -> r.mantenimiento().getVehiculo().isActivo()).toList();
        tabla.setItems(FXCollections.observableArrayList(chkSoloAlertas.isSelected() ? alertas : todos));

        long vencidos = alertas.stream().filter(r -> MantenimientoService.VENCIDO.equals(r.estado())).count();
        long porVencer = alertas.size() - vencidos;
        lblAlertas.getStyleClass().removeIf(c -> c.startsWith("banner-"));
        if (alertas.isEmpty()) {
            lblAlertas.setText("Todo al día: no hay documentos vencidos ni por vencer en los próximos " + MantenimientoService.DIAS_AVISO + " días.");
            lblAlertas.getStyleClass().add("banner-ok");
        } else {
            lblAlertas.setText("Atención: " + vencidos + " vencido(s) y " + porVencer + " por vencer en los próximos "
                    + MantenimientoService.DIAS_AVISO + " días.");
            lblAlertas.getStyleClass().add("banner-alerta");
        }
    }

    private void cargar(Mantenimiento m) {
        editando = m;
        tituloForm.setText("Editar registro #" + m.getId());
        cbVehiculo.setValue(m.getVehiculo());
        cbTipo.setValue(m.getTipo());
        dpFecha.setValue(m.getFecha());
        dpVencimiento.setValue(m.getVencimiento());
        txtKm.setText(m.getKilometraje() == null ? "" : String.valueOf(m.getKilometraje()));
        txtCosto.setText(m.getCosto() > 0 ? String.format(Locale.US, "%.2f", m.getCosto()) : "");
        txtNotas.setText(m.getNotas() == null ? "" : m.getNotas());
    }

    @FXML
    private void nuevo() {
        editando = null;
        tabla.getSelectionModel().clearSelection();
        tituloForm.setText("Registrar documento o servicio");
        cbTipo.setValue(null);
        cbTipo.getEditor().clear();
        dpFecha.setValue(LocalDate.now());
        dpVencimiento.setValue(null);
        txtKm.clear();
        txtCosto.clear();
        txtNotas.clear();
    }

    @FXML
    private void guardar() {
        try {
            Mantenimiento m = new Mantenimiento();
            if (editando != null) m.setId(editando.getId());
            m.setVehiculo(cbVehiculo.getValue());
            String tipo = cbTipo.getEditor().getText();
            m.setTipo(tipo == null || tipo.isBlank() ? cbTipo.getValue() : tipo);
            m.setFecha(dpFecha.getValue());
            m.setVencimiento(dpVencimiento.getValue());
            String km = txtKm.getText() == null ? "" : txtKm.getText().replace(",", "").replace(".", "").trim();
            if (!km.isEmpty()) {
                try {
                    m.setKilometraje(Integer.parseInt(km));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El kilometraje debe ser un número entero.");
                }
            }
            if (txtCosto.getText() != null && !txtCosto.getText().isBlank()) m.setCosto(Formato.numero(txtCosto.getText(), "el costo"));
            m.setNotas(txtNotas.getText() == null || txtNotas.getText().isBlank() ? null : txtNotas.getText().trim());
            servicio.guardar(m);
            nuevo();
            refrescar();
            alCambiar.run();
        } catch (Exception e) {
            Dialogos.error(e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        try {
            Registro r = tabla.getSelectionModel().getSelectedItem();
            if (r == null) throw new IllegalArgumentException("Selecciona un registro de la tabla.");
            if (!Dialogos.confirmar("Eliminar registro", "¿Eliminar «" + r.mantenimiento().getTipo() + "» de "
                    + r.mantenimiento().getVehiculo() + "?", "Eliminar")) return;
            servicio.eliminar(r.mantenimiento());
            nuevo();
            refrescar();
            alCambiar.run();
        } catch (Exception e) {
            Dialogos.error(e.getMessage());
        }
    }
}
