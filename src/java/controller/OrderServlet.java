package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Orders;
import hibernate.OrderItems;
import hibernate.OrderStatus;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "OrderServlet", urlPatterns = {"/OrderServlet"})
public class OrderServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "list";
        switch (action) {
            case "list":
                listOrders(request, response);
                break;
            case "view":
                viewOrder(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "updateStatus";
        switch (action) {
            case "updateStatus":
                updateOrderStatus(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void listOrders(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            Criteria c = s.createCriteria(Orders.class);
            c.addOrder(Order.desc("id"));
            List<Orders> orderList = c.list();
            JsonArray arr = new JsonArray();
            for (Orders order : orderList) {
                JsonObject o = new JsonObject();
                o.addProperty("id", order.getId());
                User user = order.getUser();
                o.addProperty("customerName", user != null ? user.getFirst_name() + " " + user.getLast_name() : "-");
                o.addProperty("date", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
                // Calculate total amount
                double amount = 0;
                Criteria ci = s.createCriteria(OrderItems.class);
                ci.add(Restrictions.eq("orders", order));
                List<OrderItems> items = ci.list();
                for (OrderItems item : items) {
                    amount += item.getQty() * (item.getProduct() != null ? item.getProduct().getPrice() : 0);
                }
                o.addProperty("amount", amount);
                // Get status
                String status = "Processing";
                if (!items.isEmpty() && items.get(0).getOrderStatus() != null) {
                    status = items.get(0).getOrderStatus().getValue();
                }
                o.addProperty("status", status);
                arr.add(o);
            }
            responseObject.add("orderList", arr);
            responseObject.addProperty("status", true);
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading orders: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void viewOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String idStr = request.getParameter("id");
        if (idStr == null) {
            responseObject.addProperty("message", "Order ID required");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseObject));
            return;
        }
        int id = Integer.parseInt(idStr);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            Orders order = (Orders) s.get(Orders.class, id);
            if (order != null) {
                JsonObject o = new JsonObject();
                o.addProperty("id", order.getId());
                User user = order.getUser();
                o.addProperty("customerName", user != null ? user.getFirst_name() + " " + user.getLast_name() : "-");
                o.addProperty("date", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");
                // Items
                Criteria ci = s.createCriteria(OrderItems.class);
                ci.add(Restrictions.eq("orders", order));
                List<OrderItems> items = ci.list();
                double amount = 0;
                JsonArray itemArr = new JsonArray();
                String status = "Processing";
                for (OrderItems item : items) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("productTitle", item.getProduct() != null ? item.getProduct().getTitle() : "");
                    itemObj.addProperty("qty", item.getQty());
                    itemObj.addProperty("price", item.getProduct() != null ? item.getProduct().getPrice() : 0);
                    itemArr.add(itemObj);
                    amount += item.getQty() * (item.getProduct() != null ? item.getProduct().getPrice() : 0);
                    if (item.getOrderStatus() != null) status = item.getOrderStatus().getValue();
                }
                o.add("items", itemArr);
                o.addProperty("amount", amount);
                o.addProperty("status", status);
                responseObject.add("order", o);
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Order not found");
            }
        } catch (Exception e) {
            responseObject.addProperty("message", "Error: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void updateOrderStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tx = null;
        try {
            BufferedReader reader = request.getReader();
            JsonObject input = gson.fromJson(reader, JsonObject.class);
            int id = input.get("id").getAsInt();
            String statusValue = input.get("status").getAsString();
            tx = s.beginTransaction();
            Orders order = (Orders) s.get(Orders.class, id);
            if (order != null) {
                // Update all order items' status
                Criteria ci = s.createCriteria(OrderItems.class);
                ci.add(Restrictions.eq("orders", order));
                List<OrderItems> items = ci.list();
                // Find or create OrderStatus
                Criteria cs = s.createCriteria(OrderStatus.class);
                cs.add(Restrictions.eq("value", statusValue));
                OrderStatus status = (OrderStatus) cs.uniqueResult();
                if (status == null) {
                    status = new OrderStatus();
                    status.setValue(statusValue);
                    s.save(status);
                }
                for (OrderItems item : items) {
                    item.setOrderStatus(status);
                    s.update(item);
                }
                tx.commit();
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Order not found");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error updating order: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
