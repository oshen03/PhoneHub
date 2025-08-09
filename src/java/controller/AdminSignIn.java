package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Admin;
import hibernate.HibernateUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "AdminSignIn", urlPatterns = {"/AdminSignIn"})
public class AdminSignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Gson gson = new Gson();
        JsonObject credentials = gson.fromJson(request.getReader(), JsonObject.class);
        JsonObject responseObj = new JsonObject();
        
        String email = credentials.get("email").getAsString();
        String password = credentials.get("password").getAsString();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = null;
        
        try {
            session = sf.openSession();
            Criteria criteria = session.createCriteria(Admin.class);
            criteria.add(Restrictions.eq("email", email));
            criteria.add(Restrictions.eq("password", password));
            
            Admin admin = (Admin) criteria.uniqueResult();
            
            if (admin != null) {
                HttpSession httpSession = request.getSession();
                httpSession.setAttribute("admin", admin);
                responseObj.addProperty("status", true);
            } else {
                responseObj.addProperty("status", false);
                responseObj.addProperty("message", "Invalid credentials");
            }
        } catch (Exception e) {
            responseObj.addProperty("status", false);
            responseObj.addProperty("message", "Database error occurred");
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObj));
    }
}