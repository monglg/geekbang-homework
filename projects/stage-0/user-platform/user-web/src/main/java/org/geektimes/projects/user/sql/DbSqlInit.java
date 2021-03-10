package org.geektimes.projects.user.sql;

import org.geektimes.projects.user.web.context.ComponentContext;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashSet;

public class DbSqlInit {


    public static final String DROP_USERS_TABLE_DDL_SQL = "DROP TABLE users";

    public static final String CREATE_USERS_TABLE_DDL_SQL = "CREATE TABLE users(" +
            "id INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), " +
            "name VARCHAR(16) NOT NULL, " +
            "password VARCHAR(64) NOT NULL, " +
            "email VARCHAR(64) NOT NULL, " +
            "phoneNumber VARCHAR(64) NOT NULL" +
            ")";

    public static void initSql() {
        try {
            synchronized (DbSqlInit.class) {

                DataSource dataSource = ComponentContext.getComponentContext().getComponent("jdbc/UserPlatformDB");
                Connection connection = dataSource.getConnection();
                DatabaseMetaData meta = connection.getMetaData();
                ResultSet res = meta.getTables(null, null, null, new String[]{"TABLE"});
                HashSet<String> set = new HashSet<String>();
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

        } catch (SQLException e) {
            throw new RuntimeException(e.getCause());
        }
    }

}
