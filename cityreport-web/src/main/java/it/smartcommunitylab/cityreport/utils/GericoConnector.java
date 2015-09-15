package it.smartcommunitylab.cityreport.utils;

import it.smartcommunitylab.cityreport.data.IssueRepository;
import it.smartcommunitylab.cityreport.model.Issuer;
import it.smartcommunitylab.cityreport.model.Location;
import it.smartcommunitylab.cityreport.model.ServiceIssue;
import it.smartcommunitylab.cityreport.services.IssueManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.network.RemoteConnector;

@Component
public class GericoConnector {
	
	private static final String F_EXTERNAL_ID_NEW = "external_id_new";
	private static final String F_EXTERNAL_ID = "external_id";
	private static final String F_NOTE_EXTERNAL = "note_external";
	private static final String F_MAC = "mac";
	private static final String F_STRADA = "strada";
	private static final String F_LONGITUDINE = "longitudine";
	private static final String F_LATITUDINE = "latitudine";
	private static final String F_STATO = "stato";
	private static final String F_NOTE = "note";
	private static final String F_OGGETTO = "oggetto";
	private static final String F_EMAIL = "email";
	private static final String F_ORIGINE_DETTAGLIO = "origine_dettaglio";
	private static final String F_ORIGINE = "origine";
	private static final String F_MEZZO = "mezzo";
	private static final String F_PUBBLICO = "pubblico";
	private static final String F_ATTIVATORE = "attivatore";
	private static final String APP_NAME = "App segnala";
	private static final String APP_ID = "app_s";

	@Autowired
	private IssueRepository issueRepository;	
	
	@Autowired
	private IssueManager manager;
	
	private final static Logger logger = LoggerFactory.getLogger(GericoConnector.class);

	@SuppressWarnings("unchecked")
	@Scheduled(initialDelay=30000, fixedRate=7200000)
	public void getIssues() throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
		logger.debug("Scheduled Gerico");
		
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Object> map = mapper.readValue(new URL("https://www2.comune.rovereto.tn.it/gerico/extra/opendata_richieste/"), Map.class);
		List<Map<String, Object>> data = (List<Map<String, Object>>)map.get("data");
		
		for (Map<String, Object> entry: data) {
			String attivatore = (String)entry.get(F_ATTIVATORE);
			if (APP_NAME.equals(attivatore) || APP_ID.equals(attivatore)) {
				processEntry(entry);
			} else if ("1".equals(entry.get(F_PUBBLICO))) {
				processExternalEntry(entry);
			}
		}
	}	
	
	
	@SuppressWarnings("unchecked")
	public void getIssues(String year, String fromMonth, String toMonth) throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Object> map = mapper.readValue(new URL("https://www2.comune.rovereto.tn.it/gerico/extra/opendata_richieste/0/" + year + "/" + fromMonth + "/" + toMonth), Map.class);
		List<Map<String, Object>> data = (List<Map<String, Object>>)map.get("data");
		
		for (Map<String, Object> entry: data) {
			String attivatore = (String)entry.get(F_ATTIVATORE);
			if (APP_NAME.equals(attivatore) || APP_ID.equals(attivatore)) {
				processEntry(entry);
			} else if ("1".equals(entry.get(F_PUBBLICO))) {
				processExternalEntry(entry);
			}
		}
	}
	
	/**
	 * @param entry
	 */
	private void processExternalEntry(Map<String, Object> entry) {
		String externalId = (String)entry.get(F_EXTERNAL_ID);
		ServiceIssue issue = issueRepository.findByExternalId(externalId);

		String status = (String)entry.get(F_STATO);
		if ("A".equals(status)) {
			status = Constants.STATUS_OPEN;
		} else if ("C".equals(status)) {
			status = Constants.STATUS_CLOSED;
		}  

//		System.out.println(entry);
//		System.out.println("_____________________________");		
		
		if (issue != null) {
			logger.info("Updating " + issue.getExternalId());
			issueRepository.save(issue);
		} else {
			logger.info("Creating " + externalId);
			issue = new ServiceIssue();
			issue.setProviderId("ComuneRovereto");
			issue.setServiceId("problems");
			issue.setExternalId(externalId);
			issue.setCreated(System.currentTimeMillis());
			issue.setAttribute(new HashMap<String, Object>());
			issue.getAttribute().put("title", entry.get(F_OGGETTO));
			issue.getAttribute().put("description", F_NOTE);
			issue.setLocation(new Location());
			issue.getLocation().setAddress((String) entry.get(F_STRADA));
			issue.setIssuer(new Issuer());
			try {
				issue.getLocation().setCoordinates(new double[]{
						Double.parseDouble((String)entry.get(F_LATITUDINE)),
						Double.parseDouble((String)entry.get(F_LONGITUDINE))
				});
			} catch (Exception e) {
				return;
			}
			
			issueRepository.save(issue);
		}
		issue.setStatus(status);
		issue.setStatusNotes((String)entry.get(F_NOTE_EXTERNAL));
		issue.setNotes((String)entry.get(F_NOTE_EXTERNAL));
	}


	private void processEntry(Map<String, Object> entry) {
		String externalId = (String)entry.get(F_EXTERNAL_ID);
		ServiceIssue issue = issueRepository.findByExternalId(externalId);

//		System.out.println(entry);
//		System.out.println("_____________________________");		
		
		if (issue != null) {
			logger.info("Updating " + issue.getExternalId());
			String status = (String)entry.get(F_STATO);
			if ("A".equals(status)) {
				status = Constants.STATUS_OPEN;
			} else if ("C".equals(status)) {
				status = Constants.STATUS_CLOSED;
			}  
			issue.setStatus(status);
			issue.setStatusNotes((String)entry.get(F_NOTE_EXTERNAL));
			issue.setNotes((String)entry.get(F_NOTE_EXTERNAL));
			issueRepository.save(issue);
		}
	}
	

	/** 
	 * 
	 * @param issue
	 * @return
	 */
	public boolean sendIssue(ServiceIssue issue) {
		boolean ok = true;

		try {

			Map<String, Object> data = new HashMap<String, Object>();

//			String id = "" + manager.increaseCounter();
//			data.put("external_id", id);

			String id = new ObjectId().toString();
			data.put(F_EXTERNAL_ID_NEW, id);

			data.put(F_ATTIVATORE, APP_ID);
			data.put(F_MEZZO, APP_ID);
			data.put(F_ORIGINE, "cit");
//			data.put("operatore_inserimento", "loris");
			data.put(F_ORIGINE_DETTAGLIO, issue.getIssuer().fullName());
			data.put(F_EMAIL, issue.getIssuer().getEmail());

			String oggetto = "";
			if (issue.getAttribute() != null && issue.getAttribute().containsKey("title")) {
				oggetto = (String) issue.getAttribute().get("title");
			}

			data.put(F_OGGETTO, oggetto);

			String note = "";
			if (issue.getAttribute() != null && issue.getAttribute().containsKey("description")) {
				note += (String) issue.getAttribute().get("description") + "\n";
			}
			if (issue.getMedia() != null) {
				for (String media : issue.getMedia()) {
					note += media + "\n";
				}
			}
			note = URLEncoder.encode(note, "utf-8");
			data.put(F_NOTE, note);
			data.put(F_STATO, "A");

			if (issue.getLocation() != null) {
				if (issue.getLocation().getAddress() != null) {
					data.put(F_STRADA, issue.getLocation().getAddress());
				}
				if (issue.getLocation().getCoordinates() != null) {
					data.put(F_LATITUDINE, "" + issue.getLocation().getCoordinates()[0]);
					data.put(F_LONGITUDINE, "" + issue.getLocation().getCoordinates()[1]);
				}
				//
			}

			data.put(F_MAC, generateMac("286b5d03a4b2fa092f091c2b982cb028090e2936", id));
			String body = new ObjectMapper().writeValueAsString(data);
			body = URLEncoder.encode(body, "utf-8");

			String result = RemoteConnector.getJSON("https://www2.comune.rovereto.tn.it/", "gerico/ws_crea_richiesta/" + body, null);
			try {
				issue.setExternalId(id);
				issueRepository.save(issue);
			} catch (Exception e) {
				logger.error("ws_crea_richiesta returned: " + result);
				e.printStackTrace();
				ok = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ok = false;
		}

		return ok;
	}
	
	
	
	private Object generateMac(String key, String id) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		return DigestUtils.shaHex((id+key).getBytes("utf8"));
	}
	
	
}
