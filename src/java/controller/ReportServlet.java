package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Orders;
import hibernate.OrderItems;
import hibernate.User;
import hibernate.Product;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
                getSummary(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void getSummary(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            // Sales Summary
            JsonObject salesSummary = new JsonObject();
            // Total orders
            Criteria c1 = s.createCriteria(Orders.class);
            salesSummary.addProperty("totalOrders", c1.list().size());
            // Total customers
            Criteria c2 = s.createCriteria(User.class);
            salesSummary.addProperty("totalCustomers", c2.list().size());
            // Total sales (sum of all order items)
            double totalSales = 0;
            Criteria c3 = s.createCriteria(OrderItems.class);
            List<OrderItems> allItems = c3.list();
            for (OrderItems item : allItems) {
                if (item.getProduct() != null) {
                    totalSales += item.getQty() * item.getProduct().getPrice();
                }
            }
            salesSummary.addProperty("totalSales", totalSales);
            responseObject.add("salesSummary", salesSummary);

            // Stock Summary
            JsonObject stockSummary = new JsonObject();
            // Low stock products (qty <= 5)
            Criteria c4 = s.createCriteria(Product.class);
            c4.add(Restrictions.le("qty", 5));
            c4.add(Restrictions.gt("qty", 0));
            stockSummary.addProperty("lowStockCount", c4.list().size());
            // Out of stock products (qty = 0)
            Criteria c5 = s.createCriteria(Product.class);
            c5.add(Restrictions.eq("qty", 0));
            stockSummary.addProperty("outOfStockCount", c5.list().size());
            responseObject.add("stockSummary", stockSummary);

            responseObject.addProperty("status", true);
        } catch (Exception e) {
            responseObject.addProperty("message", "Error generating reports: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
