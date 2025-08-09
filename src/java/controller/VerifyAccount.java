package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import java.io.PrintWriter;
import javax.json.Json;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "VerifyAccount", urlPatterns = {"/VerifyAccount"})
public class VerifyAccount extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        HttpSession ses = request.getSession();

        if (ses.getAttribute("email") == null) {
//            responseObject.addProperty("message", "Email not found");
            responseObject.addProperty("message", "1");
        } else {

            String email = ses.getAttribute("email").toString();

            JsonObject verification = gson.fromJson(request.getReader(), JsonObject.class);

            String verificationCode = verification.get("verificationCode").getAsString();
//            System.out.println(verificationCode);

            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            Criteria criteria = s.createCriteria(User.class);

            Criterion crt1 = Restrictions.eq("email", email);
            Criterion crt2 = Restrictions.eq("verification", verificationCode);

            criteria.add(crt1);
            criteria.add(crt2);

            if (criteria.list().isEmpty()) {
                responseObject.addProperty("message", "Invalid verification code");
            } else {
                User user = (User) criteria.list().get(0);
                user.setVerification("verified");

                s.update(user);
                s.beginTransaction().commit();
                s.close();
                
                //store user in session
                ses.setAttribute("user", user);

                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "verification successfully");
            }
        }

        String responseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(responseText);
        
    }

}
