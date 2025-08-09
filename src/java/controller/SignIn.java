/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author VICTUS
 */
@WebServlet(name = "SignIn", urlPatterns = {"/SignIn"})
public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();
        JsonObject signIn = gson.fromJson(request.getReader(), JsonObject.class);

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        String email = signIn.get("email").getAsString();
        String password = signIn.get("password").getAsString();

        if (email.isEmpty()) {
            responseObject.addProperty("message", "Email Can not be empty");
        } else if (!Util.isEmailValid(email)) {
            responseObject.addProperty("message", "Please Enter valid Email");
        } else if (password.isEmpty()) {
            responseObject.addProperty("message", "Password Can not be empty");
        } else {
            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            Criteria criteria = s.createCriteria(User.class);

            Criterion crt1 = Restrictions.eq("email", email);
            Criterion crt2 = Restrictions.eq("password", password);

            criteria.add(crt1);
            criteria.add(crt2);

            if (criteria.list().isEmpty()) {
                responseObject.addProperty("message", "Invalid credentials");
            } else {

                User u = (User) criteria.list().get(0);
                responseObject.addProperty("status", true);

                HttpSession ses = request.getSession();

                if (!u.getVerification().equals("verified")) {//not verified
                    //create a Session (session management)
                    ses.setAttribute("email", email);

                    responseObject.addProperty("message", "1");

                } else { //verified

                    ses.setAttribute("user", u);
                    responseObject.addProperty("message", "2");

                }

            }

            s.close();
        }
        String ressponseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(ressponseText);
    }
        //filter ekak hadala wade kr ganna puluwn
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        Gson gson = new Gson();
//
//        JsonObject responseObject = new JsonObject();
////        responseObject.addProperty("status", false);
//
//        if (request.getSession().getAttribute("email") != null || request.getSession().getAttribute("user") != null) {
//
//            Session s = HibernateUtil.getSessionFactory().openSession();
//            Criteria c = s.createCriteria(User.class);
//
//            responseObject.addProperty("message", "1");
//        } else {
//            responseObject.addProperty("message", "2");
//
//        }
//
//        String toJson = gson.toJson(responseObject);
//        response.setContentType("application/json");
//        response.getWriter().write(toJson);
//
//    }
//
}
