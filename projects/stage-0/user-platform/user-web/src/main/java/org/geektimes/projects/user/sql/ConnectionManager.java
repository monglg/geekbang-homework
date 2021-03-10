package org.geektimes.projects.user.sql;

import org.geektimes.projects.user.domain.User;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ConnectionManager {

    private static Connection connection;

    public static final String DROP_USERS_TABLE_DDL_SQL = "DROP TABLE users";

    public static final String CREATE_USERS_TABLE_DDL_SQL = "CREATE TABLE users(" +
            "id INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
            "name VARCHAR(16) NOT NULL, " +
            "password VARCHAR(64) NOT NULL, " +
            "email VARCHAR(64) NOT NULL, " +
            "phoneNumber VARCHAR(64) NOT NULL" +
            ")";

    public static Connection getConnection() {
        return connection;
    }

    static {
        try {
            synchronized (ConnectionManager.class) {
                if (connection == null) {

                    String databaseURL = "jdbc:derby:user-platform;create=true";
                    connection = DriverManager.getConnection(databaseURL);

                    DatabaseMetaData meta = connection.getMetaData();
                    ResultSet res = meta.getTables(null, null, null, new String[]{"TABLE"});
                    HashSet<String> set=new HashSet<String>();
                    while (res.next()) {
                        set.add(res.getString("TABLE_NAME").toLowerCase());
                    }

                    Statement statement = connection.createStatement();
                    if (set.contains("users")) {
                        // 删除 users 表
                        statement.execute(DROP_USERS_TABLE_DDL_SQL);
                    }

                    // 创建 users 表
                    statement.execute(CREATE_USERS_TABLE_DDL_SQL);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getCause());
        }
    }

    public static void releaseConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public static void main(String[] args) {
        getConnection();
    }
}
