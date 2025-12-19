package Modelos;

import java.sql.Timestamp;

public class usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private int edad;
    private String email;
    private String contrasena; 
    private String rol;        // ADMIN | TECNICO | USUARIO
    private boolean activo;
    private Timestamp fechaCreacion;

    public usuario() {
    }

    public usuario(int idUsuario, String nombre, String apellido, int edad, String email, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getId() {
        return idUsuario;
    }

    public void setId(int id) {
        this.idUsuario = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getNombreCompleto() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }
}
