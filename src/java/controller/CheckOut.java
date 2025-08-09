/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Address;
import hibernate.Cart;
import hibernate.City;
import hibernate.DeliveryTypes;
import hibernate.HibernateUtil;
import hibernate.OrderItems;
import hibernate.OrderStatus;
import hibernate.Orders;
import hibernate.Product;
import hibernate.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.PayHere;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author Dilhara
 */
@WebServlet(name = "CheckOut", urlPatterns = {"/CheckOut"})
public class CheckOut extends HttpServlet {

    private static final int SELECTOR_DEFAULT_VALUE = 0;
    private static final int ORDER_PROCESSING = 1;
    private static final int WITHIN_COLOMBO = 1;
    private static final int OUT_OF_COLOMBO = 2;
    private static final int RATING_DEFAULT_VALUE = 0;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
           System.out.println("CheckOut servlet started");
           
        Gson gson = new Gson();
        JsonObject requJsonObject = gson.fromJson(request.getReader(), JsonObject.class);
   System.out.println("Request JSON: " + requJsonObject.toString());
   
   
        boolean isCurrentAddress = requJsonObject.get("isCurrentAddress").getAsBoolean();
        String firstName = requJsonObject.get("firstName").getAsString();
        String lastName = requJsonObject.get("lastName").getAsString();
        String citySelect = requJsonObject.get("citySelect").getAsString();
        String lineOne = requJsonObject.get("lineOne").getAsString();
        String lineTwo = requJsonObject.get("lineTwo").getAsString();
        String postalCode = requJsonObject.get("postalCode").getAsString();
        String mobile = requJsonObject.get("mobile").getAsString();
        String orderNotes = requJsonObject.get("orderNotes").getAsString();

            System.out.println("Parsed data - isCurrentAddress: " + isCurrentAddress + ", firstName: " + firstName);

        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();
        Transaction tr = s.beginTransaction();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        User user = (User) request.getSession().getAttribute("user");

         System.out.println("User from session: " + (user != null ? user.getFirst_name() : "null"));

        
        if (user == null) {
            responseObject.addProperty("message", "Session expired! Please log in again");
        } else {
            if (isCurrentAddress) {
                Criteria c1 = s.createCriteria(Address.class);
                c1.add(Restrictions.eq("user", user));
                c1.addOrder(Order.desc("id"));
                if (c1.list().isEmpty()) {
                    responseObject.addProperty("message",
                            "You current address is not found. Please add a new address");
                } else {
                    Address address = (Address) c1.list().get(0);
                    // Update order notes for current address
                    if (orderNotes != null && !orderNotes.isEmpty()) {
                        address.setOrderNotes(orderNotes);
                        s.update(address);
                    }
                    processCheckout(s, tr, user, address, responseObject);
                }
            } else {
                if (firstName.isEmpty()) {
                    responseObject.addProperty("message", "First Name is required.");
                } else if (lastName.isEmpty()) {
                    responseObject.addProperty("message", "Last Name is required.");
                } else if (!Util.isInteger(citySelect)) {
                    responseObject.addProperty("message", "Invalid city");
                } else if (Integer.parseInt(citySelect) == CheckOut.SELECTOR_DEFAULT_VALUE) {
                    responseObject.addProperty("message", "Invalid city");
                } else {
                    City city = (City) s.get(City.class, Integer.valueOf(citySelect));
                    if (city == null) {
                        responseObject.addProperty("message", "Invalid city name");
                    } else {
                        if (lineOne.isEmpty()) {
                            responseObject.addProperty("message", "Address line one is required");
                        } else if (lineTwo.isEmpty()) {
                            responseObject.addProperty("message", "Address line two is required");
                        } else if (postalCode.isEmpty()) {
                            responseObject.addProperty("message", "Your postal code is required");
                        } else if (!Util.isCodeValid(postalCode)) {
                            responseObject.addProperty("message", "Invalid postal code number");
                        } else if (mobile.isEmpty()) {
                            responseObject.addProperty("message", "Mobile number is required");
                        } else if (!Util.isMobileValid(mobile)) {
                            responseObject.addProperty("message", "Invalid mobile number");
                        } else {
                            Address address = new Address();
                            address.setFirstName(firstName);
                            address.setLastName(lastName);
                            address.setLineOne(lineOne);
                            address.setLineTwo(lineTwo);
                            address.setCity(city);
                            address.setPostalCode(postalCode);
                            address.setMobile(mobile);
                            address.setUser(user);
                            address.setOrderNotes(orderNotes);
                            s.save(address);

                            processCheckout(s, tr, user, address, responseObject);
                        }
                    }
                }
            }
        }

        response.setContentType("application/json");
        String toJson = gson.toJson(responseObject);
        response.getWriter().write(toJson);
    }

    private void processCheckout(Session s,
            Transaction tr,
            User user,
            Address address,
            JsonObject responseObject) {
 System.out.println("processCheckout started");
        try {
            Orders orders = new Orders();
            orders.setAddress(address);
            orders.setCreatedAt(new Date());
            orders.setUser(user);

            int orderId = (int) s.save(orders);

            Criteria c1 = s.createCriteria(Cart.class);
            c1.add(Restrictions.eq("user", user));
            List<Cart> cartList = c1.list();

            OrderStatus orderStatus = (OrderStatus) s.get(OrderStatus.class, CheckOut.ORDER_PROCESSING);
            DeliveryTypes withInColombo = (DeliveryTypes) s.get(DeliveryTypes.class, CheckOut.WITHIN_COLOMBO);
            DeliveryTypes outOfColombo = (DeliveryTypes) s.get(DeliveryTypes.class, CheckOut.OUT_OF_COLOMBO);

            double amount = 0;
            String items = "";

            for (Cart cart : cartList) {
                amount += cart.getQty() * cart.getProduct().getPrice();

                OrderItems orderItems = new OrderItems();

                if (address.getCity().getName().equalsIgnoreCase("Colombo")) { // within colombo
                    amount += cart.getQty() * withInColombo.getPrice();
                    orderItems.setDeliveryTypes(withInColombo);
                } else {// out of colombo
                    amount += cart.getQty() * outOfColombo.getPrice();
                    orderItems.setDeliveryTypes(outOfColombo);
                }
                items += cart.getProduct().getTitle() + " x " + cart.getQty() + ", ";

                Product product = cart.getProduct();
                orderItems.setOrderStatus(orderStatus);
                orderItems.setOrders(orders);
                orderItems.setProduct(product);
                orderItems.setQty(cart.getQty());
                orderItems.setRating(CheckOut.RATING_DEFAULT_VALUE); // 0

                s.save(orderItems);

                //update product qty
                product.setQty(product.getQty() - cart.getQty());
                s.update(product);

                // delete cart item
                s.delete(cart);
            }

            tr.commit();

            //PayHere process
            String merahantID = "1224009";
            String merchantSecret = "MjgyMDQ5NjUyOTQwNDA4MzQwODI0MjA0ODgzMzIyNjgwOTk5MTk0";
            String orderID = "#000" + orderId;
            String currency = "LKR";
            String formattedAmount = new DecimalFormat("0.00").format(amount);
            String merchantSecretMD5 = PayHere.generateMD5(merchantSecret);

            String hash = PayHere.generateMD5(merahantID + orderID + formattedAmount + currency + merchantSecretMD5);

            JsonObject payHereJson = new JsonObject();
            payHereJson.addProperty("sandbox", true);
            payHereJson.addProperty("merchant_id", merahantID);

            payHereJson.addProperty("return_url", "");
            payHereJson.addProperty("cancel_url", "");
            payHereJson.addProperty("notify_url", "https://55954bec92ee.ngrok-free.app/Smarttrade/VerifyPayments");

            payHereJson.addProperty("order_id", orderID);
            payHereJson.addProperty("items", items);
            payHereJson.addProperty("amount", formattedAmount);
            payHereJson.addProperty("currency", currency);
            payHereJson.addProperty("hash", hash);

            payHereJson.addProperty("first_name", user.getFirst_name());
            payHereJson.addProperty("last_name", user.getLast_name());
            payHereJson.addProperty("email", user.getEmail());

            payHereJson.addProperty("phone", address.getMobile());
            payHereJson.addProperty("address", address.getLineOne() + ", " + address.getLineTwo());
            payHereJson.addProperty("city", address.getCity().getName());
            payHereJson.addProperty("country", "Sri Lanka");

            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Checkout completed");
            responseObject.add("payhereJson", new Gson().toJsonTree(payHereJson));
            
             System.out.println("Order created successfully, proceeding with PayHere");
        } catch (Exception e) {
        System.out.println("Error in processCheckout: " + e.getMessage());
        e.printStackTrace();
        tr.rollback();
        responseObject.addProperty("message", "Checkout failed: " + e.getMessage());
   
        }
    }
}