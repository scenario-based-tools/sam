package org.st.sam.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.HierarchicalLayout;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.st.sam.log.SLog;
import org.st.sam.log.XESImport;

import com.google.gwt.dev.util.collect.HashSet;

public class LogExplore {
    public static void main(String args[]) throws IOException {
    	String logFile = "D:/LinuxShared/Logs/CelonisChallenge/EventLog_1-to-1_MultiID/DemoEventlogMultiCaseID.csv_mi_complete.xes";
    	XLog xlog = XESImport.readXLog(logFile);
    	SLog slog = new SLog(xlog);

    	System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    	
        new LogExplore(slog);
    }
    
    private SLog slog;
    private Layout layout;
    private Graph graph;
    
    private int[] occurrence;
    private int[][] dfg;
    public String abstractNames[];
    public String fullNames[];
    
    public short f2a[];
    private short SOURCE_i;
    private short SINK_i;
    
    private int edge_threshold;

    public LogExplore(SLog slog) {
    	this.slog = slog;
    	
    	setupNames();

    	selectEvents(getAllEvents());
    	
    	Set<Integer> allTraces = new HashSet<Integer>();
    	for (int i=0; i<slog.traces.length; i++) allTraces.add(i);
    	recomputeDFG(allTraces);
    	
    	initializeGraph();
        
        Node source = graph.addNode(String.valueOf(f2a[SOURCE_i]));
        source.addAttribute("ui.style", "shape:circle; size: 20px; fill-color: #ccffcc;");
        source.addAttribute("ui.label", abstractNames[f2a[SOURCE_i]]);
        
        Node sink = graph.addNode(String.valueOf(f2a[SINK_i]));;
        sink.addAttribute("ui.style", "shape:circle; size: 20px; fill-color: #ffcccc;");
        sink.addAttribute("ui.label", abstractNames[f2a[SINK_i]]);

		for (int i=0; i< abstractNames.length; i++) {
			if (i == f2a[SINK_i] || i == f2a[SOURCE_i]) continue;
			
        	Node n = graph.addNode(String.valueOf(i));
        	n.addAttribute("ui.label", abstractNames[i]);
            n.addAttribute("ui.style", "shape:box; size: 60px,20px; fill-color: #ccccff;");
        }
        
        setEdgeFilter(10);
        
        source.addAttribute("layout.frozen");
    	source.setAttribute("x", 0);
    	source.setAttribute("y", 2);
    	
    	sink.addAttribute("layout.frozen");
    	sink.setAttribute("x", 0);
    	sink.setAttribute("y", -2);
        
    	
        //recomputeLayout();
        //explore(source);
    }
    
    public void recomputeLayout() {
        // iterate the compute() method a number of times
        while(layout.getStabilization() < 0.9){
        	layout.compute();
        	try { Thread.sleep(10); } catch (Exception e) {}
        }   
    }

    public void explore(Node source) {
        Iterator<? extends Node> k = source.getBreadthFirstIterator();

        while (k.hasNext()) {
            Node next = k.next();
            next.setAttribute("ui.class", "marked");
            sleep();
        }
    }
    
    public void close() {
    	graph.clear();
    }

    protected void sleep() {
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    protected String styleSheet =
        "node {" +
        "	fill-color: black;" +
        "}" +
        "node.marked {" +
        "	fill-color: red;" +
        "}";

	public void setEdgeFilter(int filter) {
		edge_threshold = filter;
		recomputeGraph();
		recomputeLayout();
	}
	
	private void initializeGraph() {
        graph = new SingleGraph("Log visualization");
        
        layout = new HierarchicalLayout();// SpringBox(false, new Random(0));
        graph.addSink(layout);
        layout.addAttributeSink(graph);
        
        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.addAttribute("layout.quality", 1);
        graph.setAutoCreate(true);
        graph.setStrict(false);
        graph.display(true);
	}
	
	private void setupNames() {
        Set<String> abstractNameSet = new HashSet<String>();
        Map<String, String> fullToAbstractNames = new HashMap<String, String>();
        
        fullNames = slog.originalNames;
        
        String SOURCE = ">";
        String SINK = "[ ]";
        abstractNameSet.add(SOURCE);
        abstractNameSet.add(SINK);
        fullToAbstractNames.put(SOURCE, SOURCE);
        fullToAbstractNames.put(SINK, SINK);
        
        for (String fullName : slog.originalNames) {
        	String eventName = fullName.substring(fullName.lastIndexOf('|')+1);
        	abstractNameSet.add(eventName);
        	fullToAbstractNames.put(fullName, eventName);
        }

        Map<String, Short> abstractNameIndex = new HashMap<String, Short>();
        abstractNames = new String[abstractNameSet.size()];
        for (String name : abstractNameSet) {
        	short i = (short)abstractNameIndex.size();
        	abstractNameIndex.put(name, i);
        	abstractNames[i] = name;
        }
        
        f2a = new short[slog.originalNames.length+2];
        for (int i=0;i<slog.originalNames.length;i++) {
        	String abstractName = fullToAbstractNames.get(slog.originalNames[i]);
        	f2a[i] = abstractNameIndex.get(abstractName);
        }
        SOURCE_i = (short)slog.originalNames.length;
        SINK_i = (short)(slog.originalNames.length+1);
        f2a[SOURCE_i] = abstractNameIndex.get(SOURCE);
        f2a[SINK_i] = abstractNameIndex.get(SINK);
	}
	
	private boolean flag_recomputeAllEdges = true;
	
	public void recomputeDFG(Collection<Integer> traces) {
        occurrence = new int[abstractNames.length];
        dfg = new int[abstractNames.length][abstractNames.length];
        
        for (int t=0; t<slog.traces.length; t++) {
        	if (!traces.contains(t)) continue;
        		
        	short trace[] = slog.traces[t];
        	
        	ArrayList<Short> aTrace = new ArrayList<Short>();
        	aTrace.add(SOURCE_i);
        	for (int i=0;i<trace.length;i++) {
        		if (chosenEvents.contains(f2a[trace[i]])) aTrace.add(trace[i]);
        	}
        	aTrace.add(SINK_i);
        	//System.out.println(aTrace);

        	for (int i=0; i<aTrace.size()-1; i++) {
        		occurrence[f2a[aTrace.get(i)]]++;
        		dfg[f2a[aTrace.get(i)]][f2a[aTrace.get(i+1)]]++;
        	}
        	occurrence[f2a[aTrace.get(aTrace.size()-1)]]++;
        }
        flag_recomputeAllEdges = true;
	}
	
	public void recomputeGraph() {
		
		if (flag_recomputeAllEdges) {
			for (Edge e : graph.getEdgeSet()) {
				graph.removeEdge(e);
			}
			flag_recomputeAllEdges = false;
		}
		
        for (short i=0; i<dfg.length; i++) {
        	if (!chosenEvents.contains(i)) continue;
        	
        	for (short j=0; j<dfg[i].length; j++) {
        		if (!chosenEvents.contains(j)) continue;
        		
        		if (dfg[i][j] > 0) {
        			String edgeId = i+"-"+j;
        			
        			if (dfg[i][j] >= edge_threshold || i == f2a[SOURCE_i] || j == f2a[SINK_i]) {
        				if (graph.getEdge(edgeId) == null) {
                			Edge e = graph.addEdge(edgeId, String.valueOf(i), String.valueOf(j), true);
                			double edgeWidth = Math.round(Math.log10(dfg[i][j])/3+1);
                			
                			String color = "blue";
                			if (dfg[i][j] < edge_threshold) color = "#bbbbdd"; // if source/sink edges are below threshold, make them less visible
                			
                			e.addAttribute("ui.style", "size:"+edgeWidth+"px; fill-color: "+color+";");
                			e.addAttribute("ui.label", dfg[i][j]);
                			if (i == j)
                				e.addAttribute("layout.weight", .2);
        				}
        			} else {
        				graph.removeEdge(edgeId);
        			}
        		}
        	}
        }

        for (Node n : graph.getEachNode()) {
        	System.out.print(n+" "+n.getAttribute("ui.label")+" ");
        	if (n.getEdgeSet().isEmpty()) {
        		System.out.println("hide");
        		n.setAttribute("ui.hide");
        	} else {
        		System.out.println("unhide");
        		n.removeAttribute("ui.hide");
        	}
        }
	}
	
	private Set<Short> chosenEvents = null;

	public void selectEvents(Collection<Short> events) {
		chosenEvents = new HashSet<Short>();
		chosenEvents.addAll(events);
		chosenEvents.add(f2a[SOURCE_i]);
		chosenEvents.add(f2a[SINK_i]);
		System.out.println("chosen: "+events);
	}
	
	public Set<Short> getAllEvents() {
    	Set<Short> allEvents = new HashSet<Short>();
    	for (short i=0; i<abstractNames.length; i++) {
    		allEvents.add(i);
    	}
    	return allEvents;
	}
}
