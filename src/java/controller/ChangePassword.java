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
import model.Mail;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author Oshen Sathsara <oshensathsara2003@gmail.com>
 */
@WebServlet(name = "ChangePassword", urlPatterns = {"/ChangePassword"})
public class ChangePassword extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        Gson gson = new Gson();
        JsonObject responseJson = new JsonObject();
        
        try {
            JsonObject requestJson = gson.fromJson(request.getReader(), JsonObject.class);
            String action = requestJson.get("action").getAsString();
            
            if ("sendCode".equals(action)) {
                handleSendCode(requestJson, responseJson);
            } else if ("resetPassword".equals(action)) {
                handleResetPassword(requestJson, responseJson);
            } else {
                responseJson.addProperty("status", false);
                responseJson.addProperty("message", "Invalid action");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Server error occurred");
        }
        
        response.getWriter().write(gson.toJson(responseJson));
    }
    
    private void handleSendCode(JsonObject requestJson, JsonObject responseJson) {
        String email = requestJson.get("email").getAsString();
        
        // Validate email
        if (email == null || email.trim().isEmpty()) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Email is required");
            return;
        }
        
        if (!Util.isEmailValid(email)) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Invalid email format");
            return;
        }
        
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        
        try {
            transaction = session.beginTransaction();
            
            // Check if admin exists with this email
            Criteria criteria = session.createCriteria(Admin.class);
            criteria.add(Restrictions.eq("email", email));
            Admin admin = (Admin) criteria.uniqueResult();
            
            if (admin == null) {
                responseJson.addProperty("status", false);
                responseJson.addProperty("message", "No admin account found with this email");
                return;
            }
            
            // Generate verification code
            final String verificationCode = Util.generateCode();
            
            // Update admin with verification code
            admin.setV_code(Integer.parseInt(verificationCode));
            session.update(admin);
            
            transaction.commit();
            
            // Send email in separate thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Mail.sendMail(email, "PhoneHub Password Reset", 
                            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                            "<h2 style='color: #FF6B00;'>Password Reset Request</h2>" +
                            "<p>You have requested to reset your password. Please use the verification code below:</p>" +
                            "<div style='background: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0;'>" +
                            "<h1 style='color: #FF6B00; font-size: 36px; margin: 0;'>" + verificationCode + "</h1>" +
                            "</div>" +
                            "<p>This code will expire in 10 minutes for security purposes.</p>" +
                            "<p>If you didn't request this password reset, please ignore this email.</p>" +
                            "<p style='color: #666;'>Best regards,<br>PhoneHub Team</p>" +
                            "</div>");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            
            responseJson.addProperty("status", true);
            responseJson.addProperty("message", "Verification code sent successfully");
            
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Failed to send verification code");
        } finally {
            session.close();
        }
    }
    
    private void handleResetPassword(JsonObject requestJson, JsonObject responseJson) {
        String email = requestJson.get("email").getAsString();
        String verificationCode = requestJson.get("verificationCode").getAsString();
        String newPassword = requestJson.get("newPassword").getAsString();
        
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Email is required");
            return;
        }
        
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Verification code is required");
            return;
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "New password is required");
            return;
        }
        
        if (!Util.isEmailValid(email)) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Invalid email format");
            return;
        }
        
        if (!Util.isCodeValid(verificationCode)) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Invalid verification code format");
            return;
        }
        
        if (!Util.isPasswordValid(newPassword)) {
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Password must be at least 8 characters with uppercase, lowercase, number and special character");
            return;
        }
        
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        
        try {
            transaction = session.beginTransaction();
            
            // Find admin with email and verification code
            Criteria criteria = session.createCriteria(Admin.class);
            criteria.add(Restrictions.eq("email", email));
            criteria.add(Restrictions.eq("v_code", Integer.parseInt(verificationCode)));
            Admin admin = (Admin) criteria.uniqueResult();
            
            if (admin == null) {
                responseJson.addProperty("status", false);
                responseJson.addProperty("message", "Invalid verification code or email");
                return;
            }
            
            // Update password and clear verification code
            admin.setPassword(newPassword);
            admin.setV_code(0); // Clear the verification code
            session.update(admin);
            
            transaction.commit();
            
            responseJson.addProperty("status", true);
            responseJson.addProperty("message", "Password reset successfully");
            
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            responseJson.addProperty("status", false);
            responseJson.addProperty("message", "Failed to reset password");
        } finally {
            session.close();
        }
    }
}