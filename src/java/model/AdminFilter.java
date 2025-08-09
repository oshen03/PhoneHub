package model;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/*
 * @author Oshen Sathsara <oshensathsara2003@gmail.com>
 */
@WebFilter(urlPatterns = {"/admin.html", "/DashboardServlet", "/ProductServlet", "/OrderServlet", "/CustomerServlet", "/ReportServlet", "/AdminSettingsServlet"})
public class AdminFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get session without creating a new one
        HttpSession session = httpRequest.getSession(false);
        
        // Check if admin is logged in
        if (session == null || session.getAttribute("admin") == null) {
            // Admin not logged in - redirect to login page
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin-login.html");
            return;
        }
        
        // Admin is logged in - continue with the request
        chain.doFilter(request, response);
    }
}