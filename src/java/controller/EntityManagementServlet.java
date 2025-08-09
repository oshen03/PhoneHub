package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "EntityManagementServlet", urlPatterns = {"/EntityManagementServlet"})
public class EntityManagementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String entity = request.getParameter("entity");
        String action = request.getParameter("action");
        
        if ("list".equals(action)) {
            listEntities(entity, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String entity = request.getParameter("entity");
        String action = request.getParameter("action");
        
        if ("add".equals(action)) {
            addEntity(entity, request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String entity = request.getParameter("entity");
        String id = request.getParameter("id");
        deleteEntity(entity, id, response);
    }

    private void listEntities(String entity, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        
        try {
            JsonArray entitiesArray = new JsonArray();
            
            switch (entity.toLowerCase()) {
                case "brand":
                    List<Brand> brands = session.createCriteria(Brand.class).addOrder(Order.asc("name")).list();
                    for (Brand brand : brands) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", brand.getId());
                        obj.addProperty("name", brand.getName());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "color":
                    List<Color> colors = session.createCriteria(Color.class).addOrder(Order.asc("value")).list();
                    for (Color color : colors) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", color.getId());
                        obj.addProperty("value", color.getValue());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "storage":
                    List<Storage> storages = session.createCriteria(Storage.class).addOrder(Order.asc("value")).list();
                    for (Storage storage : storages) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", storage.getId());
                        obj.addProperty("value", storage.getValue());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "quality":
                    List<Quality> qualities = session.createCriteria(Quality.class).addOrder(Order.asc("value")).list();
                    for (Quality quality : qualities) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", quality.getId());
                        obj.addProperty("value", quality.getValue());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "city":
                    List<City> cities = session.createCriteria(City.class).addOrder(Order.asc("name")).list();
                    for (City city : cities) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", city.getId());
                        obj.addProperty("name", city.getName());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "deliverytype":
                    List<DeliveryTypes> deliveryTypes = session.createCriteria(DeliveryTypes.class).addOrder(Order.asc("name")).list();
                    for (DeliveryTypes dt : deliveryTypes) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", dt.getId());
                        obj.addProperty("name", dt.getName());
                        obj.addProperty("price", dt.getPrice());
                        entitiesArray.add(obj);
                    }
                    break;
                    
                case "model":
                    List<Model> models = session.createCriteria(Model.class).addOrder(Order.asc("name")).list();
                    for (Model model : models) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", model.getId());
                        obj.addProperty("name", model.getName());
                        if (model.getBrand() != null) {
                            obj.addProperty("brandId", model.getBrand().getId());
                            obj.addProperty("brandName", model.getBrand().getName());
                        }
                        entitiesArray.add(obj);
                    }
                    break;
                    
                default:
                    responseObject.addProperty("message", "Unknown entity type");
                    break;
            }
            
            responseObject.add("entities", entitiesArray);
            responseObject.addProperty("status", true);
            
        } catch (Exception e) {
            responseObject.addProperty("message", "Error loading entities: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void addEntity(String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        Transaction tx = null;
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject input = gson.fromJson(reader, JsonObject.class);
            
            tx = session.beginTransaction();
            
            switch (entity.toLowerCase()) {
                case "brand":
                    Brand brand = new Brand();
                    brand.setName(input.get("name").getAsString());
                    session.save(brand);
                    break;
                    
                case "color":
                    Color color = new Color();
                    color.setValue(input.get("value").getAsString());
                    session.save(color);
                    break;
                    
                case "storage":
                    Storage storage = new Storage();
                    storage.setValue(input.get("value").getAsString());
                    session.save(storage);
                    break;
                    
                case "quality":
                    Quality quality = new Quality();
                    quality.setValue(input.get("value").getAsString());
                    session.save(quality);
                    break;
                    
                case "city":
                    City city = new City();
                    city.setName(input.get("name").getAsString());
                    session.save(city);
                    break;
                    
                case "deliverytype":
                    DeliveryTypes dt = new DeliveryTypes();
                    dt.setName(input.get("name").getAsString());
                    dt.setPrice(input.get("price").getAsDouble());
                    session.save(dt);
                    break;
                    
                case "model":
                    Model model = new Model();
                    model.setName(input.get("name").getAsString());
                    int brandId = input.get("brandId").getAsInt();
                    Brand modelBrand = (Brand) session.get(Brand.class, brandId);
                    model.setBrand(modelBrand);
                    session.save(model);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entity);
            }
            
            tx.commit();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", entity + " added successfully");
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error adding " + entity + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }

    private void deleteEntity(String entity, String id, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();
        Transaction tx = null;
        
        try {
            tx = session.beginTransaction();
            int entityId = Integer.parseInt(id);
            
            switch (entity.toLowerCase()) {
                case "brand":
                    Brand brand = (Brand) session.get(Brand.class, entityId);
                    if (brand != null) session.delete(brand);
                    break;
                    
                case "color":
                    Color color = (Color) session.get(Color.class, entityId);
                    if (color != null) session.delete(color);
                    break;
                    
                case "storage":
                    Storage storage = (Storage) session.get(Storage.class, entityId);
                    if (storage != null) session.delete(storage);
                    break;
                    
                case "quality":
                    Quality quality = (Quality) session.get(Quality.class, entityId);
                    if (quality != null) session.delete(quality);
                    break;
                    
                case "city":
                    City city = (City) session.get(City.class, entityId);
                    if (city != null) session.delete(city);
                    break;
                    
                case "deliverytype":
                    DeliveryTypes dt = (DeliveryTypes) session.get(DeliveryTypes.class, entityId);
                    if (dt != null) session.delete(dt);
                    break;
                    
                case "model":
                    Model model = (Model) session.get(Model.class, entityId);
                    if (model != null) session.delete(model);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entity);
            }
            
            tx.commit();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", entity + " deleted successfully");
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            responseObject.addProperty("message", "Error deleting " + entity + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseObject));
    }
}