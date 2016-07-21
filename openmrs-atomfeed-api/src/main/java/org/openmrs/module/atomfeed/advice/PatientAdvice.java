package org.openmrs.module.atomfeed.advice;

import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.joda.time.DateTime;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class PatientAdvice implements AfterReturningAdvice, MethodBeforeAdvice {
    private static final String TEMPLATE = "/openmrs/ws/rest/v1/patient/%s?v=full";
    public static final String CATEGORY = "OpenSRP_Patient";
    public static final String SAVE_PATIENT_METHOD = "savePatient";
    private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
    private EventService eventService;
    
    private Boolean isNewEntry;
    
    private Logger log = Logger.getLogger(getClass().getName());

    public PatientAdvice() throws SQLException {
        atomFeedSpringTransactionManager = new AtomFeedSpringTransactionManager(getSpringPlatformTransactionManager());
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);

        this.eventService = new EventServiceImpl(allEventRecordsQueue);
    }

    // is save patient id is null or lte 0 but in before advice
    // is update patient id is valid
    
    private boolean hasOpenSRPIdentifier(Patient p) {
		for (PatientIdentifier pi : p.getIdentifiers()) {
			if(pi.getIdentifierType().getName().toLowerCase().matches("opensrp.*uid")){
				return true;
			}
		}
		return false;
	}
        
    @Override
    public void afterReturning(Object returnValue, Method method, Object[] arguments, Object target) throws Throwable {
    	if (method.getName().equals(SAVE_PATIENT_METHOD)) {
	        if (isNewEntry == null) {
				log.info("Can not determine the transaction type. Service should read entries not having OpenSRP UID");
				process((Patient) returnValue, "Patient");
	        }
	        // if new entry and not sent from SRP i.e. skip entry sent via opensrp
	        else if(isNewEntry && !hasOpenSRPIdentifier((Patient) returnValue)){
	        	process((Patient) returnValue, "Patient_Save");
	        }
	        // if not a new entry
	        else if(isNewEntry == false){
	        	process((Patient) returnValue, "Patient_Update");
	        }            
        }
    }

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals(SAVE_PATIENT_METHOD)) {
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
				log.info("NO Patient argument found for before advice. Can not determine transaction type");
			}
		}
	}
    
	private void process(Patient patient, String title) {
		String contents = String.format(TEMPLATE, patient.getUuid());
        final Event event = new Event(UUID.randomUUID().toString(), title, DateTime.now(), (URI) null, contents, CATEGORY);

        atomFeedSpringTransactionManager.executeWithTransaction(
            new AFTransactionWorkWithoutResult() {
                @Override
                protected void doInTransaction() {
                    eventService.notify(event);
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
