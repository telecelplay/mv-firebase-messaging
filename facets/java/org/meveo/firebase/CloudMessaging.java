package org.meveo.firebase;

import java.util.Map;
import java.time.OffsetDateTime;
import java.time.Instant;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import com.google.gson.*;

import org.meveo.model.customEntities.Credential;
import org.meveo.model.customEntities.FCMToken;
import org.meveo.service.storage.RepositoryService;
import org.meveo.model.storage.Repository;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.credentials.CredentialHelperService;
import org.meveo.model.persistence.CEIUtils;
import org.apache.commons.codec.digest.DigestUtils;

public class CloudMessaging extends Script {

	private static final Logger log = LoggerFactory.getLogger(CloudMessaging.class);
	private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
	private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
	private Repository defaultRepo = repositoryService.findDefaultRepository();

	static final private String FCM_DOMAIN = "fcm.googleapis.com";

	private String userId;
	private String title;
	private String body;
	private String result;

	public void setUserId(String userId){
		this.userId = userId;
	}

	public void setTitle(String title){
		this.title = title;
	}
	
	public void setBody(String body){
		this.body = body;
	}
  
    public String getResult(){
        return result;
    }

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		Credential credential = CredentialHelperService.getCredential(FCM_DOMAIN, crossStorageApi, defaultRepo);
		if (credential == null) {
			throw new BusinessException("No credential found for " + FCM_DOMAIN);
		} else {
			log.info("using credential {} with username {}", credential.getUuid(), credential.getUsername());
		}
		FCMToken token = null;
		try{ 
			token = crossStorageApi.find(defaultRepo,FCMToken.class).by("userId",userId).getResult();
		} catch (Exception e){
			throw new BusinessException("TOKEN_NOT_FOUND");
		}
		Client client = ClientBuilder.newClient();
		client.register(new CredentialHelperService.LoggingFilter());
		WebTarget target = client.target("https://fcm.googleapis.com/fcm/send");
      String data =  "{\n"
		+"\"userId\": \""+userId+"\"\n"
		+"}";
		String reqBody = "{\n"
		+"\"to\": \""+token.getToken()+"\",\n"
		+"\"title\": \""+title+"\",\n"
		+"\"body\": \""+body+"\"\n"
		+"}";
		Response response = CredentialHelperService.setCredential(target.request(), credential).post(Entity.json(reqBody));
		result = response.readEntity(String.class);
		log.info("response  :" + result);
	}

}