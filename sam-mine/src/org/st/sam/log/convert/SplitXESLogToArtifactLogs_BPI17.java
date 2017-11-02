package org.st.sam.log.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.st.sam.log.SEvent;
import org.st.sam.log.XESExport;
import org.st.sam.log.XESImport;

import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.HashSet;
import com.thoughtworks.xstream.core.util.Pool.Factory;

public class SplitXESLogToArtifactLogs_BPI17 {
	
	private final XLog inputlog;
	private XFactory factory = XFactoryRegistry.instance().currentDefault();
	
	public SplitXESLogToArtifactLogs_BPI17(XLog inputlog) {
		this.inputlog = inputlog;
	}
	
	public XLog enrichLog(XLog log) {
		for (XTrace trace : log) enrichTrace(trace);
		return log;
	}
	
	private Set<String> objectTypes = new HashSet<String>();
	private Map<String, Set<String>> childObjects = new HashMap<String,Set<String>>();
	
	public XTrace enrichTrace(XTrace t) {
		
		for (XEvent e : t) {
			String obj = getObject_BPI17(t, e);
			String objID = getObjectID_BPI17(t, e, obj);
			
			String parentObj = getParentObject_BPI17(t, e, obj);
			
			e.getAttributes().put("object:type", factory.createAttributeLiteral("object:type", obj, null));
			e.getAttributes().put("object:instance", factory.createAttributeLiteral("object:instance", objID, null));

			objectTypes.add(obj);
			if (!childObjects.containsKey(obj)) childObjects.put(obj, new HashSet<String>());

			if (parentObj != null) {
				String parentObjID = getObjectID_BPI17(t, e, parentObj);
				e.getAttributes().put("object:parent_type", factory.createAttributeLiteral("object:parent_type", parentObj, null));
				e.getAttributes().put("object:parent_instance", factory.createAttributeLiteral("object:parent_instance", parentObjID, null));
				childObjects.get(parentObj).add(obj);
			}
			
		}
		
		return t;
	}
	
	public String getObject_BPI17(XTrace t, XEvent e) {
		String origin = ((XAttributeLiteral)e.getAttributes().get("EventOrigin")).getValue();
		return origin;
	}
	
	public String getParentObject_BPI17(XTrace t, XEvent e, String object) {
		if(object.equals("Offer")) return "Application";
		if(object.equals("Workflow")) return "Application";
		if(object.equals("Application")) return null;
		return null;
	}
	
	public String getObjectID_BPI17(XTrace t, XEvent e, String obj) {
		if (obj.equals("Application")) return XConceptExtension.instance().extractName(t);
		if (obj.equals("Workflow")) return XConceptExtension.instance().extractName(t).replace("Application", "Workflow");
		if (obj.equals("Offer")) {
			if (XConceptExtension.instance().extractName(e).equals("O_Create Offer")) return ((XAttributeLiteral)e.getAttributes().get("EventID")).getValue();
			else if (e.getAttributes().containsKey("OfferID")) return ((XAttributeLiteral)e.getAttributes().get("OfferID")).getValue();
		}
		return null;
	}
	
	
	public static void main(String[] args) throws IOException {
		//String inputfile = args[0];
		String inputfile = "D:/LinuxShared/Logs/BPIC17/BPI_Challenge_2017_filtered80.xes.gz";
		
		System.out.println("Reading log");
		String ext = XESImport.getLogFileExtension(inputfile);
		XLog inputlog = XESImport.readXLog(inputfile);
		
		System.out.println("Enriching log");
		SplitXESLogToArtifactLogs_BPI17 enricher = new SplitXESLogToArtifactLogs_BPI17(inputlog);
		XLog enrichedLog = enricher.enrichLog(inputlog);
		
		XFactory xlogFactory = enricher.factory;
		
		for (String obj : enricher.objectTypes) {

			XLog outputLog = xlogFactory.createLog();
			XAttributeMap outputAttr = xlogFactory.createAttributeMap();
			outputAttr.putAll(inputlog.getAttributes());
			outputLog.setAttributes(outputAttr);
			
			outputLog.getGlobalTraceAttributes().add(xlogFactory.createAttributeLiteral(XConceptExtension.KEY_NAME, "EMPTY", XConceptExtension.instance()));
			Set<String> interactionReferences = new HashSet<String>();
			
			int count = 0;
			
			for (XTrace t : enrichedLog) {
				if (count++ > 100) continue;
				
				Set<String> instancesInTrace = new HashSet<String>();
				for (XEvent e : t) {
					if (e.getAttributes().containsKey("object:type") 
						&& ((XAttributeLiteral)e.getAttributes().get("object:type")).getValue().equals(obj))
					{
						String instance = ((XAttributeLiteral)e.getAttributes().get("object:instance")).getValue();
						instancesInTrace.add(instance);
					}
				}

				// map: ParentInstance -> (map: ChildObjectType -> Set of Child Instances)
				Map<String, Map<String, Set<String>>> childInstancesInTrace = new HashMap<String, Map<String, Set<String>>>();
				for (XEvent e : t) {
					if (e.getAttributes().containsKey("object:parent_type") 
						&& ((XAttributeLiteral)e.getAttributes().get("object:parent_type")).getValue().equals(obj))
					{
						String child_type = ((XAttributeLiteral)e.getAttributes().get("object:type")).getValue();
						String child_instance = ((XAttributeLiteral)e.getAttributes().get("object:instance")).getValue();
						String parent_instance = ((XAttributeLiteral)e.getAttributes().get("object:parent_instance")).getValue();
					
						if (!childInstancesInTrace.containsKey(parent_instance)) childInstancesInTrace.put(parent_instance, new HashMap<String, Set<String>>());
						if (!childInstancesInTrace.get(parent_instance).containsKey(child_type)) childInstancesInTrace.get(parent_instance).put(child_type, new HashSet<String>());
						childInstancesInTrace.get(parent_instance).get(child_type).add(child_instance);
					}
				}
				
				for (String inst : instancesInTrace) {
					XAttributeMap t2Attr = xlogFactory.createAttributeMap();
					t2Attr.putAll(t.getAttributes());

					XTrace t2 = enricher.factory.createTrace(t2Attr);
					XConceptExtension.instance().assignName(t2, inst);
					
					for (XEvent e : t) {
						if (e.getAttributes().containsKey("object:type") 
							&& ((XAttributeLiteral)e.getAttributes().get("object:type")).getValue().equals(obj))
						{
							t2.add(e);
						}
					}
					
					if (childInstancesInTrace.containsKey(inst)) {
						for (String child_type : childInstancesInTrace.get(inst).keySet()) {
							interactionReferences.add(child_type);
							
							XAttributeLiteral refs = xlogFactory.createAttributeLiteral("Interaction_"+child_type, "MULTIPLE", null);
							XAttributeMap ref_ids = xlogFactory.createAttributeMap();
							for (String child_id : childInstancesInTrace.get(inst).get(child_type)) {
								XAttribute ref_id = xlogFactory.createAttributeLiteral(child_id, "COMPOSITE", null);
								ref_ids.put(child_id, ref_id);
							}
							refs.setAttributes(ref_ids);
							
							t2.getAttributes().put(child_type, refs);
						}
					}
					
					outputLog.add(t2);
				}
			}
			
			for (String child_type : interactionReferences) {
				outputLog.getGlobalTraceAttributes().add(xlogFactory.createAttributeLiteral("Interaction_"+child_type, "MULTIPLE", null));
			}
			
			XConceptExtension.instance().assignName(outputLog, XConceptExtension.instance().extractName(outputLog)+"_"+obj);
			outputLog.getExtensions().add(XConceptExtension.instance());
			outputLog.getExtensions().add(XTimeExtension.instance());
			
			System.out.println("Writing log");
			// strip xes extension
			int pos_xes = inputfile.lastIndexOf(ext);
			String outputfile = inputfile.substring(0, pos_xes)+"_"+obj+ext;
			XESExport.writeLog(outputLog, outputfile);
		}
	}

}
