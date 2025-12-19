package Modelos;

import java.sql.Timestamp;

public class ticket {

    private int idTicket;
    private String asunto;
    private String prioridad; // Alta | Media | Baja
    private String estado;    // Abierto | En proceso | Resuelto | Denegado
    private Timestamp fechaCreacion;
    private Timestamp fechaResolucion;
    private String codigo;
    private String descripcion;
    private String solucion;

    private int idCreador;         
    private Integer idAsignado;    
    private String tecnicoAsignadoNombre;

    public ticket() {
    }

    public int getIdTicket() {
        return idTicket;
    }

    public void setIdTicket(int idTicket) {
        this.idTicket = idTicket;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Timestamp getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(Timestamp fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getSolucion() {
        return solucion;
    }

    public void setSolucion(String solucion) {
        this.solucion = solucion;
    }

    public int getIdCreador() {
        return idCreador;
    }

    public void setIdCreador(int idCreador) {
        this.idCreador = idCreador;
    }

    public Integer getIdAsignado() {
        return idAsignado;
    }

    public void setIdAsignado(Integer idAsignado) {
        this.idAsignado = idAsignado;
    }

    public String getTecnicoAsignadoNombre() {
        return tecnicoAsignadoNombre;
    }

    public void setTecnicoAsignadoNombre(String tecnicoAsignadoNombre) {
        this.tecnicoAsignadoNombre = tecnicoAsignadoNombre;
    }
}
