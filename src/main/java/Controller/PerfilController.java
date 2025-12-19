package Controller;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import Modelos.usuario;
import Servicio.servicioUsuario;

@WebServlet("/PerfilController")
public class PerfilController extends HttpServlet {

    private final servicioUsuario su = new servicioUsuario();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario u = (usuario) req.getSession().getAttribute("user");

        String html = TemplateUtil.load(getServletContext(), "/profile.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(u, "perfil"));
        html = TemplateUtil.replace(html, "{{NOMBRE}}", TemplateUtil.esc(u.getNombre()));
        html = TemplateUtil.replace(html, "{{APELLIDO}}", TemplateUtil.esc(u.getApellido()));
        html = TemplateUtil.replace(html, "{{EDAD}}", String.valueOf(u.getEdad()));
        html = TemplateUtil.replace(html, "{{EMAIL}}", TemplateUtil.esc(u.getEmail()));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(u.getRol()));
        html = TemplateUtil.replace(html, "{{MSG}}", "");

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        usuario u = (usuario) req.getSession().getAttribute("user");

        String nombre = req.getParameter("nombre");
        String apellido = req.getParameter("apellido");
        int edad = parseInt(req.getParameter("edad"));
        String email = req.getParameter("email");

        String np1 = req.getParameter("newPassword");
        String np2 = req.getParameter("newPassword2");

        String msg = su.updatePerfil(u.getIdUsuario(), nombre, apellido, edad, email, np1, np2);

        // refrescar user en sesión
        usuario actualizado = su.buscarPorId(u.getIdUsuario());
        req.getSession().setAttribute("user", actualizado);

        String html = TemplateUtil.load(getServletContext(), "/profile.html");
        html = TemplateUtil.replace(html, "{{SIDEBAR}}", UiUtil.sidebar(actualizado, "perfil"));
        html = TemplateUtil.replace(html, "{{NOMBRE}}", TemplateUtil.esc(actualizado.getNombre()));
        html = TemplateUtil.replace(html, "{{APELLIDO}}", TemplateUtil.esc(actualizado.getApellido()));
        html = TemplateUtil.replace(html, "{{EDAD}}", String.valueOf(actualizado.getEdad()));
        html = TemplateUtil.replace(html, "{{EMAIL}}", TemplateUtil.esc(actualizado.getEmail()));
        html = TemplateUtil.replace(html, "{{ROL}}", TemplateUtil.esc(actualizado.getRol()));
        html = TemplateUtil.replace(html, "{{MSG}}", TemplateUtil.esc(msg));

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(html);
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s == null ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
