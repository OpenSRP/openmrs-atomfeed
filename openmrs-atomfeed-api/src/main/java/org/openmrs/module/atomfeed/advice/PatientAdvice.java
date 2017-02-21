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
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtra;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtraJdbcImpl;
import org.openmrs.module.atomfeed.api.EventRecordExtra;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.transaction.PlatformTransactionManager;

public class PatientAdvice implements AfterReturningAdvice, MethodBeforeAdvice {
    private static final String TEMPLATE = "/openmrs/ws/rest/v1/patient/%s?v=full";
    public static final String CATEGORY = "OpenSRP_Patient";
    public static final String SAVE_PATIENT_METHOD = "savePatient";
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;
    
    private Boolean isNewEntry;
    
	protected Log log = LogFactory.getLog(getClass());

    public PatientAdvice() throws SQLException {
        atomFeedSpringTransactionManager = new AtomFeedSpringTransactionManager(getSpringPlatformTransactionManager());
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);

        this.eventService = new EventServiceImpl(allEventRecordsQueue);
    }
        
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] arguments, Object target) throws Throwable {
    	if (method.getName().equals(SAVE_PATIENT_METHOD)) {
    		logDebug("In method SAVE_PATIENT for atomfeed with isNewEntry="+isNewEntry);
    		
        	User u = Context.getAuthenticatedUser();
        	boolean shouldCreateAtomfeed = shouldCreateAtomfeed(u);
        	
        	if(shouldCreateAtomfeed == false){
				logInfo("PATIENT: Atomfeed would be skipped (not created) as current user data entries are configured to be ignored");
				return;
        	}
        	
	        if (isNewEntry == null) {
				logWarn("PATIENT: Can not determine the transaction type. Data syncer Service should handle Save and Update on its own");
				process((Patient) returnValue, "Patient", formatDataEntrySource(u));
	        }
	        // if new entry and not sent from SRP i.e. skip entry sent via opensrp
	        else if(isNewEntry){
	        	logDebug("PATIENT: Insert found. Creating patient_save atomfeed for "+u);
	        	process((Patient) returnValue, "Patient_Save", formatDataEntrySource(u));
	        }
	        // if not a new entry and no role for data edit found ignorable
	        else if(isNewEntry == false){
        		logInfo("PATIENT: Creating atomfeed for patient_update for "+u);
        		process((Patient) returnValue, "Patient_Update", formatDataEntrySource(u));	        		
	        }            
        }
    }
    
	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals(SAVE_PATIENT_METHOD)) {
    		logDebug("In method SAVE_PATIENT for atomfeed before advice");

			Patient p = args.length>0&&args[0] instanceof Patient?(Patient)args[0]:null;
			if(p != null){
				if(p.getPatientId() == null || p.getPatientId() <= 0){
					isNewEntry = true;
				}
				else {
					isNewEntry = false;
				}
			}
			else {
				logWarn("NO Patient argument found for before advice. Can not determine transaction type");
			}
		}
	}
    
	private void process(Patient patient, String title, final String dataEnrySource) {
		String contents = String.format(TEMPLATE, patient.getUuid());
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
