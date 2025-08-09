/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.HibernateUtil;
import hibernate.Product;
import hibernate.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author Dilhara
 */
@WebServlet(name = "RemoveFromCart", urlPatterns = {"/RemoveFromCart"})
public class RemoveFromCart extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String productId = request.getParameter("productId");
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        System.out.println("RemoveFromCart called with productId: " + productId); // Debug log

        if (productId == null || productId.trim().isEmpty()) {
            responseObject.addProperty("message", "Product ID is required!");
        } else if (!Util.isInteger(productId)) {
            responseObject.addProperty("message", "Invalid product ID!");
        } else {
            try {
                User user = (User) request.getSession().getAttribute("user");
                
                if (user != null) {
                    // Handle database cart removal for logged-in users
                    removeFromDatabaseCart(user, Integer.valueOf(productId), responseObject);
                } else {
                    // Handle session cart removal for guest users
                    removeFromSessionCart(request, Integer.valueOf(productId), responseObject);
                }
            } catch (Exception e) {
                System.err.println("Error in RemoveFromCart: " + e.getMessage());
                e.printStackTrace();
                responseObject.addProperty("message", "An error occurred while removing the product from cart");
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = gson.toJson(responseObject);
        System.out.println("RemoveFromCart response: " + jsonResponse); // Debug log
        response.getWriter().write(jsonResponse);
    }

    private void removeFromDatabaseCart(User user, int productId, JsonObject responseObject) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tr = s.beginTransaction();

        try {
            Product product = (Product) s.get(Product.class, productId);
            if (product == null) {
                responseObject.addProperty("message", "Product not found");
                return;
            }

            Criteria c1 = s.createCriteria(Cart.class);
            c1.add(Restrictions.eq("user", user));
            c1.add(Restrictions.eq("product", product));

            List<Cart> cartItems = c1.list();
            if (!cartItems.isEmpty()) {
                Cart cartItem = cartItems.get(0);
                s.delete(cartItem);
                tr.commit();
                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "Product removed from cart successfully");
            } else {
                responseObject.addProperty("message", "Product not found in cart");
            }
        } catch (Exception e) {
            tr.rollback();
            System.err.println("Error removing from database cart: " + e.getMessage());
            e.printStackTrace();
            responseObject.addProperty("message", "Error removing product from cart");
        } finally {
            s.close();
        }
    }

    private void removeFromSessionCart(HttpServletRequest request, int productId, JsonObject responseObject) {
        HttpSession session = request.getSession();
        ArrayList<Cart> sessionCarts = (ArrayList<Cart>) session.getAttribute("sessionCart");

        if (sessionCarts == null || sessionCarts.isEmpty()) {
            responseObject.addProperty("message", "Cart is empty");
            return;
        }

        boolean itemRemoved = false;
        Iterator<Cart> iterator = sessionCarts.iterator();

        while (iterator.hasNext()) {
            Cart cart = iterator.next();
            if (cart.getProduct().getId() == productId) {
                iterator.remove();
                itemRemoved = true;
                break;
            }
        }

        if (itemRemoved) {
            session.setAttribute("sessionCart", sessionCarts);
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Product removed from cart successfully");
        } else {
            responseObject.addProperty("message", "Product not found in cart");
        }
    }
}