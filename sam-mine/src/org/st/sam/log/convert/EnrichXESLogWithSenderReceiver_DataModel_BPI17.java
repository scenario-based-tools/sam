package org.st.sam.log.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.st.sam.log.SEvent;
import org.st.sam.log.XESExport;
import org.st.sam.log.XESImport;

import com.google.gwt.dev.util.collect.HashMap;

public class EnrichXESLogWithSenderReceiver_DataModel_BPI17 {
	
	private final XLog inputlog;
	private XFactory factory = XFactoryRegistry.instance().currentDefault();
	
	public EnrichXESLogWithSenderReceiver_DataModel_BPI17(XLog inputlog) {
		this.inputlog = inputlog;
	}
	
	public XLog enrichLog(XLog log) {
		for (XTrace trace : log) enrichTrace(trace);
		return log;
	}
	
	
	public XTrace enrichTrace(XTrace t) {
		
		// for all objects occurring in this trace, create a list of object identifiers occurring in this trace
		Map<String, List<String>> subcaseIDs = new HashMap<String, List<String>>();
		
		for (XEvent e : t) {
			String obj = getObject_BPI17(t, e);
			String objID = getObjectID_BPI17(t, e, obj);
			if (!subcaseIDs.containsKey(obj)) subcaseIDs.put(obj, new ArrayList<String>());
			if (!subcaseIDs.get(obj).contains(objID)) subcaseIDs.get(obj).add(objID);
		}
		
		// abstract the identifiers through enumeration
		Map<String, String> abstr = new HashMap<String, String>();
		for (String obj : subcaseIDs.keySet()) {
			for (int i=0; i<subcaseIDs.get(obj).size(); i++) {
				// first character of the object + sequence number of IDs of that object in the case
				String abstractedID = obj+"_"+Integer.toString(i+1);
				abstr.put(obj+"#"+subcaseIDs.get(obj).get(i), abstractedID);
			}
		}
		
		XEvent pre_event = null;
		
		for (int i=0; i<t.size(); i++) {
			XEvent e = t.get(i);
	
			String obj = getObject_BPI17(t, e);
			String objID = getObjectID_BPI17(t, e, obj);
			String parentObj = getParentObject_BPI17(t, e, obj);
			String parentObjID = (parentObj != null) ? getObjectID_BPI17(t, e, parentObj) : null;
			
			XEvent e_pre;
			String obj_pre = null;
			String objID_pre = null;
			String parent_obj_pre = null;
			String parent_obj_pre_id = null;
			if (i > 0) {
				e_pre = t.get(i-1);
				obj_pre = getObject_BPI17(t, e_pre);
				objID_pre = getObjectID_BPI17(t, e_pre, obj_pre);
				parent_obj_pre = getParentObject_BPI17(t, e_pre, obj_pre);
				parent_obj_pre_id = (parent_obj_pre != null) ? getObjectID_BPI17(t, e_pre, parent_obj_pre) : null;
			}
			
			String sender = obj;
			String senderid = objID;
			if (obj_pre != null && !obj.equals(obj_pre)) {
				 if (parent_obj_pre != null && parent_obj_pre.equals(obj) && parent_obj_pre_id.equals(objID)) {
					 // return a call from child object (pre) to parent (this)
					 sender = parent_obj_pre;
					 senderid = parent_obj_pre_id;
					 
//				 } else if (parent_obj_pre != null && parentObj != null
//						 && parent_obj_pre.equals(parentObj) && parent_obj_pre_id.equals(parentObjID)) {
//					 // direct call from child object (pre) to other child object (this) of the same parent
//					 sender = obj_pre;
//					 senderid = objID_pre;
					 
				 } else {
					 // otherwise: call from parent object
					 sender = parentObj;
					 senderid = parentObjID;
				 }
			}

			String shortNameSend = abstr.get(sender+"#"+senderid);
			String shortNameRecv = abstr.get(obj+"#"+objID);

			e.getAttributes().put(SEvent.FIELD_SENDER, factory.createAttributeLiteral(SEvent.FIELD_SENDER, shortNameSend, null));
			e.getAttributes().put(SEvent.FIELD_SENDERID, factory.createAttributeLiteral(SEvent.FIELD_SENDERID, senderid, null));				

			e.getAttributes().put(SEvent.FIELD_RECEIVER, factory.createAttributeLiteral(SEvent.FIELD_RECEIVER, shortNameRecv, null));
			e.getAttributes().put(SEvent.FIELD_RECEIVERID, factory.createAttributeLiteral(SEvent.FIELD_RECEIVERID, objID, null));
			
			pre_event = e;
		}
		
		return t;
	}
	
	public String getObject_BPI17(XTrace t, XEvent e) {
		String origin = ((XAttributeLiteral)e.getAttributes().get("EventOrigin")).getValue();
		return origin;
	}
	
	public String getObjectID_BPI17(XTrace t, XEvent e, String obj) {
		if (obj.equals("Application")) return XConceptExtension.instance().extractName(t);
		if (obj.equals("Workflow")) return XConceptExtension.instance().extractName(t);
		if (obj.equals("Offer")) {
			if (XConceptExtension.instance().extractName(e).equals("O_Create Offer")) return ((XAttributeLiteral)e.getAttributes().get("EventID")).getValue();
			else if (e.getAttributes().containsKey("OfferID")) return ((XAttributeLiteral)e.getAttributes().get("OfferID")).getValue();
		}
		return null;
	}
	
	public String getParentObject_BPI17(XTrace t, XEvent e, String object) {
		if(object.equals("Offer")) return "Application";
		if(object.equals("Workflow")) return "Application";
		if(object.equals("Application")) return null;
		return null;
	}

	
	public static void main(String[] args) throws IOException {
		//String inputfile = args[0];
		String inputfile = "D:/LinuxShared/Logs/BPIC17/BPI_Challenge_2017.xes.gz";
		
		System.out.println("Reading log");
		String ext = XESImport.getLogFileExtension(inputfile);
		XLog inputlog = XESImport.readXLog(inputfile);
		
		System.out.println("Enriching log");
		EnrichXESLogWithSenderReceiver_DataModel_BPI17 enricher = new EnrichXESLogWithSenderReceiver_DataModel_BPI17(inputlog);
		XLog outputlog = enricher.enrichLog(inputlog);

		
		System.out.println("Writing log");
		// strip xes extension
		int pos_xes = inputfile.lastIndexOf(ext);
		String outputfile = inputfile.substring(0, pos_xes)+"_mi_DM"+ext;
		XESExport.writeLog(outputlog, outputfile);
		
		XLog outputLog_1000 = enricher.factory.createLog(outputlog.getAttributes());
		int i=0;
		for (XTrace t : outputlog) {
			outputLog_1000.add(t);
			if (i++ > 1000) break;
		}
		String outputfile_1000 = inputfile.substring(0, pos_xes)+"_mi_DM_1000"+ext;
		XESExport.writeLog(outputLog_1000, outputfile_1000);

	}

}
