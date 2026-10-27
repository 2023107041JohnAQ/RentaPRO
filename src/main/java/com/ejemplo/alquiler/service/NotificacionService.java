package com.ejemplo.alquiler.service;

import com.ejemplo.alquiler.model.AjusteCorreo;
import com.ejemplo.alquiler.model.Cliente;
import com.ejemplo.alquiler.model.Notificacion;
import com.ejemplo.alquiler.model.Pago;
import com.ejemplo.alquiler.model.Reserva;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.repository.AjusteCorreoRepository;
import com.ejemplo.alquiler.repository.NotificacionRepository;
import com.ejemplo.alquiler.service.AlquilerService.CuentaReserva;
import com.ejemplo.alquiler.util.Formato;
import com.ejemplo.alquiler.util.PlantillaCorreo;
import com.ejemplo.alquiler.util.PlantillaCorreo.Fila;
import com.ejemplo.alquiler.util.Recargos;
import jakarta.mail.internet.MimeMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Service
public class NotificacionService {

    public static final String ENVIADO = "ENVIADO";
    public static final String ERROR = "ERROR";
    public static final String OMITIDO = "OMITIDO";

    public static final long DIAS_ENTRE_AVISOS = 3;

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public record Resultado(String estado, String mensaje) {
        public boolean exito() {
            return ENVIADO.equals(estado);
        }
    }

    private record Contenido(String asunto, String html, String texto) {
    }

    private final AjusteCorreoRepository ajustesRepo;
    private final NotificacionRepository historialRepo;
    private final AlquilerService alquiler;

    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "envio-correos");
        t.setDaemon(true);
        return t;
    });

    public NotificacionService(AjusteCorreoRepository ajustesRepo, NotificacionRepository historialRepo, AlquilerService alquiler) {
        this.ajustesRepo = ajustesRepo;
        this.historialRepo = historialRepo;
        this.alquiler = alquiler;
    }

    public AjusteCorreo ajustes() {
        return ajustesRepo.findById(1L).orElseGet(AjusteCorreo::new);
    }

    public AjusteCorreo guardarAjustes(AjusteCorreo nuevo) {
        if (nuevo.getClave() == null || nuevo.getClave().isEmpty()) nuevo.setClave(ajustes().getClave());
        nuevo.setId(1L);
        if (nuevo.getEmpresa() == null || nuevo.getEmpresa().isBlank()) nuevo.setEmpresa("RentaPro");
        if (nuevo.getPuerto() < 1 || nuevo.getPuerto() > 65535)
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535 (por ejemplo 587).");
        if (nuevo.isActivo()) validarConexion(nuevo);
        return ajustesRepo.save(nuevo);
    }

    private void validarConexion(AjusteCorreo a) {
        if (esVacio(a.getHost())) throw new IllegalArgumentException("Indica el servidor de correo (por ejemplo smtp.gmail.com).");
        if (esVacio(a.getUsuario())) throw new IllegalArgumentException("Indica el usuario (tu correo) con el que se enviarán los avisos.");
        if (esVacio(a.getClave())) throw new IllegalArgumentException("Indica la contraseña del correo (en Gmail, una contraseña de aplicación).");
        if (a.getPuerto() < 1 || a.getPuerto() > 65535) throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535.");
        if (!CORREO.matcher(direccionRemitente(a)).matches())
            throw new IllegalArgumentException("El correo de envío no es válido. Indica un «Correo remitente» con formato nombre@dominio.com.");
    }

    private boolean configurado(AjusteCorreo a) {
        return !esVacio(a.getHost()) && !esVacio(a.getUsuario()) && !esVacio(a.getClave());
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    private static String direccionRemitente(AjusteCorreo a) {
        return esVacio(a.getRemitenteCorreo()) ? a.getUsuario().trim() : a.getRemitenteCorreo().trim();
    }

    public List<Notificacion> historial() {
        return historialRepo.findAll(PageRequest.of(0, 300, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }

    private void registrar(Reserva r, String tipo, String clave, String destino, String asunto, String estado, String detalle) {
        Notificacion n = new Notificacion();
        n.setReserva(r);
        n.setTipo(tipo);
        n.setClave(clave);
        n.setDestinatario(destino);
        n.setAsunto(asunto);
        n.setFechaHora(LocalDateTime.now());
        n.setEstado(estado);
        n.setDetalle(detalle == null ? "" : (detalle.length() > 480 ? detalle.substring(0, 480) : detalle));
        historialRepo.save(n);
    }

    public CompletableFuture<Resultado> reservaCreada(Reserva r) {
        return encolar(r, "Reserva creada", "RESERVA-CREADA", AjusteCorreo::isAvisoReserva, a -> {
            Vehiculo v = r.getVehiculo();
            List<Fila> filas = new ArrayList<>();
            filas.add(Fila.de("Reserva", "#" + r.getId()));
            filas.add(Fila.de("Vehículo", v.nombreCompleto() + " (" + v.getPlaca() + ")"));
            filas.add(Fila.de("Desde", Formato.fechaLarga(r.getFechaInicio())));
            filas.add(Fila.de("Hasta", Formato.fechaLarga(r.getFechaFin())));
            filas.add(Fila.de("Días", String.valueOf(r.dias())));
            filas.add(Fila.de("Tarifa diaria", Formato.moneda(v.getTarifaDiaria())));
            filas.add(Fila.resaltada("Total estimado", Formato.moneda(r.getTotalEstimado())));
            return armar(a, r, "Reserva #" + r.getId() + " confirmada", "Tu reserva fue registrada",
                    "Tu reserva quedó registrada con los siguientes datos.", filas,
                    "Recuerda devolver el vehículo en la fecha indicada para evitar recargos por retraso.");
        });
    }

    public CompletableFuture<Resultado> reservaCancelada(Reserva r, double reembolso) {
        return encolar(r, "Reserva cancelada", "RESERVA-CANCELADA", AjusteCorreo::isAvisoReserva, a -> {
            Vehiculo v = r.getVehiculo();
            List<Fila> filas = new ArrayList<>();
            filas.add(Fila.de("Reserva", "#" + r.getId()));
            filas.add(Fila.de("Vehículo", v.nombreCompleto() + " (" + v.getPlaca() + ")"));
            filas.add(Fila.de("Periodo", Formato.fecha(r.getFechaInicio()) + " al " + Formato.fecha(r.getFechaFin())));
            if (reembolso > 0.004) filas.add(Fila.resaltada("Reembolso registrado", Formato.moneda(reembolso)));
            return armar(a, r, "Reserva #" + r.getId() + " cancelada", "Tu reserva fue cancelada",
                    "Te confirmamos que la siguiente reserva fue cancelada.", filas,
                    reembolso > 0.004 ? "Se registró un reembolso por el monto que ya habías pagado." : "");
        });
    }

    public CompletableFuture<Resultado> pagoRegistrado(Pago p) {
        Reserva r = p.getReserva();
        return encolar(r, "Pago registrado", "PAGO-" + p.getId(), AjusteCorreo::isAvisoPago, a -> {
            CuentaReserva cuenta = alquiler.cuenta(r);
            List<Fila> filas = new ArrayList<>();
            filas.add(Fila.de("Reserva", "#" + r.getId()));
            filas.add(Fila.de("Fecha de pago", Formato.fecha(p.getFechaPago())));
            filas.add(Fila.de("Método", p.getMetodo()));
            filas.add(Fila.de("Concepto", p.getConcepto()));
            filas.add(Fila.resaltada("Monto recibido", Formato.moneda(p.getMonto())));
            filas.add(cuenta.saldo() > 0.004
                    ? Fila.de("Saldo pendiente", Formato.moneda(cuenta.saldo()))
                    : Fila.de("Saldo pendiente", "Sin saldo: cuenta al día"));
            return armar(a, r, "Pago recibido · Reserva #" + r.getId(), "Recibimos tu pago",
                    "Registramos tu pago correctamente. ¡Gracias!", filas, "");
        });
    }

    public CompletableFuture<Integer> procesarAvisosAutomaticos() {
        return CompletableFuture.supplyAsync(this::escanear, hilo);
    }

    private int escanear() {
        AjusteCorreo a = ajustes();
        if (!a.isActivo() || !configurado(a)) return -1;
        LocalDate hoy = LocalDate.now();
        int enviados = 0;
        for (CuentaReserva c : alquiler.cuentas()) {
            Reserva r = c.reserva();
            String tipo = null;
            String clave = null;
            boolean repetible = false;
            Contenido contenido = null;
            if (r.estaAbierta()) {
                long dias = ChronoUnit.DAYS.between(hoy, r.getFechaFin());
                if (c.diasRetraso() > 0) {
                    if (a.isAvisoMora()) {
                        tipo = "Retraso en la devolución";
                        clave = "MORA";
                        repetible = true;
                        contenido = contenidoMora(a, c);
                    }
                } else if ((dias == 0 || dias == 1) && a.isAvisoRecordatorio()) {
                    tipo = "Recordatorio de devolución";
                    clave = dias == 0 ? "RECORDATORIO-HOY" : "RECORDATORIO-MANANA";
                    contenido = contenidoRecordatorio(a, c, dias);
                }
            } else if (AlquilerService.DEVUELTA.equals(r.getEstado()) && c.saldo() > 0.004 && a.isAvisoMora()) {
                tipo = "Saldo pendiente";
                clave = "SALDO";
                repetible = true;
                contenido = contenidoSaldo(a, c);
            }
            if (clave == null || yaAvisado(r.getId(), clave, repetible)) continue;
            Resultado res = enviar(r, tipo, clave, contenido, a);
            if (res.exito()) enviados++;
            if (ERROR.equals(res.estado())) break;
        }
        return enviados;
    }

    private boolean yaAvisado(Long reservaId, String clave, boolean repetible) {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_ENTRE_AVISOS);
        return historialRepo.findByReservaId(reservaId).stream()
                .filter(n -> clave.equals(n.getClave()) && !ERROR.equals(n.getEstado()))
                .anyMatch(n -> !repetible || (n.getFechaHora() != null && n.getFechaHora().isAfter(limite)));
    }

    private Contenido contenidoRecordatorio(AjusteCorreo a, CuentaReserva c, long dias) {
        Reserva r = c.reserva();
        Vehiculo v = r.getVehiculo();
        double porDia = Recargos.recargo(1, v.getTarifaDiaria(), alquiler.getPorcentajeRecargo());
        List<Fila> filas = new ArrayList<>();
        filas.add(Fila.de("Reserva", "#" + r.getId()));
        filas.add(Fila.de("Vehículo", v.nombreCompleto() + " (" + v.getPlaca() + ")"));
        filas.add(Fila.resaltada("Fecha de devolución", Formato.fechaLarga(r.getFechaFin())));
        filas.add(Fila.de("Recargo por cada día de retraso", Formato.moneda(porDia)));
        if (c.saldo() > 0.004) filas.add(Fila.resaltada("Saldo pendiente", Formato.moneda(c.saldo())));
        String titulo = dias == 0 ? "Tu alquiler vence hoy" : "Tu alquiler vence mañana";
        return armar(a, r, (dias == 0 ? "Hoy vence" : "Mañana vence") + " tu alquiler · Reserva #" + r.getId(), titulo,
                dias == 0 ? "Hoy es la fecha de devolución del vehículo." : "Mañana es la fecha de devolución del vehículo.",
                filas, "Si necesitas más tiempo, comunícate con nosotros antes de la fecha de devolución.");
    }

    private Contenido contenidoMora(AjusteCorreo a, CuentaReserva c) {
        Reserva r = c.reserva();
        Vehiculo v = r.getVehiculo();
        long pct = Math.round(alquiler.getPorcentajeRecargo() * 100);
        List<Fila> filas = new ArrayList<>();
        filas.add(Fila.de("Reserva", "#" + r.getId()));
        filas.add(Fila.de("Vehículo", v.nombreCompleto() + " (" + v.getPlaca() + ")"));
        filas.add(Fila.de("Fecha de devolución pactada", Formato.fecha(r.getFechaFin())));
        filas.add(Fila.de("Días de retraso", String.valueOf(c.diasRetraso())));
        filas.add(Fila.de("Alquiler", Formato.moneda(c.totalAlquiler())));
        filas.add(Fila.resaltada("Recargo acumulado", Formato.moneda(c.recargo())));
        filas.add(Fila.de("Pagado", Formato.moneda(c.pagado())));
        filas.add(Fila.resaltada("Saldo por pagar", Formato.moneda(c.saldo())));
        return armar(a, r, "Aviso: tu alquiler tiene " + c.diasRetraso() + " día(s) de retraso · Reserva #" + r.getId(),
                "Tu alquiler está vencido",
                "El vehículo aún no figura como devuelto. Por cada día de retraso se cobra un recargo del " + pct + " % de la tarifa diaria.",
                filas, "Por favor devuelve el vehículo lo antes posible o comunícate con nosotros.");
    }

    private Contenido contenidoSaldo(AjusteCorreo a, CuentaReserva c) {
        Reserva r = c.reserva();
        Vehiculo v = r.getVehiculo();
        List<Fila> filas = new ArrayList<>();
        filas.add(Fila.de("Reserva", "#" + r.getId()));
        filas.add(Fila.de("Vehículo", v.nombreCompleto() + " (" + v.getPlaca() + ")"));
        filas.add(Fila.de("Alquiler", Formato.moneda(c.totalAlquiler())));
        if (c.recargo() > 0.004) filas.add(Fila.de("Recargo por retraso", Formato.moneda(c.recargo())));
        filas.add(Fila.de("Pagado", Formato.moneda(c.pagado())));
        filas.add(Fila.resaltada("Saldo pendiente", Formato.moneda(c.saldo())));
        return armar(a, r, "Saldo pendiente · Reserva #" + r.getId(), "Tienes un saldo pendiente",
                "Tu alquiler ya fue devuelto, pero todavía queda un saldo por pagar.", filas,
                "Puedes pagarlo en efectivo, Yape, Plin, tarjeta o transferencia. Gracias.");
    }

    private Contenido armar(AjusteCorreo a, Reserva r, String asunto, String titulo, String intro, List<Fila> filas, String cierre) {
        String empresa = esVacio(a.getEmpresa()) ? "RentaPro" : a.getEmpresa().trim();
        String nombre = r.getCliente().getNombres();
        return new Contenido(asunto + " · " + empresa,
                PlantillaCorreo.html(empresa, titulo, nombre, intro, filas, cierre, a.getTelefonoContacto()),
                PlantillaCorreo.texto(empresa, titulo, nombre, intro, filas, cierre, a.getTelefonoContacto()));
    }

    public CompletableFuture<Resultado> enviarPrueba(AjusteCorreo form, String destino) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (esVacio(form.getClave())) form.setClave(ajustes().getClave());
                validarConexion(form);
                String para = destino == null ? "" : destino.trim();
                if (!CORREO.matcher(para).matches())
                    throw new IllegalArgumentException("Escribe un correo válido para recibir la prueba.");
                String empresa = esVacio(form.getEmpresa()) ? "RentaPro" : form.getEmpresa().trim();
                List<Fila> filas = List.of(Fila.de("Servidor", form.getHost().trim() + ":" + form.getPuerto()),
                        Fila.resaltada("Estado", "Configuración correcta"));
                Contenido c = new Contenido("Correo de prueba · " + empresa,
                        PlantillaCorreo.html(empresa, "Correo de prueba", "equipo",
                                "Si recibes este mensaje, los avisos a tus clientes se enviarán correctamente.", filas, "", form.getTelefonoContacto()),
                        PlantillaCorreo.texto(empresa, "Correo de prueba", "equipo",
                                "Si recibes este mensaje, los avisos a tus clientes se enviarán correctamente.", filas, "", form.getTelefonoContacto()));
                try {
                    enviarCorreo(form, para, c);
                    registrar(null, "Correo de prueba", "PRUEBA", para, c.asunto(), ENVIADO, "");
                    return new Resultado(ENVIADO, "Correo de prueba enviado a " + para + ". Revisa tu bandeja de entrada (y la carpeta de spam).");
                } catch (Exception e) {
                    String motivo = motivo(e);
                    registrar(null, "Correo de prueba", "PRUEBA", para, c.asunto(), ERROR, motivo);
                    return new Resultado(ERROR, "No se pudo enviar el correo de prueba: " + motivo);
                }
            } catch (IllegalArgumentException e) {
                return new Resultado(ERROR, e.getMessage());
            }
        }, hilo);
    }

    private CompletableFuture<Resultado> encolar(Reserva r, String tipo, String clave, Predicate<AjusteCorreo> avisoActivo,
                                                 Function<AjusteCorreo, Contenido> armador) {
        return CompletableFuture.supplyAsync(() -> {
            AjusteCorreo a = ajustes();
            if (!a.isActivo() || !avisoActivo.test(a) || !configurado(a)) return null;
            return enviar(r, tipo, clave, armador.apply(a), a);
        }, hilo);
    }

    private Resultado enviar(Reserva r, String tipo, String clave, Contenido contenido, AjusteCorreo a) {
        Cliente cliente = r.getCliente();
        String correo = cliente.getCorreo() == null ? "" : cliente.getCorreo().trim();
        if (!CORREO.matcher(correo).matches()) {
            registrar(r, tipo, clave, correo, contenido.asunto(), OMITIDO, "El cliente no tiene un correo válido registrado.");
            return new Resultado(OMITIDO, "No se envió el aviso: " + cliente.getNombres() + " no tiene un correo válido registrado.");
        }
        try {
            enviarCorreo(a, correo, contenido);
            registrar(r, tipo, clave, correo, contenido.asunto(), ENVIADO, "");
            return new Resultado(ENVIADO, "Aviso enviado a " + correo + ".");
        } catch (Exception e) {
            String motivo = motivo(e);
            registrar(r, tipo, clave, correo, contenido.asunto(), ERROR, motivo);
            return new Resultado(ERROR, "No se pudo enviar el aviso a " + correo + ": " + motivo);
        }
    }

    private void enviarCorreo(AjusteCorreo a, String destino, Contenido c) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(a.getHost().trim());
        sender.setPort(a.getPuerto());
        sender.setUsername(a.getUsuario().trim());
        sender.setPassword(a.getClave());
        sender.setDefaultEncoding("UTF-8");
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "15000");
        p.put("mail.smtp.writetimeout", "15000");
        String seguridad = a.getSeguridad() == null ? "STARTTLS" : a.getSeguridad();
        if ("SSL".equalsIgnoreCase(seguridad)) {
            p.put("mail.smtp.ssl.enable", "true");
        } else if ("STARTTLS".equalsIgnoreCase(seguridad)) {
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
        }
        MimeMessage mensaje = sender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(mensaje, true, "UTF-8");
        String nombre = esVacio(a.getRemitenteNombre()) ? (esVacio(a.getEmpresa()) ? "RentaPro" : a.getEmpresa().trim()) : a.getRemitenteNombre().trim();
        h.setFrom(direccionRemitente(a), nombre);
        h.setTo(destino);
        h.setSubject(c.asunto());
        h.setText(c.texto(), c.html());
        sender.send(mensaje);
    }

    private static String motivo(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        String todo = (e.toString() + " " + m).toLowerCase();
        if (todo.contains("authentication") || todo.contains("535") || todo.contains("username and password"))
            m = "el servidor rechazó el usuario o la contraseña. En Gmail hay que usar una «contraseña de aplicación», no la contraseña normal.";
        else if (todo.contains("unknownhost") || todo.contains("unresolved") || todo.contains("no address associated"))
            m = "no se encontró el servidor de correo. Revisa el nombre del servidor y tu conexión a internet.";
        else if (todo.contains("timed out") || todo.contains("timeout") || todo.contains("connect"))
            m = "no se pudo conectar con el servidor de correo (revisa el puerto, la seguridad y tu conexión a internet).";
        return m.length() > 400 ? m.substring(0, 400) : m;
    }
}
