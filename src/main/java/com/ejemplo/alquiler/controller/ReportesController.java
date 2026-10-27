package com.ejemplo.alquiler.controller;

import com.ejemplo.alquiler.service.ReporteService;
import com.ejemplo.alquiler.service.ReporteService.FilaMora;
import com.ejemplo.alquiler.service.ReporteService.FilaVehiculo;
import com.ejemplo.alquiler.service.ReporteService.Mes;
import com.ejemplo.alquiler.service.ReporteService.Reporte;
import com.ejemplo.alquiler.util.Formato;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Component
public class ReportesController {

    private final ReporteService servicio;
    private Reporte ultimo;

    public ReportesController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @FXML private DatePicker dpDesde, dpHasta;
    @FXML private Label lblIngresos, lblAlquileres, lblOcupacion, lblTicket, lblPorCobrar, lblRecargos, lblPeriodo, lblMensaje;
    @FXML private BarChart<String, Number> grafico;
    @FXML private TableView<FilaVehiculo> tablaVehiculos;
    @FXML private TableColumn<FilaVehiculo, String> colVeh, colTipo, colReservas, colDias, colOcup, colFact;
    @FXML private TableView<FilaMora> tablaMora;
    @FXML private TableColumn<FilaMora, String> colMoCliente, colMoTel, colMoVeh, colMoDias, colMoRecargo, colMoSaldo;

    @FXML
    public void initialize() {
        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);

        colVeh.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehiculo()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tipo()));
        colReservas.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().reservas())));
        colDias.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().diasAlquilados())));
        colOcup.setCellValueFactory(c -> new SimpleStringProperty(porcentaje(c.getValue().ocupacion())));
        colFact.setCellValueFactory(c -> new SimpleStringProperty(Formato.moneda(c.getValue().facturado())));

        colMoCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cliente()));
        colMoTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().telefono() == null ? "" : c.getValue().telefono()));
        colMoVeh.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehiculo()));
        colMoDias.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().diasRetraso() + " día(s)"));
        colMoRecargo.setCellValueFactory(c -> new SimpleStringProperty(Formato.moneda(c.getValue().recargo())));
        colMoSaldo.setCellValueFactory(c -> new SimpleStringProperty(Formato.moneda(c.getValue().saldo())));

        refrescar();
    }

    private static String porcentaje(double v) {
        return String.format(Locale.US, "%.1f %%", v);
    }

    public void refrescar() {
        if (tablaVehiculos == null) return;
        try {
            Reporte r = servicio.generar(dpDesde.getValue(), dpHasta.getValue());
            ultimo = r;
            mostrar(r);
            lblMensaje.setVisible(false);
            lblMensaje.setManaged(false);
        } catch (Exception e) {
            lblMensaje.setText(e.getMessage() == null ? "No se pudo generar el reporte." : e.getMessage());
            lblMensaje.getStyleClass().removeIf(c -> c.startsWith("banner-"));
            lblMensaje.getStyleClass().add("banner-alerta");
            lblMensaje.setVisible(true);
            lblMensaje.setManaged(true);
        }
    }

    private void mostrar(Reporte r) {
        var s = r.resumen();
        lblPeriodo.setText("Del " + Formato.fecha(r.desde()) + " al " + Formato.fecha(r.hasta()) + " · " + s.dias() + " día(s) · flota activa: " + s.flotaActiva());
        lblIngresos.setText(Formato.moneda(s.ingresosCobrados()));
        lblAlquileres.setText(String.valueOf(s.alquileres()));
        lblOcupacion.setText(porcentaje(s.ocupacion()));
        lblTicket.setText(Formato.moneda(s.ticketPromedio()));
        lblPorCobrar.setText(Formato.moneda(s.porCobrar()));
        lblRecargos.setText(Formato.moneda(s.recargos()));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Mes m : r.meses()) serie.getData().add(new XYChart.Data<>(m.etiqueta(), m.monto()));
        grafico.getData().clear();
        grafico.getData().add(serie);

        tablaVehiculos.setItems(FXCollections.observableArrayList(r.vehiculos()));
        tablaMora.setItems(FXCollections.observableArrayList(r.mora()));
    }

    @FXML
    private void generar() {
        refrescar();
    }

    @FXML
    private void esteMes() {
        LocalDate hoy = LocalDate.now();
        poner(hoy.withDayOfMonth(1), hoy);
    }

    @FXML
    private void mesAnterior() {
        LocalDate primero = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        poner(primero, primero.withDayOfMonth(primero.lengthOfMonth()));
    }

    @FXML
    private void ultimos30() {
        LocalDate hoy = LocalDate.now();
        poner(hoy.minusDays(29), hoy);
    }

    @FXML
    private void esteAnio() {
        LocalDate hoy = LocalDate.now();
        poner(hoy.withDayOfYear(1), hoy);
    }

    private void poner(LocalDate desde, LocalDate hasta) {
        dpDesde.setValue(desde);
        dpHasta.setValue(hasta);
        refrescar();
    }

    @FXML
    private void exportar() {
        refrescar();
        if (ultimo == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte (se abre con Excel)");
        fc.setInitialFileName("reporte-rentapro-" + ultimo.desde() + "_a_" + ultimo.hasta() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel / CSV (*.csv)", "*.csv"));
        File destino = fc.showSaveDialog(tablaVehiculos.getScene().getWindow());
        if (destino == null) return;
        try {
            Files.writeString(destino.toPath(), "﻿" + csv(ultimo), StandardCharsets.UTF_8);
            lblMensaje.setText("Reporte guardado en: " + destino.getAbsolutePath());
            lblMensaje.getStyleClass().removeIf(c -> c.startsWith("banner-"));
            lblMensaje.getStyleClass().add("banner-ok");
            lblMensaje.setVisible(true);
            lblMensaje.setManaged(true);
        } catch (Exception e) {
            com.ejemplo.alquiler.util.Dialogos.error("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    private static String csv(Reporte r) {
        StringBuilder sb = new StringBuilder("sep=;\r\n");
        var s = r.resumen();
        fila(sb, "REPORTE RENTAPRO");
        fila(sb, "Periodo", Formato.fecha(r.desde()) + " al " + Formato.fecha(r.hasta()));
        fila(sb);
        fila(sb, "RESUMEN");
        fila(sb, "Ingresos cobrados (S/)", num(s.ingresosCobrados()));
        fila(sb, "Facturado (S/)", num(s.facturado()));
        fila(sb, "Alquileres", String.valueOf(s.alquileres()));
        fila(sb, "Cancelaciones", String.valueOf(s.cancelaciones()));
        fila(sb, "Ocupación de la flota (%)", num(s.ocupacion()));
        fila(sb, "Ticket promedio (S/)", num(s.ticketPromedio()));
        fila(sb, "Recargos por retraso (S/)", num(s.recargos()));
        fila(sb, "Por cobrar (S/)", num(s.porCobrar()));
        fila(sb);
        fila(sb, "INGRESOS COBRADOS POR MES");
        fila(sb, "Mes", "Monto (S/)");
        for (Mes m : r.meses()) fila(sb, m.etiqueta(), num(m.monto()));
        fila(sb);
        fila(sb, "RENTABILIDAD POR VEHICULO");
        fila(sb, "Vehiculo", "Tipo", "Reservas", "Dias alquilados", "Ocupacion (%)", "Facturado (S/)");
        for (FilaVehiculo v : r.vehiculos())
            fila(sb, v.vehiculo(), v.tipo(), String.valueOf(v.reservas()), String.valueOf(v.diasAlquilados()), num(v.ocupacion()), num(v.facturado()));
        fila(sb);
        fila(sb, "CLIENTES EN MORA (a hoy)");
        fila(sb, "Cliente", "Telefono", "Vehiculo", "Dias de retraso", "Recargo (S/)", "Saldo (S/)");
        for (FilaMora m : r.mora())
            fila(sb, m.cliente(), m.telefono() == null ? "" : m.telefono(), m.vehiculo(), String.valueOf(m.diasRetraso()), num(m.recargo()), num(m.saldo()));
        return sb.toString();
    }

    private static String num(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static void fila(StringBuilder sb, String... celdas) {
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) sb.append(';');
            String c = celdas[i] == null ? "" : celdas[i];
            if (c.contains(";") || c.contains("\"") || c.contains("\n")) c = "\"" + c.replace("\"", "\"\"") + "\"";
            sb.append(c);
        }
        sb.append("\r\n");
    }
}
