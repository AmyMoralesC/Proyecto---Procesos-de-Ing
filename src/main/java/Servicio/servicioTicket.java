package Servicio;

import Modelos.ticket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class servicioTicket extends servicio {

    // Usuario final: solo sus tickets
    public List<ticket> listarTicketsUsuario(int idUsuario, String q, String prioridad, String estado) {
        return listarTicketsBase(
                "WHERE t.id_creador=?",
                new Object[]{idUsuario},
                q, prioridad, estado
        );
    }

    // Técnico/Admin: todos (con info de técnico asignado), pero NO datos personales del creador
    public List<ticket> listarTicketsOperador(String q, String prioridad, String estado) {
        return listarTicketsBase(
                "WHERE 1=1",
                new Object[]{},
                q, prioridad, estado
        );
    }

    private List<ticket> listarTicketsBase(String baseWhere, Object[] baseParams,
            String q, String prioridad, String estado) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<ticket> out = new ArrayList<>();
        try {
            conectar();
            StringBuilder sql = new StringBuilder(
                    "SELECT t.id_ticket, t.asunto, t.prioridad, t.estado, t.fecha_creacion, t.fecha_resolucion, "
                    + "t.codigo, t.descripcion, t.solucion, t.id_creador, t.id_asignado, "
                    + "COALESCE(CONCAT(u.nombre,' ',u.apellido), 'Sin asignar') AS tecnico_asignado "
                    + "FROM tickets t "
                    + "LEFT JOIN usuarios u ON u.id_usuario = t.id_asignado "
                    + baseWhere + " "
            );

            List<Object> params = new ArrayList<>();
            for (Object p : baseParams) {
                params.add(p);
            }

            if (q != null && !q.trim().isEmpty()) {
                sql.append("AND (t.asunto LIKE ? OR t.codigo LIKE ? OR t.descripcion LIKE ?) ");
                String like = "%" + q.trim() + "%";
                params.add(like);
                params.add(like);
                params.add(like);
            }
            if (prioridad != null && !prioridad.trim().isEmpty()) {
                sql.append("AND t.prioridad=? ");
                params.add(prioridad.trim());
            }
            if (estado != null && !estado.trim().isEmpty()) {
                sql.append("AND t.estado=? ");
                params.add(estado.trim());
            }

            sql.append("ORDER BY t.id_ticket DESC");

            ps = conexion.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                ticket t = new ticket();
                t.setIdTicket(rs.getInt("id_ticket"));
                t.setAsunto(rs.getString("asunto"));
                t.setPrioridad(rs.getString("prioridad"));
                t.setEstado(rs.getString("estado"));
                t.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                t.setFechaResolucion(rs.getTimestamp("fecha_resolucion"));
                t.setCodigo(rs.getString("codigo"));
                t.setDescripcion(rs.getString("descripcion"));
                t.setSolucion(rs.getString("solucion"));
                t.setIdCreador(rs.getInt("id_creador"));
                t.setIdAsignado((Integer) rs.getObject("id_asignado"));
                t.setTecnicoAsignadoNombre(rs.getString("tecnico_asignado"));
                out.add(t);
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_LISTAR_TICKETS", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public ticket getById(int idTicket) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "SELECT id_ticket, asunto, prioridad, estado, fecha_creacion, fecha_resolucion, codigo, descripcion, solucion, id_creador, id_asignado "
                    + "FROM tickets WHERE id_ticket=?"
            );
            ps.setInt(1, idTicket);
            rs = ps.executeQuery();
            if (rs.next()) {
                ticket t = new ticket();
                t.setIdTicket(rs.getInt("id_ticket"));
                t.setAsunto(rs.getString("asunto"));
                t.setPrioridad(rs.getString("prioridad"));
                t.setEstado(rs.getString("estado"));
                t.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                t.setFechaResolucion(rs.getTimestamp("fecha_resolucion"));
                t.setCodigo(rs.getString("codigo"));
                t.setDescripcion(rs.getString("descripcion"));
                t.setSolucion(rs.getString("solucion"));
                t.setIdCreador(rs.getInt("id_creador"));
                t.setIdAsignado((Integer) rs.getObject("id_asignado"));
                return t;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_GET_TICKET", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    // USUARIO FINAL: crear ticket
    public void crearTicket(String asunto, String prioridad, String codigo, String descripcion, int idCreador) {
        PreparedStatement ps = null;
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "INSERT INTO tickets(asunto, prioridad, estado, codigo, descripcion, id_creador) "
                    + "VALUES(?, ?, 'Abierto', ?, ?, ?)"
            );
            ps.setString(1, asunto);
            ps.setString(2, prioridad);
            ps.setString(3, codigo);
            ps.setString(4, descripcion);
            ps.setInt(5, idCreador);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_CREAR_TICKET", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    // ADMIN: asignar ticket a técnico (o null para desasignar)
    public void asignarTicket(int idTicket, Integer idTecnico) {
        PreparedStatement ps = null;
        try {
            conectar();
            ps = conexion.prepareStatement("UPDATE tickets SET id_asignado=? WHERE id_ticket=?");
            if (idTecnico == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, idTecnico);
            }
            ps.setInt(2, idTicket);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ERROR_ASIGNAR_TICKET", e);
        } finally {
            close(ps);
            desconectar();
        }
    }

    // TECNICO: resolver/denegar (solo si: está sin asignación o asignado a él)
    public void resolverTicket(int idTicket, int idTecnico, String nuevoEstado, String solucion, String comentario) {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            conectar();
            conexion.setAutoCommit(false);

            // 1) Bloqueo lógico: solo si está sin asignación o asignado a él
            // Si está sin asignación, lo toma automáticamente
            ps1 = conexion.prepareStatement(
                    "UPDATE tickets "
                    + "SET id_asignado = IF(id_asignado IS NULL, ?, id_asignado), "
                    + "    estado=?, solucion=?, fecha_resolucion=IF(? IN ('Resuelto','Denegado'), CURRENT_TIMESTAMP, NULL) "
                    + "WHERE id_ticket=? AND (id_asignado IS NULL OR id_asignado=?)"
            );
            ps1.setInt(1, idTecnico);
            ps1.setString(2, nuevoEstado);
            ps1.setString(3, solucion);
            ps1.setString(4, nuevoEstado);
            ps1.setInt(5, idTicket);
            ps1.setInt(6, idTecnico);

            int updated = ps1.executeUpdate();
            if (updated == 0) {
                conexion.rollback();
                throw new RuntimeException("NO_AUTORIZADO_RESOLVER");
            }

            // 2) Comentario (bitácora)
            if (comentario != null && !comentario.trim().isEmpty()) {
                ps2 = conexion.prepareStatement(
                        "INSERT INTO comentarios_ticket(id_ticket, id_usuario, comentario) VALUES(?,?,?)"
                );
                ps2.setInt(1, idTicket);
                ps2.setInt(2, idTecnico);
                ps2.setString(3, comentario);
                ps2.executeUpdate();
            }

            conexion.commit();
        } catch (Exception e) {
            try {
                if (conexion != null) {
                    conexion.rollback();
                }
            } catch (Exception ignored) {
            }
            throw new RuntimeException("ERROR_RESOLVER_TICKET", e);
        } finally {
            try {
                if (conexion != null) {
                    conexion.setAutoCommit(true);
                }
            } catch (Exception ignored) {
            }
            close(ps2);
            close(ps1);
            desconectar();
        }
    }

    // KPI para dashboard técnico/admin
    public int countByEstado(String estado) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conectar();
            ps = conexion.prepareStatement("SELECT COUNT(*) c FROM tickets WHERE estado=?");
            ps.setString(1, estado);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_COUNT_ESTADO", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public int countByPrioridad(String prioridad) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conectar();
            ps = conexion.prepareStatement("SELECT COUNT(*) c FROM tickets WHERE prioridad=?");
            ps.setString(1, prioridad);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_COUNT_PRIORIDAD", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    // ===== KPI "VISIBLES" por rol =====
    // Regla:
    // - ADMIN y TECNICO ven global.
    // - USUARIO ve solo sus tickets.
    public int countVisibleFor(Modelos.usuario u) {
        if ("USUARIO".equals(u.getRol())) {
            return countWhere("WHERE id_creador=?", new Object[]{u.getIdUsuario()});
        }
        return countWhere("WHERE 1=1", new Object[]{});
    }

    public int countPrioridadVisibleFor(Modelos.usuario u, String prioridad) {
        if ("USUARIO".equals(u.getRol())) {
            return countWhere("WHERE id_creador=? AND prioridad=?", new Object[]{u.getIdUsuario(), prioridad});
        }
        return countWhere("WHERE prioridad=?", new Object[]{prioridad});
    }

    public int countEstadoVisibleFor(Modelos.usuario u, String estado) {
        if ("USUARIO".equals(u.getRol())) {
            return countWhere("WHERE id_creador=? AND estado=?", new Object[]{u.getIdUsuario(), estado});
        }
        return countWhere("WHERE estado=?", new Object[]{estado});
    }

    private int countWhere(String where, Object[] params) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conectar();
            ps = conexion.prepareStatement("SELECT COUNT(*) c FROM tickets " + where);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_COUNT_GENERIC", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public List<String> listarComentarios(int idTicket) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> out = new ArrayList<>();
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "SELECT comentario FROM comentarios_ticket WHERE id_ticket=? ORDER BY id_comentario ASC"
            );
            ps.setInt(1, idTicket);
            rs = ps.executeQuery();
            while (rs.next()) {
                out.add(rs.getString("comentario"));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_LISTAR_COMENTARIOS", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

    public List<Modelos.usuario> listarTecnicos() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Modelos.usuario> out = new ArrayList<>();
        try {
            conectar();
            ps = conexion.prepareStatement(
                    "SELECT id_usuario, nombre, apellido, rol "
                    + "FROM usuarios "
                    + "WHERE UPPER(rol)='TECNICO' "
                    + "ORDER BY nombre, apellido"
            );
            rs = ps.executeQuery();
            while (rs.next()) {
                Modelos.usuario u = new Modelos.usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setApellido(rs.getString("apellido"));
                u.setRol(rs.getString("rol"));
                out.add(u);
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("ERROR_LISTAR_TECNICOS", e);
        } finally {
            close(rs);
            close(ps);
            desconectar();
        }
    }

}
