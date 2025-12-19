package Servicio;

import java.sql.*;

public class servicio {

    protected Connection conexion = null;

    private String host = "localhost";
    private String puerto = "3306";
    private String sid = "bdticketes";
    private String usuario = "root";
    private String clave = "admin"; // No olvidar cambiar contraseña si no es "admin"

    public void conectar() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexion = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + puerto + "/" + sid + "?serverTimezone=UTC",
                    usuario, clave
            );
        } catch (Exception e) {
            throw new RuntimeException("ERROR_CONEXION_BD", e);
        }
    }

    public void desconectar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                conexion = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(AutoCloseable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception ignored) {
        }
    }
}
