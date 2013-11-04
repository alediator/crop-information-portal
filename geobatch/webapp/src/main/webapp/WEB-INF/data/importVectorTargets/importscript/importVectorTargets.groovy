import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.sql.*;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;

import it.geosolutions.geobatch.actions.ds2ds.dao.FeatureConfiguration;
import it.geosolutions.geobatch.flow.event.ProgressListenerForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.geosolutions.geobatch.action.scripting.ScriptingAction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import it.geosolutions.geobatch.destination.VectorTarget;

public Map execute(Map argsMap) throws Exception {

	Map result = new HashMap();
	List resultEvents = new ArrayList();
	
	final Logger LOGGER = LoggerFactory.getLogger(ScriptingAction.class);
	final ProgressListenerForwarder listenerForwarder=argsMap.get("listenerForwarder");
	
	Queue events = (Queue)argsMap.get("events");
	FileSystemEvent event = (FileSystemEvent) events.peek();
	
	
	try {
		FeatureConfiguration cfg = FeatureConfiguration.fromXML(new FileInputStream(event.getSource()));
		String typeName = cfg.getTypeName();
		if(typeName == null) {
			throw new IllegalArgumentException("No typeName available to import");
		}
		Map<String, Serializable> dbParams = cfg.getDataStore();
		if(dbParams == null) {
			throw new IllegalArgumentException("No datastore connection available in input");
		}
		
		VectorTarget targetsHandler = new VectorTarget(typeName, listenerForwarder);        
		targetsHandler.importTarget(dbParams, null, true);             
		
		resultEvents.add(event);
		result.put("return", resultEvents);
		return result;
	} catch (FileNotFoundException e) {
		throw e;
	} catch (IOException e) {
		if(LOGGER.isErrorEnabled()) {
			LOGGER.error("Error importing arcs: "+e.getMessage());
		}
		throw e;;
	}
}
