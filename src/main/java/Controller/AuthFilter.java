package Controller;

import Modelos.usuario;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class AuthFilter implements Filter {

    // Rutas públicas (no requieren sesión)
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login.html",
            "/AuthController"
    );

    // Carpetas públicas (assets, css, js, etc.)
    private static final String[] PUBLIC_PREFIXES = {
        "/assets/",
        "/META-INF/",
        "/WEB-INF/" // normalmente no se accede directo, pero igual lo dejamos
    };

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();          // /GeneradorDeTickets2
        String uri = req.getRequestURI();           // /GeneradorDeTickets2/TicketController
        String path = uri.substring(ctx.length());  // /TicketController

        // 1) Si es recurso público -> pasar sin validar
        if (isPublic(path)) {
            chain.doFilter(req, resp);
            return;
        }

        // 2) Validar sesión
        HttpSession session = req.getSession(false);
        usuario u = (session != null) ? (usuario) session.getAttribute("user") : null;

        if (u == null) {
            resp.sendRedirect("login.html");
            return;
        }

        // 3) Validación por rol (opcional pero recomendado)
        String rol = (u.getRol() == null) ? "" : u.getRol().trim().toUpperCase();

        if (path.startsWith("/UsuarioController") && !rol.equals("ADMIN")) {
            resp.sendRedirect("index.html");
            return;
        }

        // 4) Si todo ok -> continuar
        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
    }

    private boolean isPublic(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) {
                return true;
            }
        }

        return false;
    }
}
