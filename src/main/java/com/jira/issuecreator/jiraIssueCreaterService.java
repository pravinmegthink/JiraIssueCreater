package com.jira.issuecreator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;




public class jiraIssueCreaterService {
	private static String JIRA_URL;
    private static String JIRA_USERNAME;
    private static String JIRA_API_TOKEN;
    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;
   // private static String SOURCE_ID;
    //private static String ENTITLEMENT_NAME;
    private static String IDN_URL;
    private static String CLIENT_ID;
    private static String CLIENT_SECRET;
    private static String ENT_FILTER;
    private static String  TOKEN_URL;
    private static String  BASE_URL;
    private static String SOURCE_FILTER;
    private static String SOURCE_NAME;
    private static String ENTITLEMENT_PREFIX;
    
    private static final Logger logger = LogManager.getLogger(jiraIssueCreaterService.class.getName());

  //to load properties from config.properties file
    
    public void loadProperties(String externalConfigPath) throws IOException {
    	logger.info("loadProperties method start");
        Properties props = new Properties();

        // Load the config.properties file from the resources directory
        try (InputStream input = JiraIssueCreator.class.getResourceAsStream(externalConfigPath)) {
            if (input == null) {
            	logger.error("Sorry, unable to find " + externalConfigPath);
                return;
            }

            // Load properties from the file
            props.load(input);

            // Assign values to variables
            JIRA_URL = props.getProperty("JIRA_URL");
            JIRA_USERNAME = props.getProperty("JIRA_USERNAME");
            JIRA_API_TOKEN = props.getProperty("JIRA_API_TOKEN");
            DB_URL = props.getProperty("DB_URL");
            DB_USERNAME = props.getProperty("DB_USERNAME");
            DB_PASSWORD = props.getProperty("DB_PASSWORD");
            //SOURCE_ID = props.getProperty("SOURCE_ID");
           // ENTITLEMENT_NAME=props.getProperty("ENTITLEMENT_NAME");
            CLIENT_ID = props.getProperty("CLIENT_ID");
            CLIENT_SECRET = props.getProperty("CLIENT_SECRET");
            TOKEN_URL=props.getProperty("TOKEN_URL");
            IDN_URL=props.getProperty("IDN_URL");
            ENT_FILTER=props.getProperty("ENT_FILTER");
            BASE_URL=props.getProperty("BASE_URL");
            SOURCE_FILTER=props.getProperty("SOURCE_FILTER");
            SOURCE_NAME=props.getProperty("SOURCE_NAME");
            ENTITLEMENT_PREFIX=props.getProperty("ENTITLEMENT_PREFIX");
            
            // Check if properties were loaded correctly
            if (JIRA_URL == null || JIRA_USERNAME == null || JIRA_API_TOKEN == null) {
            	logger.error("Missing Jira configuration!");
            	logger.info("loadProperties method end");
            }
            if (DB_URL == null || DB_USERNAME == null || DB_PASSWORD == null) {
            	logger.error("Missing database configuration!");
            	logger.info("loadProperties method end");
            }
            logger.info("loadProperties method end");
        }catch (IOException e) {
       
        	logger.error("An error occurred while loading properties from config file", e);
        	logger.info("loadProperties method end");
        }
        
    }
//to get the entitlement deatils using source id and entitlement name
    
public String getEntitlementDetails() {
    	
        logger.info("getEntitlementDetails method start");

        // Get the access token
        String token = getAccessToken();
        String SOURCE_ID= getSourceId(token,SOURCE_NAME);
        RestTemplate restTemplate = new RestTemplate();
        String ENTITLEMENT_NAME_POSTFIX = "Meg_test3";
		//String ENTITLEMENT_NAME = ENTITLEMENT_PREFIX+ENTITLEMENT_NAME_POSTFIX+"|";
        // Construct the URL
        String url = IDN_URL + SOURCE_ID + ENT_FILTER + ENTITLEMENT_NAME_POSTFIX + '"';
        logger.info("Request URL: " + url);

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token); // Add Bearer prefix
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Create HTTP entity with headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            // Make the GET request
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                String responseBody = responseEntity.getBody();
                logger.info("Response Body: " + responseBody);

                JSONArray entitlements = new JSONArray(responseBody);

                if (entitlements.length() > 0) {
                	logger.info("getEntitlementDetails method end");
                    return responseBody;
                } else {
                	logger.error("No entitlements found.");
                    return "No entitlements found.";
                }
            } else {
            	logger.error("Failed to fetch entitlements.");
                return "Failed to fetch entitlements: " + responseEntity.getStatusCode() ;
            }
        } catch (RestClientException e) {
            logger.error("An error occurred while getting entitlement using source ID and entitlement name", e);
            return "An error occurred while getting entitlement using source ID and entitlement name.";
        }
    }

    
  //to create jira issue
    
public String createJiraIssue(String jsonRequestBody) throws IOException, SQLException {
	
        logger.info("createJiraIssue method start");

        // Create RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Set up the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String auth = "Basic " + java.util.Base64.getEncoder().encodeToString((JIRA_USERNAME + ":" + JIRA_API_TOKEN).getBytes());
        headers.set("Authorization", auth);

        // Create the HttpEntity object with headers and body
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

        try {
            // Send the request and get the response
            ResponseEntity<String> response = restTemplate.exchange(JIRA_URL, HttpMethod.POST, requestEntity, String.class);

            // Check if the response was successful
            if (response.getStatusCode() == HttpStatus.CREATED) {
            	logger.info("createJiraIssue method end");
                return response.getBody();
            } else {
            	logger.error("Unexpected response status: " + response.getStatusCode());
                throw new IOException("Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Unable to create a Jira issue", e);
            return "An error occurred while creating the Jira issue";
        }
    }

  //to get the access token 
    public String getAccessToken() {
        
        logger.info("getAccessToken method start");

        RestTemplate restTemplate = new RestTemplate();

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String URL=BASE_URL+TOKEN_URL;

        // Prepare form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", CLIENT_ID);
        formData.add("client_secret", CLIENT_SECRET);

        // Create HTTP entity with headers and form data
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        try {
            // Make the POST request
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(URL, requestEntity, String.class);

            // Log response details
            logger.info("Token created successfully " );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Extract the access token
                String accessToken = jsonNode.get("access_token").asText();
                logger.info("getAccessToken method end   "+ accessToken);
                
                //getSourceId(accessToken);
                return accessToken;
            } else {
            	 logger.error("Request failed: " + responseEntity.getStatusCode());
                return responseEntity.getBody();
            }
        } catch (RestClientException | IOException e) {
            logger.error("An error occurred", e);
            return "Exception: " + e.getMessage();
        }
    }

  //to save data in the database table created
	/*
	 * public void saveToDatabase( String sourceName, String sourceId, String
	 * sourceType, String workitemId, String actionedbyId, String actionedbyName,
	 * String beneficiaryId, String beneficiaryName, String accountId, String
	 * accountName, String accessType, String accessId, String accessName, String
	 * accessValue, String createdDate, String jsmId, String jsmProvisionerType,
	 * String jsmProvisionerName, String jsmProvisionerEmail, String actionType,
	 * String jsmProvisionerId) throws SQLException {
	 * logger.info("saveToDatabase method start");
	 * 
	 * String sql =
	 * "INSERT INTO jira_issues ( sourceName, sourceId, sourceType, workitemId, actionedbyId, actionedbyName, "
	 * +
	 * "beneficiaryId, beneficiaryName, accountId, accountName, accessType, accessId, accessName, accessValue, "
	 * +
	 * "createdDate, jsmId, jsmProvisionerType, jsmProvisionerName, jsmProvisionerEmail, actionType,jsmProvisionerId) "
	 * + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	 * 
	 * try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME,
	 * DB_PASSWORD); PreparedStatement pstmt = conn.prepareStatement(sql)) {
	 * 
	 * 
	 * pstmt.setString(1, sourceName); pstmt.setString(2, sourceId);
	 * 
	 * logger.info("jsm_id"+jsmId);
	 * 
	 * if (sourceType != null) pstmt.setString(3, sourceType); else pstmt.setNull(3,
	 * Types.VARCHAR);
	 * 
	 * if (workitemId != null) pstmt.setString(4, workitemId); else pstmt.setNull(4,
	 * Types.VARCHAR);
	 * 
	 * if (actionedbyId != null) pstmt.setString(5, actionedbyId); else
	 * pstmt.setNull(5, Types.VARCHAR);
	 * 
	 * if (actionedbyName != null) pstmt.setString(6, actionedbyName); else
	 * pstmt.setNull(6, Types.VARCHAR);
	 * 
	 * if (beneficiaryId != null) pstmt.setString(7, beneficiaryId); else
	 * pstmt.setNull(7, Types.VARCHAR);
	 * 
	 * if (beneficiaryName != null) pstmt.setString(8, beneficiaryName); else
	 * pstmt.setNull(8, Types.VARCHAR);
	 * 
	 * if (accountId != null) pstmt.setString(9, accountId); else pstmt.setNull(9,
	 * Types.VARCHAR);
	 * 
	 * if (accountName != null) pstmt.setString(10, accountName); else
	 * pstmt.setNull(10, Types.VARCHAR);
	 * 
	 * if (accessType != null) pstmt.setString(11, accessType); else
	 * pstmt.setNull(11, Types.VARCHAR);
	 * 
	 * if (accessId != null) pstmt.setString(12, accessId); else pstmt.setNull(12,
	 * Types.VARCHAR);
	 * 
	 * if (accessName != null) pstmt.setString(13, accessName); // Ensure this
	 * matches your DB column name else pstmt.setNull(13, Types.VARCHAR);
	 * 
	 * if (accessValue != null) pstmt.setString(14, accessValue); else
	 * pstmt.setNull(14, Types.VARCHAR);
	 * 
	 * if (createdDate != null) pstmt.setString(15, createdDate); else
	 * pstmt.setNull(15, Types.VARCHAR);
	 * 
	 * if (jsmId != null) pstmt.setString(16, jsmId); else pstmt.setNull(16,
	 * Types.VARCHAR);
	 * 
	 * if (jsmProvisionerType != null) pstmt.setString(17, jsmProvisionerType); else
	 * pstmt.setNull(17, Types.VARCHAR);
	 * 
	 * if (jsmProvisionerName != null) pstmt.setString(18, jsmProvisionerName); else
	 * pstmt.setNull(18, Types.VARCHAR);
	 * 
	 * if (jsmProvisionerEmail != null) pstmt.setString(19, jsmProvisionerEmail);
	 * else pstmt.setNull(19, Types.VARCHAR);
	 * 
	 * if (actionType != null) pstmt.setString(20, actionType); else
	 * pstmt.setNull(20, Types.VARCHAR);
	 * 
	 * if (jsmProvisionerId != null) pstmt.setString(21, jsmProvisionerId); else
	 * pstmt.setNull(21, Types.VARCHAR);
	 * 
	 * 
	 * int rowsAffected = pstmt.executeUpdate(); if (rowsAffected > 0) {
	 * logger.info("saveToDatabase method end");
	 * logger.info("Issue saved to the database successfully."); } }catch
	 * (SQLException e) {
	 * logger.error("An error occurred while creating connection with database", e);
	 * 
	 * } }
	 */    
    public void saveToDatabase(
            String sourceName, String sourceId, String sourceType, String workitemId,
            String actionedbyId, String actionedbyName, String beneficiaryId, String beneficiaryName,
            String accountId, String accountName, String accessType, String accessId, String accessName,
            String accessValue, String jsmId, String jsmProvisionerType, String jsmProvisionerName,
            String jsmProvisionerEmail, String actionType, String jsmProvisionerId) throws SQLException {
    
    logger.info("saveToDatabase method start");

    // Get the current system date
    String createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    String sql = "INSERT INTO jira_issues (sourceName, sourceId, sourceType, workitemId, actionedbyId, actionedbyName, " +
                "beneficiaryId, beneficiaryName, accountId, accountName, accessType, accessId, accessName, accessValue, " +
                "createdDate, jsmId, jsmProvisionerType, jsmProvisionerName, jsmProvisionerEmail, actionType, jsmProvisionerId) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, sourceName);
        pstmt.setString(2, sourceId);

        logger.info("jsm_id " + jsmId);

        if (sourceType != null) pstmt.setString(3, sourceType);
        else pstmt.setNull(3, Types.VARCHAR);

        if (workitemId != null) pstmt.setString(4, workitemId);
        else pstmt.setNull(4, Types.VARCHAR);

        if (actionedbyId != null) pstmt.setString(5, actionedbyId);
        else pstmt.setNull(5, Types.VARCHAR);

        if (actionedbyName != null) pstmt.setString(6, actionedbyName);
        else pstmt.setNull(6, Types.VARCHAR);

        if (beneficiaryId != null) pstmt.setString(7, beneficiaryId);
        else pstmt.setNull(7, Types.VARCHAR);

        if (beneficiaryName != null) pstmt.setString(8, beneficiaryName);
        else pstmt.setNull(8, Types.VARCHAR);

        if (accountId != null) pstmt.setString(9, accountId);
        else pstmt.setNull(9, Types.VARCHAR);

        if (accountName != null) pstmt.setString(10, accountName);
        else pstmt.setNull(10, Types.VARCHAR);

        if (accessType != null) pstmt.setString(11, accessType);
        else pstmt.setNull(11, Types.VARCHAR);

        if (accessId != null) pstmt.setString(12, accessId);
        else pstmt.setNull(12, Types.VARCHAR);

        if (accessName != null) pstmt.setString(13, accessName);
        else pstmt.setNull(13, Types.VARCHAR);

        if (accessValue != null) pstmt.setString(14, accessValue);
        else pstmt.setNull(14, Types.VARCHAR);

        pstmt.setString(15, createdDate);  // Set the current system date

        if (jsmId != null) pstmt.setString(16, jsmId);
        else pstmt.setNull(16, Types.VARCHAR);

        if (jsmProvisionerType != null) pstmt.setString(17, jsmProvisionerType);
        else pstmt.setNull(17, Types.VARCHAR);

        if (jsmProvisionerName != null) pstmt.setString(18, jsmProvisionerName);
        else pstmt.setNull(18, Types.VARCHAR);

        if (jsmProvisionerEmail != null) pstmt.setString(19, jsmProvisionerEmail);
        else pstmt.setNull(19, Types.VARCHAR);

        if (actionType != null) pstmt.setString(20, actionType);
        else pstmt.setNull(20, Types.VARCHAR);

        if (jsmProvisionerId != null) pstmt.setString(21, jsmProvisionerId);
        else pstmt.setNull(21, Types.VARCHAR);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
            logger.info("saveToDatabase method end");
            logger.info("Issue saved to the database successfully.");
        }
    } catch (SQLException e) {
        logger.error("An error occurred while creating connection with database", e);
    }
}
    public String getSourceId(String token ,String SOURCE_NAME ) {
        logger.info("getSourceId method start");

        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + SOURCE_FILTER + SOURCE_NAME+"\"";
        logger.info("Request URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token); // Add Bearer prefix
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            String responseBody = responseEntity.getBody();
            logger.info("Response Body: " + responseBody);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                try {
                    // Use ObjectMapper to parse the JSON response
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    
                    // Check if the response is an array or an object as expected
                    JsonNode sources = rootNode.isArray() ? rootNode : rootNode.path("sources");
                    
                    if (sources.isArray() && sources.size() > 0) {
                        JsonNode source = sources.get(0);
                        String sourceId = source.path("id").asText();
                        logger.info("Source ID: " + sourceId);
                        return sourceId;
                    } else {
                        logger.error("No sources found.");
                        return "No sources found.";
                    }
                } catch (Exception e) {
                    logger.error("Error parsing JSON response", e);
                    return "Error parsing JSON response.";
                }
            } else {
                logger.error("Failed to fetch sources. Status Code: " + responseEntity.getStatusCode());
                return "Failed to fetch sources: " + responseEntity.getStatusCode();
            }
        } catch (RestClientException e) {
            logger.error("An error occurred while getting source ID", e);
            return "An error occurred while getting source ID.";
        }
    }

}
