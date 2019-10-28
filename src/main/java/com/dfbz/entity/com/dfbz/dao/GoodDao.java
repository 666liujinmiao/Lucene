package com.dfbz.entity.com.dfbz.dao;

import com.dfbz.entity.Good;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GoodDao {

    public List<Good> findAll(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn= DriverManager.getConnection("jdbc:mysql:///liu","root","root");
            Statement st = conn.createStatement();

            ArrayList<Good> goods = new ArrayList<Good>();

            ResultSet rs = st.executeQuery("select * from goods");
            while(rs.next()){
                Integer id=rs.getInt("id");
                String name=rs.getString("name");
                String title=rs.getString("title");
                Double price=rs.getDouble("price");
                String pic=rs.getString("pic");

                Good good = new Good();
                good.setId(id);
                good.setName(name);
                good.setPic(pic);
                good.setPrice(price);
                good.setTitle(title);

                goods.add(good);

            }

            conn.close();
            return goods;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
