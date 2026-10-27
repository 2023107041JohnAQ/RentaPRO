package com.ejemplo.alquiler.model;

import jakarta.persistence.*;

import java.util.List;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    public static final List<String> TIPOS = List.of("Auto", "Camioneta", "Moto", "Scooter", "Cuatrimoto", "Furgoneta", "Otro");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String placa;
    @Column(nullable = false)
    private String marca;
    @Column(nullable = false)
    private String modelo;
    @Column(nullable = false)
    private String tipo;
    @Column(nullable = false)
    private double tarifaDiaria;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(length = 500)
    private String imagenPath;

    public Vehiculo() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getPlaca() { return placa; }
    public void setPlaca(String v) { placa = v; }
    public String getMarca() { return marca; }
    public void setMarca(String v) { marca = v; }
    public String getModelo() { return modelo; }
    public void setModelo(String v) { modelo = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public double getTarifaDiaria() { return tarifaDiaria; }
    public void setTarifaDiaria(double v) { tarifaDiaria = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { activo = v; }
    public String getImagenPath() { return imagenPath; }
    public void setImagenPath(String v) { imagenPath = v; }

    public String nombreCompleto() {
        return marca + " " + modelo;
    }

    public boolean esMoto() {
        return tipo != null && (tipo.equalsIgnoreCase("Moto") || tipo.equalsIgnoreCase("Scooter") || tipo.equalsIgnoreCase("Cuatrimoto"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehiculo)) return false;
        Vehiculo otro = (Vehiculo) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Vehiculo.class.hashCode();
    }

    @Override
    public String toString() {
        return placa + " - " + marca + " " + modelo;
    }
}
