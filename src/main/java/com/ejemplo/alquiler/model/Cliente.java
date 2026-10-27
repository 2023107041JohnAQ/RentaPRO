package com.ejemplo.alquiler.model;

import jakarta.persistence.*;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String documento;
    @Column(nullable = false)
    private String nombres;
    @Column(nullable = false)
    private String telefono;
    private String correo;
    private String direccion;
    private String licencia;

    public Cliente() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getDocumento() { return documento; }
    public void setDocumento(String v) { documento = v; }
    public String getNombres() { return nombres; }
    public void setNombres(String v) { nombres = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { telefono = v; }
    public String getCorreo() { return correo; }
    public void setCorreo(String v) { correo = v; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = v; }
    public String getLicencia() { return licencia; }
    public void setLicencia(String v) { licencia = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente otro = (Cliente) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Cliente.class.hashCode();
    }

    @Override
    public String toString() {
        return documento + " - " + nombres;
    }
}
