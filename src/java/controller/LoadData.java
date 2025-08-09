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
import java.util.Locale;
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
@WebServlet(name = "LoadData", urlPatterns = {"/LoadData"})
public class LoadData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        Gson gson = new Gson();
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session s = sf.openSession();

        //search-brands
        Criteria c1 = s.createCriteria(Brand.class);
        List<Brand> brandList = c1.list();

        //get-models
        Criteria c2 = s.createCriteria(Model.class);
        List<Model> modelList = c2.list();
        //get-models-end

        //get-colors
        Criteria c3 = s.createCriteria(Color.class);
        List<Color> colorList = c3.list();
        //get-colors-end

        //get-storage
        Criteria c4 = s.createCriteria(Storage.class);
        List<Storage> storageList = c4.list();
        //get-storage-end

        //get-quality
        Criteria c5 = s.createCriteria(Quality.class);
        List<Quality> qualityList = c5.list();
        //get-quality-end

        responseObject.add("brands", gson.toJsonTree(brandList));
        responseObject.add("colors", gson.toJsonTree(colorList));
        responseObject.add("storages", gson.toJsonTree(storageList));
        responseObject.add("qualities", gson.toJsonTree(qualityList));

        String toJson = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(toJson);
        s.close();
    }

}
