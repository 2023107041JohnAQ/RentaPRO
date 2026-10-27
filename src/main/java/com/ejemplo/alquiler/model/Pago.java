package com.ejemplo.alquiler.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Reserva reserva;
    @Column(nullable = false)
    private LocalDate fechaPago;
    @Column(nullable = false)
    private double monto;
    @Column(nullable = false)
    private String metodo;
    @Column(nullable = false)
    private String concepto;

    public Pago() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva v) { reserva = v; }
    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate v) { fechaPago = v; }
    public double getMonto() { return monto; }
    public void setMonto(double v) { monto = v; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String v) { metodo = v; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String v) { concepto = v; }
}
