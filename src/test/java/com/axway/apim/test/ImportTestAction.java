package com.axway.apim.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.App;
import com.axway.apim.lib.CommandParameters;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.exceptions.ValidationException;

public class ImportTestAction extends AbstractTestAction {
	
	public static String API_DEFINITION = "apiDefinition";
	public static String API_CONFIG = "apiConfig";
	
	private static Logger LOG = LoggerFactory.getLogger(ImportTestAction.class);
	
	@Override
	public void doExecute(TestContext context) {
		String origApiDefinition 			= context.getVariable(API_DEFINITION);
		String origConfigFile 			= context.getVariable(API_CONFIG);
		String stage				= null;
		String apiDefinition			= null;
		try {
			stage 				= context.getVariable("stage");
		} catch (CitrusRuntimeException ignore) {};
		if(!origApiDefinition.contains("http://") && !origApiDefinition.contains("https://")) {
			apiDefinition = replaceDynamicContentInFile(origApiDefinition, context);
		} else {
			apiDefinition = origApiDefinition;
		}
		String configFile = replaceDynamicContentInFile(origConfigFile, context);
		LOG.info("Using Replaced Swagger-File: " + apiDefinition);
		LOG.info("Using Replaced configFile-File: " + configFile);
		LOG.info("API-Manager import is using user: '"+context.replaceDynamicContentInString("${oadminPassword1}")+"'");
		int expectedReturnCode = 0;
		try {
			expectedReturnCode 	= Integer.parseInt(context.getVariable("expectedReturnCode"));
		} catch (Exception ignore) {};
		
		String enforce = "false";
		String ignoreQuotas = "false";
		String ignoreAdminAccount = "false";
		String clientOrgsMode = CommandParameters.MODE_REPLACE;
		String clientAppsMode = CommandParameters.MODE_REPLACE;;
		
		try {
			enforce = context.getVariable("enforce");
		} catch (Exception ignore) {};
		try {
			ignoreQuotas = context.getVariable("ignoreQuotas");
		} catch (Exception ignore) {};
		try {
			clientOrgsMode = context.getVariable("clientOrgsMode");
		} catch (Exception ignore) {};
		try {
			clientAppsMode = context.getVariable("clientAppsMode");
		} catch (Exception ignore) {};
		try {
			ignoreAdminAccount = context.getVariable("ignoreAdminAccount");
		} catch (Exception ignore) {};
		
		
		if(stage==null) {
			stage = "NOT_SET";
		} else {
			// We need to prepare the dynamic staging file used during the test.
			String stageConfigFile = origConfigFile.substring(0, origConfigFile.lastIndexOf(".")+1) + stage + origConfigFile.substring(origConfigFile.lastIndexOf("."));
			// This creates the dynamic staging config file! (Fort testing, we also support reading out of a file directly)
			stage = replaceDynamicContentInFile(stageConfigFile, context);
		}

		String[] args = new String[] { 
				"-a", apiDefinition, 
				"-c", configFile, 
				"-h", context.replaceDynamicContentInString("${apiManagerHost}"), 
				"-p", context.replaceDynamicContentInString("${oadminUsername1}"), 
				"-u", context.replaceDynamicContentInString("${oadminPassword1}"),
				"-s", stage, 
				"-f", enforce, 
				"-iq", ignoreQuotas, 
				"-clientOrgsMode", clientOrgsMode, 
				"-clientAppsMode", clientAppsMode, 
				"-ignoreAdminAccount", ignoreAdminAccount};
		LOG.info("Ignoring admin account: '"+ignoreAdminAccount+"'. Enforce breaking change: " + enforce);
		int rc = App.run(args);
		if(expectedReturnCode!=rc) {
			throw new ValidationException("Expected RC was: " + expectedReturnCode + " but got: " + rc);
		}
	}
	
	/**
	 * To make testing easier we allow reading test-files from classpath as well
	 */
	private String replaceDynamicContentInFile(String pathToFile, TestContext context) {
		
		File inputFile = new File(pathToFile);
		InputStream is = null;
		OutputStream os = null;
		try {
			if(inputFile.exists()) { 
				is = new FileInputStream(pathToFile);
			} else {
				is = this.getClass().getResourceAsStream(pathToFile);
			}
			if(is == null) {
				throw new IOException("Unable to read swagger file from: " + pathToFile);
			}
			String jsonData = IOUtils.toString(is);
			String filename = pathToFile.substring(pathToFile.lastIndexOf("/")+1); // e.g.: petstore.json, no-change-xyz-config.<stage>.json, 
			String prefix = filename.substring(0, filename.indexOf("."));
			String suffix = filename.substring(filename.indexOf("."));
			String jsonReplaced = context.replaceDynamicContentInString(jsonData);
			File tempFile = File.createTempFile(prefix, suffix);
			os = new FileOutputStream(tempFile);
			IOUtils.write(jsonReplaced, os);
			tempFile.deleteOnExit();
			return tempFile.getAbsolutePath();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(os!=null)
				try {
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return null;
	}
}
