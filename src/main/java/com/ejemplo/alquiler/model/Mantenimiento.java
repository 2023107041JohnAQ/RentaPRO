package com.ejemplo.alquiler.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Entity
@Table(name = "mantenimientos")
public class Mantenimiento {

    public static final List<String> TIPOS = List.of("SOAT", "Revisión técnica", "Seguro", "Tarjeta de propiedad",
            "Cambio de aceite", "Frenos", "Llantas", "Batería", "Servicio general", "Otro");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Vehiculo vehiculo;
    @Column(nullable = false)
    private String tipo;
    @Column(nullable = false)
    private LocalDate fecha;
    private LocalDate vencimiento;
    private Integer kilometraje;
    private Double costo;
    @Column(length = 500)
    private String notas;

    public Mantenimiento() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo v) { vehiculo = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { fecha = v; }
    public LocalDate getVencimiento() { return vencimiento; }
    public void setVencimiento(LocalDate v) { vencimiento = v; }
    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer v) { kilometraje = v; }
    public double getCosto() { return costo == null ? 0 : costo; }
    public void setCosto(Double v) { costo = v; }
    public String getNotas() { return notas; }
    public void setNotas(String v) { notas = v; }
}
