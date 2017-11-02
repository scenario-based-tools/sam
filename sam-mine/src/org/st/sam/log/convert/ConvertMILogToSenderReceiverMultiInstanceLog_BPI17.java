package org.st.sam.log.convert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.st.sam.log.SEvent;
import org.st.sam.log.XESExport;

import com.google.gwt.dev.util.collect.HashMap;

public class ConvertMILogToSenderReceiverMultiInstanceLog_BPI17 {
	
	public static ArrayList<String[]> readCSV(String csvFile) throws Exception {
		BufferedReader br2 = new BufferedReader(new FileReader(csvFile));
		ArrayList<String[]> log = new ArrayList<String[]>();
		
		String line2;
		int lineNo2 = 0;
		while ((line2 = br2.readLine()) != null) {
			String[] entries = line2.split(";");
			if (lineNo2 == 0) {
				
			} else {
				log.add(entries);
			}
			lineNo2++;
		}
		br2.close();
		return log;
	}
	
	private static void addEventToTrace(XFactory xlogFactory, Map<String, XTrace> traces, XEvent e, String caseID) {
		XTrace trace = null;
		if (!traces.containsKey(caseID)) {
			trace = xlogFactory.createTrace();
			XConceptExtension.instance().assignName(trace, caseID);
			traces.put(caseID, trace);
		} else {
			trace = traces.get(caseID);
		}
		trace.add(e);
	}
	
	public static void main(String[] args) throws Exception {
		
		final XFactory xlogFactory = XFactoryRegistry.instance().currentDefault();
		final XLog log = xlogFactory.createLog();
		DateFormat f = new SimpleDateFormat("dd.MM.yyyy hh:mm");
		
		//String inputLog = args[0];
		String inputLog = "D:/LinuxShared/Logs/CelonisChallenge/EventLog_1-to-1_MultiID/DemoEventlogMultiCaseID.csv";
		
		ArrayList<String[]> csvLog = readCSV(inputLog);
		int CASE_ID = 0;
		int ACTIVITY_ID = 1;
		int ACTIVITY = 2;
		int TIME = 3;
		int DeliveryID = 4;
		int BillingDocID = 5;
		int InvoiceID = 6;
		int PaymentID = 7;
		
		Map<String, XTrace> traces = new HashMap<String, XTrace>();
		
		String currentCase = null;
		Map<String, ArrayList<String>> subCaseIDs = new HashMap<String,  ArrayList<String>>();
		
		for (String[] rawEvent : csvLog) {
			
			String method = rawEvent[ACTIVITY];
			String senderName = "Case";
			String senderID = rawEvent[CASE_ID];
			
			if (method.equals("Clear Invoice")) {
				senderName = "Payment";
				senderID = rawEvent[PaymentID];
			}
			
			if (!rawEvent[CASE_ID].equals(currentCase)) {
				currentCase = rawEvent[CASE_ID];
				subCaseIDs.clear();
				subCaseIDs.put("Case", new ArrayList<String>());
				subCaseIDs.get("Case").add(currentCase);
				subCaseIDs.put("Delivery", new ArrayList<String>());
				subCaseIDs.put("BillingDoc", new ArrayList<String>());
				subCaseIDs.put("Invoice", new ArrayList<String>());
				subCaseIDs.put("Payment", new ArrayList<String>());
			}
			
			for (int r = DeliveryID; r <= PaymentID && r < rawEvent.length; r++) {
				if (rawEvent[r] == null || rawEvent[r].length() == 0) continue;
				
				String recvName;
				if (r == DeliveryID) recvName = "Delivery";
				else if (r == BillingDocID) recvName = "BillingDoc";
				else if (r == InvoiceID) recvName = "Invoice";
				else if (r == PaymentID) recvName = "Payment";
				else continue;
				
				String recvID = rawEvent[r];
				if (!subCaseIDs.get(recvName).contains(recvID)) subCaseIDs.get(recvName).add(recvID);
			}
			
			boolean foundEvent = false;
			for (int r = DeliveryID; r <= PaymentID && r < rawEvent.length; r++) {
				if (rawEvent[r] == null || rawEvent[r].length() == 0) continue;
				
				String recvName;
				if (r == DeliveryID) recvName = "Delivery";
				else if (r == BillingDocID) recvName = "BillingDoc";
				else if (r == InvoiceID) recvName = "Invoice";
				else if (r == PaymentID) recvName = "Payment";
				else continue;
				
				String recvID = rawEvent[r];
				int subRecvID = subCaseIDs.get(recvName).indexOf(recvID);
				String recvName_IDd = recvName+"_"+subRecvID;
				
				int subSendID = subCaseIDs.get(senderName).indexOf(senderID);
				String senderName_IDd = senderName+"_"+subSendID;
				
				if (r == PaymentID && method.equals("Clear Invoice")) continue;
				
				String result = "true";
				SEvent e = new SEvent(method, senderName_IDd, senderID, recvName_IDd, recvID, result);
			    XEvent xe = e.toXEvent(xlogFactory);
			    
			    Date timestamp = f.parse(rawEvent[TIME]);
			    XTimeExtension.instance().assignTimestamp(xe, timestamp);
			    addEventToTrace(xlogFactory, traces, xe, rawEvent[CASE_ID]);
			    foundEvent = true;
			}
			if (!foundEvent) {
				String recvName = senderName;
				String recvID = senderID;
				String result = "true";
				
				int subRecvID = subCaseIDs.get(recvName).indexOf(recvID);
				String recvName_IDd = recvName+"_"+subRecvID;
				
				int subSendID = subCaseIDs.get(senderName).indexOf(senderID);
				String senderName_IDd = senderName+"_"+subSendID;
				
				SEvent e = new SEvent(method, senderName_IDd, senderID, recvName_IDd, recvID, result);
			    XEvent xe = e.toXEvent(xlogFactory);

			    Date timestamp = f.parse(rawEvent[TIME]);
			    XTimeExtension.instance().assignTimestamp(xe, timestamp);
			    addEventToTrace(xlogFactory, traces, xe, rawEvent[CASE_ID]);
			}
		}
		
		for (XTrace t : traces.values()) {
			log.add(t);
		}
		System.out.println("Writing "+inputLog+"_mi.xes");
	    XESExport.writeLog(log, inputLog+"_mi.xes");
		
		
	}

}
