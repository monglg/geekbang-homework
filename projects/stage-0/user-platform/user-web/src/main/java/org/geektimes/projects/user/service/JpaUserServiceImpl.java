package org.geektimes.projects.user.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.repository.DatabaseUserRepository;
import org.geektimes.projects.user.repository.UserRepository;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @Author: menglinggang
 * @Date: 2021-03-03
 * @Time: 5:27 下午
 */
public class JpaUserServiceImpl implements UserService {

    Logger logger = Logger.getLogger(JpaUserServiceImpl.class.getName());

    @Resource(name = "bean/EntityManager")
    private EntityManager entityManager;
    @Resource(name = "bean/Validator")
    private Validator validator;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean register(User user) {

        Set<ConstraintViolation<User>> validates = validator.validate(user);
        logger.log(Level.SEVERE, "" + validates.size());


        entityManager.getTransaction().begin();
        entityManager.persist(user);
        entityManager.getTransaction().commit();
        return true;
    }

    @Override
    public boolean deregister(User user) {
        return update(user);
    }

    @Override
    public boolean update(User user) {
        return true;
    }

    @Override
    public User queryUserById(Long id) {
        return entityManager.find(User.class, id);
    }

    @Override
    public User queryUserByNameAndPassword(String name, String password) {
        Query query = entityManager.createQuery("select u from User u where u.name = ?1 and u.password = ?2");
        query.setParameter(1, name);
        query.setParameter(2, password);

        Object singleResult = query.getSingleResult();
        if (singleResult instanceof User) {
             return (User) singleResult;
        }

        return null;
    }


}
