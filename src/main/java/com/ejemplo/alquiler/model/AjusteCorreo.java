package com.ejemplo.alquiler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Entity
@Table(name = "ajustes_correo")
public class AjusteCorreo {

    @Id
    private Long id = 1L;
    private boolean activo;
    private String host = "smtp.gmail.com";
    private int puerto = 587;
    private String seguridad = "STARTTLS";
    private String usuario;
    @Column(length = 300)
    private String clave;
    private String remitenteNombre;
    private String remitenteCorreo;
    private String empresa = "RentaPro";
    private String telefonoContacto;
    private boolean avisoReserva = true;
    private boolean avisoPago = true;
    private boolean avisoRecordatorio = true;
    private boolean avisoMora = true;

    public AjusteCorreo() {
    }

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { activo = v; }
    public String getHost() { return host; }
    public void setHost(String v) { host = v; }
    public int getPuerto() { return puerto; }
    public void setPuerto(int v) { puerto = v; }
    public String getSeguridad() { return seguridad; }
    public void setSeguridad(String v) { seguridad = v; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String v) { usuario = v; }
    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public String getRemitenteNombre() { return remitenteNombre; }
    public void setRemitenteNombre(String v) { remitenteNombre = v; }
    public String getRemitenteCorreo() { return remitenteCorreo; }
    public void setRemitenteCorreo(String v) { remitenteCorreo = v; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String v) { empresa = v; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String v) { telefonoContacto = v; }
    public boolean isAvisoReserva() { return avisoReserva; }
    public void setAvisoReserva(boolean v) { avisoReserva = v; }
    public boolean isAvisoPago() { return avisoPago; }
    public void setAvisoPago(boolean v) { avisoPago = v; }
    public boolean isAvisoRecordatorio() { return avisoRecordatorio; }
    public void setAvisoRecordatorio(boolean v) { avisoRecordatorio = v; }
    public boolean isAvisoMora() { return avisoMora; }
    public void setAvisoMora(boolean v) { avisoMora = v; }
}
