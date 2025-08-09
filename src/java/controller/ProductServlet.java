package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Brand;
import hibernate.HibernateUtil;
import hibernate.Product;
import hibernate.Status;
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

@WebServlet(name = "ProductServlet", urlPatterns = {"/ProductServlet"})
public class ProductServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "list";
        switch (action) {
            case "list":
                listProducts(request, response);
                break;
            case "view":
                viewProduct(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "add";
        switch (action) {
            case "add":
                addProduct(request, response);
                break;
            case "edit":
                editProduct(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "delete";
        switch (action) {
            case "delete":
                deleteProduct(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void listProducts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            Criteria c = s.createCriteria(Product.class);
            c.addOrder(Order.desc("id"));
            List<Product> productList = c.list();
            responseObject.add("productList", gson.toJsonTree(productList));
            responseObject.addProperty("status", true);
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading products: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void addProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tx = null;
        try {
            BufferedReader reader = request.getReader();
            Product input = gson.fromJson(reader, Product.class);
            tx = s.beginTransaction();
            // Set status to active by default if not provided
            if (input.getStatus() == null) {
                Status status = (Status) s.get(Status.class, 1); // 1 = active
                input.setStatus(status);
            }
            s.save(input);
            tx.commit();
            responseObject.addProperty("status", true);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error adding product: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void viewProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String idStr = request.getParameter("id");
        if (idStr == null) {
            responseObject.addProperty("message", "Product ID required");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseObject));
            return;
        }
        int id = Integer.parseInt(idStr);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        try {
            Product product = (Product) s.get(Product.class, id);
            if (product != null) {
                responseObject.add("product", gson.toJsonTree(product));
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Product not found");
            }
        } catch (Exception e) {
            responseObject.addProperty("message", "Error: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void editProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tx = null;
        try {
            BufferedReader reader = request.getReader();
            Product input = gson.fromJson(reader, Product.class);
            if (input.getId() == 0) {
                responseObject.addProperty("message", "Product ID required");
            } else {
                tx = s.beginTransaction();
                Product product = (Product) s.get(Product.class, input.getId());
                if (product != null) {
                    product.setTitle(input.getTitle());
                    product.setPrice(input.getPrice());
                    product.setQty(input.getQty());
                    product.setStatus(input.getStatus());
//                    product.setBrand(input.getBrand()); 
                    s.update(product);
                    tx.commit();
                    responseObject.addProperty("status", true);
                } else {
                    responseObject.addProperty("message", "Product not found");
                }
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error editing product: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void deleteProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String idStr = request.getParameter("id");
        if (idStr == null) {
            responseObject.addProperty("message", "Product ID required");
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
            Product product = (Product) s.get(Product.class, id);
            if (product != null) {
                s.delete(product);
                tx.commit();
                responseObject.addProperty("status", true);
            } else {
                responseObject.addProperty("message", "Product not found");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error deleting product: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}
