package com.ejemplo.alquiler.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    private Reserva reserva;
    @Column(nullable = false)
    private LocalDate fechaFirma;
    @Column(nullable = false)
    private String codigo;
    @Column(nullable = false)
    private String estado = "VIGENTE";

    public Contrato() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva v) { reserva = v; }
    public LocalDate getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(LocalDate v) { fechaFirma = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { codigo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }

    @Override
    public String toString() {
        return codigo;
    }
}
