package manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import util.Logger;

import data.PDocument;
import data.TokenItem;

public class QueryManager {
	private static QueryManager sQueryManager;

	public static QueryManager getInstance() {
		if (sQueryManager == null) {
			sQueryManager = new QueryManager();
		}
		return sQueryManager;
	}

	public String searchExplain;

	public PriorityQueue<PDocument> query(List<String> tls) {
		
		return Ranker.rankRelatedDocsWithScoreAG1(tls);
	}

	public PriorityQueue<PDocument> query(String query) {
		query = query.toLowerCase();
		TermsManager tm = TermsManager.getTermManagerInstance();
		List<String> tls = tm.tokenizeRemStopAndStem(query);
		Logger.pArray(tls, "tokens in query  ");

		if (tls.size() == 0) {
			searchExplain = "Did NOT find any token in the query(maybe removed by stop word removal algorithm)";
			Logger.warning(searchExplain);
			return null;
		}
		return query(tls);

	}

	public void doSample() {
		String[] querys = { "abc def kkk", "coffee zhang", "doc abd", "some",
				"gogogogog, craft", "not in the", "what to do", "cat","craft","facebook","mobile",
				"bad add ok", "startup" };
		for (String q : querys) {
			query(q);
		}
	}
	
	

}
