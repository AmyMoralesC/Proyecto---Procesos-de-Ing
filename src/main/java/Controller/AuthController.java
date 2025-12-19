package Controller;

import Modelos.usuario;
import Servicio.servicioUsuario;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/AuthController")
public class AuthController extends HttpServlet {

    private final servicioUsuario su = new servicioUsuario();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String accion = req.getParameter("accion");

        if ("logout".equals(accion)) {
            req.getSession().invalidate();
            resp.sendRedirect("login.html");
            return;
        }

        if ("login".equals(accion)) {

            // asegura admin inicial
            su.ensureAdminInicial();

            String email = req.getParameter("email");
            String password = req.getParameter("password");

            usuario u = su.login(email, password);

            if (u == null) {
                resp.sendRedirect("login.html?err=1");
                return;
            }

            // guardar sesión
            HttpSession ses = req.getSession(true);
            ses.setAttribute("user", u);

            String rol = u.getRol();

            // ==========================
            // REDIRECCIÓN POR ROL
            // ==========================
            if ("USUARIO".equals(rol)) {
                resp.sendRedirect("TicketController?view=mis");
                return;
            }

            if ("TECNICO".equals(rol) || "ADMIN".equals(rol)) {
                resp.sendRedirect("DashboardController");
                return;
            }

            // fallback de seguridad
            resp.sendRedirect("login.html?err=2");
        }
    }
}
