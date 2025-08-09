package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.*;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/DashboardServlet"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            // Get summary statistics
            JsonObject summary = getSummaryStatistics(session);
            responseObject.add("summary", summary);
            
            // Get recent orders
            JsonObject recentOrders = getRecentOrders(session);
            responseObject.add("recentOrders", recentOrders);
            
            // Get recent products
            JsonObject recentProducts = getRecentProducts(session);
            responseObject.add("recentProducts", recentProducts);
            
            // Get additional dashboard metrics
            JsonObject metrics = getDashboardMetrics(session);
            responseObject.add("metrics", metrics);
            
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
    
    private JsonObject getSummaryStatistics(Session session) {
        JsonObject summary = new JsonObject();
        
        try {
            // Total Orders
            Criteria orderCriteria = session.createCriteria(Orders.class);
            Long totalOrders = (Long) orderCriteria.setProjection(Projections.rowCount()).uniqueResult();
            summary.addProperty("totalOrders", totalOrders != null ? totalOrders : 0);
            
            // Total Products
            Criteria productCriteria = session.createCriteria(Product.class);
            Long totalProducts = (Long) productCriteria.setProjection(Projections.rowCount()).uniqueResult();
            summary.addProperty("totalProducts", totalProducts != null ? totalProducts : 0);
            
            // Total Customers
            Criteria userCriteria = session.createCriteria(User.class);
            Long totalCustomers = (Long) userCriteria.setProjection(Projections.rowCount()).uniqueResult();
            summary.addProperty("totalCustomers", totalCustomers != null ? totalCustomers : 0);
            
            // Low Stock Count (products with qty <= 10)
            Criteria lowStockCriteria = session.createCriteria(Product.class);
            lowStockCriteria.add(Restrictions.le("qty", 10));
            Long lowStockCount = (Long) lowStockCriteria.setProjection(Projections.rowCount()).uniqueResult();
            summary.addProperty("lowStockCount", lowStockCount != null ? lowStockCount : 0);
            
            // Total Revenue - calculate from order items
            String revenueQuery = "SELECT SUM(oi.qty * p.price) FROM OrderItems oi JOIN oi.product p";
            Query query = session.createQuery(revenueQuery);
            Object revenueResult = query.uniqueResult();
            Double totalRevenue = 0.0;
            if (revenueResult != null) {
                if (revenueResult instanceof BigDecimal) {
                    totalRevenue = ((BigDecimal) revenueResult).doubleValue();
                } else if (revenueResult instanceof Double) {
                    totalRevenue = (Double) revenueResult;
                }
            }
            summary.addProperty("totalRevenue", totalRevenue);
            
            // Orders this month
            String monthlyOrdersQuery = "SELECT COUNT(*) FROM Orders o WHERE MONTH(o.createdAt) = MONTH(CURRENT_DATE()) AND YEAR(o.createdAt) = YEAR(CURRENT_DATE())";
            Long monthlyOrders = (Long) session.createQuery(monthlyOrdersQuery).uniqueResult();
            summary.addProperty("monthlyOrders", monthlyOrders != null ? monthlyOrders : 0);
            
        } catch (Exception e) {
            System.err.println("Error calculating summary statistics: " + e.getMessage());
            e.printStackTrace();
            // Set default values on error
            summary.addProperty("totalOrders", 0);
            summary.addProperty("totalProducts", 0);
            summary.addProperty("totalCustomers", 0);
            summary.addProperty("lowStockCount", 0);
            summary.addProperty("totalRevenue", 0.0);
            summary.addProperty("monthlyOrders", 0);
        }
        
        return summary;
    }
    
    private JsonObject getRecentOrders(Session session) {
        JsonObject recentOrders = new JsonObject();
        JsonArray ordersArray = new JsonArray();
        
        try {
            Criteria criteria = session.createCriteria(Orders.class);
            criteria.addOrder(Order.desc("createdAt"));
            criteria.setMaxResults(5);
            List<Orders> orders = criteria.list();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            for (Orders order : orders) {
                JsonObject orderObj = new JsonObject();
                orderObj.addProperty("id", order.getId());
                
                if (order.getUser() != null) {
                    JsonObject userObj = new JsonObject();
                    userObj.addProperty("first_name", order.getUser().getFirst_name());
                    userObj.addProperty("last_name", order.getUser().getLast_name());
                    orderObj.add("user", userObj);
                }
                
                orderObj.addProperty("createdAt", order.getCreatedAt() != null ? 
                    sdf.format(order.getCreatedAt()) : "N/A");
                
                // Calculate order total
                Double orderTotal = calculateOrderTotal(session, order.getId());
                orderObj.addProperty("total", orderTotal);
                
                // Add order status if available
                orderObj.addProperty("status", "Processing"); // Default status
                
                ordersArray.add(orderObj);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading recent orders: " + e.getMessage());
            e.printStackTrace();
        }
        
        recentOrders.add("orders", ordersArray);
        return recentOrders;
    }
    
    private JsonObject getRecentProducts(Session session) {
        JsonObject recentProducts = new JsonObject();
        JsonArray productsArray = new JsonArray();
        
        try {
            Criteria criteria = session.createCriteria(Product.class);
            criteria.addOrder(Order.desc("created_at"));
            criteria.setMaxResults(5);
            List<Product> products = criteria.list();
            
            for (Product product : products) {
                JsonObject productObj = new JsonObject();
                productObj.addProperty("id", product.getId());
                productObj.addProperty("title", product.getTitle());
                productObj.addProperty("price", product.getPrice());
                productObj.addProperty("qty", product.getQty());
                
                // Add model and brand information
                if (product.getModel() != null) {
                    JsonObject modelObj = new JsonObject();
                    modelObj.addProperty("id", product.getModel().getId());
                    modelObj.addProperty("name", product.getModel().getName());
                    
                    if (product.getModel().getBrand() != null) {
                        JsonObject brandObj = new JsonObject();
                        brandObj.addProperty("id", product.getModel().getBrand().getId());
                        brandObj.addProperty("name", product.getModel().getBrand().getName());
                        modelObj.add("brand", brandObj);
                    }
                    productObj.add("model", modelObj);
                }
                
                // Add stock status
                String stockStatus = product.getQty() > 0 ? "In Stock" : "Out of Stock";
                productObj.addProperty("stockStatus", stockStatus);
                
                productsArray.add(productObj);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading recent products: " + e.getMessage());
            e.printStackTrace();
        }
        
        recentProducts.add("products", productsArray);
        return recentProducts;
    }
    
    private JsonObject getDashboardMetrics(Session session) {
        JsonObject metrics = new JsonObject();
        
        try {
            // Top selling product this month
            String topProductQuery = "SELECT p.title, SUM(oi.qty) as totalSold " +
                                   "FROM OrderItems oi JOIN oi.product p JOIN oi.orders o " +
                                   "WHERE MONTH(o.createdAt) = MONTH(CURRENT_DATE()) " +
                                   "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE()) " +
                                   "GROUP BY p.id, p.title " +
                                   "ORDER BY totalSold DESC";
            Query topProductQ = session.createQuery(topProductQuery);
            topProductQ.setMaxResults(1);
            List<Object[]> topProduct = topProductQ.list();
            
            if (!topProduct.isEmpty()) {
                Object[] product = topProduct.get(0);
                JsonObject topProductObj = new JsonObject();
                topProductObj.addProperty("title", (String) product[0]);
                topProductObj.addProperty("soldCount", ((Long) product[1]).intValue());
                metrics.add("topProduct", topProductObj);
            }
            
            // New customers this month
            String newCustomersQuery = "SELECT COUNT(*) FROM User u " +
                                     "WHERE MONTH(u.created_at) = MONTH(CURRENT_DATE()) " +
                                     "AND YEAR(u.created_at) = YEAR(CURRENT_DATE())";
            Long newCustomers = (Long) session.createQuery(newCustomersQuery).uniqueResult();
            metrics.addProperty("newCustomers", newCustomers != null ? newCustomers : 0);
            
            // Pending orders count
            String pendingOrdersQuery = "SELECT COUNT(*) FROM Orders o WHERE o.status = :status";
            Query pendingQ = session.createQuery(pendingOrdersQuery);
            pendingQ.setParameter("status", "Processing");
            Long pendingOrders = (Long) pendingQ.uniqueResult();
            metrics.addProperty("pendingOrders", pendingOrders != null ? pendingOrders : 0);
            
        } catch (Exception e) {
            System.err.println("Error calculating dashboard metrics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return metrics;
    }
    
    private Double calculateOrderTotal(Session session, int orderId) {
        try {
            String totalQuery = "SELECT SUM(oi.qty * p.price) FROM OrderItems oi " +
                              "JOIN oi.product p WHERE oi.orders.id = :orderId";
            Query query = session.createQuery(totalQuery);
            query.setParameter("orderId", orderId);
            Object result = query.uniqueResult();
            
            if (result != null) {
                if (result instanceof BigDecimal) {
                    return ((BigDecimal) result).doubleValue();
                } else if (result instanceof Double) {
                    return (Double) result;
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating order total for order " + orderId + ": " + e.getMessage());
        }
        return 0.0;
    }
}