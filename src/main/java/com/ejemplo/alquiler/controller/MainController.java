package com.ejemplo.alquiler.controller;

import com.ejemplo.alquiler.JavaFxApplication;
import com.ejemplo.alquiler.model.Cliente;
import com.ejemplo.alquiler.model.Contrato;
import com.ejemplo.alquiler.model.Pago;
import com.ejemplo.alquiler.model.Reserva;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.service.AlquilerService;
import com.ejemplo.alquiler.service.AlquilerService.CuentaReserva;
import com.ejemplo.alquiler.service.ContratoPdfService;
import com.ejemplo.alquiler.service.ImagenService;
import com.ejemplo.alquiler.service.MantenimientoService;
import com.ejemplo.alquiler.service.NotificacionService;
import com.ejemplo.alquiler.util.Formato;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Jhon Alessandro Arpi Quispe
 * @author Julio Cesar Tutacano de la Cruz
 * @author Jhunior Eric Herrera Condori
 */
@Component
public class MainController {

    private final AlquilerService service;
    private final ContratoPdfService pdf;
    private final ImagenService imagenes;
    private final NotificacionService notificaciones;
    private final NotificacionesController notificacionesCtl;
    private final MantenimientoService mantenimiento;
    private final MantenimientoController mantenimientoCtl;
    private final ReportesController reportesCtl;
    private final RespaldoController respaldoCtl;

    public MainController(AlquilerService service, ContratoPdfService pdf, ImagenService imagenes,
                          NotificacionService notificaciones, NotificacionesController notificacionesCtl,
                          MantenimientoService mantenimiento, MantenimientoController mantenimientoCtl,
                          ReportesController reportesCtl, RespaldoController respaldoCtl) {
        this.service = service;
        this.pdf = pdf;
        this.imagenes = imagenes;
        this.notificaciones = notificaciones;
        this.notificacionesCtl = notificacionesCtl;
        this.mantenimiento = mantenimiento;
        this.mantenimientoCtl = mantenimientoCtl;
        this.reportesCtl = reportesCtl;
        this.respaldoCtl = respaldoCtl;
    }

    @FXML private Label kpiFlota, kpiDisponibles, kpiActivas, kpiPorCobrar, kpiIngresos, kpiAlertas;
    @FXML private TabPane tabs;
    @FXML private ToggleGroup grupoMenu;
    @FXML private Label lblPagina;

    @FXML private Label tituloFormVehiculo;
    @FXML private StackPane fotoPreview;
    @FXML private TextField placa, marca, modelo, tarifa, buscarVehiculo, buscarCliente;
    @FXML private ComboBox<String> cbTipo;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardarVehiculo;
    @FXML private TableView<Vehiculo> tablaVehiculos;
    @FXML private TableColumn<Vehiculo, Vehiculo> colFoto;
    @FXML private TableColumn<Vehiculo, String> colPlaca, colVehiculo, colTipo, colActivo;
    @FXML private TableColumn<Vehiculo, Number> colTarifa;

    @FXML private Label tituloFormCliente;
    @FXML private TextField documento, nombres, telefono, correo, direccion, licencia;
    @FXML private Button btnGuardarCliente;
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colDocumento, colCliente, colTelefono, colCorreo;

    @FXML private DatePicker inicioDisponibilidad, finDisponibilidad, inicioReserva, finReserva;
    @FXML private Label lblDisponibles, lblTotalReserva;
    @FXML private HBox panelDisponibles;
    @FXML private ComboBox<Vehiculo> vehiculoReserva;
    @FXML private ComboBox<Cliente> clienteReserva;
    @FXML private TableView<Reserva> tablaReservas;
    @FXML private TableColumn<Reserva, String> colReservaNro, colReservaCliente, colReservaVehiculo, colReservaPeriodo, colEstado, colReservaPago;
    @FXML private TableColumn<Reserva, Number> colTotal, colReservaSaldo;

    @FXML private ComboBox<Reserva> reservaContrato;
    @FXML private StackPane contratoFoto;
    @FXML private Label lblContratoResumen, lblCarpetaContratos;
    @FXML private TableView<Contrato> tablaContratos;
    @FXML private TableColumn<Contrato, String> colCtCodigo, colCtCliente, colCtVehiculo, colCtFecha, colCtEstado;

    @FXML private ComboBox<Reserva> reservaDevolucion, reservaPago;
    @FXML private DatePicker fechaDevolucion;
    @FXML private Label lblReglaRecargo, bannerRetraso, lblPrevista, lblReal, lblDias, lblTarifaDia, lblRecargoDia,
            lblRecargo, lblTotalAlquiler, lblPagado, lblSaldoDev, lblSaldoPago;
    @FXML private TextField montoPago;
    @FXML private ComboBox<String> metodoPago, conceptoPago;
    @FXML private TableView<Pago> tablaPagos;
    @FXML private TableColumn<Pago, String> colPagoFecha, colPagoReserva, colPagoMetodo, colPagoConcepto;
    @FXML private TableColumn<Pago, Number> colPagoMonto;

    @FXML private Label lblPorCobrar, lblDetalleSaldo;
    @FXML private CheckBox chkSoloDeuda;
    @FXML private TableView<CuentaReserva> tablaCuentas;
    @FXML private TableColumn<CuentaReserva, String> colCuNro, colCuCliente, colCuVehiculo, colCuRetraso, colCuEstado;
    @FXML private TableColumn<CuentaReserva, Number> colCuAlquiler, colCuRecargo, colCuPagado, colCuSaldo;

    @FXML private Label estado;

    private final ObservableList<Vehiculo> vehiculosBase = FXCollections.observableArrayList();
    private FilteredList<Vehiculo> vehiculosFiltrados;
    private final ObservableList<Cliente> clientesBase = FXCollections.observableArrayList();
    private FilteredList<Cliente> clientesFiltrados;
    private Vehiculo vehiculoEditando;
    private String imagenActual;
    private File imagenPendiente;
    private Cliente clienteEditando;

    private Map<Long, CuentaReserva> cuentasPorReserva = Map.of();
    private List<CuentaReserva> cuentasActuales = List.of();
    private String ultimaFirma;
    private boolean actualizando;

    @FXML
    public void initialize() {
        configurarVehiculos();
        configurarClientes();
        configurarReservas();
        configurarContratos();
        configurarDevolucionYPagos();
        configurarCuentas();

        LocalDate hoy = LocalDate.now();
        inicioDisponibilidad.setValue(hoy);
        finDisponibilidad.setValue(hoy.plusDays(2));
        inicioReserva.setValue(hoy);
        finReserva.setValue(hoy.plusDays(2));
        inicioDisponibilidad.valueProperty().addListener((o, a, b) -> actualizarDisponibles(true, true));
        finDisponibilidad.valueProperty().addListener((o, a, b) -> actualizarDisponibles(true, true));

        mantenimientoCtl.setAlCambiar(this::actualizarKpis);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> sincronizarMenu());
        sincronizarMenu();
        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> actualizarOperaciones(false));
        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(20), e -> actualizarOperaciones(false)));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();

        refrescar();
    }

    private void configurarVehiculos() {
        cbTipo.setItems(FXCollections.observableArrayList(Vehiculo.TIPOS));

        colFoto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colFoto.setSortable(false);
        colFoto.setCellFactory(col -> new TableCell<Vehiculo, Vehiculo>() {
            @Override
            protected void updateItem(Vehiculo v, boolean vacio) {
                super.updateItem(v, vacio);
                setText(null);
                setGraphic(vacio || v == null ? null : imagenes.miniatura(v.getImagenPath(), 72, 48));
            }
        });
        colPlaca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlaca()));
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombreCompleto()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        colTarifa.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTarifaDiaria()));
        colTarifa.setCellFactory(col -> celdaMoneda());
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Disponible" : "Inactivo"));
        tablaVehiculos.setFixedCellSize(56);

        vehiculosFiltrados = new FilteredList<>(vehiculosBase, v -> true);
        SortedList<Vehiculo> ordenados = new SortedList<>(vehiculosFiltrados);
        ordenados.comparatorProperty().bind(tablaVehiculos.comparatorProperty());
        tablaVehiculos.setItems(ordenados);

        buscarVehiculo.textProperty().addListener((o, a, texto) -> vehiculosFiltrados.setPredicate(v -> coincide(v, texto)));
        tablaVehiculos.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v != null) cargarVehiculo(v);
        });
        actualizarFotoPreview();
    }

    private boolean coincide(Vehiculo v, String texto) {
        if (texto == null || texto.isBlank()) return true;
        String t = texto.toLowerCase(Locale.ROOT).trim();
        return (v.getPlaca() + " " + v.getMarca() + " " + v.getModelo() + " " + v.getTipo()).toLowerCase(Locale.ROOT).contains(t);
    }

    @FXML
    private void elegirImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar fotografía del vehículo o moto");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes (png, jpg, gif, bmp)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File archivo = fc.showOpenDialog(placa.getScene().getWindow());
        if (archivo != null) {
            imagenPendiente = archivo;
            actualizarFotoPreview();
            mensaje("Imagen seleccionada: " + archivo.getName() + ". Pulsa Guardar para aplicarla.");
        }
    }

    @FXML
    private void quitarImagen() {
        imagenPendiente = null;
        imagenActual = null;
        actualizarFotoPreview();
    }

    private void actualizarFotoPreview() {
        String ruta = imagenPendiente != null ? imagenPendiente.getAbsolutePath() : imagenActual;
        fotoPreview.getChildren().setAll(imagenes.miniatura(ruta, 300, 150));
    }

    @FXML
    private void guardarVehiculo() {
        try {
            Vehiculo v = new Vehiculo();
            if (vehiculoEditando != null) v.setId(vehiculoEditando.getId());
            v.setPlaca(placa.getText() == null ? "" : placa.getText().trim().toUpperCase());
            v.setMarca(marca.getText() == null ? "" : marca.getText().trim());
            v.setModelo(modelo.getText() == null ? "" : modelo.getText().trim());
            v.setTipo(cbTipo.getValue());
            v.setTarifaDiaria(Formato.numero(tarifa.getText(), "la tarifa diaria"));
            v.setActivo(chkActivo.isSelected());
            v.setImagenPath(imagenPendiente != null ? imagenes.guardar(imagenPendiente) : imagenActual);

            boolean edicion = vehiculoEditando != null;
            service.guardarVehiculo(v);
            limpiarFormularioVehiculo();
            refrescar();
            mensaje(edicion ? "Vehículo actualizado correctamente." : "Vehículo registrado correctamente.");
        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void nuevoVehiculo() {
        tablaVehiculos.getSelectionModel().clearSelection();
        limpiarFormularioVehiculo();
    }

    @FXML
    private void eliminarVehiculo() {
        try {
            Vehiculo v = tablaVehiculos.getSelectionModel().getSelectedItem();
            if (v == null) throw new IllegalArgumentException("Selecciona un vehículo de la tabla.");
            if (!confirmar("¿Eliminar " + v + "?\nSi ya tiene reservas solo se marcará como inactivo.")) return;
            boolean eliminado = service.eliminarVehiculo(v);
            limpiarFormularioVehiculo();
            refrescar();
            mensaje(eliminado ? "Vehículo eliminado." : "El vehículo tiene historial de reservas: se marcó como inactivo.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void cargarVehiculo(Vehiculo v) {
        vehiculoEditando = v;
        placa.setText(v.getPlaca());
        marca.setText(v.getMarca());
        modelo.setText(v.getModelo());
        cbTipo.setValue(v.getTipo());
        tarifa.setText(String.format(Locale.US, "%.2f", v.getTarifaDiaria()));
        chkActivo.setSelected(v.isActivo());
        imagenActual = v.getImagenPath();
        imagenPendiente = null;
        tituloFormVehiculo.setText("Editando: " + v.getPlaca());
        btnGuardarVehiculo.setText("Actualizar");
        actualizarFotoPreview();
    }

    private void limpiarFormularioVehiculo() {
        vehiculoEditando = null;
        imagenActual = null;
        imagenPendiente = null;
        placa.clear();
        marca.clear();
        modelo.clear();
        tarifa.clear();
        cbTipo.setValue(null);
        chkActivo.setSelected(true);
        tituloFormVehiculo.setText("Registrar vehículo o moto");
        btnGuardarVehiculo.setText("Guardar");
        actualizarFotoPreview();
    }

    private void configurarClientes() {
        colDocumento.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDocumento()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombres()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colCorreo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCorreo() == null ? "" : c.getValue().getCorreo()));
        clientesFiltrados = new FilteredList<>(clientesBase, cl -> true);
        SortedList<Cliente> clientesOrdenados = new SortedList<>(clientesFiltrados);
        clientesOrdenados.comparatorProperty().bind(tablaClientes.comparatorProperty());
        tablaClientes.setItems(clientesOrdenados);
        buscarCliente.textProperty().addListener((o, a, texto) -> clientesFiltrados.setPredicate(cl -> coincide(cl, texto)));
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> {
            if (c != null) cargarCliente(c);
        });
    }

    private boolean coincide(Cliente cl, String texto) {
        if (texto == null || texto.isBlank()) return true;
        String t = texto.toLowerCase(Locale.ROOT).trim();
        String correo = cl.getCorreo() == null ? "" : cl.getCorreo();
        return (cl.getDocumento() + " " + cl.getNombres() + " " + cl.getTelefono() + " " + correo).toLowerCase(Locale.ROOT).contains(t);
    }

    @FXML
    private void guardarCliente() {
        try {
            Cliente c = new Cliente();
            if (clienteEditando != null) c.setId(clienteEditando.getId());
            c.setDocumento(documento.getText() == null ? "" : documento.getText().trim());
            c.setNombres(nombres.getText() == null ? "" : nombres.getText().trim());
            c.setTelefono(telefono.getText() == null ? "" : telefono.getText().trim());
            c.setCorreo(correo.getText() == null ? "" : correo.getText().trim());
            c.setDireccion(direccion.getText() == null ? "" : direccion.getText().trim());
            c.setLicencia(licencia.getText() == null ? "" : licencia.getText().trim());
            boolean edicion = clienteEditando != null;
            service.guardarCliente(c);
            limpiarFormularioCliente();
            refrescar();
            mensaje(edicion ? "Cliente actualizado correctamente." : "Cliente registrado correctamente.");
        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void nuevoCliente() {
        tablaClientes.getSelectionModel().clearSelection();
        limpiarFormularioCliente();
    }

    @FXML
    private void eliminarCliente() {
        try {
            Cliente c = tablaClientes.getSelectionModel().getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Selecciona un cliente de la tabla.");
            if (!confirmar("¿Eliminar al cliente " + c + "?")) return;
            service.eliminarCliente(c);
            limpiarFormularioCliente();
            refrescar();
            mensaje("Cliente eliminado.");
        } catch (Exception e) {
            error(e);
        }
    }

    private void cargarCliente(Cliente c) {
        clienteEditando = c;
        documento.setText(c.getDocumento());
        nombres.setText(c.getNombres());
        telefono.setText(c.getTelefono());
        correo.setText(c.getCorreo() == null ? "" : c.getCorreo());
        direccion.setText(c.getDireccion() == null ? "" : c.getDireccion());
        licencia.setText(c.getLicencia() == null ? "" : c.getLicencia());
        tituloFormCliente.setText("Editando: " + c.getNombres());
        btnGuardarCliente.setText("Actualizar");
    }

    private void limpiarFormularioCliente() {
        clienteEditando = null;
        documento.clear();
        nombres.clear();
        telefono.clear();
        correo.clear();
        direccion.clear();
        licencia.clear();
        tituloFormCliente.setText("Registrar cliente");
        btnGuardarCliente.setText("Guardar");
    }

    private void configurarReservas() {
        configurarComboVehiculo(vehiculoReserva);
        vehiculoReserva.valueProperty().addListener((o, a, v) -> {
            resaltarTarjeta(v);
            actualizarTotalReserva();
        });
        inicioReserva.valueProperty().addListener((o, a, b) -> actualizarTotalReserva());
        finReserva.valueProperty().addListener((o, a, b) -> actualizarTotalReserva());

        colReservaNro.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colReservaCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente().getNombres()));
        colReservaVehiculo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getVehiculo().getPlaca() + " · " + c.getValue().getVehiculo().nombreCompleto()));
        colReservaPeriodo.setCellValueFactory(c -> new SimpleStringProperty(
                Formato.fecha(c.getValue().getFechaInicio()) + " al " + Formato.fecha(c.getValue().getFechaFin())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(estadoVisible(c.getValue())));
        colEstado.setCellFactory(col -> new TableCell<Reserva, String>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                setText(null);
                if (vacio || texto == null) {
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(texto);
                insignia.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase(Locale.ROOT).replace(' ', '-'));
                setGraphic(insignia);
            }
        });
        colTotal.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalEstimado()));
        colTotal.setCellFactory(col -> celdaMoneda());
        colReservaPago.setCellValueFactory(c -> {
            CuentaReserva q = cuentasPorReserva.get(c.getValue().getId());
            return new SimpleStringProperty(q == null ? "" : q.estadoPago());
        });
        colReservaPago.setCellFactory(col -> celdaInsignia());
        colReservaSaldo.setCellValueFactory(c -> {
            CuentaReserva q = cuentasPorReserva.get(c.getValue().getId());
            return new SimpleDoubleProperty(q == null ? 0 : q.saldo());
        });
        colReservaSaldo.setCellFactory(col -> celdaMoneda());
    }

    private String estadoVisible(Reserva r) {
        return r.estaVencida(LocalDate.now()) ? "CON RETRASO" : r.getEstado();
    }

    @FXML
    private void comprobarDisponibilidad() {
        actualizarDisponibles(false, true);
    }

    private void actualizarDisponibles(boolean silencioso, boolean sincronizar) {
        if (panelDisponibles == null) return;
        try {
            LocalDate ini = inicioDisponibilidad.getValue();
            LocalDate fin = finDisponibilidad.getValue();
            List<Vehiculo> libres = service.disponibles(ini, fin);
            panelDisponibles.getChildren().clear();
            if (libres.isEmpty()) {
                Label vacio = new Label("No hay vehículos ni motos disponibles en ese rango de fechas.");
                vacio.getStyleClass().add("hint");
                panelDisponibles.getChildren().add(vacio);
            } else {
                libres.forEach(v -> panelDisponibles.getChildren().add(crearTarjeta(v)));
            }
            lblDisponibles.setText(libres.size() + " disponible(s) del " + Formato.fecha(ini) + " al " + Formato.fecha(fin));
            if (sincronizar) {
                inicioReserva.setValue(ini);
                finReserva.setValue(fin);
            }
            resaltarTarjeta(vehiculoReserva.getValue());
        } catch (IllegalArgumentException e) {
            if (silencioso) lblDisponibles.setText(e.getMessage());
            else error(e);
        }
    }

    private Node crearTarjeta(Vehiculo v) {
        VBox tarjeta = new VBox(4);
        tarjeta.getStyleClass().add("vcard");
        tarjeta.setUserData(v.getId());
        tarjeta.setPrefWidth(224);
        tarjeta.setMinWidth(224);

        Label titulo = new Label(v.nombreCompleto());
        titulo.getStyleClass().add("vcard-title");
        titulo.setMaxWidth(200);
        Label sub = new Label(v.getTipo() + " · " + v.getPlaca());
        sub.getStyleClass().add("vcard-sub");
        Label precio = new Label(Formato.moneda(v.getTarifaDiaria()) + " / día");
        precio.getStyleClass().add("vcard-price");

        tarjeta.getChildren().addAll(imagenes.miniatura(v.getImagenPath(), 200, 124), titulo, sub, precio);
        tarjeta.setOnMouseClicked(e -> {
            vehiculoReserva.setValue(v);
            resaltarTarjeta(v);
            mensaje("Seleccionado: " + v + ". Completa el cliente y pulsa «Crear reserva».");
        });
        return tarjeta;
    }

    private void resaltarTarjeta(Vehiculo v) {
        for (Node nodo : panelDisponibles.getChildren()) {
            boolean elegida = v != null && v.getId() != null && v.getId().equals(nodo.getUserData());
            nodo.getStyleClass().remove("vcard-selected");
            if (elegida) nodo.getStyleClass().add("vcard-selected");
        }
    }

    private void actualizarTotalReserva() {
        Vehiculo v = vehiculoReserva.getValue();
        LocalDate ini = inicioReserva.getValue();
        LocalDate fin = finReserva.getValue();
        if (v == null || ini == null || fin == null || fin.isBefore(ini)) {
            lblTotalReserva.setText("Total estimado: -");
            return;
        }
        long dias = AlquilerService.dias(ini, fin);
        lblTotalReserva.setText("Total estimado: " + dias + " día(s) × " + Formato.moneda(v.getTarifaDiaria())
                + " = " + Formato.moneda(dias * v.getTarifaDiaria()));
    }

    @FXML
    private void crearReserva() {
        try {
            Reserva r = service.reservar(vehiculoReserva.getValue(), clienteReserva.getValue(), inicioReserva.getValue(), finReserva.getValue());
            vehiculoReserva.setValue(null);
            refrescar();
            mensaje("Reserva #" + r.getId() + " creada. Total estimado: " + Formato.moneda(r.getTotalEstimado()));
            notificar(notificaciones.reservaCreada(r));
        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void cancelarReserva() {
        try {
            Reserva r = tablaReservas.getSelectionModel().getSelectedItem();
            if (r == null) throw new IllegalArgumentException("Selecciona una reserva de la tabla.");
            CuentaReserva cuenta = service.cuenta(r);
            String aviso = cuenta.pagado() > 0.004
                    ? "\nEl cliente ya pagó " + Formato.moneda(cuenta.pagado()) + ": se registrará un reembolso por ese monto."
                    : "";
            if (!confirmar("¿Cancelar la reserva #" + r.getId() + "?" + aviso)) return;
            double reembolso = service.cancelarReserva(r);
            actualizarOperaciones(true);
            mensaje("Reserva #" + r.getId() + " cancelada: el vehículo ya figura como disponible."
                    + (reembolso > 0 ? " Reembolso registrado: " + Formato.moneda(reembolso) + "." : ""));
            notificar(notificaciones.reservaCancelada(r, reembolso));
        } catch (Exception e) {
            error(e);
        }
    }

    private void configurarContratos() {
        configurarComboReserva(reservaContrato);
        reservaContrato.valueProperty().addListener((o, a, r) -> mostrarResumenContrato(r));
        lblCarpetaContratos.setText("Los PDF se guardan en: " + pdf.carpeta());

        colCtCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCtCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReserva().getCliente().getNombres()));
        colCtVehiculo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReserva().getVehiculo().toString()));
        colCtFecha.setCellValueFactory(c -> new SimpleStringProperty(Formato.fecha(c.getValue().getFechaFirma())));
        colCtEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReserva().getEstado()));
        tablaContratos.setRowFactory(tv -> {
            TableRow<Contrato> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) abrirContratoSeleccionado();
            });
            return fila;
        });
        mostrarResumenContrato(null);
    }

    private void mostrarResumenContrato(Reserva r) {
        if (r == null) {
            contratoFoto.getChildren().setAll(imagenes.miniatura(null, 150, 100));
            lblContratoResumen.setText("Selecciona una reserva para ver el resumen del contrato.");
            return;
        }
        contratoFoto.getChildren().setAll(imagenes.miniatura(r.getVehiculo().getImagenPath(), 150, 100));
        String pagos = "";
        double total = r.getTotalEstimado();
        try {
            CuentaReserva q = service.cuenta(r);
            total = q.totalConRecargo();
            pagos = "\nPagado: " + Formato.moneda(q.pagado()) + "  ·  Saldo: " + Formato.moneda(q.saldo()) + "  (" + q.estadoPago() + ")";
        } catch (Exception ignorada) {
        }
        lblContratoResumen.setText("Cliente: " + r.getCliente().getNombres()
                + "\nDocumento: " + r.getCliente().getDocumento()
                + "\nVehículo: " + r.getVehiculo().nombreCompleto() + " (" + r.getVehiculo().getPlaca() + ")"
                + "\nPeriodo: " + Formato.fecha(r.getFechaInicio()) + " al " + Formato.fecha(r.getFechaFin()) + " (" + r.dias() + " día/s)"
                + "\nTotal: " + Formato.moneda(total) + pagos);
    }

    @FXML
    private void crearContrato() {
        try {
            Reserva reserva = reservaContrato.getValue();
            Contrato c = service.crearContrato(reserva);
            Path archivo = pdf.generar(c, service.cuenta(reserva));
            actualizarOperaciones(true);
            abrir(archivo);
            mensaje("Contrato " + c.getCodigo() + " generado: " + archivo + "  ·  Usa «Guardar copia...» para descargarlo donde quieras.");
        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void abrirContratoSeleccionado() {
        try {
            Contrato c = tablaContratos.getSelectionModel().getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Selecciona un contrato de la tabla.");
            Path archivo = pdf.generar(c, service.cuenta(c.getReserva()));
            abrir(archivo);
            mensaje("Contrato " + c.getCodigo() + " abierto (con el estado de cuenta de hoy).");
        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void guardarCopiaContrato() {
        try {
            Contrato c = contratoElegido();
            if (c == null) throw new IllegalArgumentException("Selecciona un contrato de la tabla (o genera uno primero) para guardar su copia.");
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar copia del contrato");
            fc.setInitialDirectory(carpetaDescargas());
            fc.setInitialFileName(pdf.nombreSugerido(c));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
            File destino = fc.showSaveDialog(lblCarpetaContratos.getScene().getWindow());
            if (destino == null) return;
            if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
            }
            Path guardado = pdf.exportar(c, service.cuenta(c.getReserva()), destino.toPath());
            mensaje("Copia del contrato " + c.getCodigo() + " guardada en: " + guardado);
        } catch (Exception e) {
            error(e);
        }
    }

    private Contrato contratoElegido() {
        Contrato c = tablaContratos.getSelectionModel().getSelectedItem();
        if (c != null) return c;
        Reserva r = reservaContrato.getValue();
        if (r == null) return null;
        return service.contratos().stream().filter(x -> x.getReserva().getId().equals(r.getId())).findFirst().orElse(null);
    }

    private File carpetaDescargas() {
        for (String nombre : new String[]{"Downloads", "Descargas"}) {
            File carpeta = new File(System.getProperty("user.home"), nombre);
            if (carpeta.isDirectory()) return carpeta;
        }
        return new File(System.getProperty("user.home"));
    }

    @FXML
    private void abrirCarpetaContratos() {
        try {
            Files.createDirectories(pdf.carpeta());
            abrir(pdf.carpeta());
        } catch (Exception e) {
            error(e);
        }
    }

    private void configurarDevolucionYPagos() {
        configurarComboReserva(reservaDevolucion);
        configurarComboReserva(reservaPago);
        lblReglaRecargo.setText(String.format(Locale.US,
                "Regla: por cada día de retraso se cobra el %.0f%% de la tarifa diaria. El cálculo se actualiza al elegir la reserva o la fecha.",
                service.getPorcentajeRecargo() * 100));

        fechaDevolucion.setValue(LocalDate.now());
        reservaDevolucion.valueProperty().addListener((o, a, r) -> {
            if (actualizando) return;
            if (r != null) fechaDevolucion.setValue(LocalDate.now());
            actualizarResumenDevolucion();
        });
        fechaDevolucion.valueProperty().addListener((o, a, f) -> actualizarResumenDevolucion());
        reservaPago.valueProperty().addListener((o, a, r) -> {
            if (actualizando) return;
            actualizarSaldoPago();
        });

        metodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Yape", "Plin", "Tarjeta", "Transferencia"));
        metodoPago.setValue("Efectivo");
        conceptoPago.setItems(FXCollections.observableArrayList("Adelanto", "Saldo", "Recargo por retraso", "Garantía"));

        colPagoFecha.setCellValueFactory(c -> new SimpleStringProperty(Formato.fecha(c.getValue().getFechaPago())));
        colPagoReserva.setCellValueFactory(c -> new SimpleStringProperty(
                "#" + c.getValue().getReserva().getId() + " · " + c.getValue().getReserva().getCliente().getNombres()));
        colPagoMonto.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getMonto()));
        colPagoMonto.setCellFactory(col -> celdaMoneda());
        colPagoMetodo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMetodo()));
        colPagoConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));

        actualizarResumenDevolucion();
        actualizarSaldoPago();
    }

    private void actualizarResumenDevolucion() {
        Reserva r = reservaDevolucion.getValue();
        LocalDate real = fechaDevolucion.getValue();
        if (r == null) {
            limpiarResumenDevolucion("Selecciona una reserva para ver el cálculo del recargo.");
            return;
        }
        if (real == null) {
            limpiarResumenDevolucion("Elige la fecha real de devolución.");
            return;
        }
        try {
            AlquilerService.ResumenDevolucion rs = service.resumenDevolucion(r, real);
            lblPrevista.setText(Formato.fecha(rs.fechaPrevista()));
            lblReal.setText(Formato.fecha(rs.fechaReal()));
            lblDias.setText(rs.diasRetraso() + " día(s)");
            lblTarifaDia.setText(Formato.moneda(rs.tarifaDiaria()));
            lblRecargoDia.setText(Formato.moneda(rs.recargoPorDia()));
            lblRecargo.setText(rs.conRetraso() ? Formato.moneda(rs.recargo()) : "S/ 0.00");
            marcar(lblRecargo, "big-number", rs.conRetraso() ? "big-alert" : "big-ok");
            lblTotalAlquiler.setText(Formato.moneda(rs.totalAlquiler()));
            lblPagado.setText(Formato.moneda(rs.pagado()));
            lblSaldoDev.setText(Formato.moneda(rs.saldo()));
            if (rs.conRetraso()) {
                bannerRetraso.setText("Devolución con " + rs.diasRetraso() + " día(s) de retraso. Cargo adicional: " + Formato.moneda(rs.recargo())
                        + "  (" + rs.diasRetraso() + " × " + Formato.moneda(rs.recargoPorDia()) + ")");
                marcar(bannerRetraso, "banner-alerta");
            } else {
                bannerRetraso.setText("Devolución dentro del plazo. No se aplica ningún recargo.");
                marcar(bannerRetraso, "banner-ok");
            }
        } catch (Exception e) {
            limpiarResumenDevolucion(e.getMessage());
        }
    }

    private void limpiarResumenDevolucion(String mensaje) {
        for (Label l : new Label[]{lblPrevista, lblReal, lblDias, lblTarifaDia, lblRecargoDia, lblRecargo, lblTotalAlquiler, lblPagado, lblSaldoDev}) {
            l.setText("-");
        }
        marcar(lblRecargo, "big-number");
        bannerRetraso.setText(mensaje);
        marcar(bannerRetraso, "banner-info");
    }

    @FXML
    private void devolver() {
        try {
            Reserva r = reservaDevolucion.getValue();
            AlquilerService.ResumenDevolucion rs = service.devolver(r, fechaDevolucion.getValue());
            reservaDevolucion.setValue(null);
            actualizarOperaciones(true);
            seleccionarReservaPago(r);
            actualizarSaldoPago();
            if (rs.conRetraso()) {
                mensaje(String.format(Locale.US, "Devolución registrada con %d día(s) de retraso. Cargo adicional: %s. Saldo por cobrar: %s (regístralo en «Registrar pago»).",
                        rs.diasRetraso(), Formato.moneda(rs.recargo()), Formato.moneda(rs.saldo())));
            } else {
                mensaje("Devolución registrada sin recargos. Saldo por cobrar: " + Formato.moneda(rs.saldo()) + ".");
            }
        } catch (Exception e) {
            error(e);
        }
    }

    private void actualizarEtiquetaSaldo() {
        Reserva r = reservaPago.getValue();
        if (r == null) {
            lblSaldoPago.setText("Saldo pendiente: -");
            lblDetalleSaldo.setText("");
            return;
        }
        try {
            CuentaReserva q = service.cuenta(r);
            lblSaldoPago.setText("Saldo pendiente de la reserva #" + r.getId() + ": " + Formato.moneda(q.saldo()));
            lblDetalleSaldo.setText("Alquiler " + Formato.moneda(q.totalAlquiler())
                    + (q.recargo() > 0 ? "  +  recargo por " + q.diasRetraso() + " día(s) de retraso " + Formato.moneda(q.recargo()) : "")
                    + "  -  pagado " + Formato.moneda(q.pagado()) + "   ·   " + q.estadoPago());
        } catch (Exception e) {
            lblSaldoPago.setText("Saldo pendiente: -");
            lblDetalleSaldo.setText(e.getMessage() == null ? "" : e.getMessage());
        }
    }

    private void actualizarSaldoPago() {
        actualizarEtiquetaSaldo();
        Reserva r = reservaPago.getValue();
        if (r == null) return;
        double saldo = service.saldoPendiente(r);
        montoPago.setText(saldo > 0 ? String.format(Locale.US, "%.2f", saldo) : "");
    }

    private void seleccionarReservaPago(Reserva r) {
        if (r == null) return;
        reservaPago.getItems().stream().filter(x -> x.equals(r)).findFirst().ifPresent(reservaPago::setValue);
    }

    @FXML
    private void registrarPago() {
        try {
            double monto = Formato.numero(montoPago.getText(), "el monto");
            String concepto = conceptoPago.getEditor().getText();
            Pago pago = service.registrarPago(reservaPago.getValue(), monto, metodoPago.getValue(), concepto);
            conceptoPago.setValue(null);
            conceptoPago.getEditor().clear();
            actualizarOperaciones(true);
            actualizarSaldoPago();
            mensaje("Pago de " + Formato.moneda(monto) + " registrado. Saldos, reservas y disponibilidad ya están actualizados.");
            notificar(notificaciones.pagoRegistrado(pago));
        } catch (Exception e) {
            error(e);
        }
    }

    private void configurarCuentas() {
        colCuNro.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().reserva().getId()));
        colCuCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reserva().getCliente().getNombres()));
        colCuVehiculo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().reserva().getVehiculo().getPlaca() + " · " + c.getValue().reserva().getVehiculo().nombreCompleto()));
        colCuAlquiler.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().totalAlquiler()));
        colCuAlquiler.setCellFactory(col -> celdaMoneda());
        colCuRetraso.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().conRetraso() ? c.getValue().diasRetraso() + " día(s)" : "-"));
        colCuRecargo.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().recargo()));
        colCuRecargo.setCellFactory(col -> celdaMoneda());
        colCuPagado.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().pagado()));
        colCuPagado.setCellFactory(col -> celdaMoneda());
        colCuSaldo.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().saldo()));
        colCuSaldo.setCellFactory(col -> celdaMoneda());
        colCuEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estadoPago()));
        colCuEstado.setCellFactory(col -> celdaInsignia());

        tablaCuentas.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> {
            if (actualizando || c == null) return;
            seleccionarReservaPago(c.reserva());
        });
        chkSoloDeuda.selectedProperty().addListener((o, a, b) -> aplicarFiltroCuentas());
    }

    private void aplicarFiltroCuentas() {
        CuentaReserva sel = tablaCuentas.getSelectionModel().getSelectedItem();
        Long idSel = sel == null ? null : sel.reserva().getId();
        List<CuentaReserva> visibles = cuentasActuales.stream()
                .filter(c -> !AlquilerService.ANULADA.equals(c.estadoPago()))
                .filter(c -> !chkSoloDeuda.isSelected() || c.saldo() > 0.004)
                .toList();
        tablaCuentas.setItems(FXCollections.observableArrayList(visibles));
        if (idSel != null) {
            visibles.stream().filter(c -> idSel.equals(c.reserva().getId())).findFirst()
                    .ifPresent(c -> tablaCuentas.getSelectionModel().select(c));
        }
        double porCobrar = cuentasActuales.stream().mapToDouble(CuentaReserva::saldo).sum();
        double recargos = cuentasActuales.stream().filter(c -> c.saldo() > 0.004).mapToDouble(CuentaReserva::recargo).sum();
        long mora = cuentasActuales.stream().filter(c -> AlquilerService.EN_MORA.equals(c.estadoPago())).count();
        lblPorCobrar.setText("Total por cobrar: " + Formato.moneda(porCobrar)
                + "   ·   Recargos por retraso acumulados: " + Formato.moneda(recargos)
                + "   ·   Reservas en mora: " + mora);
    }

    private <S> TableCell<S, String> celdaInsignia() {
        return new TableCell<S, String>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                setText(null);
                if (vacio || texto == null || texto.isBlank()) {
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label(texto);
                insignia.getStyleClass().addAll("badge", "badge-" + texto.toLowerCase(Locale.ROOT).replace(' ', '-'));
                setGraphic(insignia);
            }
        };
    }

    private void refrescar() {
        List<Vehiculo> vs = service.vehiculos();
        List<Cliente> cs = service.clientes();

        tablaVehiculos.getSelectionModel().clearSelection();
        tablaClientes.getSelectionModel().clearSelection();
        vehiculosBase.setAll(vs);
        clientesBase.setAll(cs);
        asignar(vehiculoReserva, vs.stream().filter(Vehiculo::isActivo).toList());
        asignar(clienteReserva, cs);

        actualizarOperaciones(true);
    }

    private void actualizarOperaciones(boolean forzar) {
        try {
            List<Reserva> rs = service.reservas();
            List<Contrato> cts = service.contratos();
            List<Pago> ps = service.pagos();
            List<CuentaReserva> cuentas = service.cuentas();

            StringBuilder firma = new StringBuilder(LocalDate.now().toString());
            for (Reserva r : rs) {
                firma.append('|').append(r.getId()).append(':').append(r.getEstado()).append(':').append(r.getFechaFin())
                        .append(':').append(r.getFechaDevolucionReal()).append(':').append(r.getRecargo());
            }
            for (Pago p : ps) firma.append('|').append(p.getId()).append(':').append(p.getMonto());
            firma.append('|').append(cts.size());
            if (!forzar && firma.toString().equals(ultimaFirma)) return;
            ultimaFirma = firma.toString();

            actualizando = true;
            try {
                Reserva reservaSel = tablaReservas.getSelectionModel().getSelectedItem();
                Long idReservaSel = reservaSel == null ? null : reservaSel.getId();
                Contrato contratoSel = tablaContratos.getSelectionModel().getSelectedItem();
                Long idContratoSel = contratoSel == null ? null : contratoSel.getId();

                cuentasActuales = cuentas;
                cuentasPorReserva = cuentas.stream().collect(Collectors.toMap(c -> c.reserva().getId(), c -> c));

                tablaReservas.setItems(FXCollections.observableArrayList(rs));
                if (idReservaSel != null) {
                    rs.stream().filter(r -> idReservaSel.equals(r.getId())).findFirst()
                            .ifPresent(r -> tablaReservas.getSelectionModel().select(r));
                }

                List<Reserva> noCanceladas = rs.stream().filter(r -> !AlquilerService.CANCELADA.equals(r.getEstado())).toList();
                asignar(reservaContrato, noCanceladas);
                asignar(reservaPago, noCanceladas);
                asignar(reservaDevolucion, rs.stream().filter(Reserva::estaAbierta).toList());

                tablaContratos.setItems(FXCollections.observableArrayList(cts));
                if (idContratoSel != null) {
                    cts.stream().filter(c -> idContratoSel.equals(c.getId())).findFirst()
                            .ifPresent(c -> tablaContratos.getSelectionModel().select(c));
                }
                tablaPagos.setItems(FXCollections.observableArrayList(ps));
                aplicarFiltroCuentas();

                actualizarKpis();
                actualizarDisponibles(true, false);
                actualizarResumenDevolucion();
                actualizarEtiquetaSaldo();
                mostrarResumenContrato(reservaContrato.getValue());
            } finally {
                actualizando = false;
            }
        } catch (Exception e) {
            if (forzar) error(e);
        }
    }

    private <T> void asignar(ComboBox<T> combo, List<T> items) {
        T previo = combo.getValue();
        combo.setItems(FXCollections.observableArrayList(items));
        if (previo != null) {
            int pos = items.indexOf(previo);
            combo.setValue(pos >= 0 ? items.get(pos) : null);
        }
    }

    private void actualizarKpis() {
        AlquilerService.ResumenPanel p = service.panel();
        kpiFlota.setText(String.valueOf(p.flota()));
        kpiDisponibles.setText(String.valueOf(p.disponiblesHoy()));
        kpiActivas.setText(String.valueOf(p.reservasActivas()));
        kpiPorCobrar.setText(Formato.moneda(p.porCobrar()));
        long alertas = mantenimiento.contarAlertas();
        kpiAlertas.setText(String.valueOf(alertas));
        kpiAlertas.getStyleClass().remove("kpi-alert");
        if (alertas > 0) kpiAlertas.getStyleClass().add("kpi-alert");
        kpiIngresos.setText(Formato.moneda(p.ingresos()));
        boolean hayMora = cuentasActuales.stream().anyMatch(c -> AlquilerService.EN_MORA.equals(c.estadoPago()));
        kpiPorCobrar.getStyleClass().remove("kpi-alert");
        if (hayMora) kpiPorCobrar.getStyleClass().add("kpi-alert");
    }

    private void configurarComboVehiculo(ComboBox<Vehiculo> combo) {
        combo.setCellFactory(lv -> new ListCell<Vehiculo>() {
            @Override
            protected void updateItem(Vehiculo v, boolean vacio) {
                super.updateItem(v, vacio);
                if (vacio || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(v + " · " + Formato.moneda(v.getTarifaDiaria()) + "/día");
                    setGraphic(imagenes.miniatura(v.getImagenPath(), 54, 36));
                }
            }
        });
        combo.setButtonCell(new ListCell<Vehiculo>() {
            @Override
            protected void updateItem(Vehiculo v, boolean vacio) {
                super.updateItem(v, vacio);
                setText(vacio || v == null ? null : v.toString());
                setGraphic(null);
            }
        });
    }

    private void configurarComboReserva(ComboBox<Reserva> combo) {
        combo.setCellFactory(lv -> new ListCell<Reserva>() {
            @Override
            protected void updateItem(Reserva r, boolean vacio) {
                super.updateItem(r, vacio);
                aplicarTextoReserva(this, r, vacio);
            }
        });
        combo.setButtonCell(new ListCell<Reserva>() {
            @Override
            protected void updateItem(Reserva r, boolean vacio) {
                super.updateItem(r, vacio);
                aplicarTextoReserva(this, r, vacio);
            }
        });
    }

    private void aplicarTextoReserva(ListCell<Reserva> celda, Reserva r, boolean vacio) {
        if (vacio || r == null) {
            celda.setText(null);
            celda.setStyle("");
            return;
        }
        boolean retraso = r.estaVencida(LocalDate.now());
        celda.setText(r + (retraso ? "  ·  CON RETRASO" : ""));
        celda.setStyle(retraso ? "-fx-text-fill: #b3261e; -fx-font-weight: bold;" : "");
    }

    private <S> TableCell<S, Number> celdaMoneda() {
        return new TableCell<S, Number>() {
            @Override
            protected void updateItem(Number valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Formato.moneda(valor.doubleValue()));
            }
        };
    }

    private void marcar(Label etiqueta, String... clases) {
        etiqueta.getStyleClass().removeIf(c -> c.startsWith("big-") || c.startsWith("banner-"));
        etiqueta.getStyleClass().addAll(clases);
    }

    private void abrir(Path ruta) {
        try {
            HostServices servicios = JavaFxApplication.hostServices();
            if (servicios != null) {
                servicios.showDocument(ruta.toUri().toString());
                return;
            }
        } catch (Exception ignorada) {
        }
        Thread hilo = new Thread(() -> {
            try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    throw new IllegalStateException("El sistema no permite abrir archivos desde la aplicación.");
                }
                Desktop.getDesktop().open(ruta.toFile());
            } catch (Exception e) {
                Platform.runLater(() -> mensaje("No se pudo abrir automáticamente. Búscalo en: " + ruta));
            }
        }, "abrir-archivo");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mensaje(String texto) {
        estado.setText(texto);
    }

    private boolean confirmar(String texto) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.OK, ButtonType.CANCEL);
        alerta.setHeaderText("Confirmar acción");
        estilizar(alerta);
        return alerta.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void error(Exception e) {
        String texto = e.getMessage() == null ? "Revisa los datos ingresados." : e.getMessage();
        Alert alerta = new Alert(Alert.AlertType.WARNING, texto, ButtonType.OK);
        alerta.setHeaderText("No se pudo completar la operación");
        estilizar(alerta);
        alerta.showAndWait();
    }

    private void estilizar(Alert alerta) {
        java.net.URL css = getClass().getResource("/css/app.css");
        if (css != null) alerta.getDialogPane().getStylesheets().add(css.toExternalForm());
    }

    @FXML
    private void irAPestana(javafx.event.ActionEvent e) {
        int i = grupoMenu.getToggles().indexOf((Toggle) e.getSource());
        if (i >= 0) tabs.getSelectionModel().select(i);
        sincronizarMenu();
    }

    private void sincronizarMenu() {
        int i = tabs.getSelectionModel().getSelectedIndex();
        if (i >= 0 && i < grupoMenu.getToggles().size()) grupoMenu.selectToggle(grupoMenu.getToggles().get(i));
        Tab t = tabs.getSelectionModel().getSelectedItem();
        if (t != null) lblPagina.setText(t.getText());
        if (t != null) {
            switch (t.getText()) {
                case "Notificaciones" -> notificacionesCtl.refrescar();
                case "Reportes" -> reportesCtl.refrescar();
                case "Mantenimiento" -> mantenimientoCtl.refrescar();
                case "Respaldos" -> respaldoCtl.refrescar();
                default -> { }
            }
        }
    }

    private void notificar(CompletableFuture<NotificacionService.Resultado> envio) {
        envio.whenComplete((r, ex) -> Platform.runLater(() -> {
            notificacionesCtl.refrescar();
            if (ex != null) mensaje(estado.getText() + "  ·  Correo: no se pudo enviar el aviso.");
            else if (r != null) mensaje(estado.getText() + "  ·  Correo: " + r.mensaje());
        }));
    }
}
