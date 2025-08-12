package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Admin;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "AdminSettingsServlet", urlPatterns = {"/AdminSettingsServlet"})
public class AdminSettingsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "profile";
        switch (action) {
            case "profile":
                getProfile(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "update";
        switch (action) {
            case "update":
                updateProfile(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void getProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            
            Admin admin = (Admin) s.get(Admin.class, 1);
            if (admin != null) {
                JsonObject adminObj = new JsonObject();
                adminObj.addProperty("name", admin.getFirst_name() + " " + admin.getLast_name());
                adminObj.addProperty("email", admin.getEmail());
                
                responseObject.add("admin", adminObj);
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Admin profile not found");
            }
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading profile: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tx = null;
        try {
            BufferedReader reader = request.getReader();
            JsonObject input = gson.fromJson(reader, JsonObject.class);
            String name = input.get("name").getAsString();
            String email = input.get("email").getAsString();
            String password = input.has("password") ? input.get("password").getAsString() : null;
            
           
            Admin admin = (Admin) s.get(Admin.class, 1);
            if (admin != null) {
                tx = s.beginTransaction();
                // Split name into first and last name
                String[] nameParts = name.split(" ", 2);
                admin.setFirst_name(nameParts[0]);
                admin.setLast_name(nameParts.length > 1 ? nameParts[1] : "");
                admin.setEmail(email);
                if (password != null && !password.isEmpty()) {
                    admin.setPassword(password);
                }
                s.update(admin);
                tx.commit();
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Admin profile not found");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error updating profile: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
