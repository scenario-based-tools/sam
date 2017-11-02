package org.st.sam.mine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.st.sam.log.SLogTree.SupportedWord;
import org.st.sam.log.SLogTreeNode;
import org.st.sam.log.XESExport;
import org.st.sam.log.XESImport;
import org.st.sam.mine.MineLSC.Configuration;
import org.st.sam.mine.collect.SimpleArrayList;

import com.google.gwt.dev.util.collect.HashMap;

public class RunExperimentMineGappedSequences {
	
	private static String SLASH = System.getProperty("file.separator");
	
	private long runTime_minerBranch;
	private long runTime_pruning;
	private MineLSC minerBranch;
	
	private static boolean isSubSequence(short[] short_seq, short[] long_seq) {

		if (short_seq.length > long_seq.length)
			return false;

		int l = 0;
		int s = 0;
		while (s < short_seq.length) {

			// for each letter 'short_seq[s]' find a letter 'long_seq[l]'
			// starting at the last position
			while (l < long_seq.length) {
				if (short_seq[s] == long_seq[l]) {
					// short_seq[s] has a corresponding letter in other at
					// position 'l'
					break;
				}
				l++;
			}
			if (l == long_seq.length) {
				// could not find this[t] in other at any position
				return false;
			}
			l++; // move to next letter of 'other'
			s++;
		}
		return true; // first letter of long_seq[] after the last letter of
						// short_seq[]
	}
	
	public ArrayList<SupportedWord> runMiner_Branch(final String logFile, final XLog xlog, final int minSupportThreshold, final int repetition) throws IOException {
		runTime_minerBranch = System.currentTimeMillis();

		Configuration config_branch = Configuration.mineBranching();
		config_branch.allowEventRepetitions = true;
		config_branch.maxRepetitionCount = repetition;
		config_branch.forceDirectlyFollows = false; // gapped sequences
		config_branch.discoverScenarios = false; 	// only discover words, not scenarios
		minerBranch = new MineLSC(config_branch);
		
		minerBranch.OPTIONS_WEIGHTED_OCCURRENCE = true;
		System.out.println("mining words from " + logFile);

		minerBranch.mineLSCs(xlog, minSupportThreshold, 1.0);
		runTime_minerBranch = System.currentTimeMillis() - runTime_minerBranch;
		

		System.out.println("pruning words");
	    runTime_pruning = System.currentTimeMillis();

	    int total = 0;
	    Map<Integer, SimpleArrayList<SupportedWord>> supportedWordsClasses = new HashMap<Integer, SimpleArrayList<SupportedWord>>();
	    int largestWordSize = 0;
	    for (SupportedWord w : minerBranch.getSupportedWords()) {
	      if (!supportedWordsClasses.containsKey(w.word.length)) {
	        supportedWordsClasses.put(w.word.length, new SimpleArrayList<SupportedWord>());
	      }
	      supportedWordsClasses.get(w.word.length).add(w);
	      if (largestWordSize < w.word.length) largestWordSize = w.word.length;
	      total += 1; //w.length;
	    }
	    System.out.println("found "+total+" supported words");
	    
	    
	    Configuration config = config_branch;
	    MineBranchingTree tree = minerBranch.getTree();
	    int done = 0;
	    
	    ArrayList<SupportedWord> words = new ArrayList<SupportedWord>();
	    SimpleArrayList<SupportedWord> toRemove = new SimpleArrayList<SupportedWord>();
	    
		for (int size = largestWordSize; size > 0; size--) {

			if (!supportedWordsClasses.containsKey(size))
				continue;

			// LinkedList<short[]> mined = new LinkedList<short[]>();
			for (SupportedWord _w : supportedWordsClasses.get(size)) {

				short[] w = _w.word;

				done++;
				if (done % 100 == 0)
					System.out.print(".");
				if (done % 7000 == 0) {
					System.out.println(done * 100 / total + "% (found " + words.size() + " so far)");
				}

				SimpleArrayList<SLogTreeNode[]> occ = tree.countOccurrences(w, null, null, config.forceDirectlyFollows);
				_w.support = tree.getTotalOccurrences(occ, true);

				if (config.removeSubsumed) {
					boolean is_subsumed = false;
					for (int s2_index = 0; s2_index < words.size(); s2_index++) {
						SupportedWord s2 = words.get(s2_index);
						if (isSubSequence(w, s2.word) && _w.support <= s2.support) {
							is_subsumed = true;
							break;
						}
					}
					if (!is_subsumed)
						words.add(_w);
				}
			}
		}
	      
	    if (config.removeSubsumed) {
	      System.out.println("\nRemoving remaining implied scenarios ("+words.size()+")...");  
	      boolean changed = true;
	      int removed = 0;
	      while(changed) {
	        changed = false;
	  
	        toRemove.quickClear();
	        
	        for (SupportedWord s : words) {
	          for (SupportedWord s2 : words) {
	            if (s == s2) continue;
	            if (s.word.length < s2.word.length && s.support <= s2.support && isSubSequence(s.word, s2.word)) {
	              System.out.println(s2+"  implies  "+s);
	              toRemove.add(s);
	              break;
	            }
	          }
	          if (toRemove.size() > 0) {
	            break;
	          }
	        }
	        
	        //System.out.println("removing all "+toRemove);
	        for (SupportedWord s_toRemove : toRemove) {
	          words.remove(s_toRemove);
	          removed++;
	          changed = true;
	        }
	      }
		    System.out.println("\nRemoved: "+removed);
	    }
	    System.out.println("\nRemaining words: "+words.size());
	    runTime_pruning = System.currentTimeMillis() - runTime_pruning;
	    
		return words;
	}
	
	public String runExperiment() throws IOException {
		XLog xlog = XESImport.readXLog(experimentFileRoot+SLASH+inputFile);
		ArrayList<SupportedWord> words = runMiner_Branch(inputFile, xlog, support, repetition);

		XFactory f = XFactoryRegistry.instance().currentDefault();
		
		XLog outlog = f.createLog();
		for (SupportedWord w : words) {
			XTrace t = f.createTrace();
			for (short e : w.word) {
				XEvent ev = f.createEvent();
				String fullName = minerBranch.getSLog().originalNames[e];
				String methodName = fullName.substring(fullName.lastIndexOf('|')+1, fullName.indexOf('+'));
				XConceptExtension.instance().assignName(ev, methodName);
				t.add(ev);
			}
			t.getAttributes().put("support", f.createAttributeDiscrete("support", w.support, null));
			outlog.add(t);
		}
		
		XESExport.writeLog(outlog, experimentFileRoot+SLASH+inputFile+"_words_s"+support+"_r"+repetition+".xes.gz");
		
		System.out.println("mining: "+runTime_minerBranch+" (ms)");
		System.out.println("mining: "+runTime_pruning+" (ms)");
		
		return "done";
	}
	
	protected String experimentFileRoot;
	protected String outputGroupDir = "";
	protected String inputFile;
	protected int support;
	protected int repetition;
	protected double confidence_branch;
	protected double confidence_linear;
	protected double density;
	protected double fraction = 1.0;

	protected void printHelp() {
		System.out.println("usage:  mine_words <inputfile.xes.gz> <support> <repetition_count>");
		System.out.println("  <inputfile>           path to log file");
		System.out.println("  <support>             minimum support threshold (integers > 0)");
		System.out.println("  <repetition_count>    number of times an event may repeat in a word");
	}

	public boolean readCommandLine(String[] args) {
		if (args.length != 3) {
			printHelp();
			return false;
		}

		File f = new File(args[0]);

		experimentFileRoot = f.getParent();
		inputFile = f.getName();
		try {
			support = Integer.parseInt(args[1]);
			repetition = Integer.parseInt(args[2]);

			if (support < 1)
				throw new NumberFormatException("support must be larger than 0");
			
			if (repetition < 0)
				throw new NumberFormatException("repetition must be >= 0");

		} catch (NumberFormatException e) {
			System.err.println(e);
			printHelp();
			return false;
		}

		return true;
	}
	
	public static void main(String[] args) throws Exception {

		RunExperimentMineGappedSequences exp = new RunExperimentMineGappedSequences();
		if (!exp.readCommandLine(args)) return;
		
	    exp.runExperiment();
	}

}
