package com.axway.apim.api.definition;

import com.axway.apim.api.IAPI;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;

public class WSDLSpecification extends APISpecification {
	
	JsonNode wsdl = null;
	
	public WSDLSpecification(byte[] apiSpecificationContent, String backendBasepath) throws AppException {
		super(apiSpecificationContent, backendBasepath);
	}

	@Override
	public int getAPIDefinitionType() throws AppException {
		return IAPI.WSDL_API;
	}

	@Override
	protected void configureBasepath() throws AppException {
		
	}
	
	
	@Override
	public boolean configure() throws AppException {
		if(apiSpecificationFile.toLowerCase().endsWith(".url")) {
			apiSpecificationFile = Utils.getAPIDefinitionUriFromFile(apiSpecificationFile);
		}
		if(apiSpecificationFile.toLowerCase().endsWith("?wsdl") ||
				apiSpecificationFile.toLowerCase().endsWith(".wsdl") ||
				apiSpecificationFile.toLowerCase().endsWith("?singlewsdl")) {
			return true;
		}
		return false;
	}
}
