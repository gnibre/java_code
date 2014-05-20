package util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StopWords {
	public static Set<String> stopSet;

	static {
		final List<String> stopWords = Arrays.asList("a", "an", "and", "are",
				"as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
				"it", "no", "not", "of", "on", "or", "such", "that", "the",
				"their", "then", "there", "these", "they", "this", "to", "was",
				"will", "with");
		stopSet = new HashSet<String>();
		stopSet.addAll(stopWords);
		stopSet = Collections.unmodifiableSet(stopSet);
	}

//	public static List<String> removeStopWords(List<String> terms) {
//		List<String> result = new LinkedList<String>();
//		for (String term : terms) {
//			if (term != null) {
//				if (!stopSet.contains(term.toLowerCase())) {
//
//					result.add(term);
//				}
//			}
//		}
//		return result;
//
//	}
}
