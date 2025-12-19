package Controller;

import Modelos.ticket;
import Modelos.usuario;
import Servicio.servicioTicket;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet("/TicketController")
public class TicketController extends HttpServlet {

    private final servicioTicket st = new servicioTicket();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario u = (usuario) req.getSession().getAttribute("user");
        if (u == null) {
            resp.sendRedirect("login.html");
            return;
        }

        String view = nvl(req.getParameter("view"));

        // defaults por rol
        if (view.isEmpty()) {
            if (isUsuario(u)) {
                view = "mis";
            } else {
                view = "operador";
            }
        }

        switch (view) {
            case "mis":
                renderMis(req, resp, u);
                return;
            case "operador":
                renderOperador(req, resp, u);
                return;
            case "resolver":
                renderResolver(req, resp, u);
                return;
            case "ver":
                renderVer(req, resp, u);
                return;
            case "admin":
                renderAdmin(req, resp, u);
                return;
            default:
                resp.sendRedirect("TicketController?view=" + (isUsuario(u) ? "mis" : "operador"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario u = (usuario) req.getSession().getAttribute("user");
        if (u == null) {
            resp.sendRedirect("login.html");
            return;
        }

        String accion = nvl(req.getParameter("accion"));

        if ("crear".equals(accion)) {
            postCrear(req, resp, u);
            return;
        }

        if ("resolver".equals(accion)) {
            postResolver(req, resp, u);
            return;
        }

        if ("asignar".equals(accion)) {
            postAsignar(req, resp, u);
            return;
        }

        // fallback
        resp.sendRedirect("TicketController?view=" + (isUsuario(u) ? "mis" : "operador"));
    }

    // =================== POSTS ===================
    private void postCrear(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        // Solo USUARIO crea
        if (!isUsuario(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        String asunto = nvl(req.getParameter("asunto"));
        String prioridad = nvl(req.getParameter("prioridad"));
        String codigo = nvl(req.getParameter("codigo"));
        String descripcion = nvl(req.getParameter("descripcion"));

        if (asunto.isEmpty() || prioridad.isEmpty() || codigo.isEmpty() || descripcion.isEmpty()) {
            resp.sendRedirect("TicketController?view=mis&msg=" + url("Complete todos los campos"));
            return;
        }

        st.crearTicket(asunto, prioridad, codigo, descripcion, u.getIdUsuario());

        resp.sendRedirect("TicketController?view=mis&msg=" + url("Ticket creado"));
    }

    private void postResolver(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        
        if (!isTecnico(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        int idTicket = parseInt(req.getParameter("id"), -1);
        if (idTicket <= 0) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        String nuevoEstado = nvl(req.getParameter("estado"));     // "En proceso" / "Resuelto" / "Denegado" ...
        String solucion = nvl(req.getParameter("solucion"));
        String comentario = nvl(req.getParameter("comentario"));

        if (nuevoEstado.isEmpty()) {
            resp.sendRedirect("TicketController?view=resolver&id=" + idTicket + "&msg=" + url("Seleccione un estado"));
            return;
        }
        if (("Resuelto".equals(nuevoEstado) || "Denegado".equals(nuevoEstado)) && solucion.isEmpty()) {
            resp.sendRedirect("TicketController?view=resolver&id=" + idTicket + "&msg=" + url("Agregue una solución"));
            return;
        }

        // el servicio retorna void
        st.resolverTicket(idTicket, u.getIdUsuario(), nuevoEstado, solucion, comentario);

        resp.sendRedirect("TicketController?view=operador&msg=" + url("Ticket actualizado"));
    }

    private void postAsignar(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        if (!isAdmin(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        int idTicket = parseInt(req.getParameter("id"), -1);
        if (idTicket <= 0) {
            resp.sendRedirect("TicketController?view=operador&msg=" + url("Ticket inválido"));
            return;
        }

        String idTecStr = nvl(req.getParameter("id_tecnico"));
        Integer idTecnico = null;
        if (!idTecStr.isEmpty()) {
            int tmp = parseInt(idTecStr, -1);
            if (tmp > 0) {
                idTecnico = tmp;
            }
        }

        st.asignarTicket(idTicket, idTecnico);
        resp.sendRedirect("TicketController?view=admin&id=" + idTicket + "&msg=" + url("Asignación actualizada"));
    }

    // =================== RENDERS ===================
    private void renderMis(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        if (!isUsuario(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        String q = nvl(req.getParameter("q"));
        String p = nvl(req.getParameter("p"));
        String e = nvl(req.getParameter("e"));
        String msg = nvl(req.getParameter("msg"));

        List<ticket> tickets = st.listarTicketsUsuario(u.getIdUsuario(), q, p, e);

        String html = TemplateUtil.load(getServletContext(), "/tickets.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "tickets"));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));
        html = TemplateUtil.replace(html, "{{VIEW}}", "mis");
        html = TemplateUtil.replace(html, "{{Q}}", TemplateUtil.esc(q));
        html = TemplateUtil.replace(html, "{{OPTS_PRIORIDAD}}", optsPrioridad(p));
        html = TemplateUtil.replace(html, "{{OPTS_ESTADO}}", optsEstado(e));
        html = TemplateUtil.replace(html, "{{TICKETS_ROWS}}", buildRowsUsuario(tickets));
        
        html = TemplateUtil.replace(html, "{{BTN_NUEVO_TICKET}}", btnNuevoTicket());
        html = TemplateUtil.replace(html, "{{MODAL_NUEVO_TICKET}}", modalNuevoTicket(msg));

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private void renderOperador(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        // ADMIN o TECNICO
        if (isUsuario(u)) {
            resp.sendRedirect("TicketController?view=mis");
            return;
        }

        String q = nvl(req.getParameter("q"));
        String p = nvl(req.getParameter("p"));
        String e = nvl(req.getParameter("e"));
        String msg = nvl(req.getParameter("msg"));

        List<ticket> tickets = st.listarTicketsOperador(q, p, e);

        String html = TemplateUtil.load(getServletContext(), "/tickets.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "tickets"));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));
        html = TemplateUtil.replace(html, "{{VIEW}}", "operador");
        html = TemplateUtil.replace(html, "{{Q}}", TemplateUtil.esc(q));
        html = TemplateUtil.replace(html, "{{OPTS_PRIORIDAD}}", optsPrioridad(p));
        html = TemplateUtil.replace(html, "{{OPTS_ESTADO}}", optsEstado(e));
        html = TemplateUtil.replace(html, "{{TICKETS_ROWS}}", buildRowsOperador(tickets, u));

        html = TemplateUtil.replace(html, "{{BTN_NUEVO_TICKET}}", "");
        html = TemplateUtil.replace(html, "{{MODAL_NUEVO_TICKET}}", modalNuevoTicket(msg));

        html = TemplateUtil.replace(html, "{{MODAL_NUEVO_TICKET}}", "");

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private void renderResolver(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        if (!isTecnico(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        int id = parseInt(req.getParameter("id"), -1);
        if (id <= 0) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        ticket t = st.getById(id);
        if (t == null) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        // permisos: si está asignado a otro, no deja
        Integer asignado = t.getIdAsignado();
        if (asignado != null && asignado != u.getIdUsuario()) {
            resp.sendRedirect("TicketController?view=operador&msg=" + url("No autorizado"));
            return;
        }

        String msg = nvl(req.getParameter("msg"));

        String html = TemplateUtil.load(getServletContext(), "/ticket_resolver.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "tickets"));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));
        html = TemplateUtil.replace(html, "{{MSG}}", TemplateUtil.esc(msg));

        html = TemplateUtil.replace(html, "{{ID_TICKET}}", String.valueOf(t.getIdTicket()));
        html = TemplateUtil.replace(html, "{{ASUNTO}}", TemplateUtil.esc(nvl(t.getAsunto())));
        html = TemplateUtil.replace(html, "{{DESCRIPCION}}", TemplateUtil.esc(nvl(t.getDescripcion())));
        html = TemplateUtil.replace(html, "{{CODIGO}}", TemplateUtil.esc(nvl(t.getCodigo())));
        html = TemplateUtil.replace(html, "{{PRIORIDAD}}", TemplateUtil.esc(nvl(t.getPrioridad())));
        html = TemplateUtil.replace(html, "{{ESTADO}}", TemplateUtil.esc(nvl(t.getEstado())));

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private void renderVer(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        if (!isUsuario(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        int id = parseInt(req.getParameter("id"), -1);
        if (id <= 0) {
            resp.sendRedirect("TicketController?view=mis");
            return;
        }

        ticket t = st.getById(id);
        if (t == null || t.getIdCreador() != u.getIdUsuario()) {
            resp.sendRedirect("TicketController?view=mis&msg=" + url("No autorizado"));
            return;
        }

        String html = TemplateUtil.load(getServletContext(), "/ticket_ver.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "tickets"));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));

        html = TemplateUtil.replace(html, "{{ASUNTO}}", TemplateUtil.esc(nvl(t.getAsunto())));
        html = TemplateUtil.replace(html, "{{PRIORIDAD}}", TemplateUtil.esc(nvl(t.getPrioridad())));
        html = TemplateUtil.replace(html, "{{ESTADO}}", TemplateUtil.esc(nvl(t.getEstado())));
        html = TemplateUtil.replace(html, "{{CODIGO}}", TemplateUtil.esc(nvl(t.getCodigo())));
        html = TemplateUtil.replace(html, "{{DESCRIPCION}}", TemplateUtil.esc(nvl(t.getDescripcion())));
        html = TemplateUtil.replace(html, "{{SOLUCION}}", TemplateUtil.esc(nvl(t.getSolucion())));

        html = TemplateUtil.replace(html, "{{COMENTARIOS}}",
                buildComentarios(st.listarComentarios(id)));

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private void renderAdmin(HttpServletRequest req, HttpServletResponse resp, usuario u) throws IOException {
        if (!isAdmin(u)) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        int id = parseInt(req.getParameter("id"), -1);
        if (id <= 0) {
            resp.sendRedirect("TicketController?view=operador");
            return;
        }

        ticket t = st.getById(id);
        if (t == null) {
            resp.sendRedirect("TicketController?view=operador&msg=" + url("Ticket no existe"));
            return;
        }

        String msg = nvl(req.getParameter("msg"));

        String html = TemplateUtil.load(getServletContext(), "/ticket_admin.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "tickets"));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));
        html = TemplateUtil.replace(html, "{{MSG}}", TemplateUtil.esc(msg));

        html = TemplateUtil.replace(html, "{{ID_TICKET}}", String.valueOf(t.getIdTicket()));
        html = TemplateUtil.replace(html, "{{ASUNTO}}", TemplateUtil.esc(nvl(t.getAsunto())));
        html = TemplateUtil.replace(html, "{{PRIORIDAD}}", TemplateUtil.esc(nvl(t.getPrioridad())));
        html = TemplateUtil.replace(html, "{{ESTADO}}", TemplateUtil.esc(nvl(t.getEstado())));
        html = TemplateUtil.replace(html, "{{CODIGO}}", TemplateUtil.esc(nvl(t.getCodigo())));
        html = TemplateUtil.replace(html, "{{DESCRIPCION}}", TemplateUtil.esc(nvl(t.getDescripcion())));
        html = TemplateUtil.replace(html, "{{SOLUCION}}", TemplateUtil.esc(nvl(t.getSolucion())));

        html = TemplateUtil.replace(html, "{{COMENTARIOS}}",
                buildComentarios(st.listarComentarios(id)));

        html = TemplateUtil.replace(html, "{{OPTS_TECNICOS}}",
                buildSelectTecnicos(st.listarTecnicos(), t.getIdAsignado()));

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    // =================== HTML builders ===================
    private String btnNuevoTicket() {
        return ""
                + "<button class='btn btn-primary' data-bs-toggle='modal' data-bs-target='#modalNuevoTicket'>"
                + "<i class='bi bi-plus-lg'></i> Nuevo ticket</button>";
    }

    private String modalNuevoTicket(String msg) {
        String safeMsg = TemplateUtil.esc(nvl(msg));

        return ""
                + "<div class='modal fade' id='modalNuevoTicket' tabindex='-1' aria-hidden='true'>"
                + "  <div class='modal-dialog modal-lg modal-dialog-centered'>"
                + "    <div class='modal-content cardx'>"
                + "      <div class='modal-header border-0'>"
                + "        <div>"
                + "          <h5 class='modal-title mb-0'>Crear ticket</h5>"
                + "          <div class='small-muted'>Complete la información</div>"
                + "        </div>"
                + "        <button type='button' class='btn-close btn-close-white' data-bs-dismiss='modal' aria-label='Close'></button>"
                + "      </div>"
                + "      <form method='post' action='TicketController'>"
                + "        <input type='hidden' name='accion' value='crear'>"
                + "        <div class='modal-body pt-0'>"
                + "          <div class='row g-3'>"
                + "            <div class='col-12'>"
                + "              <label class='form-label'>Asunto</label>"
                + "              <input class='form-control' name='asunto' required>"
                + "            </div>"
                + "            <div class='col-md-4'>"
                + "              <label class='form-label'>Prioridad</label>"
                + "              <select class='form-select' name='prioridad' required>"
                + "                <option value=''>Seleccione</option>"
                + "                <option>Alta</option>"
                + "                <option>Media</option>"
                + "                <option>Baja</option>"
                + "              </select>"
                + "            </div>"
                + "            <div class='col-md-8'>"
                + "              <label class='form-label'>Código</label>"
                + "              <input class='form-control' name='codigo' placeholder='Ej: INC-2025-001' required>"
                + "            </div>"
                + "            <div class='col-12'>"
                + "              <label class='form-label'>Descripción</label>"
                + "              <textarea class='form-control' name='descripcion' rows='4' required></textarea>"
                + "            </div>"
                + "            <div class='col-12'>"
                + "              <div class='form-text text-warning'>" + safeMsg + "</div>"
                + "            </div>"
                + "          </div>"
                + "        </div>"
                + "        <div class='modal-footer border-0'>"
                + "          <button type='button' class='btn btn-outline-light' data-bs-dismiss='modal'>Cancelar</button>"
                + "          <button class='btn btn-primary'><i class='bi bi-check2-circle'></i> Crear</button>"
                + "        </div>"
                + "      </form>"
                + "    </div>"
                + "  </div>"
                + "</div>";
    }

    private String buildRowsUsuario(List<ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return "<tr><td colspan='7' class='text-center text-secondary py-4'>Sin resultados</td></tr>";
        }

        StringBuilder sb = new StringBuilder();
        for (ticket t : tickets) {
            sb.append("<tr>");
            sb.append("<td>").append(t.getIdTicket()).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getAsunto()))).append("</td>");
            sb.append("<td>").append(badge(nvl(t.getPrioridad()))).append("</td>");
            sb.append("<td>").append(badge(nvl(t.getEstado()))).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getCodigo()))).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getTecnicoAsignadoNombre()))).append("</td>");
            sb.append("<td class='text-end'>");
            sb.append("<a class='btn btn-outline-light btn-sm' href='TicketController?view=ver&id=")
                    .append(t.getIdTicket())
                    .append("'><i class='bi bi-eye'></i> Ver</a>");
            sb.append("</td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    private String buildRowsOperador(List<ticket> tickets, usuario u) {
        if (tickets == null || tickets.isEmpty()) {
            return "<tr><td colspan='7' class='text-center text-secondary py-4'>Sin resultados</td></tr>";
        }

        boolean esTecnico = isTecnico(u);
        boolean esAdmin = isAdmin(u);

        StringBuilder sb = new StringBuilder();
        for (ticket t : tickets) {
            sb.append("<tr>");
            sb.append("<td>").append(t.getIdTicket()).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getAsunto()))).append("</td>");
            sb.append("<td>").append(badge(nvl(t.getPrioridad()))).append("</td>");
            sb.append("<td>").append(badge(nvl(t.getEstado()))).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getCodigo()))).append("</td>");
            sb.append("<td>").append(TemplateUtil.esc(nvl(t.getTecnicoAsignadoNombre()))).append("</td>");

            sb.append("<td class='text-end'>");

            // TECNICO: resolver
            if (esTecnico) {
                sb.append("<a class='btn btn-outline-light btn-sm' href='TicketController?view=resolver&id=")
                        .append(t.getIdTicket())
                        .append("'><i class='bi bi-wrench-adjustable'></i> Resolver</a> ");
            }

            // ADMIN: ver + asignar (detalle admin)
            if (esAdmin) {
                sb.append("<a class='btn btn-outline-light btn-sm' href='TicketController?view=admin&id=")
                        .append(t.getIdTicket())
                        .append("'><i class='bi bi-eye'></i> Ver</a>");
            }

            if (!esTecnico && !esAdmin) {
                sb.append("<span class='text-secondary'>—</span>");
            }

            sb.append("</td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    // =================== helpers ===================
    private String optsPrioridad(String selected) {
        return ""
                + opt("Alta", selected)
                + opt("Media", selected)
                + opt("Baja", selected);
    }

    private String optsEstado(String selected) {
        return ""
                + opt("Abierto", selected)
                + opt("En proceso", selected)
                + opt("Resuelto", selected)
                + opt("Denegado", selected);
    }

    private String opt(String value, String selected) {
        String sel = value.equals(selected) ? " selected" : "";
        return "<option value='" + TemplateUtil.esc(value) + "'" + sel + ">" + TemplateUtil.esc(value) + "</option>";
    }

    private String badge(String text) {
        String t = nvl(text);
        return "<span class='badge rounded-pill text-bg-secondary'>" + TemplateUtil.esc(t) + "</span>";
    }

    private static String nvl(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private boolean isUsuario(usuario u) {
        return u != null && "USUARIO".equalsIgnoreCase(u.getRol());
    }

    private boolean isAdmin(usuario u) {
        return u != null && "ADMIN".equalsIgnoreCase(u.getRol());
    }

    private boolean isTecnico(usuario u) {
        return u != null && "TECNICO".equalsIgnoreCase(u.getRol());
    }

    private String buildComentarios(List<String> comentarios) {
        if (comentarios == null || comentarios.isEmpty()) {
            return "<div class='text-secondary'>Sin comentarios</div>";
        }
        StringBuilder sb = new StringBuilder();
        for (String c : comentarios) {
            sb.append("<div class='cardx p-2 mb-2'>")
                    .append(TemplateUtil.esc(c))
                    .append("</div>");
        }
        return sb.toString();
    }

    private String buildSelectTecnicos(List<Modelos.usuario> tecnicos, Integer selectedId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<option value=''>Sin asignar</option>");

        if (tecnicos == null) {
            return sb.toString();
        }

        for (Modelos.usuario tec : tecnicos) {
            int id = tec.getIdUsuario();
            String nombre = nvl(tec.getNombre()) + " " + nvl(tec.getApellido());
            String sel = (selectedId != null && selectedId == id) ? " selected" : "";
            sb.append("<option value='").append(id).append("'").append(sel).append(">")
                    .append(TemplateUtil.esc(nombre.trim()))
                    .append("</option>");
        }
        return sb.toString();
    }

}
