package org.openmrs.module.atomfeed.transaction.support;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.jdbc.ReturningWork;
import org.ict4h.atomfeed.jdbc.JdbcConnectionProvider;
import org.ict4h.atomfeed.transaction.AFTransactionManager;
import org.ict4h.atomfeed.transaction.AFTransactionWork;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class AtomFeedSpringTransactionManager implements AFTransactionManager, JdbcConnectionProvider {
    private PlatformTransactionManager transactionManager;
    private Map<AFTransactionWork.PropagationDefinition, Integer> propagationMap = new HashMap<AFTransactionWork.PropagationDefinition, Integer>();

    public AtomFeedSpringTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        propagationMap.put(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRED, TransactionDefinition.PROPAGATION_REQUIRED);
        propagationMap.put(AFTransactionWork.PropagationDefinition.PROPAGATION_REQUIRES_NEW, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T executeWithTransaction(final AFTransactionWork<T> action) throws RuntimeException {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Integer txPropagationDef = getTxPropagation(action.getTxPropagationDefinition());
        transactionTemplate.setPropagationBehavior(txPropagationDef);
        return transactionTemplate.execute( new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                return action.execute();
            }
        });
    }

    private Integer getTxPropagation(AFTransactionWork.PropagationDefinition propagationDefinition) {
        return propagationMap.get(propagationDefinition);
    }

    /**
     * @see org.ict4h.atomfeed.jdbc.JdbcConnectionProvider
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        //TODO: ensure that only connection associated with current thread current transaction is given
    	Connection connection = getSession().doReturningWork(new ReturningWork<Connection>() {
            @Override
            public Connection execute(Connection conn) throws SQLException {
                return conn;
            }
        });
    	return connection;
    }

    private DbSession getSession() {
        ServiceContext serviceContext = ServiceContext.getInstance();
        Class klass = serviceContext.getClass();
        try {
            Field field = klass.getDeclaredField("applicationContext");
            field.setAccessible(true);
            ApplicationContext applicationContext = (ApplicationContext) field.get(serviceContext);
            DbSessionFactory factory = (DbSessionFactory) applicationContext.getBean("dbSessionFactory");
            return factory.getCurrentSession();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
