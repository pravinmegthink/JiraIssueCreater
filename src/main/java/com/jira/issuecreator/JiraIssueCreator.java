package com.jira.issuecreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JiraIssueCreator {

	public static void main(String[] args) {

		final Logger logger = LogManager.getLogger(jiraIssueCreaterService.class.getName());
		
		//String SOURCE_NAME = "Jira_Ent";
		jiraIssueCreaterService jiraService = new jiraIssueCreaterService();
		try {

			jiraService.loadProperties("/config.properties");
			jiraService.getAccessToken();
			String result = jiraService.getEntitlementDetails();

			ObjectMapper mapper = new ObjectMapper();

			// Extract the first element of the array

			JsonNode jsonArray = mapper.readTree(result);
			JsonNode jsonObject = jsonArray.get(0);

			// Extract the values for source.name, attributes.name, and source.id

			String sourceName = jsonObject.path("source").path("name").asText();
			String accessName = jsonObject.path("attributes").path("name").asText();
			String sourceId = jsonObject.path("source").path("id").asText();
			String sourceType = jsonObject.path("source").path("type").asText();

			// Extract attributes from nested 'attributes' object
			JsonNode attributes = jsonObject.get("attributes");

			String attribute3 = attributes.get("attribute3").asText();
			String attribute4 = attributes.get("attribute4").asText();
			String attribute2 = attributes.get("attribute2").asText();
			String attribute1 = attributes.get("attribute1").asText();
			String disabled = attributes.get("disabled").asText();

			// Boolean disabled =false;

			String requestBody = String.format("{\n" + "  \"fields\": {\n"
					+ "    \"summary\": \"Testing db save 21\",\n" 
					+ "    \"customfield_10068\": \"%s\",\n"
					+ "    \"customfield_10069\": \"%s\",\n" 
					+ "    \"customfield_10070\": { \"id\": \"10046\" },\n"
					+ "    \"customfield_10071\": \"%s\",\n" 
					+ "    \"customfield_10010\": null,\n"
					+ "    \"project\": { \"id\": \"10001\" },\n" 
					+ "    \"issuetype\": { \"id\": \"10005\" },\n"
					+ "    \"description\": {\n" 
					+ "      \"content\": [\n" 
					+ "        {\n"
					+ "          \"content\": [\n" 
					+ "            {\n" 
					+ "              \"type\": \"text\",\n"
					+ "              \"text\": \"available text formatting \\n https://jira.atlassian.com/secure/WikiRendererHelpAction.jspa?section=all\"\n"
					+ "            }\n" + "          ],\n" 
					+ "          \"type\": \"paragraph\"\n" + "        }\n"
					+ "      ],\n"
					+ "      \"type\": \"doc\",\n" + "      \"version\": 1\n" + "    },\n"
					+ "    \"customfield_10072\": [\n" + "      {\n" 
					+ "        \"workspaceId\": \"10047\",\n"
					+ "        \"id\": \"10048\",\n" 
					+ "        \"objectId\": \"10049\"\n" 
					+ "      }\n" + "    ],\n"
					+ "    \"labels\": [\"PAMS\"]\n" + "  }\n" + "}", accessName, sourceName, sourceId);
			// custom_field 10027 id=attribute1,workspaceId=attribute2,id=attribute4, objectId=attribute3

			// String issue_response = jiraService.createJiraIssue(requestBody);
			
      logger.info(requestBody);
			if (disabled.equals("FALSE")) {
				String issue_response = jiraService.createJiraIssue(requestBody);
				JsonNode jsonResponse = mapper.readTree(issue_response);

				String jsmId = jsonResponse.get("id").asText();

				jiraService.saveToDatabase(sourceName, sourceId, sourceType, null, null, null, null, null, null, null, null, null,
						accessName, null, jsmId, null, null, null, null, null);

			}else {
				logger.info("jira issue is not created .");
			}

			
		} catch (Exception e) {
			logger.error("An error occurred", e);
			e.printStackTrace();
		}
	}

}
