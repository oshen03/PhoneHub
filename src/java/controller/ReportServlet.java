package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.*;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "ReportServlet", urlPatterns = {"/ReportServlet"})
public class ReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "summary";
        
        switch (action) {
            case "summary":
                getSummaryReports(response);
                break;
            case "sales":
                getSalesReport(response);
                break;
            case "inventory":
                getInventoryReport(response);
                break;
            case "customers":
                getCustomerReport(response);
                break;
            case "brands":
                getBrandReport(response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void getSummaryReports(HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonObject salesSummary = new JsonObject();
            
            // Total Sales Revenue
            String revenueQuery = "SELECT SUM(oi.qty * p.price) FROM OrderItems oi JOIN oi.product p";
            Query query = session.createQuery(revenueQuery);
            Double totalSales = (Double) query.uniqueResult();
            salesSummary.addProperty("totalSales", totalSales != null ? totalSales : 0.0);
            
            // Total Orders
            String orderCountQuery = "SELECT COUNT(*) FROM Orders";
            Long totalOrders = (Long) session.createQuery(orderCountQuery).uniqueResult();
            salesSummary.addProperty("totalOrders", totalOrders != null ? totalOrders : 0);
            
            // Total Customers
            String customerCountQuery = "SELECT COUNT(*) FROM User";
            Long totalCustomers = (Long) session.createQuery(customerCountQuery).uniqueResult();
            salesSummary.addProperty("totalCustomers", totalCustomers != null ? totalCustomers : 0);
            
            // Average Order Value
            if (totalOrders > 0 && totalSales != null) {
                double avgOrderValue = totalSales / totalOrders;
                salesSummary.addProperty("avgOrderValue", avgOrderValue);
            } else {
                salesSummary.addProperty("avgOrderValue", 0.0);
            }
            
            responseObject.add("salesSummary", salesSummary);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating summary report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getSalesReport(HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonArray salesData = new JsonArray();
            
            // Monthly Sales Report
            String monthlySalesQuery = 
                "SELECT MONTH(o.createdAt) as month, YEAR(o.createdAt) as year, " +
                "COUNT(DISTINCT o.id) as orderCount, SUM(oi.qty * p.price) as revenue " +
                "FROM Orders o, OrderItems oi JOIN oi.product p " +
                "WHERE o.id = oi.orders.id " +
                "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
                "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC";
            
            List<Object[]> results = session.createQuery(monthlySalesQuery).list();
            
            for (Object[] row : results) {
                JsonObject monthlyData = new JsonObject();
                monthlyData.addProperty("month", (Integer) row[0]);
                monthlyData.addProperty("year", (Integer) row[1]);
                monthlyData.addProperty("orderCount", ((Long) row[2]).intValue());
                monthlyData.addProperty("revenue", row[3] != null ? ((Double) row[3]) : 0.0);
                salesData.add(monthlyData);
            }
            
            responseObject.add("monthlySales", salesData);
            
            // Top Selling Products
            JsonArray topProducts = new JsonArray();
            String topProductsQuery = 
                "SELECT p.title, p.id, SUM(oi.qty) as totalSold, SUM(oi.qty * p.price) as revenue " +
                "FROM OrderItems oi JOIN oi.product p " +
                "GROUP BY p.id, p.title " +
                "ORDER BY totalSold DESC";
            
            List<Object[]> topProductResults = session.createQuery(topProductsQuery).setMaxResults(10).list();
            
            for (Object[] row : topProductResults) {
                JsonObject productData = new JsonObject();
                productData.addProperty("title", (String) row[0]);
                productData.addProperty("id", (Integer) row[1]);
                productData.addProperty("totalSold", ((Long) row[2]).intValue());
                productData.addProperty("revenue", row[3] != null ? ((Double) row[3]) : 0.0);
                topProducts.add(productData);
            }
            
            responseObject.add("topProducts", topProducts);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating sales report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getInventoryReport(HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonArray inventoryData = new JsonArray();
            
            // Low Stock Products
            String lowStockQuery = 
                "SELECT p.id, p.title, p.qty, p.price, b.name as brandName " +
                "FROM Product p LEFT JOIN p.model m LEFT JOIN m.brand b " +
                "WHERE p.qty <= 10 " +
                "ORDER BY p.qty ASC";
            
            List<Object[]> lowStockResults = session.createQuery(lowStockQuery).list();
            
            for (Object[] row : lowStockResults) {
                JsonObject productData = new JsonObject();
                productData.addProperty("id", (Integer) row[0]);
                productData.addProperty("title", (String) row[1]);
                productData.addProperty("qty", (Integer) row[2]);
                productData.addProperty("price", (Double) row[3]);
                productData.addProperty("brandName", row[4] != null ? (String) row[4] : "N/A");
                inventoryData.add(productData);
            }
            
            responseObject.add("lowStockProducts", inventoryData);
            
            // Stock Value by Brand
            JsonArray stockByBrand = new JsonArray();
            String stockValueQuery = 
                "SELECT b.name, COUNT(p.id) as productCount, SUM(p.qty * p.price) as stockValue " +
                "FROM Product p LEFT JOIN p.model m LEFT JOIN m.brand b " +
                "GROUP BY b.id, b.name " +
                "ORDER BY stockValue DESC";
            
            List<Object[]> stockResults = session.createQuery(stockValueQuery).list();
            
            for (Object[] row : stockResults) {
                JsonObject brandData = new JsonObject();
                brandData.addProperty("brandName", row[0] != null ? (String) row[0] : "Unknown");
                brandData.addProperty("productCount", ((Long) row[1]).intValue());
                brandData.addProperty("stockValue", row[2] != null ? ((Double) row[2]) : 0.0);
                stockByBrand.add(brandData);
            }
            
            responseObject.add("stockByBrand", stockByBrand);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating inventory report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getCustomerReport(HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonArray customerData = new JsonArray();
            
            // Top Customers by Order Count
            String topCustomersQuery = 
                "SELECT u.id, u.first_name, u.last_name, u.email, COUNT(o.id) as orderCount, " +
                "SUM(oi.qty * p.price) as totalSpent " +
                "FROM User u, Orders o, OrderItems oi LEFT JOIN oi.product p " +
                "WHERE u.id = o.user.id AND o.id = oi.orders.id " +
                "GROUP BY u.id, u.first_name, u.last_name, u.email " +
                "HAVING COUNT(o.id) > 0 " +
                "ORDER BY orderCount DESC";
            
            List<Object[]> customerResults = session.createQuery(topCustomersQuery).setMaxResults(20).list();
            
            for (Object[] row : customerResults) {
                JsonObject customerObj = new JsonObject();
                customerObj.addProperty("id", (Integer) row[0]);
                customerObj.addProperty("firstName", (String) row[1]);
                customerObj.addProperty("lastName", (String) row[2]);
                customerObj.addProperty("email", (String) row[3]);
                customerObj.addProperty("orderCount", ((Long) row[4]).intValue());
                customerObj.addProperty("totalSpent", row[5] != null ? ((Double) row[5]) : 0.0);
                customerData.add(customerObj);
            }
            
            responseObject.add("topCustomers", customerData);
            
            // Customer Registration Trends
            JsonArray registrationTrends = new JsonArray();
            String registrationQuery = 
                "SELECT MONTH(u.created_at) as month, YEAR(u.created_at) as year, COUNT(u.id) as newCustomers " +
                "FROM User u " +
                "GROUP BY YEAR(u.created_at), MONTH(u.created_at) " +
                "ORDER BY YEAR(u.created_at) DESC, MONTH(u.created_at) DESC";
            
            List<Object[]> registrationResults = session.createQuery(registrationQuery).setMaxResults(12).list();
            
            for (Object[] row : registrationResults) {
                JsonObject trendData = new JsonObject();
                trendData.addProperty("month", (Integer) row[0]);
                trendData.addProperty("year", (Integer) row[1]);
                trendData.addProperty("newCustomers", ((Long) row[2]).intValue());
                registrationTrends.add(trendData);
            }
            
            responseObject.add("registrationTrends", registrationTrends);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating customer report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getBrandReport(HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonArray brandData = new JsonArray();
            
            // Brand Performance Report
            String brandPerformanceQuery = 
                "SELECT b.name, COUNT(p.id) as productCount, AVG(p.price) as avgPrice, " +
                "SUM(p.qty) as totalStock, SUM(oi.qty) as totalSold, SUM(oi.qty * p.price) as revenue " +
                "FROM Brand b, Model m, Product p, OrderItems oi " +
                "WHERE b.id = m.brand.id AND m.id = p.model.id AND p.id = oi.product.id " +
                "GROUP BY b.id, b.name " +
                "ORDER BY revenue DESC";
            
            List<Object[]> brandResults = session.createQuery(brandPerformanceQuery).list();
            
            for (Object[] row : brandResults) {
                JsonObject brandObj = new JsonObject();
                brandObj.addProperty("brandName", (String) row[0]);
                brandObj.addProperty("productCount", ((Long) row[1]).intValue());
                brandObj.addProperty("avgPrice", row[2] != null ? ((Double) row[2]) : 0.0);
                brandObj.addProperty("totalStock", row[3] != null ? ((Long) row[3]).intValue() : 0);
                brandObj.addProperty("totalSold", row[4] != null ? ((Long) row[4]).intValue() : 0);
                brandObj.addProperty("revenue", row[5] != null ? ((Double) row[5]) : 0.0);
                brandData.add(brandObj);
            }
            
            responseObject.add("brandPerformance", brandData);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating brand report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}