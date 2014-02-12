package org.openmrs.module.atomfeed.common.repository;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ict4h.atomfeed.jdbc.JdbcConnectionProvider;
import org.openmrs.api.context.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Scope("prototype")
 public class OpenMRSJdbcConnectionProvider implements JdbcConnectionProvider {

    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus; // TODO : Mujir/Sush - can this be a field. One instance of bean.

    @Autowired
    public OpenMRSJdbcConnectionProvider(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getSession().connection();
    }

    private Session getSession() {
        ServiceContext serviceContext = ServiceContext.getInstance();
        Class klass = serviceContext.getClass();
        try {
            Field field = klass.getDeclaredField("applicationContext");
            field.setAccessible(true);
            ApplicationContext applicationContext = (ApplicationContext) field.get(serviceContext);
            SessionFactory factory = (SessionFactory) applicationContext.getBean("sessionFactory");
            return factory.getCurrentSession();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startTransaction() {
        transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
    }

    @Override
    public void commit() {
        transactionManager.commit(transactionStatus);
    }

    @Override
    public void rollback() {
        transactionManager.rollback(transactionStatus);
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
    }
}