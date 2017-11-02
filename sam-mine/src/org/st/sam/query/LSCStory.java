package org.st.sam.query;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.st.sam.log.SScenario;

public class LSCStory {
	
	public SScenario s;
	private LogExplore q;
	int num;
	
	public LSCStory(SScenario s, LogExplore q, int num) {
		this.s = s;
		this.q = q;
		this.num = num;
	}
	
	private String getStory() {
		if (s.pre.length < s.main.length) {
			return getStoryPre();
		} else {
			return getStoryMain();
		}
	}
	
	private String getStoryPre() {
		return "whenever "+getChart(s.pre)+" happpened "+getChart(s.main)+" happens";
	}
	
	private String getStoryMain() {
		return getChart(s.main)+" after "+getChart(s.pre)+" happend";
	}

	private Set<Short> repeated(short[] chart) {
		int[] count = new int[q.abstractNames.length];
		for (int i=0; i<chart.length; i++) count[q.f2a[chart[i]]] += 1;
		Set<Short> repeated = new HashSet<Short>();
		for (short i=0; i<count.length; i++) {
			if (count[i] > 1) repeated.add(i);
		}
		return repeated;
	}
	
	private String getChart(short[] chart) {
		
		Set<Short> repeated = repeated(chart);
		if (repeated.size() > 0) return getChartRepeated(chart, repeated);
		
		if (chart.length == 1) return q.abstractNames[q.f2a[chart[0]]];
		if (chart.length == 2) return "first "+q.abstractNames[q.f2a[chart[0]]]+" then "+q.abstractNames[q.f2a[chart[1]]];

		// chart.length > 2
		String seq = "";
		for (int i=0; i<chart.length; i++) {
			if (i > 0) seq += ", ";
			seq += q.abstractNames[q.f2a[chart[i]]];
		}
		return seq;
	}

	private String getChartRepeated(short[] chart, Set<Short> repeated) {
		
		Set<Short> _repeated = new HashSet<Short>();
		_repeated.addAll(repeated);
		
		String seq = "";
		boolean first = true;
		for (int i=0; i<chart.length; i++) {
			if (_repeated.contains(q.f2a[chart[i]])) {
				if (!first) seq += ", ";
				seq += q.abstractNames[q.f2a[chart[i]]];
				first = false;
				_repeated.remove(q.f2a[chart[i]]);
			}
		}
		return seq+" repeatedly";
	}

	public String toString() {
		String story = s.support+" (#"+num+") "+getStory();
		return story;
	}
	
	public static class LSCStoryComparator implements Comparator<LSCStory> {

		@Override
		public int compare(LSCStory o1, LSCStory o2) {
			double o1_10 = Math.log10(o1.s.support);
			double o2_10 = Math.log10(o2.s.support);
			
			if (Math.round(o1_10) == Math.round(o2_10)) {
				return o1.getStory().compareTo(o2.getStory());	
			} else {
				if (o1_10 < o2_10) return +1;
				else return -1;
			}
		}
		
	}

}
