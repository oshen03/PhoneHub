/*
 * Fixed CheckSessionCart with proper entity handling
 */
package controller;

import hibernate.Cart;
import hibernate.HibernateUtil;
import hibernate.Product;
import hibernate.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 *
 * @author Oshen Sathsara <oshensathsara2003@gmail.com>
 */
@WebServlet(name = "CheckSessionCart", urlPatterns = {"/CheckSessionCart"})
public class CheckSessionCart extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Create JSON response
        com.google.gson.JsonObject responseObject = new com.google.gson.JsonObject();
        
        try {
            User user = (User) request.getSession().getAttribute("user");
            boolean isLoggedIn = user != null;
            responseObject.addProperty("status", isLoggedIn);
            responseObject.addProperty("isLoggedIn", isLoggedIn);
            
            System.out.println("CheckSessionCart - User logged in: " + isLoggedIn);
            
            // Only process session cart if user is logged in
            if (isLoggedIn) {
                processSessionCart(request, user);
            }
            
        } catch (Exception e) {
            System.err.println("Error in CheckSessionCart: " + e.getMessage());
            e.printStackTrace();
            responseObject.addProperty("status", false);
            responseObject.addProperty("isLoggedIn", false);
        }
        
        // Send JSON response
        response.setContentType("application/json");
        response.getWriter().write(new com.google.gson.Gson().toJson(responseObject));
    }
    
    private void processSessionCart(HttpServletRequest request, User user) {
        ArrayList<Cart> sessionCarts = (ArrayList<Cart>) request.getSession().getAttribute("sessionCart");
        
        if (sessionCarts == null || sessionCarts.isEmpty()) {
            System.out.println("No session cart items to process");
            return;
        }
        
        System.out.println("Processing " + sessionCarts.size() + " session cart items");
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = null;
        Transaction transaction = null;
        
        try {
            session = sf.openSession();
            transaction = session.beginTransaction();
            
            // Get fresh user reference from database
            User dbUser = (User) session.get(User.class, user.getId());
            if (dbUser == null) {
                System.err.println("User not found in database: " + user.getId());
                return;
            }
            
            for (Cart sessionCart : sessionCarts) {
                try {
                    processCartItem(session, dbUser, sessionCart);
                } catch (Exception e) {
                    System.err.println("Error processing cart item for product " + 
                                     sessionCart.getProduct().getId() + ": " + e.getMessage());
                    // Continue with other items
                }
            }
            
            // Commit all changes at once
            transaction.commit();
            System.out.println("All session cart items processed successfully");
            
            // Clear session cart only after successful commit
            request.getSession().removeAttribute("sessionCart");
            
        } catch (Exception e) {
            System.err.println("Error in session cart processing: " + e.getMessage());
            e.printStackTrace();
            
            if (transaction != null) {
                try {
                    transaction.rollback();
                    System.out.println("Transaction rolled back");
                } catch (Exception rollbackEx) {
                    System.err.println("Error during rollback: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception closeEx) {
                    System.err.println("Error closing session: " + closeEx.getMessage());
                }
            }
        }
    }
    
    private void processCartItem(Session session, User dbUser, Cart sessionCart) throws Exception {
        Integer productId = sessionCart.getProduct().getId();
        Integer sessionQty = sessionCart.getQty();
        
        // Get fresh product reference from database
        Product dbProduct = (Product) session.get(Product.class, productId);
        if (dbProduct == null) {
            System.err.println("Product not found: " + productId);
            return;
        }
        
        // Check if this product is already in user's cart
        String hql = "FROM Cart c WHERE c.user.id = :userId AND c.product.id = :productId";
        Query query = session.createQuery(hql);
        query.setParameter("userId", dbUser.getId());
        query.setParameter("productId", productId);
        
        @SuppressWarnings("unchecked")
        List<Cart> existingCarts = query.list();
        
        if (!existingCarts.isEmpty()) {
            // Update existing cart item
            Cart existingCart = existingCarts.get(0);
            int newQty = existingCart.getQty() + sessionQty;
            
            if (newQty <= dbProduct.getQty()) {
                existingCart.setQty(newQty);
                session.update(existingCart);
                System.out.println("Updated cart item - Product: " + productId + ", New Qty: " + newQty);
            } else {
                System.out.println("Cannot update - insufficient stock for product: " + productId + 
                                 " (Available: " + dbProduct.getQty() + ", Requested: " + newQty + ")");
            }
        } else {
            // Create new cart item
            if (sessionQty <= dbProduct.getQty()) {
                Cart newCart = new Cart();
                newCart.setUser(dbUser);
                newCart.setProduct(dbProduct);
                newCart.setQty(sessionQty);
                session.save(newCart);
                
                System.out.println("Added new cart item - Product: " + productId + ", Qty: " + sessionQty);
            } else {
                System.out.println("Cannot add - insufficient stock for product: " + productId + 
                                 " (Available: " + dbProduct.getQty() + ", Requested: " + sessionQty + ")");
            }
        }
    }
}