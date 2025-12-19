package Controller;

import Modelos.usuario;
import Servicio.servicioUsuario;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/UsuarioController")
public class UsuarioController extends HttpServlet {

    private final servicioUsuario su = new servicioUsuario();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario me = (usuario) req.getSession().getAttribute("user");
        if (me == null) {
            resp.sendRedirect("login.html");
            return;
        }
        if (!"ADMIN".equals(me.getRol())) {
            resp.sendRedirect("DashboardController");
            return;
        }

        String q = nvl(req.getParameter("q"));
        String rol = nvl(req.getParameter("rol"));
        String view = nvl(req.getParameter("view"));
        int idEdit = parseInt(req.getParameter("id"));

        List<usuario> lista = su.listar(q, rol);

        String openEdit = "0";
        String eId = "", eNombre = "", eApellido = "", eEdad = "0", eEmail = "";
        String eOptsRol = optsRolSelect("USUARIO");
        String eOptsActivo = optsActivoSelect(true);

        if ("edit".equals(view) && idEdit > 0) {
            usuario u = su.buscarPorId(idEdit);
            if (u != null) {
                openEdit = "1";
                eId = String.valueOf(u.getIdUsuario());
                eNombre = TemplateUtil.esc(u.getNombre());
                eApellido = TemplateUtil.esc(u.getApellido());
                eEdad = String.valueOf(u.getEdad());
                eEmail = TemplateUtil.esc(u.getEmail());
                eOptsRol = optsRolSelect(u.getRol());
                eOptsActivo = optsActivoSelect(u.isActivo());
            }
        }

        String html = TemplateUtil.load(getServletContext(), "/usuarios.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(me, "usuarios"));
        html = TemplateUtil.replace(html, "{{Q}}", TemplateUtil.esc(q));
        html = TemplateUtil.replace(html, "{{OPTS_ROL}}", optsRolFilter(rol));
        html = TemplateUtil.replace(html, "{{USUARIOS_ROWS}}", buildRows(lista, me));
        html = TemplateUtil.replace(html, "{{MSG}}", TemplateUtil.esc(nvl(req.getParameter("msg"))));

        // modal editar
        html = TemplateUtil.replace(html, "{{OPEN_EDIT}}", openEdit);
        html = TemplateUtil.replace(html, "{{E_ID}}", eId);
        html = TemplateUtil.replace(html, "{{E_NOMBRE}}", eNombre);
        html = TemplateUtil.replace(html, "{{E_APELLIDO}}", eApellido);
        html = TemplateUtil.replace(html, "{{E_EDAD}}", eEdad);
        html = TemplateUtil.replace(html, "{{E_EMAIL}}", eEmail);
        html = TemplateUtil.replace(html, "{{E_OPTS_ROL}}", eOptsRol);
        html = TemplateUtil.replace(html, "{{E_OPTS_ACTIVO}}", eOptsActivo);

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario me = (usuario) req.getSession().getAttribute("user");
        if (me == null) {
            resp.sendRedirect("login.html");
            return;
        }
        if (!"ADMIN".equals(me.getRol())) {
            resp.sendRedirect("DashboardController");
            return;
        }

        String action = nvl(req.getParameter("action"));

        try {
            if ("create".equals(action)) {
                String nombre = nvl(req.getParameter("nombre"));
                String apellido = nvl(req.getParameter("apellido"));
                int edad = parseInt(req.getParameter("edad"));
                String email = nvl(req.getParameter("email"));
                String rol = nvl(req.getParameter("rol"));
                String pass = nvl(req.getParameter("password"));

                su.crearUsuario(nombre, apellido, edad, email, pass, rol);
                resp.sendRedirect("UsuarioController?msg=Usuario creado");
                return;
            }

            if ("update".equals(action)) {
                int id = parseInt(req.getParameter("id"));
                String nombre = nvl(req.getParameter("nombre"));
                String apellido = nvl(req.getParameter("apellido"));
                int edad = parseInt(req.getParameter("edad"));
                String email = nvl(req.getParameter("email"));
                String rol = nvl(req.getParameter("rol"));
                boolean activo = "1".equals(nvl(req.getParameter("activo"))) || "true".equalsIgnoreCase(nvl(req.getParameter("activo")));
                String newPass = nvl(req.getParameter("newPassword"));

                su.editarUsuario(id, nombre, apellido, edad, email, rol, activo, newPass);
                resp.sendRedirect("UsuarioController?msg=Usuario actualizado");
                return;
            }

            if ("delete".equals(action)) {
                int id = parseInt(req.getParameter("id"));

                if (id == me.getIdUsuario()) {
                    resp.sendRedirect("UsuarioController?msg=No puede eliminar su propio usuario");
                    return;
                }

                su.eliminarUsuarioConTickets(id);
                resp.sendRedirect("UsuarioController?msg=Usuario eliminado");
                return;
            }

            // fallback
            resp.sendRedirect("UsuarioController?msg=Acción no válida");

        } catch (Exception ex) {
            ex.printStackTrace();
            resp.sendRedirect("UsuarioController?msg=Error: " + safeMsg(ex.getMessage()));
        }
    }

    // ================== TABLA ==================
    private String buildRows(List<usuario> usuarios, usuario me) {
        if (usuarios == null || usuarios.isEmpty()) {
            return "<tr><td colspan='6' class='text-center text-secondary py-4'>Sin resultados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (usuario u : usuarios) {
            sb.append("<tr>");
            sb.append("<td>").append(u.getIdUsuario()).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(u.getNombre()))
                    .append(" ").append(TemplateUtil.esc(u.getApellido())).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(u.getEmail())).append("</td>");
            sb.append("<td><span class='badge-soft badge-open'>").append(TemplateUtil.esc(u.getRol())).append("</span></td>");
            sb.append("<td>").append(u.isActivo() ? "Sí" : "No").append("</td>");

            sb.append("<td class='text-end'>");

            sb.append("<a class='btn btn-sm btn-outline-light me-2' href='UsuarioController?view=edit&id=")
                    .append(u.getIdUsuario()).append("'><i class='bi bi-pencil'></i> Editar</a>");

            // Eliminar (POST + confirm). No permite borrarse a sí mismo
            if (me != null && u.getIdUsuario() != me.getIdUsuario()) {
                sb.append("<form method='post' action='UsuarioController' class='d-inline' ")
                        .append("onsubmit=\"return confirm('¿Eliminar este usuario?\\n\\nSe eliminará el usuario y TODOS sus tickets/comentarios relacionados.\\n\\nEsta acción no se puede deshacer.');\">")
                        .append("<input type='hidden' name='action' value='delete'>")
                        .append("<input type='hidden' name='id' value='").append(u.getIdUsuario()).append("'>")
                        .append("<button class='btn btn-sm btn-outline-danger' type='submit'>")
                        .append("<i class='bi bi-trash'></i> Eliminar</button>")
                        .append("</form>");
            } else {
                sb.append("<span class='text-secondary'>—</span>");
            }

            sb.append("</td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    // filtro roles
    private String optsRolFilter(String sel) {
        return opt("ADMIN", sel) + opt("TECNICO", sel) + opt("USUARIO", sel);
    }

    // select rol (modal)
    private String optsRolSelect(String sel) {
        return optS("USUARIO", sel) + optS("TECNICO", sel) + optS("ADMIN", sel);
    }

    private String optsActivoSelect(boolean activo) {
        if (activo) {
            return "<option value='1' selected>Sí</option><option value='0'>No</option>";
        }
        return "<option value='1'>Sí</option><option value='0' selected>No</option>";
    }

    private String opt(String v, String sel) {
        return v.equals(sel) ? "<option selected value='" + v + "'>" + v + "</option>" : "<option value='" + v + "'>" + v + "</option>";
    }

    private String optS(String v, String sel) {
        return v.equals(sel) ? "<option selected value='" + v + "'>" + v + "</option>" : "<option value='" + v + "'>" + v + "</option>";
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(nvl(s));
        } catch (Exception e) {
            return 0;
        }
    }

    private String safeMsg(String s) {
        return s == null ? "desconocido" : s.replaceAll("[\\r\\n]", " ");
    }
}
