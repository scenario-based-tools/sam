package org.st.sam.query;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import org.deckfour.xes.model.XLog;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.layout.HierarchicalLayout;
import org.graphstream.ui.layout.Layout;
import org.st.sam.log.SLog;
import org.st.sam.log.SLogTree;
import org.st.sam.log.SLogTreeNode;
import org.st.sam.log.SScenario;
import org.st.sam.log.XESImport;
import org.st.sam.mine.MineBranchingTree;
import org.st.sam.mine.MineLSC;
import org.st.sam.mine.MineLSC.Configuration;
import org.st.sam.mine.collect.SimpleArrayList;
import org.st.sam.mine.datastructure.LSC;

import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.HashSet;




public class SamQueryModel {
	
	public static void main(String s[]) throws IOException {
		
    	//String logFile = s[0];
    	//String logFile = "D:/LinuxShared/Logs/CelonisChallenge/EventLog_1-to-1_MultiID/DemoEventlogMultiCaseID.csv_mi_complete.xes";
		String logFile = "D:/LinuxShared/Logs/Roadfines/Road_Traffic_Fine_Management_Process.xes.gz";
    	XLog xlog = XESImport.readXLog(logFile);
    	SLog slog = new SLog(xlog);

    	System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    	
        LogExplore expl = new LogExplore(slog);
        SamQueryModel model = new SamQueryModel(xlog, expl); 
	}
	
	protected final LogExplore expl;
	private final XLog xlog;
	
	private MineLSC minerBranch;
	
	public SamQueryModel (XLog _xlog, LogExplore _expl) {
		
		this.expl = _expl;
		this.xlog = _xlog;
	
		final SamQueryModel q = this;
		
		JFrame frame = new JFrame("Query Panel");
		// Add a window listner for close button
		frame.addWindowListener(new WindowAdapter() {
	
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		final JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
		final JLabel filterThreshold = new JLabel("filter");
		filterThreshold.setPreferredSize(new Dimension(60, 25));
		filterPanel.add(filterThreshold);
		final JTextField filterField = new JTextField(6);
		filterField.setText("10");
		filterField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int filter = Integer.parseInt(filterField.getText());
					if (filter >= 0) {
						q.expl.setEdgeFilter(filter);
					}
				} catch (NumberFormatException ex) {
				}
			}
		});
		filterPanel.add(filterField);
		
		final JPanel queryPanel = new JPanel();
		queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.X_AXIS));
		final JLabel queryLabel = new JLabel("query");
		queryLabel.setPreferredSize(new Dimension(60, 25));
		queryPanel.add(queryLabel);
		final JTextField queryField = new JTextField();
		queryField.setText("1,2,3");
		queryField.setPreferredSize(new Dimension(200, 25));
		queryField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String traceIndices[] = queryField.getText().split(",");
				Set<Integer> traces = new HashSet<Integer>();
				for (String s : traceIndices) {
					try {
						int index = Integer.parseInt(s);
						traces.add(index);
					} catch (NumberFormatException ex) {
					}
				}
				q.expl.recomputeDFG(traces);
				q.expl.recomputeGraph();
				q.expl.recomputeLayout();
			}
		});
		queryPanel.add(queryField);
		
		final JPanel computeScenarioPanel = new JPanel();
		computeScenarioPanel.setLayout(new BoxLayout(computeScenarioPanel, BoxLayout.X_AXIS));
		final JButton computeScenarioBtn = new JButton("get scenarios");
		computeScenarioPanel.add(computeScenarioBtn);
		final JLabel scenarioSupportLabel = new JLabel("support");
		scenarioSupportLabel.setPreferredSize(new Dimension(60, 25));
		computeScenarioPanel.add(scenarioSupportLabel);
		final JTextField supportField = new JTextField(3);
		supportField.setText("10");
		computeScenarioPanel.add(supportField);
		final JLabel scenarioConfidenceLabel = new JLabel("confidence");
		scenarioConfidenceLabel.setPreferredSize(new Dimension(60, 25));
		computeScenarioPanel.add(scenarioConfidenceLabel);
		final JTextField confidenceField = new JTextField(5);
		confidenceField.setText("1.0");
		computeScenarioPanel.add(confidenceField);
		
		final JList<LSCStory> scenarioList = new JList<LSCStory>();
		scenarioList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		scenarioList.setLayoutOrientation(JList.VERTICAL);
		scenarioList.setVisibleRowCount(20);
		JScrollPane scenarioListScroller = new JScrollPane(scenarioList);
		
		computeScenarioBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int support = Integer.parseInt(supportField.getText());
					double confidence = Double.parseDouble(confidenceField.getText());
					
				    Configuration config_branch = Configuration.mineBranching();
				    config_branch.allowEventRepetitions = false;
				    
				    minerBranch = new MineLSC(config_branch);
				    minerBranch.OPTIONS_WEIGHTED_OCCURRENCE = true;
				    System.out.println("mining branching lscs");
				    minerBranch.mineLSCs(xlog, support, confidence);

				    List<LSC> lscs = minerBranch.getLSCs();
				    
					ArrayList<LSCStory> stories = new ArrayList<LSCStory>();
					int lscNum = 0;
				    for (LSC lsc : lscs) {
				    	SScenario s = minerBranch.getScenarios().get(lsc);
				    	stories.add(new LSCStory(s, q.expl, lscNum++));
				    }
				    Collections.sort(stories, new LSCStory.LSCStoryComparator());

				    DefaultListModel<LSCStory> model = new DefaultListModel<LSCStory>();
				    for (LSCStory story : stories) model.addElement(story);
					scenarioList.setModel(model);
					
				} catch (NumberFormatException ex) {
					System.err.println(ex);
				} catch (IOException ex) {
					System.err.println(ex);
				}
			}
		});
		
		final String OP_OR = "OR";
		final String OP_AND = "AND";
		
		final JComboBox<String> operatorCombo = new JComboBox<>();
		operatorCombo.addItem(OP_OR);
		operatorCombo.addItem(OP_AND);
		
		final JButton queryByScenariosBtn = new JButton("select 'all events' where 'scenario in trace'");
		queryByScenariosBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				Set<Integer> traces = new HashSet<Integer>();
				boolean first = true;
						
				List<LSCStory> selected = scenarioList.getSelectedValuesList();
				for (LSCStory l : selected) {
					
					SScenario s = l.s;
					MineBranchingTree tree = minerBranch.getTree();
					
					tree.confidence(s, true);
					
					Set<Integer> traces_s = new HashSet<Integer>();
					for (SLogTreeNode[] occ : tree.getOccurrences(s)) {
						SimpleArrayList<Integer> indices = tree.nodeInTrace.get(occ[occ.length-1]);
						for (int index : indices) {
							traces_s.add(index);
						}
					}
					if (first) {
						traces.addAll(traces_s);
						first = false;
					} else {
						if (operatorCombo.getSelectedItem() == OP_OR) traces.addAll(traces_s);
						else traces.retainAll(traces_s);
					}
				}
				
				q.expl.selectEvents(q.expl.getAllEvents());
				
				for (int i=0; i<5; i++) {
					q.expl.recomputeDFG(traces);
					q.expl.recomputeGraph();
					q.expl.recomputeLayout();
				}
				
			}
		});
		
		final JButton queryByScenarios2Btn = new JButton("select 'event in scenario' where 'scenario in trace'");
		queryByScenarios2Btn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				Set<Integer> traces = new HashSet<Integer>();
				Set<Short> events = new HashSet<Short>();
				boolean first = true;
				
				List<LSCStory> selected = scenarioList.getSelectedValuesList();
				for (LSCStory l : selected) {
					
					SScenario s = l.s;
					MineBranchingTree tree = minerBranch.getTree();
					
					tree.confidence(s, true);
					Set<Integer> traces_s = new HashSet<Integer>();
					for (SLogTreeNode[] occ : tree.getOccurrences(s)) {
						SimpleArrayList<Integer> indices = tree.nodeInTrace.get(occ[occ.length-1]);
						for (int index : indices) {
							traces_s.add(index);
						}
					}
					if (first) {
						traces.addAll(traces_s);
						first = false;
					} else {
						if (operatorCombo.getSelectedItem() == OP_OR) traces.addAll(traces_s);
						else traces.retainAll(traces_s);
					}
					
					for (short ev : s.word) events.add(q.expl.f2a[ev]);
				}
				

				q.expl.selectEvents(events); 
				
				for (int i=0; i<5; i++) {
					q.expl.recomputeDFG(traces);
					q.expl.recomputeGraph();
					q.expl.recomputeLayout();
				}
				
			}
		});
		
		final JButton showScenarioBtn = new JButton("show scenario");
		showScenarioBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				for (Node n : graph.getNodeSet()) {
					graph.removeNode(n);
				}
				
				Map<String, List<String>> components = new HashMap<String, List<String>>();
				
				Map<Node, Integer> count = new java.util.HashMap<Node, Integer>();
		        
		        LSCStory s = scenarioList.getSelectedValue();
		        short prev_ev = -1;
		        int num = 0;
		        for (short ev : s.s.word) {
		        	Node n = graph.addNode(String.valueOf(ev));
		        	n.setAttribute("ui.label", q.expl.abstractNames[q.expl.f2a[ev]]);
		        	
		        	String component = q.expl.fullNames[ev].split("\\|")[0];
		        	if (!components.containsKey(component)) components.put(component, new ArrayList<String>());
		        	components.get(component).add(String.valueOf(ev));
		        	
		        	boolean preChart = (num < s.s.pre.length);
		        	if (preChart) {
		        		n.addAttribute("ui.style", "shape:box; size: 60px,20px; fill-color: #aaaaff;");
		        	} else {
		        		n.addAttribute("ui.style", "shape:box; size: 60px,20px; fill-color: #ffaaaa;");
		        	}
		        	if (prev_ev >= 0) {
		        		Edge edge = graph.addEdge(String.valueOf(prev_ev)+"-"+String.valueOf(ev), String.valueOf(prev_ev), String.valueOf(ev));
		        		if (preChart) {
		        			edge.addAttribute("ui.style", "fill-color: #2222ff;");
		        		} else {
		        			edge.addAttribute("ui.style", "fill-color: #ff2222;");
		        		}
		        	}
		        	prev_ev = ev;
		        	num++;
		        	
		        	count.put(n, num);
		        }
		        
		        int cCount = 0;
		        for (String c : components.keySet()) {
		        	List<String> events = components.get(c);
		        	if (events.size() <= 1) continue;
		        	
		        	for (int i=0; i<events.size();i++) {
		        		Node n = graph.getNode(events.get(i));
		        		n.setAttribute("x", -cCount*0.5);
		        		n.setAttribute("y", -count.get(n)*0.2);
		        		n.addAttribute("layout.frozen");
		        	}
		        	cCount++;
		        }
			}
		});

		
		mainPanel.add(filterPanel);
		mainPanel.add(queryPanel);
		mainPanel.add(computeScenarioPanel);
		mainPanel.add(scenarioListScroller);
		mainPanel.add(operatorCombo);
		mainPanel.add(queryByScenariosBtn);
		mainPanel.add(queryByScenarios2Btn);
		mainPanel.add(showScenarioBtn);

		frame.getContentPane().add(mainPanel);
		frame.pack();
		frame.setVisible(true);
		
		initScenarioView();
	}
	
	public Graph graph;
	private void initScenarioView () {
		graph = new SingleGraph("Scenario visualization");
        
        String styleSheet =
        		"node { "
        		        + "     shape: rounded-box; "
        		        + "     padding: 5px; "
        		        + "     fill-color: white; "
        		        + "     stroke-mode: plain; "
        		        + "     size-mode: fit; "
        		        + "} "
        		        + "edge { "
        		        + "     shape: freeplane; "
        		        + "}";
        
        Layout layout = new HierarchicalLayout();// SpringBox(false, new Random(0));
        graph.addSink(layout);
        layout.addAttributeSink(graph);
        
        graph.addAttribute("ui.stylesheet", styleSheet);
        graph.addAttribute("ui.antialias");
        graph.addAttribute("ui.quality");
        graph.addAttribute("layout.quality", 1);
        graph.setAutoCreate(true);
        graph.setStrict(false);
        graph.display(true);
	}

}
