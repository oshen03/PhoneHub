/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Brand;
import hibernate.Color;
import hibernate.HibernateUtil;
import hibernate.Model;
import hibernate.Product;
import hibernate.Quality;
import hibernate.Status;
import hibernate.Storage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

/**
 *
 * @author Dilhara
 */
@WebServlet(name = "SearchProducts", urlPatterns = {"/SearchProducts"})
public class SearchProducts extends HttpServlet {

    private static final int MAX_RESULT = 9;
    private static final int ACTIVE_ID = 1;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        JsonObject resposeObject = new JsonObject();
        resposeObject.addProperty("status", true);

        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();

        try {
            Criteria c1 = s.createCriteria(Product.class); // get all products for the filtering

            // Get brand filters
            String[] brandIds = request.getParameterValues("brand[]");
            if (brandIds != null && brandIds.length > 0) {
                List<Brand> brands = new ArrayList<>();
                for (String brandId : brandIds) {
                    try {
                        Brand brand = (Brand) s.get(Brand.class, Integer.parseInt(brandId));
                        if (brand != null) {
                            brands.add(brand);
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid brand ID
                    }
                }
                if (!brands.isEmpty()) {
                    Criteria c3 = s.createCriteria(Model.class);
                    c3.add(Restrictions.in("brand", brands));
                    List<Model> modelList = c3.list();
                    if (!modelList.isEmpty()) {
                        c1.add(Restrictions.in("model", modelList));
                    }
                }
            }

            // Get quality filters
            String[] qualityIds = request.getParameterValues("quality[]");
            if (qualityIds != null && qualityIds.length > 0) {
                List<Quality> qualities = new ArrayList<>();
                for (String qualityId : qualityIds) {
                    try {
                        Quality quality = (Quality) s.get(Quality.class, Integer.parseInt(qualityId));
                        if (quality != null) {
                            qualities.add(quality);
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid quality ID
                    }
                }
                if (!qualities.isEmpty()) {
                    c1.add(Restrictions.in("quality", qualities));
                }
            }

            // Get color filters
            String[] colorIds = request.getParameterValues("color[]");
            if (colorIds != null && colorIds.length > 0) {
                List<Color> colors = new ArrayList<>();
                for (String colorId : colorIds) {
                    try {
                        Color color = (Color) s.get(Color.class, Integer.parseInt(colorId));
                        if (color != null) {
                            colors.add(color);
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid color ID
                    }
                }
                if (!colors.isEmpty()) {
                    c1.add(Restrictions.in("color", colors));
                }
            }

            // Get storage filters
            String[] storageIds = request.getParameterValues("storage[]");
            if (storageIds != null && storageIds.length > 0) {
                List<Storage> storages = new ArrayList<>();
                for (String storageId : storageIds) {
                    try {
                        Storage storage = (Storage) s.get(Storage.class, Integer.parseInt(storageId));
                        if (storage != null) {
                            storages.add(storage);
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid storage ID
                    }
                }
                if (!storages.isEmpty()) {
                    c1.add(Restrictions.in("storage", storages));
                }
            }

            // Get price range
            String minPrice = request.getParameter("priceRange[min]");
            String maxPrice = request.getParameter("priceRange[max]");
            if (minPrice != null && maxPrice != null) {
                try {
                    double min = Double.parseDouble(minPrice);
                    double max = Double.parseDouble(maxPrice);
                    if (min >= 0 && max >= 0 && min <= max) {
                        c1.add(Restrictions.ge("price", min));
                        c1.add(Restrictions.le("price", max));
                    }
                } catch (NumberFormatException e) {
                    // Handle invalid price format
                }
            }

            // Get search key
            String searchKey = request.getParameter("searchKey");
            if (searchKey != null && !searchKey.trim().isEmpty()) {
                c1.add(Restrictions.or(
                    Restrictions.ilike("title", "%" + searchKey.trim() + "%"),
                    Restrictions.ilike("description", "%" + searchKey.trim() + "%")
                ));
            }

            // Filter by active status
            Status status = (Status) s.get(Status.class, SearchProducts.ACTIVE_ID); // get Active product [1 = Active]
            if (status != null) {
                c1.add(Restrictions.eq("status", status));
            }

            // Get pagination parameter first
            String firstResultStr = request.getParameter("firstResult");
            int firstResult = 0;
            if (firstResultStr != null) {
                try {
                    firstResult = Integer.parseInt(firstResultStr);
                    if (firstResult < 0) {
                        firstResult = 0;
                    }
                } catch (NumberFormatException e) {
                    // Handle invalid firstResult format
                    firstResult = 0;
                }
            }

            // Get total count without pagination
            @SuppressWarnings("unchecked")
            List<Product> allProducts = c1.list();
            int totalProducts = allProducts.size();
            resposeObject.addProperty("totalProducts", totalProducts);

            // Get sort order
            String sortBy = request.getParameter("sortBy");
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                if (sortBy.equals("Sort by Latest")) {
                    c1.addOrder(Order.desc("id"));
                } else if (sortBy.equals("Sort by Oldest")) {
                    c1.addOrder(Order.asc("id"));
                } else if (sortBy.equals("Sort by Name")) {
                    c1.addOrder(Order.asc("title"));
                } else if (sortBy.equals("Sort by Price")) {
                    c1.addOrder(Order.asc("price"));
                }
            } else {
                // Default sort by latest
                c1.addOrder(Order.desc("id"));
            }

            // Apply pagination for actual results
            c1.setFirstResult(firstResult);
            c1.setMaxResults(SearchProducts.MAX_RESULT);

            // Get filtered product list with pagination
            @SuppressWarnings("unchecked")
            List<Product> products = c1.list();
            
            // Clean up user data for security
            for (Product product : products) {
                product.setUser(null);
            }

            resposeObject.add("products", gson.toJsonTree(products));
            resposeObject.addProperty("firstResult", firstResult);
            resposeObject.addProperty("maxResults", SearchProducts.MAX_RESULT);
            
        } catch (Exception e) {
            resposeObject.addProperty("status", false);
            resposeObject.addProperty("error", "Error occurred while searching products: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // hibernate session close
            s.close();
        }

        String toJson = gson.toJson(resposeObject);
        response.getWriter().write(toJson);
    }
}