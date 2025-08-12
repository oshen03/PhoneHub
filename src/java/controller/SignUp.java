package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Mail;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SignUp", urlPatterns = {"/SignUp"})
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();
        JsonObject user = gson.fromJson(request.getReader(), JsonObject.class);

        String firstName = user.get("firstName").getAsString();
        String lastName = user.get("lastName").getAsString();
        final String email = user.get("email").getAsString();
        String password = user.get("password").getAsString();
        String confirmPassword = user.get("confirmPassword").getAsString();

        // Validations
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        if (firstName.isEmpty()) {
            responseObject.addProperty("message", "First Name cannot be empty");
        } else if (lastName.isEmpty()) {
            responseObject.addProperty("message", "Last Name cannot be empty");
        } else if (email.isEmpty()) {
            responseObject.addProperty("message", "Email cannot be empty");
        } else if (!Util.isEmailValid(email)) {
            responseObject.addProperty("message", "Please enter a valid email");
        } else if (password.isEmpty()) {
            responseObject.addProperty("message", "Password cannot be empty");
        } else if (confirmPassword.isEmpty()) {
            responseObject.addProperty("message", "Please confirm your password by retyping");
        } else if (!Util.isPasswordValid(password)) {
            responseObject.addProperty("message", "Please enter a valid password");
        } else if (!password.equals(confirmPassword)) {
            responseObject.addProperty("message", "Password and confirm password do not match");
        } else {
            // Save user
            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            try {
                Criteria criteria = s.createCriteria(User.class);
                criteria.add(Restrictions.eq("email", email));

                if (!criteria.list().isEmpty()) {
                    responseObject.addProperty("message", "This email already exists");
                } else {
                    User u = new User();

                    u.setFirst_name(firstName);
                    u.setLast_name(lastName);
                    u.setEmail(email);
                    u.setPassword(password);

                    // Generate verification code
                    final String verificationCode = Util.generateCode();
                    u.setVerification(verificationCode);
                    u.setCreated_at(new Date());

                    s.beginTransaction();
                    s.save(u);
                    s.getTransaction().commit();

                    // Send email in background thread
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Mail.sendMail(email, "PhoneHub Account Verification",
                                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                                    + "<h2 style='color: #FF6B00;'>Password Reset Request</h2>"
                                    + "<p>You have requested to reset your password. Please use the verification code below:</p>"
                                    + "<div style='background: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;'>"
                                    + "<h1 style='color: #FF6B00; font-size: 36px; margin: 0;'>" + verificationCode + "</h1>"
                                    + "</div>"
                                    + "<p>This code will expire in 10 minutes for security purposes.</p>"
                                    + "<p>If you didn't request this password reset, please ignore this email.</p>"
                                    + "<p style='color: #666;'>Best regards,<br>PhoneHub Team</p>"
                                    + "</div>");
                        }
                    }).start();

                    // Create session (session management)
                    HttpSession ses = request.getSession();
                    ses.setAttribute("email", email);

                    responseObject.addProperty("status", true);
                    responseObject.addProperty("message", "Registration successful");
                }
            } catch (Exception e) {
                if (s.getTransaction() != null) {
                    s.getTransaction().rollback();
                }
                responseObject.addProperty("message", "Registration failed. Please try again.");
                e.printStackTrace();
            } finally {
                s.close();
            }
        }

        String responseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(responseText);
    }
}
