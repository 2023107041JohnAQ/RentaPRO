package com.ejemplo.alquiler.controller;

import com.ejemplo.alquiler.model.AjusteCorreo;
import com.ejemplo.alquiler.model.Notificacion;
import com.ejemplo.alquiler.service.NotificacionService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Component
public class NotificacionesController {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionService servicio;

    public NotificacionesController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    @FXML private CheckBox chkActivo, chkReserva, chkPago, chkRecordatorio, chkMora;
    @FXML private TextField txtHost, txtPuerto, txtUsuario, txtRemitenteNombre, txtRemitenteCorreo, txtEmpresa, txtTelefono, txtPrueba;
    @FXML private PasswordField txtClave;
    @FXML private ComboBox<String> cbSeguridad;
    @FXML private Label lblMensaje, lblResumen;
    @FXML private Button btnProbar, btnAvisosAhora;
    @FXML private TableView<Notificacion> tabla;
    @FXML private TableColumn<Notificacion, String> colFecha, colCliente, colCorreo, colTipo, colEstado, colDetalle;

    @FXML
    public void initialize() {
        cbSeguridad.setItems(FXCollections.observableArrayList("STARTTLS", "SSL", "NINGUNA"));

        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHora() == null ? "" : FECHA_HORA.format(c.getValue().getFechaHora())));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReserva() == null
                ? "—"
                : c.getValue().getReserva().getCliente().getNombres() + " · #" + c.getValue().getReserva().getId()));
        colCorreo.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getDestinatario())));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getTipo())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(nz(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                getStyleClass().removeAll("badge", "badge-enviado", "badge-error", "badge-omitido");
                if (vacio || texto == null || texto.isBlank()) {
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
        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(
                nz(c.getValue().getDetalle()).isBlank() ? nz(c.getValue().getAsunto()) : c.getValue().getDetalle()));

        cargarAjustes();
        refrescar();

        PauseTransition inicio = new PauseTransition(Duration.seconds(8));
        inicio.setOnFinished(e -> revisarAvisos(false));
        inicio.play();
        Timeline reloj = new Timeline(new KeyFrame(Duration.minutes(30), e -> revisarAvisos(false)));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    public void refrescar() {
        if (tabla == null) return;
        List<Notificacion> lista = servicio.historial();
        tabla.setItems(FXCollections.observableArrayList(lista));
        long enviados = lista.stream().filter(n -> NotificacionService.ENVIADO.equals(n.getEstado())).count();
        long errores = lista.stream().filter(n -> NotificacionService.ERROR.equals(n.getEstado())).count();
        lblResumen.setText(enviados + " enviado(s)" + (errores > 0 ? " · " + errores + " con error" : ""));
    }

    @FXML
    private void actualizarHistorial() {
        refrescar();
    }

    @FXML
    private void enviarAvisosAhora() {
        revisarAvisos(true);
    }

    private void revisarAvisos(boolean manual) {
        if (manual) {
            btnAvisosAhora.setDisable(true);
            mostrar(true, "Revisando reservas y enviando los avisos pendientes...");
        }
        servicio.procesarAvisosAutomaticos().whenComplete((n, ex) -> Platform.runLater(() -> {
            btnAvisosAhora.setDisable(false);
            if (ex != null) {
                if (manual) mostrar(false, "No se pudieron revisar los avisos: " + ex.getMessage());
                return;
            }
            refrescar();
            if (n == null) return;
            if (n < 0) {
                if (manual) mostrar(false, "Los avisos están desactivados o falta completar la configuración del correo.");
            } else if (n == 0) {
                if (manual) mostrar(true, "No hay avisos pendientes por enviar.");
            } else {
                mostrar(true, n + " aviso(s) automático(s) enviado(s) a clientes.");
            }
        }));
    }

    private void cargarAjustes() {
        AjusteCorreo a = servicio.ajustes();
        chkActivo.setSelected(a.isActivo());
        chkReserva.setSelected(a.isAvisoReserva());
        chkPago.setSelected(a.isAvisoPago());
        chkRecordatorio.setSelected(a.isAvisoRecordatorio());
        chkMora.setSelected(a.isAvisoMora());
        txtHost.setText(nz(a.getHost()));
        txtPuerto.setText(String.valueOf(a.getPuerto()));
        cbSeguridad.setValue(a.getSeguridad() == null ? "STARTTLS" : a.getSeguridad());
        txtUsuario.setText(nz(a.getUsuario()));
        txtClave.clear();
        txtClave.setPromptText(nz(a.getClave()).isEmpty()
                ? "Contraseña o clave de aplicación"
                : "•••••••• (guardada; déjala vacía para conservarla)");
        txtRemitenteNombre.setText(nz(a.getRemitenteNombre()));
        txtRemitenteCorreo.setText(nz(a.getRemitenteCorreo()));
        txtEmpresa.setText(nz(a.getEmpresa()));
        txtTelefono.setText(nz(a.getTelefonoContacto()));
    }

    private AjusteCorreo leerFormulario() {
        AjusteCorreo a = new AjusteCorreo();
        a.setActivo(chkActivo.isSelected());
        a.setAvisoReserva(chkReserva.isSelected());
        a.setAvisoPago(chkPago.isSelected());
        a.setAvisoRecordatorio(chkRecordatorio.isSelected());
        a.setAvisoMora(chkMora.isSelected());
        a.setHost(txtHost.getText());
        try {
            a.setPuerto(Integer.parseInt(txtPuerto.getText().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El puerto debe ser un número (por ejemplo 587).");
        }
        a.setSeguridad(cbSeguridad.getValue() == null ? "STARTTLS" : cbSeguridad.getValue());
        a.setUsuario(txtUsuario.getText());
        a.setClave(txtClave.getText());
        a.setRemitenteNombre(txtRemitenteNombre.getText());
        a.setRemitenteCorreo(txtRemitenteCorreo.getText());
        a.setEmpresa(txtEmpresa.getText());
        a.setTelefonoContacto(txtTelefono.getText());
        return a;
    }

    @FXML
    private void guardar() {
        try {
            servicio.guardarAjustes(leerFormulario());
            cargarAjustes();
            mostrar(true, chkActivo.isSelected()
                    ? "Configuración guardada. Los avisos a clientes están activados."
                    : "Configuración guardada. Los avisos a clientes están desactivados.");
        } catch (Exception e) {
            mostrar(false, e.getMessage() == null ? "No se pudo guardar la configuración." : e.getMessage());
        }
    }

    @FXML
    private void probar() {
        try {
            AjusteCorreo a = leerFormulario();
            btnProbar.setDisable(true);
            mostrar(true, "Enviando correo de prueba...");
            servicio.enviarPrueba(a, txtPrueba.getText()).whenComplete((r, ex) -> Platform.runLater(() -> {
                btnProbar.setDisable(false);
                if (ex != null) mostrar(false, "No se pudo enviar el correo de prueba: " + ex.getMessage());
                else mostrar(r.exito(), r.mensaje());
                refrescar();
            }));
        } catch (Exception e) {
            btnProbar.setDisable(false);
            mostrar(false, e.getMessage() == null ? "Revisa los datos del formulario." : e.getMessage());
        }
    }

    private void mostrar(boolean ok, String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        lblMensaje.getStyleClass().removeIf(c -> c.startsWith("banner-"));
        lblMensaje.getStyleClass().add(ok ? "banner-ok" : "banner-alerta");
    }
}
