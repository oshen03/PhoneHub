package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@WebServlet(name = "AdminSessionCheck", urlPatterns = {"/AdminSessionCheck"})
public class AdminSessionCheck extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        JsonObject responseObj = new JsonObject();
        Gson gson = new Gson();
        
        // Get session without creating a new one
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("admin") != null) {
            responseObj.addProperty("loggedIn", true);
            responseObj.addProperty("message", "Admin session active");
        } else {
            responseObj.addProperty("loggedIn", false);
            responseObj.addProperty("message", "No active admin session");
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObj));
    }
}