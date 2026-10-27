package com.ejemplo.alquiler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Reserva reserva;
    private String tipo;
    private String clave;
    private String destinatario;
    private String asunto;
    private LocalDateTime fechaHora;
    private String estado;
    @Column(length = 500)
    private String detalle;

    public Notificacion() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva v) { reserva = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String v) { destinatario = v; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String v) { asunto = v; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime v) { fechaHora = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String v) { detalle = v; }
}
