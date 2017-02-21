package org.openmrs.module.atomfeed;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;

public class Utils {

	static Log log = LogFactory.getLog("org.openmrs.module.atomfeed");
	//static org.apache.log4j.Logger l = org.apache.log4j.Logger.getLogger("org.openmrs.module.atomfeed" );
//	static org.slf4j.Logger lflog = LoggerFactory.getLogger("org.openmrs.module.atomfeed");
	
	public static void logInfo(String message){
		//l.info("L:"+message);
		log.info("LOG:"+message);
		//lflog.info("LFLOG:"+message);
	}
	
	public static void logDebug(String message){
		//l.info("L:"+message);
		log.debug("LOG:"+message);
		//lflog.info("LFLOG:"+message);
	}
	
	public static void logWarn(String message){
		//l.info("L:"+message);
		log.warn("LOG:"+message);
		//lflog.info("LFLOG:"+message);
	}
	
	public static boolean shouldCreateAtomfeed(User dataEntryUser) {
    	String ignoreRoles = Context.getAdministrationService().getGlobalProperty("atomfeed.data-update.data-entry.roles-to-ignore");
    	logDebug("Ignorable Roles list:"+ignoreRoles);
    	if(StringUtils.isBlank(ignoreRoles)){//no role to ignore. go ahead and create feed
    		return true;
    	}
    	String[] rl = ignoreRoles.trim().split(",");
    	for (String r : rl) {
    		for (Role drl : dataEntryUser.getRoles()) {
    			// has a role marked as ignorable.. donot create atomfeed.. donot use user.hasRole .. it doesnt work
				if(drl.getName().equalsIgnoreCase(r.trim())){
					logInfo("Ignorable Role found:"+drl.getName());
					return false;
				}
			}
		}
		return true;// create atomfeed. no role found to be ignored
	}
	
	public static String formatDataEntrySource(User user) {
		String deSource = "";
		for (Role r : user.getRoles()) {
			deSource += r.getName()+",";
		}
		return deSource;
	}
}
