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

package it.smartcommunitylab.cityreport.controllers;

import it.smartcommunitylab.cityreport.model.Response;
import it.smartcommunitylab.cityreport.model.Service;
import it.smartcommunitylab.cityreport.services.ServiceManager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author raman
 *
 */
@Controller
public class ServiceController {

	@Autowired
	private ServiceManager manager;

	@RequestMapping(method=RequestMethod.GET, value="/{providerId}/services/{serviceId}")
	public @ResponseBody Response<Service> getService(@PathVariable String providerId, @PathVariable String serviceId) {
		return new Response<Service>(manager.findService(serviceId, providerId));
	}
	
	@RequestMapping(method=RequestMethod.GET, value="/{providerId}/services")
	public @ResponseBody Response<List<Service>> getServices(@PathVariable String providerId) {
		return new Response<List<Service>>(manager.findServices(providerId));
	}
	
	@ExceptionHandler(Exception.class)
	public @ResponseBody Response<Void> handleExceptions(Exception exception) {
		return new Response<Void>(500, exception.getMessage());
	}

}
