package Controller;

import Modelos.usuario;
import Servicio.servicioTicket;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/DashboardController")
public class DashboardController extends HttpServlet {

    private final servicioTicket st = new servicioTicket();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario u = (usuario) req.getSession().getAttribute("user");

        if ("USUARIO".equals(u.getRol())) {
            resp.sendRedirect("TicketController?view=mis");
            return;
        }

        // Admin y Técnico ven KPIs globales
        int total = st.countVisibleFor(u);
        int alta = st.countPrioridadVisibleFor(u, "Alta");
        int media = st.countPrioridadVisibleFor(u, "Media");
        int baja = st.countPrioridadVisibleFor(u, "Baja");

        int abierto = st.countEstadoVisibleFor(u, "Abierto");
        int proceso = st.countEstadoVisibleFor(u, "En proceso");
        int resuelto = st.countEstadoVisibleFor(u, "Resuelto");
        int denegado = st.countEstadoVisibleFor(u, "Denegado");

        String html = TemplateUtil.load(getServletContext(), "/index.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", sidebar(u, "dashboard"));
        html = TemplateUtil.replace(html, "{{NOMBRE}}", TemplateUtil.esc(u.getNombre()));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));

        html = TemplateUtil.replace(html, "{{KPI_TOTAL}}", String.valueOf(total));
        html = TemplateUtil.replace(html, "{{KPI_ALTA}}", String.valueOf(alta));
        html = TemplateUtil.replace(html, "{{KPI_MEDIA}}", String.valueOf(media));
        html = TemplateUtil.replace(html, "{{KPI_BAJA}}", String.valueOf(baja));

        html = TemplateUtil.replace(html, "{{KPI_ABIERTO}}", String.valueOf(abierto));
        html = TemplateUtil.replace(html, "{{KPI_PROCESO}}", String.valueOf(proceso));
        html = TemplateUtil.replace(html, "{{KPI_RESUELTO}}", String.valueOf(resuelto));
        html = TemplateUtil.replace(html, "{{KPI_DENEGADO}}", String.valueOf(denegado));

        html = TemplateUtil.replace(html, "{{TICKETS_VIEW}}", "operador");

        String nota = ("ADMIN".equals(u.getRol()))
                ? "Nota: como administrador usted puede asignar técnicos a tickets y ver el seguimiento de estos, pero no puede interferir directamente sin aprobación de RH."
                : "Nota: puede resolver tickets sin asignación o asignados a usted. Los demás que aparecen asignados serán solo de lectura.";
        html = TemplateUtil.replace(html, "{{NOTA_ROL}}", nota);

        String acciones = ""
                + "<a class='btn btn-primary' href='TicketController?view=operador'><i class='bi bi-inboxes'></i> Ver tickets</a>"
                + "<a class='btn btn-outline-light' href='PerfilController'><i class='bi bi-person'></i> Mi perfil</a>"
                + ("ADMIN".equals(u.getRol())
                ? "<a class='btn btn-outline-light' href='UsuarioController'><i class='bi bi-people'></i> Usuarios</a>"
                : "");
        html = TemplateUtil.replace(html, "{{ACCIONES_RAPIDAS}}", acciones);

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private String sidebar(usuario u, String active) {
        StringBuilder sb = new StringBuilder();
        sb.append("<aside class='sidebar'>")
                .append("<div class='brand'><span class='dot'></span><div><h1>Gestor de Tickets</h1><p>Soporte técnico</p></div></div>");

        sb.append("<a class='navlink ")
                .append("dashboard".equals(active) ? "active" : "")
                .append("' href='DashboardController'><i class='bi bi-house'></i> Inicio</a>");

        sb.append("<a class='navlink ")
                .append("tickets".equals(active) ? "active" : "")
                .append("' href='TicketController?view=")
                .append("USUARIO".equals(u.getRol()) ? "mis" : "operador")
                .append("'><i class='bi bi-inboxes'></i> Tickets</a>");

        if ("ADMIN".equals(u.getRol())) {
            sb.append("<a class='navlink ")
                    .append("usuarios".equals(active) ? "active" : "")
                    .append("' href='UsuarioController'><i class='bi bi-people'></i> Usuarios</a>");
        }

        sb.append("<hr style='border-color: rgba(148,163,184,.22)'/>");

        sb.append("<a class='navlink ")
                .append("perfil".equals(active) ? "active" : "")
                .append("' href='PerfilController'><i class='bi bi-person'></i> Perfil</a>");

        sb.append("<form method='post' action='AuthController' style='margin-top:10px;'>")
                .append("<input type='hidden' name='accion' value='logout'>")
                .append("<button class='navlink w-100 text-start' type='submit' style='background:transparent;'>")
                .append("<i class='bi bi-box-arrow-right'></i> Cerrar sesión</button></form>")
                .append("</aside>");
        return sb.toString();
    }
}
