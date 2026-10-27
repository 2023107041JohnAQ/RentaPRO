package com.ejemplo.alquiler.model;

import com.ejemplo.alquiler.util.Formato;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Vehiculo vehiculo;
    @ManyToOne(optional = false)
    private Cliente cliente;
    @Column(nullable = false)
    private LocalDate fechaInicio;
    @Column(nullable = false)
    private LocalDate fechaFin;
    @Column(nullable = false)
    private String estado = "RESERVADA";
    private double totalEstimado;
    private LocalDate fechaDevolucionReal;
    private Double recargo;

    public Reserva() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo v) { vehiculo = v; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente v) { cliente = v; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate v) { fechaInicio = v; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate v) { fechaFin = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public double getTotalEstimado() { return totalEstimado; }
    public void setTotalEstimado(double v) { totalEstimado = v; }
    public LocalDate getFechaDevolucionReal() { return fechaDevolucionReal; }
    public void setFechaDevolucionReal(LocalDate v) { fechaDevolucionReal = v; }
    public double getRecargo() { return recargo == null ? 0 : recargo; }
    public void setRecargo(double v) { recargo = v; }

    public long dias() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }

    public boolean estaAbierta() {
        return "RESERVADA".equals(estado) || "EN ALQUILER".equals(estado);
    }

    public boolean estaVencida(LocalDate hoy) {
        return estaAbierta() && fechaFin != null && hoy.isAfter(fechaFin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva)) return false;
        Reserva otra = (Reserva) o;
        return id != null && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return Reserva.class.hashCode();
    }

    @Override
    public String toString() {
        String cli = cliente == null ? "?" : cliente.getNombres();
        String veh = vehiculo == null ? "?" : vehiculo.getPlaca();
        return "#" + id + " · " + cli + " · " + veh + " · " + Formato.fecha(fechaInicio) + " al " + Formato.fecha(fechaFin);
    }
}
