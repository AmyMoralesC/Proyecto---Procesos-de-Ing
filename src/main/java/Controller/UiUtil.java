package Controller;

import Modelos.usuario;

public class UiUtil {

    public static String sidebar(usuario u, String active) {
        String rol = u.getRol();

        String actDash = "dashboard".equals(active) ? "active" : "";
        String actTickets = "tickets".equals(active) ? "active" : "";
        String actUsuarios = "usuarios".equals(active) ? "active" : "";
        String actPerfil = "perfil".equals(active) ? "active" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("<aside class='sidebar'>")
                .append("<div class='brand'><span class='dot'></span><div><h1>Gestor de Tickets</h1><p>Soporte técnico</p></div></div>");

        sb.append("<a class='navlink ").append(actDash)
                .append("' href='DashboardController'><i class='bi bi-house'></i> Inicio</a>");

        // link tickets depende del rol
        String viewTickets = "USUARIO".equals(rol) ? "mis" : "operador";
        sb.append("<a class='navlink ").append(actTickets)
                .append("' href='TicketController?view=").append(viewTickets)
                .append("'><i class='bi bi-inboxes'></i> Tickets</a>");

        if ("ADMIN".equals(rol)) {
            sb.append("<a class='navlink ").append(actUsuarios)
                    .append("' href='UsuarioController'><i class='bi bi-people'></i> Usuarios</a>");
        }

        sb.append("<hr style='border-color: rgba(148,163,184,.22)'/>");

        sb.append("<a class='navlink ").append(actPerfil)
                .append("' href='PerfilController'><i class='bi bi-person'></i> Perfil</a>");

        sb.append("<form method='post' action='AuthController' style='margin-top:10px;'>")
                .append("<input type='hidden' name='accion' value='logout'>")
                .append("<button class='navlink w-100 text-start' type='submit' style='background:transparent;'>")
                .append("<i class='bi bi-box-arrow-right'></i> Cerrar sesión</button></form>")
                .append("</aside>");

        return sb.toString();
    }
}
