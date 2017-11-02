package org.st.sam.log;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;

public class SEvent {

  public String method;
  public String sender;
  public String senderID;
  public String receiver;
  public String receiverID;
  public String returnValue;
  
  public static final String FIELD_RESULT = "result";
  public static final String FIELD_METHOD = XConceptExtension.KEY_NAME;
  public static final String FIELD_RECEIVERID = "receiverID";
  public static final String FIELD_RECEIVER = "receiver";
  public static final String FIELD_SENDERID = "senderID";
  public static final String FIELD_SENDER = "sender";
  
  public SEvent(String m, String s, String sID, String r, String rID, String val) {
    method = m;
    sender = s;
    senderID = sID;
    receiver = r;
    receiverID = rID;
    returnValue = val;
  }
  
  public SEvent(XEvent e) {
	// method name is name+life-cycle or just name, if no life-cycle is defined
    method = (getValue(e, XLifecycleExtension.KEY_TRANSITION, null) != null) ? getValue(e, FIELD_METHOD, null)+"+"+getValue(e, XLifecycleExtension.KEY_TRANSITION, null) : getValue(e, FIELD_METHOD, null); 
    sender = getValue(e, FIELD_SENDER, "default_object");
    senderID = getValue(e, FIELD_SENDERID, "0");
    receiver = getValue(e, FIELD_RECEIVER, "default_object");
    receiverID = getValue(e, FIELD_RECEIVERID, "0");
    returnValue = getValue(e, FIELD_RESULT, "0");
  }
  
  private static String getValue(XEvent e, String key, String default_value) {
	  if (e.getAttributes().containsKey(key)) return ((XAttributeLiteral)e.getAttributes().get(key)).getValue();
	  else return default_value;
  }
  
  public XEvent toXEvent(XFactory factory) {
    XAttributeMap attributes = factory.createAttributeMap();
    XAttribute a_name = factory.createAttributeLiteral(FIELD_METHOD, method, XConceptExtension.instance()); 
    XAttribute a_complete = factory.createAttributeLiteral("lifecycle:transition", "complete",
        XLifecycleExtension.instance());

    XAttribute a_sender = factory.createAttributeLiteral(FIELD_SENDER, sender, null);
    XAttribute a_senderID = factory.createAttributeLiteral(FIELD_SENDERID, senderID, null);
        
    XAttribute a_receiver = factory.createAttributeLiteral(FIELD_RECEIVER, receiver, null);
    XAttribute a_receiverID = factory.createAttributeLiteral(FIELD_RECEIVERID, receiverID, null);
    
    XAttribute a_result = factory.createAttributeLiteral(FIELD_RESULT, returnValue, null);
    
    attributes.put(SEvent.FIELD_METHOD, a_name);
    attributes.put("lifecycle:transition", a_complete);
    attributes.put(SEvent.FIELD_SENDER, a_sender);
    attributes.put(SEvent.FIELD_SENDERID, a_senderID);
    attributes.put(SEvent.FIELD_RECEIVER, a_receiver);
    attributes.put(SEvent.FIELD_RECEIVERID, a_receiverID);
    attributes.put(SEvent.FIELD_RESULT, a_result);

    XEvent e = factory.createEvent(attributes);
    return e;
  }
  
  public String getCharacter() {
    return sender+"|"+receiver+"|"+method;
  }

}
