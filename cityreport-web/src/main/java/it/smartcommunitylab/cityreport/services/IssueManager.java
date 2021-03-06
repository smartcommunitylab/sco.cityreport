/*******************************************************************************
 * Copyright 2015 Smart Community Lab
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.cityreport.services;

import it.smartcommunitylab.cityreport.data.IssueRepository;
import it.smartcommunitylab.cityreport.model.ServiceIssue;
import it.smartcommunitylab.cityreport.utils.Constants;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * @author raman
 *
 */
@Component
public class IssueManager {

	private final static Logger logger = LoggerFactory.getLogger(IssueManager.class);

	@Autowired
	private IssueRepository repository;
	
	@Autowired
	private MongoTemplate mongoTemplate;

	public ServiceIssue createIssue(ServiceIssue issue) {
		logger.debug("creating issue {}",issue.getNotes());
		
		issue.setCreated(System.currentTimeMillis());
		issue.setStatus(Constants.STATUS_OPEN);
		
		return repository.save(issue);
	}
	
	@PostConstruct
	private synchronized void initVersion() {
		String counterId = ServiceIssue.class.getCanonicalName();
		DBObject counter = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(counterId)), DBObject.class, "counters");
		if (counter == null) {
			mongoTemplate.save(BasicDBObjectBuilder.start("_id", counterId).add("value", 1L).get(), "counters");
		}
	}	
	
	public long increaseCounter() {
		String counterId = ServiceIssue.class.getCanonicalName();
		DBObject counter = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(counterId)), DBObject.class, "counters");
		if (counter == null) {
			mongoTemplate.save(BasicDBObjectBuilder.start("_id", counterId).add("value", 1L).get(), "counters");
			return 1L;
		} else {
		DBObject o = mongoTemplate.findAndModify(
				Query.query(Criteria.where("_id").is(counterId)), 
				new Update().inc("value", 1), 
				DBObject.class, 
				"counters");
		return (Long)o.get("value");
		}
	}
	
	public long getCounter() {
		String counterId = ServiceIssue.class.getCanonicalName();
		DBObject counter = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(counterId)), DBObject.class, "counters");
		if (counter != null) {
			return (Long)counter.get("value");
		}
		else {
			return 0;
		}
	}	
	
	
	public ServiceIssue findIssue(String id, String serviceId, String providerId) {
		logger.debug("searching issue {}/{}/{}", id, providerId, serviceId);
		ServiceIssue issue = repository.findByProviderIdAndServiceIdAndId(providerId, serviceId, id);
		logger.debug("searching issue {}/{}/{}: found {}", id, providerId, serviceId, issue);
		return issue;
	}
	public List<ServiceIssue> findServiceIssues(String providerId, String serviceId) {
		logger.debug("searching service issues {}/{}",providerId, serviceId);
		List<ServiceIssue> result = repository.findByProviderIdAndServiceId(providerId, serviceId);
		logger.debug("searching service issues {}/{}: found {}",providerId, serviceId, result != null ? result.size() : 0);
		return result;
	}
	public List<ServiceIssue> findUserIssues(String providerId, String serviceId, String userId) {
		logger.debug("searching user issues {}/{}/{}",providerId, serviceId, userId);
		List<ServiceIssue> result = repository.findByUser(providerId, serviceId, userId);
		logger.debug("searching user issues {}/{}/{}: found {}",providerId, serviceId, userId,result != null ? result.size() : 0);
		return result;
	}

	public List<ServiceIssue> findIssues(String providerId, String serviceId, Collection<String> status, Collection<String> statusExclude, Long from, Long to, String userId, String orgId, Circle circle, Integer start, Integer count) {
		logger.debug("issues lookup {}",providerId);
		List<ServiceIssue> result = repository.search(providerId, serviceId, status, statusExclude, from, to, userId, orgId, circle, start, count);
		logger.debug("issues lookup {}: found {}",providerId, result != null ? result.size() : 0);
		return result;
	}

	public List<ServiceIssue> findIssues(String providerId, Collection<String> serviceIds, Collection<String> status, Collection<String> statusExclude, Long from, Long to, String userId, String orgId, Circle circle, Integer start, Integer count) {
		logger.debug("issues lookup {}",providerId);
		List<ServiceIssue> result = repository.search(providerId, serviceIds, status, statusExclude, from, to, userId, orgId, circle, start, count);
		logger.debug("issues lookup {}: found {}",providerId, result != null ? result.size() : 0);
		return result;
	}

	/**
	 * @param id
	 * @param providerId
	 * @return
	 */
	public ServiceIssue findProviderIssue(String id, String providerId) {
		logger.debug("searching provider issue {}/{}", id, providerId);
		ServiceIssue issue = repository.findByProviderIdAndId(providerId, id);
		logger.debug("searching issue {}/{}/{}: found {}", id, providerId, issue);
		return issue;
	}

}
