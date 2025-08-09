package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "CustomerServlet", urlPatterns = {"/CustomerServlet"})
public class CustomerServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "list";
        switch (action) {
            case "list":
                listCustomers(request, response);
                break;
            case "view":
                viewCustomer(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "block";
        switch (action) {
            case "block":
                blockCustomer(request, response);
                break;
            case "unblock":
                unblockCustomer(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void listCustomers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            Criteria c = s.createCriteria(User.class);
            c.addOrder(Order.desc("id"));
            List<User> customerList = c.list();
            JsonArray arr = new JsonArray();
            for (User user : customerList) {
                JsonObject o = new JsonObject();
                o.addProperty("id", user.getId());
                o.addProperty("name", user.getFirst_name() + " " + user.getLast_name());
                o.addProperty("email", user.getEmail());
                // For status, let's use verification: 'blocked' or 'active'
                String status = "active";
                if (user.getVerification() != null && user.getVerification().equalsIgnoreCase("blocked")) {
                    status = "blocked";
                }
                o.addProperty("status", status);
                arr.add(o);
            }
            responseObject.add("customerList", arr);
            responseObject.addProperty("status", true);
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading customers: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void viewCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String idStr = request.getParameter("id");
        if (idStr == null) {
            responseObject.addProperty("message", "Customer ID required");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseObject));
            return;
        }
        int id = Integer.parseInt(idStr);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            User user = (User) s.get(User.class, id);
            if (user != null) {
                JsonObject o = new JsonObject();
                o.addProperty("id", user.getId());
                o.addProperty("name", user.getFirst_name() + " " + user.getLast_name());
                o.addProperty("email", user.getEmail());
                String status = "active";
                if (user.getVerification() != null && user.getVerification().equalsIgnoreCase("blocked")) {
                    status = "blocked";
                }
                o.addProperty("status", status);
                o.addProperty("registered", user.getCreated_at() != null ? user.getCreated_at().toString() : "");
                // Order count (optional)
                Criteria co = s.createCriteria(hibernate.Orders.class);
                co.add(Restrictions.eq("user", user));
                o.addProperty("orderCount", co.list().size());
                responseObject.add("customer", o);
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Customer not found");
            }
        } catch (Exception e) {
            responseObject.addProperty("message", "Error: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void blockCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        updateCustomerVerification(request, response, "blocked");
    }

    private void unblockCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        updateCustomerVerification(request, response, "active");
    }

    private void updateCustomerVerification(HttpServletRequest request, HttpServletResponse response, String newStatus) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String idStr = request.getParameter("id");
        if (idStr == null) {
            responseObject.addProperty("message", "Customer ID required");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseObject));
            return;
        }
        int id = Integer.parseInt(idStr);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tx = null;
        try {
            tx = s.beginTransaction();
            User user = (User) s.get(User.class, id);
            if (user != null) {
                user.setVerification(newStatus);
                s.update(user);
                tx.commit();
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Customer not found");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error updating customer: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
