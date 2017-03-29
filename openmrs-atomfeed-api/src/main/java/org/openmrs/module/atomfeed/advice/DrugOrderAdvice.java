package org.openmrs.module.atomfeed.advice;

import static org.openmrs.module.atomfeed.Utils.formatDataEntrySource;
import static org.openmrs.module.atomfeed.Utils.logDebug;
import static org.openmrs.module.atomfeed.Utils.logInfo;
import static org.openmrs.module.atomfeed.Utils.logWarn;
import static org.openmrs.module.atomfeed.Utils.shouldCreateAtomfeed;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.joda.time.DateTime;
import org.openmrs.Order;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtra;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtraJdbcImpl;
import org.openmrs.module.atomfeed.api.EventRecordExtra;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.transaction.PlatformTransactionManager;

public class DrugOrderAdvice implements AfterReturningAdvice, MethodBeforeAdvice {
    private static final String TEMPLATE = "/openmrs/ws/rest/v1/order/%s?v=full";
    public static final String CATEGORY = "OpenSRP_DrugOrder";
    public static final String SAVE_METHOD = "saveOrder";
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;
    
    private Boolean isNewEntry;
    
	protected Log log = LogFactory.getLog(getClass());
	private boolean isDrugOrder;

    public DrugOrderAdvice() throws SQLException {
        atomFeedSpringTransactionManager = new AtomFeedSpringTransactionManager(getSpringPlatformTransactionManager());
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);

        this.eventService = new EventServiceImpl(allEventRecordsQueue);
    }
        
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] arguments, Object target) throws Throwable {
    	if (method.getName().equals(SAVE_METHOD) && isDrugOrder) {
    		logDebug("In method SAVE_DRUG_ORDER for atomfeed with isNewEntry="+isNewEntry);
    		
        	User u = Context.getAuthenticatedUser();
        	boolean shouldCreateAtomfeed = shouldCreateAtomfeed(u);
        	
        	if(shouldCreateAtomfeed == false){
				logInfo("DRUG_ORDER: Atomfeed would be skipped (not created) as current user data entries are configured to be ignored");
				return;
        	}
        	
	        if (isNewEntry == null) {
				logWarn("DRUG_ORDER: Can not determine the transaction type. Data syncer Service should handle Save and Update on its own");
				process((Order) returnValue, "Order", formatDataEntrySource(u));
	        }
	        // if new entry and not sent from SRP i.e. skip entry sent via opensrp
	        else if(isNewEntry){
	        	logDebug("DRUG_ORDER: Insert found. Creating drug_order_save atomfeed for "+u);
	        	process((Order) returnValue, "DrugOrder_Save", formatDataEntrySource(u));
	        }
	        // if not a new entry and no role for data edit found ignorable
	        else if(isNewEntry == false){
        		logInfo("ORDER: Creating atomfeed for order_update for "+u);
        		process((Order) returnValue, "DrugOrder_Update", formatDataEntrySource(u));	        		
	        }            
        }
    }
    
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		Order o = args.length>0&&args[0] instanceof Order?(Order)args[0]:null;
		isDrugOrder = o != null && ((Order) o).getOrderType().getName().equalsIgnoreCase("drugorder");
		if (method.getName().equals(SAVE_METHOD) && isDrugOrder) {
    		logDebug("In method SAVE_DRUG_ORDER for atomfeed before advice");

			if(o != null){
				if(o.getId() == null || o.getId() <= 0){
					isNewEntry = true;
				}
				else {
					isNewEntry = false;
				}
			}
			else {
				logWarn("NO DrugOrder argument found for before advice. Can not determine transaction type");
			}
		}
	}
    
	private void process(Order order, String title, final String dataEnrySource) {
		String contents = String.format(TEMPLATE, order.getUuid());
        final Event event = new Event(UUID.randomUUID().toString(), title, DateTime.now(), (URI) null, contents, CATEGORY);
        atomFeedSpringTransactionManager.executeWithTransaction(
            new AFTransactionWorkWithoutResult() {
                @Override
                protected void doInTransaction() {
                    eventService.notify(event);
                    try{
                    	AllEventRecordsExtra erx = new AllEventRecordsExtraJdbcImpl(atomFeedSpringTransactionManager);
                    	EventRecordExtra eventRecordextra = new EventRecordExtra(UUID.randomUUID().toString(), 
                    			event.getUuid(), "DATA_ENTRY_SOURCE", dataEnrySource);
						erx.add(eventRecordextra );
                    }
                    catch(Exception e){
                    	e.printStackTrace();//TODO
                    }
                }
                @Override
                public PropagationDefinition getTxPropagationDefinition() {
                    return PropagationDefinition.PROPAGATION_REQUIRED;
                }
            }
        );
	}
	
    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
}
