package org.geektimes.projects.user.repository;

import org.geektimes.function.ThrowableFunction;
import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.sql.ConnectionManager;
import org.geektimes.projects.user.sql.DBConnectionManager;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;

public class DatabaseUserRepository implements UserRepository {

    private static Logger logger = Logger.getLogger(DatabaseUserRepository.class.getName());

    /**
     * 通用处理方式
     */
    private static Consumer<Throwable> COMMON_EXCEPTION_HANDLER = e -> logger.log(Level.SEVERE, e.getMessage());

    public static final String INSERT_USER_DML_SQL = "INSERT INTO users(name,password,email,phoneNumber) VALUES (?,?,?,?)";

    public static final String QUERY_ALL_USERS_DML_SQL = "SELECT id,name,password,email,phoneNumber FROM users";

    public static final String QUERY_BY_ID_USERS_DML_SQL = "SELECT id,name,password,email,phoneNumber FROM  users where id = ?";

    public static final String DELETE_BY_ID_USERS_DML_SQL = "DELETE FROM users where id = ?";

    public static final String UPDATE_BY_ID_USERS_DML_SQL = "UPDATE users SET name= ?, password=?, email = ?, phoneNumber FROM users where id = ?";


    public DatabaseUserRepository() {
    }

    private Connection getConnection() {
        return ConnectionManager.getConnection();
    }

    @Override
    public boolean save(User user) {
        boolean success = executeUpdate(INSERT_USER_DML_SQL, COMMON_EXCEPTION_HANDLER, user.getName(),user.getPassword(), user.getEmail(), user.getPhoneNumber());
        return success;
    }

    @Override
    public boolean deleteById(Long userId) {
    boolean success = executeUpdate(DELETE_BY_ID_USERS_DML_SQL, COMMON_EXCEPTION_HANDLER, userId);
    return success;
}
    @Override
    public boolean update(User user) {
        boolean success = executeUpdate(UPDATE_BY_ID_USERS_DML_SQL, COMMON_EXCEPTION_HANDLER, user.getName(),user.getPassword(), user.getEmail(), user.getPhoneNumber());
        return success;
    }

    @Override
    public User getById(Long userId) {
        return executeQuery(QUERY_BY_ID_USERS_DML_SQL, resultSet -> {
            while (resultSet.next()) { // 如果存在并且游标滚动 // SQLException
                return parseResultSet(resultSet, User.class);
            }
            return null;
        }, COMMON_EXCEPTION_HANDLER, userId);
    }

    @Override
    public User getByNameAndPassword(String userName, String password) {
        return executeQuery("SELECT id,name,password,email,phoneNumber FROM users WHERE name=? and password=?",
                resultSet -> {
                    while (resultSet.next()) { // 如果存在并且游标滚动 // SQLException
                        return parseResultSet(resultSet, User.class);
                    }
                    return null;
                }, COMMON_EXCEPTION_HANDLER, userName, password);
    }

    @Override
    public Collection<User> getAll() {
        return executeQuery(QUERY_ALL_USERS_DML_SQL, resultSet -> {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) { // 如果存在并且游标滚动 // SQLException
                User user = parseResultSet(resultSet, User.class);
                if (user != null){
                    users.add(user);
                }
            }
            return users;
        }, e -> {
            // 异常处理
        });
    }

    private <T> T parseResultSet(ResultSet resultSet, Class<T> resultClass) throws Exception {
        // BeanInfo -> IntrospectionException
        BeanInfo userBeanInfo = Introspector.getBeanInfo(resultClass, Object.class);
        T t = resultClass.newInstance();
        for (PropertyDescriptor propertyDescriptor : userBeanInfo.getPropertyDescriptors()) {
            String fieldName = propertyDescriptor.getName();
            Class fieldType = propertyDescriptor.getPropertyType();
            String methodName = resultSetMethodMappings.get(fieldType);
            // 可能存在映射关系（不过此处是相等的）
            String columnLabel = mapColumnLabel(fieldName);
            Method resultSetMethod = ResultSet.class.getMethod(methodName, String.class);
            // 通过放射调用 getXXX(String) 方法
            Object resultValue = resultSetMethod.invoke(resultSet, columnLabel);
            // 获取 User 类 Setter方法
            // PropertyDescriptor ReadMethod 等于 Getter 方法
            // PropertyDescriptor WriteMethod 等于 Setter 方法
            Method setterMethodFromUser = propertyDescriptor.getWriteMethod();
            // 以 id 为例，  user.setId(resultSet.getLong("id"));
            setterMethodFromUser.invoke(t, resultValue);
        }
        return t;
    }

    /**
     * @param sql
     * @param function
     * @param <T>
     * @return
     */
    protected <T> T executeQuery(String sql, ThrowableFunction<ResultSet, T> function,
                                 Consumer<Throwable> exceptionHandler, Object... args) {
        Connection connection = getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Class argType = arg.getClass();

                Class wrapperType = wrapperToPrimitive(argType);

                if (wrapperType == null) {
                    wrapperType = argType;
                }

                // Boolean -> boolean
                String methodName = preparedStatementMethodMappings.get(argType);
                Method method = PreparedStatement.class.getMethod(methodName, int.class, wrapperType);
                method.invoke(preparedStatement, i + 1, arg);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            // 返回一个 POJO List -> ResultSet -> POJO List
            // ResultSet -> T
            return function.apply(resultSet);
        } catch (Throwable e) {
            exceptionHandler.accept(e);
        }
        return null;
    }

    protected boolean executeUpdate(String sql, Consumer<Throwable> exceptionHandler, Object... args) {
        Connection connection = getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Class argType = arg.getClass();

                Class wrapperType = wrapperToPrimitive(argType);

                if (wrapperType == null) {
                    wrapperType = argType;
                }

                // Boolean -> boolean
                String methodName = preparedStatementMethodMappings.get(argType);
                Method method = PreparedStatement.class.getMethod(methodName, int.class, wrapperType);
                method.invoke(preparedStatement, i + 1, arg);
            }
            return preparedStatement.execute();
        } catch (Throwable e) {
            exceptionHandler.accept(e);
        }
        return false;
    }


    private static String mapColumnLabel(String fieldName) {
        return fieldName;
    }

    /**
     * 数据类型与 ResultSet 方法名映射
     */
    static Map<Class, String> resultSetMethodMappings = new HashMap<>();

    static Map<Class, String> preparedStatementMethodMappings = new HashMap<>();

    static {
        resultSetMethodMappings.put(Long.class, "getLong");
        resultSetMethodMappings.put(String.class, "getString");
        preparedStatementMethodMappings.put(Long.class, "setLong"); // long
        preparedStatementMethodMappings.put(String.class, "setString"); //
    }
}
