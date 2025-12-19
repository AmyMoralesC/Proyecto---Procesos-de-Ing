package Servicio;

import Modelos.usuario;

import java.sql.*;

public class servicioUsuario extends servicio {

    // ====== ADMIN INICIAL ======
    public void ensureAdminInicial() {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conectar();

            ps = conexion.prepareStatement("SELECT 1 FROM usuarios WHERE email = ? LIMIT 1");
            ps.setString(1, "admin@local");
            rs = ps.executeQuery();
            if (rs.next()) {
                return;
            }
            close(rs);
            close(ps);

            // Crear admin inicial
            ps = conexion.prepareStatement(
                    "INSERT INTO usuarios (nombre, apellido, edad, email, contrasena, rol, activo, fecha_creacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())"
            );
            ps.setString(1, "Admin");
            ps.setString(2, "Inicial");
            ps.setInt(3, 20);
            ps.setString(4, "admin@local");
            ps.setString(5, "admin123");   // demo
            ps.setString(6, "ADMIN");
            ps.setBoolean(7, true);
            ps.executeUpdate();

        } catch (Exception e) {
            // No botar el sistema por el seed: solo log
            e.printStackTrace();
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    // ====== LISTAR (para usuarios.html) ======
    public java.util.List<Modelos.usuario> listar(String q, String rol) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        java.util.List<Modelos.usuario> out = new java.util.ArrayList<>();

        try {
            conectar();

            StringBuilder sql = new StringBuilder(
                    "SELECT id_usuario, nombre, apellido, edad, email, rol, activo, fecha_creacion "
                    + "FROM usuarios WHERE 1=1 "
            );
            java.util.List<Object> params = new java.util.ArrayList<>();

            if (q != null && !q.trim().isEmpty()) {
                sql.append("AND (nombre LIKE ? OR apellido LIKE ? OR email LIKE ?) ");
                String like = "%" + q.trim() + "%";
                params.add(like);
                params.add(like);
                params.add(like);
            }
            if (rol != null && !rol.trim().isEmpty()) {
                sql.append("AND rol=? ");
                params.add(rol.trim());
            }
            sql.append("ORDER BY id_usuario DESC");

            ps = conexion.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                Modelos.usuario u = new Modelos.usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setApellido(rs.getString("apellido"));
                u.setEdad(rs.getInt("edad"));
                u.setEmail(rs.getString("email"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                u.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                out.add(u);
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("ERROR_LISTAR_USUARIOS", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public Modelos.usuario buscarPorId(int idUsuario) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "SELECT id_usuario, nombre, apellido, edad, email, contrasena, rol, activo, fecha_creacion "
                    + "FROM usuarios WHERE id_usuario=?"
            );
            ps.setInt(1, idUsuario);
            rs = ps.executeQuery();
            if (rs.next()) {
                Modelos.usuario u = new Modelos.usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setApellido(rs.getString("apellido"));
                u.setEdad(rs.getInt("edad"));
                u.setEmail(rs.getString("email"));
                u.setContrasena(rs.getString("contrasena"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                u.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                return u;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_BUSCAR_USUARIO", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public String updatePerfil(int idUsuario, String nombre, String apellido, int edad,
            String email, String np1, String np2) {
        PreparedStatement ps = null;
        try {
            conectar();

            boolean quiereCambiarPass = (np1 != null && !np1.isBlank());

            if (quiereCambiarPass) {
                if (np2 == null || !np1.equals(np2)) {
                    return "Las contraseñas no coinciden.";
                }

                ps = conexion.prepareStatement(
                        "UPDATE usuarios SET nombre=?, apellido=?, edad=?, email=?, contrasena=? WHERE id_usuario=?"
                );
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setInt(3, edad);
                ps.setString(4, email);
                ps.setString(5, np1);
                ps.setInt(6, idUsuario);
            } else {
                ps = conexion.prepareStatement(
                        "UPDATE usuarios SET nombre=?, apellido=?, edad=?, email=? WHERE id_usuario=?"
                );
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setInt(3, edad);
                ps.setString(4, email);
                ps.setInt(5, idUsuario);
            }

            ps.executeUpdate();
            return "Perfil actualizado correctamente.";

        } catch (Exception e) {
            throw new RuntimeException("ERROR_UPDATE_PERFIL", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    // ====== ADMIN: crear usuario ======
    public void crearUsuario(String nombre, String apellido, int edad, String email, String password, String rol) {
        PreparedStatement ps = null;
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "INSERT INTO usuarios(nombre, apellido, edad, email, contrasena, rol, activo) VALUES(?,?,?,?,?,?,1)"
            );
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setInt(3, edad);
            ps.setString(4, email);
            ps.setString(5, password);
            ps.setString(6, rol);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_CREAR_USUARIO", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    // ====== ADMIN: editar usuario ======
    public void editarUsuario(int idUsuario, String nombre, String apellido, int edad, String email, String rol, boolean activo, String newPasswordOrNull) {
        PreparedStatement ps = null;
        try {
            conectar();
            if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
                ps = conexion.prepareStatement(
                        "UPDATE usuarios SET nombre=?, apellido=?, edad=?, email=?, rol=?, activo=?, contrasena=? WHERE id_usuario=?"
                );
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setInt(3, edad);
                ps.setString(4, email);
                ps.setString(5, rol);
                ps.setBoolean(6, activo);
                ps.setString(7, newPasswordOrNull);
                ps.setInt(8, idUsuario);
            } else {
                ps = conexion.prepareStatement(
                        "UPDATE usuarios SET nombre=?, apellido=?, edad=?, email=?, rol=?, activo=? WHERE id_usuario=?"
                );
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setInt(3, edad);
                ps.setString(4, email);
                ps.setString(5, rol);
                ps.setBoolean(6, activo);
                ps.setInt(7, idUsuario);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_EDITAR_USUARIO", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    public void eliminarUsuario(int idUsuario) {
        PreparedStatement ps = null;
        try {
            conectar();
            ps = conexion.prepareStatement("DELETE FROM usuarios WHERE id_usuario=?");
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ELIMINAR_USUARIO", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    public usuario login(String email, String password) {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conectar();

            ps = conexion.prepareStatement(
                    "SELECT id_usuario, nombre, apellido, edad, email, contrasena, rol, activo, fecha_creacion "
                    + "FROM usuarios "
                    + "WHERE email = ? AND contrasena = ? AND activo = 1 "
                    + "LIMIT 1"
            );
            ps.setString(1, email);
            ps.setString(2, password);

            rs = ps.executeQuery();
            if (rs.next()) {
                usuario u = new usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setApellido(rs.getString("apellido"));
                u.setEdad(rs.getInt("edad"));
                u.setEmail(rs.getString("email"));
                u.setContrasena(rs.getString("contrasena"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                u.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                return u;
            }
            return null;

        } catch (Exception e) {
            throw new RuntimeException("ERROR_LOGIN", e);
        } finally {

            close(rs);
            close(ps);
            desconectar();
        }
    }

    public void eliminarUsuarioConTickets(int idUsuario) {
        PreparedStatement ps = null;

        try {
            conectar();
            conexion.setAutoCommit(false);

            // 1) Borrar comentarios hechos por ese usuario
            ps = conexion.prepareStatement("DELETE FROM comentarios_ticket WHERE id_usuario = ?");
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            close(ps);

            // 2) Borrar tickets creados por ese usuario
            ps = conexion.prepareStatement("DELETE FROM tickets WHERE id_creador = ?");
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            close(ps);

            // 3) Si el usuario era técnico asignado en tickets de otros, hay que soltarlo
            ps = conexion.prepareStatement("UPDATE tickets SET id_asignado = NULL WHERE id_asignado = ?");
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            close(ps);

            // 4) Borrar usuario
            ps = conexion.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?");
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            close(ps);

            conexion.commit();

        } catch (Exception e) {
            try {
                conexion.rollback();
            } catch (Exception ignored) {
            }
            throw new RuntimeException("ERROR_ELIMINAR_USUARIO_CON_TICKETS", e);

        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (Exception ignored) {
            }
            close(ps);
            desconectar();
        }
    }

}
