package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

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
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            JsonObject salesSummary = new JsonObject();
            
            // Get all orders
            Criteria c1 = s.createCriteria(Orders.class);
            List<Orders> ordersList = c1.list();
            
            // Get all users
            Criteria c2 = s.createCriteria(User.class);
            List<User> usersList = c2.list();
            
            // Get all order items
            Criteria c3 = s.createCriteria(OrderItems.class);
            List<OrderItems> orderItemsList = c3.list();
            
            // Calculate totals
            double totalSales = 0.0;
            for (OrderItems item : orderItemsList) {
                totalSales += item.getQty() * item.getProduct().getPrice();
            }
            
            salesSummary.addProperty("totalSales", totalSales);
            salesSummary.addProperty("totalOrders", ordersList.size());
            salesSummary.addProperty("totalCustomers", usersList.size());
            
            if (ordersList.size() > 0) {
                double avgOrderValue = totalSales / ordersList.size();
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
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getSalesReport(HttpServletResponse response) throws IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            // Get all orders with order by date
            Criteria c1 = s.createCriteria(Orders.class);
            c1.addOrder(Order.desc("createdAt"));
            List<Orders> ordersList = c1.list();
            
            // Get all order items
            Criteria c2 = s.createCriteria(OrderItems.class);
            List<OrderItems> orderItemsList = c2.list();
            
            // Get all products
            Criteria c3 = s.createCriteria(Product.class);
            List<Product> productsList = c3.list();
            
            // Monthly sales data - group by month and year
            JsonArray monthlySales = new JsonArray();
            java.util.Map<String, Integer> monthlyOrderCount = new java.util.HashMap<>();
            java.util.Map<String, Double> monthlyRevenue = new java.util.HashMap<>();
            
            // Process orders to group by month/year
            for (Orders order : ordersList) {
                if (order.getCreatedAt() != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(order.getCreatedAt());
                    int month = cal.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
                    int year = cal.get(java.util.Calendar.YEAR);
                    String key = year + "-" + month;
                    
                    // Count orders
                    monthlyOrderCount.put(key, monthlyOrderCount.getOrDefault(key, 0) + 1);
                    
                    // Calculate revenue for this order
                    double orderRevenue = 0.0;
                    for (OrderItems item : orderItemsList) {
                        if (item.getOrders().getId() == order.getId()) {
                            orderRevenue += item.getQty() * item.getProduct().getPrice();
                        }
                    }
                    monthlyRevenue.put(key, monthlyRevenue.getOrDefault(key, 0.0) + orderRevenue);
                }
            }
            
            // Convert to JSON format expected by frontend
            for (String key : monthlyOrderCount.keySet()) {
                String[] parts = key.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                
                JsonObject monthData = new JsonObject();
                monthData.addProperty("month", month);
                monthData.addProperty("year", year);
                monthData.addProperty("orderCount", monthlyOrderCount.get(key));
                monthData.addProperty("revenue", monthlyRevenue.get(key));
                monthlySales.add(monthData);
                
                if (monthlySales.size() >= 12) break; // Limit to 12 months
            }
            
            // Top products by quantity sold
            JsonArray topProducts = new JsonArray();
            java.util.List<JsonObject> productDataList = new java.util.ArrayList<>();
            
            for (Product product : productsList) {
                int totalSold = 0;
                double revenue = 0.0;
                
                for (OrderItems item : orderItemsList) {
                    if (item.getProduct().getId() == product.getId()) {
                        totalSold += item.getQty();
                        revenue += item.getQty() * product.getPrice();
                    }
                }
                
                if (totalSold > 0) {
                    JsonObject productData = new JsonObject();
                    productData.addProperty("id", product.getId());
                    productData.addProperty("title", product.getTitle());
                    productData.addProperty("totalSold", totalSold);
                    productData.addProperty("revenue", revenue);
                    productDataList.add(productData);
                }
            }
            
            // Sort by totalSold descending and take top 10
            productDataList.sort((a, b) -> Integer.compare(
                b.get("totalSold").getAsInt(), 
                a.get("totalSold").getAsInt()
            ));
            
            for (int i = 0; i < Math.min(10, productDataList.size()); i++) {
                topProducts.add(productDataList.get(i));
            }
            
            responseObject.add("monthlySales", monthlySales);
            responseObject.add("topProducts", topProducts);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating sales report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getInventoryReport(HttpServletResponse response) throws IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            // Get low stock products (qty <= 10)
            Criteria c1 = s.createCriteria(Product.class);
            c1.add(Restrictions.le("qty", 10));
            c1.addOrder(Order.asc("qty"));
            List<Product> lowStockProducts = c1.list();
            
            JsonArray lowStockData = new JsonArray();
            for (Product product : lowStockProducts) {
                JsonObject productData = new JsonObject();
                productData.addProperty("id", product.getId());
                productData.addProperty("title", product.getTitle());
                productData.addProperty("qty", product.getQty());
                productData.addProperty("price", product.getPrice());
                
                if (product.getModel() != null && product.getModel().getBrand() != null) {
                    productData.addProperty("brandName", product.getModel().getBrand().getName());
                } else {
                    productData.addProperty("brandName", "N/A");
                }
                
                lowStockData.add(productData);
            }
            
            // Get all products for stock value by brand
            Criteria c2 = s.createCriteria(Product.class);
            List<Product> allProducts = c2.list();
            
            // Get all brands
            Criteria c3 = s.createCriteria(Brand.class);
            List<Brand> brandsList = c3.list();
            
            JsonArray stockByBrand = new JsonArray();
            for (Brand brand : brandsList) {
                int productCount = 0;
                double stockValue = 0.0;
                
                for (Product product : allProducts) {
                    if (product.getModel() != null && 
                        product.getModel().getBrand() != null && 
                        product.getModel().getBrand().getId() == brand.getId()) {
                        productCount++;
                        stockValue += product.getQty() * product.getPrice();
                    }
                }
                
                if (productCount > 0) {
                    JsonObject brandData = new JsonObject();
                    brandData.addProperty("brandName", brand.getName());
                    brandData.addProperty("productCount", productCount);
                    brandData.addProperty("stockValue", stockValue);
                    stockByBrand.add(brandData);
                }
            }
            
            responseObject.add("lowStockProducts", lowStockData);
            responseObject.add("stockByBrand", stockByBrand);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating inventory report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getCustomerReport(HttpServletResponse response) throws IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            // Get all users
            Criteria c1 = s.createCriteria(User.class);
            c1.addOrder(Order.desc("created_at"));
            List<User> usersList = c1.list();
            
            // Get all orders
            Criteria c2 = s.createCriteria(Orders.class);
            List<Orders> ordersList = c2.list();
            
            // Get all order items
            Criteria c3 = s.createCriteria(OrderItems.class);
            List<OrderItems> orderItemsList = c3.list();
            
            // Calculate top customers
            JsonArray topCustomers = new JsonArray();
            java.util.List<JsonObject> customerDataList = new java.util.ArrayList<>();
            
            for (User user : usersList) {
                int orderCount = 0;
                double totalSpent = 0.0;
                
                // Count orders for this user
                for (Orders order : ordersList) {
                    if (order.getUser().getId() == user.getId()) {
                        orderCount++;
                        
                        // Calculate total spent
                        for (OrderItems item : orderItemsList) {
                            if (item.getOrders().getId() == order.getId()) {
                                totalSpent += item.getQty() * item.getProduct().getPrice();
                            }
                        }
                    }
                }
                
                if (orderCount > 0) {
                    JsonObject customerData = new JsonObject();
                    customerData.addProperty("id", user.getId());
                    customerData.addProperty("firstName", user.getFirst_name());
                    customerData.addProperty("lastName", user.getLast_name());
                    customerData.addProperty("email", user.getEmail());
                    customerData.addProperty("orderCount", orderCount);
                    customerData.addProperty("totalSpent", totalSpent);
                    customerDataList.add(customerData);
                }
            }
            
            // Sort customers by orderCount descending and take top 20
            customerDataList.sort((a, b) -> Integer.compare(
                b.get("orderCount").getAsInt(), 
                a.get("orderCount").getAsInt()
            ));
            
            for (int i = 0; i < Math.min(20, customerDataList.size()); i++) {
                topCustomers.add(customerDataList.get(i));
            }
            
            // Registration trends - group by month and year
            JsonArray registrationTrends = new JsonArray();
            java.util.Map<String, Integer> monthlyRegistrations = new java.util.HashMap<>();
            
            // Process users to group by month/year
            for (User user : usersList) {
                if (user.getCreated_at() != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(user.getCreated_at());
                    int month = cal.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
                    int year = cal.get(java.util.Calendar.YEAR);
                    String key = year + "-" + month;
                    
                    monthlyRegistrations.put(key, monthlyRegistrations.getOrDefault(key, 0) + 1);
                }
            }
            
            // Convert to JSON format expected by frontend
            java.util.List<String> sortedKeys = new java.util.ArrayList<>(monthlyRegistrations.keySet());
            sortedKeys.sort(java.util.Collections.reverseOrder()); // Sort by year-month descending
            
            for (String key : sortedKeys) {
                String[] parts = key.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                
                JsonObject trendData = new JsonObject();
                trendData.addProperty("month", month);
                trendData.addProperty("year", year);
                trendData.addProperty("newCustomers", monthlyRegistrations.get(key));
                registrationTrends.add(trendData);
                
                if (registrationTrends.size() >= 12) break; // Limit to 12 months
            }
            
            responseObject.add("topCustomers", topCustomers);
            responseObject.add("registrationTrends", registrationTrends);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating customer report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void getBrandReport(HttpServletResponse response) throws IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        
        try {
            // Get all brands
            Criteria c1 = s.createCriteria(Brand.class);
            List<Brand> brandsList = c1.list();
            
            // Get all models
            Criteria c2 = s.createCriteria(Model.class);
            List<Model> modelsList = c2.list();
            
            // Get all products
            Criteria c3 = s.createCriteria(Product.class);
            List<Product> productsList = c3.list();
            
            // Get all order items
            Criteria c4 = s.createCriteria(OrderItems.class);
            List<OrderItems> orderItemsList = c4.list();
            
            JsonArray brandPerformance = new JsonArray();
            
            for (Brand brand : brandsList) {
                int productCount = 0;
                double totalPrice = 0.0;
                int totalStock = 0;
                int totalSold = 0;
                double revenue = 0.0;
                
                // Find products for this brand
                for (Product product : productsList) {
                    if (product.getModel() != null && 
                        product.getModel().getBrand() != null && 
                        product.getModel().getBrand().getId() == brand.getId()) {
                        
                        productCount++;
                        totalPrice += product.getPrice();
                        totalStock += product.getQty();
                        
                        // Calculate sold quantity and revenue
                        for (OrderItems item : orderItemsList) {
                            if (item.getProduct().getId() == product.getId()) {
                                totalSold += item.getQty();
                                revenue += item.getQty() * product.getPrice();
                            }
                        }
                    }
                }
                
                if (productCount > 0) {
                    JsonObject brandData = new JsonObject();
                    brandData.addProperty("brandName", brand.getName());
                    brandData.addProperty("productCount", productCount);
                    brandData.addProperty("avgPrice", totalPrice / productCount);
                    brandData.addProperty("totalStock", totalStock);
                    brandData.addProperty("totalSold", totalSold);
                    brandData.addProperty("revenue", revenue);
                    brandPerformance.add(brandData);
                }
            }
            
            responseObject.add("brandPerformance", brandPerformance);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating brand report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}