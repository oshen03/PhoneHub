package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.*;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/DashboardServlet"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if ("summary".equals(action)) {
            getDashboardSummary(request, response);
        } else if ("recentOrders".equals(action)) {
            getRecentOrders(request, response);
        } else if ("recentProducts".equals(action)) {
            getRecentProducts(request, response);
        } else {
            // Default: get all dashboard data
            getAllDashboardData(request, response);
        }
    }

    private void getAllDashboardData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            // Get summary statistics
            JsonObject summary = getSummaryStats(s);
            responseObject.add("summary", summary);
            
            // Get recent orders
            JsonObject recentOrders = getRecentOrdersData(s);
            responseObject.add("recentOrders", recentOrders);
            
            // Get recent products
            JsonObject recentProducts = getRecentProductsData(s);
            responseObject.add("recentProducts", recentProducts);
            
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Dashboard data loaded successfully");
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading dashboard data: " + e.getMessage());
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getDashboardSummary(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            JsonObject summary = getSummaryStats(s);
            responseObject.add("summary", summary);
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Summary loaded successfully");
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading summary: " + e.getMessage());
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getRecentOrders(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            JsonObject recentOrders = getRecentOrdersData(s);
            responseObject.add("recentOrders", recentOrders);
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Recent orders loaded successfully");
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading recent orders: " + e.getMessage());
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getRecentProducts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            JsonObject recentProducts = getRecentProductsData(s);
            responseObject.add("recentProducts", recentProducts);
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Recent products loaded successfully");
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading recent products: " + e.getMessage());
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private JsonObject getSummaryStats(Session s) {
        JsonObject summary = new JsonObject();
        
        // Total Orders
        Criteria orderCriteria = s.createCriteria(Orders.class);
        long totalOrders = orderCriteria.list().size();
        summary.addProperty("totalOrders", totalOrders);
        
        // Total Products
        Criteria productCriteria = s.createCriteria(Product.class);
        long totalProducts = productCriteria.list().size();
        summary.addProperty("totalProducts", totalProducts);
        
        // Total Customers
        Criteria userCriteria = s.createCriteria(User.class);
        long totalCustomers = userCriteria.list().size();
        summary.addProperty("totalCustomers", totalCustomers);
        
        // Calculate total revenue from orders
        double totalRevenue = 0.0;
        List<Orders> orders = orderCriteria.list();
        for (Orders order : orders) {
            // Calculate order total from order items
            Criteria orderItemsCriteria = s.createCriteria(OrderItems.class);
            orderItemsCriteria.add(Restrictions.eq("orders", order));
            List<OrderItems> items = orderItemsCriteria.list();
            
            for (OrderItems item : items) {
                totalRevenue += item.getProduct().getPrice() * item.getQty();
            }
        }
        summary.addProperty("totalRevenue", totalRevenue);
        
        // Low stock products (qty <= 5)
        Criteria lowStockCriteria = s.createCriteria(Product.class);
        lowStockCriteria.add(Restrictions.le("qty", 5));
        long lowStockCount = lowStockCriteria.list().size();
        summary.addProperty("lowStockCount", lowStockCount);
        
        // Out of stock products (qty = 0)
        Criteria outOfStockCriteria = s.createCriteria(Product.class);
        outOfStockCriteria.add(Restrictions.eq("qty", 0));
        long outOfStockCount = outOfStockCriteria.list().size();
        summary.addProperty("outOfStockCount", outOfStockCount);
        
        return summary;
    }

    private JsonObject getRecentOrdersData(Session s) {
        JsonObject recentOrders = new JsonObject();
        
        // Get recent orders with user details
        Criteria orderCriteria = s.createCriteria(Orders.class);
        orderCriteria.addOrder(Order.desc("createdAt"));
        orderCriteria.setFirstResult(0);
        orderCriteria.setMaxResults(5);
        
        List<Orders> orders = orderCriteria.list();
        recentOrders.add("orders", new Gson().toJsonTree(orders));
        
        return recentOrders;
    }

    private JsonObject getRecentProductsData(Session s) {
        JsonObject recentProducts = new JsonObject();
        
        // Get recent products
        Criteria productCriteria = s.createCriteria(Product.class);
        productCriteria.addOrder(Order.desc("created_at"));
        productCriteria.setFirstResult(0);
        productCriteria.setMaxResults(5);
        
        List<Product> products = productCriteria.list();
        recentProducts.add("products", new Gson().toJsonTree(products));
        
        return recentProducts;
    }
}
