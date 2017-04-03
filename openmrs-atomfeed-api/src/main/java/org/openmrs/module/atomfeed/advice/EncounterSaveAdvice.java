package org.openmrs.module.atomfeed.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.ict4h.atomfeed.transaction.AFTransactionWork.PropagationDefinition;
import org.joda.time.DateTime;
import org.openmrs.Encounter;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.EventPublishFilterHook;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtra;
import org.openmrs.module.atomfeed.api.AllEventRecordsExtraJdbcImpl;
import org.openmrs.module.atomfeed.api.EventRecordExtra;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.transaction.PlatformTransactionManager;

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

public class EncounterSaveAdvice implements AfterReturningAdvice, MethodBeforeAdvice{
    public static final String ENCOUNTER_REST_URL = getEncounterFeedUrl();//TODO

    public static final String CATEGORY = "OpenSRP_Encounter";
    private final AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;

    private static final String SAVE_METHOD = "saveEncounter";

    private EventService eventService;
    private Boolean isNewEntry;

    public EncounterSaveAdvice() throws SQLException {
        PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
        atomFeedSpringTransactionManager = new AtomFeedSpringTransactionManager(platformTransactionManager);
        AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);
        this.eventService = new EventServiceImpl(allEventRecordsQueue);
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object encounterService) throws Throwable {
        if (method.getName().equals(SAVE_METHOD)) {
    		logDebug("In method SAVE_ENCOUNTER for atomfeed with isNewEntry="+isNewEntry);

    		Encounter en = (Encounter) returnValue;
    		
    		User u = Context.getAuthenticatedUser();
        	boolean shouldCreateAtomfeed = shouldCreateAtomfeed(u);
        	
        	if(shouldCreateAtomfeed == false){
				logInfo("ENCOUNTER: Atomfeed would be skipped (not created) as current user data entries are configured to be ignored");
				return;
        	}
        	
	        if (isNewEntry == null) {
				logWarn("ENCOUNTER: Can not determine the transaction type. Data syncer Service should handle Save and Update on its own");
				process(en, "Encounter", formatDataEntrySource(u));
	        }
	        // if new entry and not sent from SRP i.e. skip entry sent via opensrp
	        else if(isNewEntry){
	        	logDebug("Insert found. Creating encounter_save atomfeed for "+u);
	        	process(en, "Encounter_Save", formatDataEntrySource(u));
	        }
	        // if not a new entry and no role for data edit found ignorable
	        else if(isNewEntry == false){
        		logInfo("Creating atomfeed for encounter_update for "+u);
        		process(en, "Encounter_Update", formatDataEntrySource(u));	        		
	        }            
        }
    }
    
    @Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		if (method.getName().equals(SAVE_METHOD)) {
    		logDebug("In method SAVE_ENCOUNTER for atomfeed before advice");

    		Encounter e = args.length>0&&args[0] instanceof Encounter?(Encounter)args[0]:null;
			if(e != null){
				if(e.getEncounterId() == null || e.getEncounterId() <= 0){
					isNewEntry = true;
				}
				else {
					isNewEntry = false;
				}
			}
			else {
				logWarn("NO Encounter argument found for before advice. Can not determine transaction type");
			}
		}
	}

    private void process(Encounter encounter, String title, final String dataEnrySource) {
    	if(encounter == null){
			return;
		}
		String contents = String.format(ENCOUNTER_REST_URL, encounter.getUuid());
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
    
    private static String getEncounterFeedUrl() {
        return Context.getAdministrationService().getGlobalProperty("atomfeed.encounter.feed.publish.url");
    }

    private PlatformTransactionManager getSpringPlatformTransactionManager() {
        List<PlatformTransactionManager> platformTransactionManagers = Context.getRegisteredComponents(PlatformTransactionManager.class);
        return platformTransactionManagers.get(0);
    }
}
