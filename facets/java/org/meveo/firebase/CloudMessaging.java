package org.meveo.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.meveo.model.customEntities.Notification;
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

public class CloudMessaging extends Script {

    private static final Logger log = LoggerFactory.getLogger(CloudMessaging.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    static final private String FCM_DOMAIN = "fcm.googleapis.com";

    private String userId;
    private String title;
    private String body;
    private final Map<String, Object> result = new HashMap<>();

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getResult() {
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

        FCMToken token;
        try {
            token = crossStorageApi.find(defaultRepo, FCMToken.class).by("userId", userId).getResult();
        } catch (Exception e) {
            throw new BusinessException("TOKEN_NOT_FOUND");
        }

        Client client = ClientBuilder.newClient();
        client.register(new CredentialHelperService.LoggingFilter());
        WebTarget target = client.target("https://fcm.googleapis.com/fcm/send");

        Map<String, String> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);

        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);

        Map<String, Object> content = new HashMap<>();
        content.put("to", token.getToken());
        content.put("title", title);
        content.put("body", body);
        content.put("notification", notification);
        content.put("data", data);

        String requestBody = new Gson().toJson(content);

        log.info("notification content :{}", requestBody);
        String error = null;
        try {
            Response response = CredentialHelperService.setCredential(target.request(), credential)
                                                       .post(Entity.json(requestBody));
            result.put("status", "success");
            result.put("result", response.readEntity(String.class));
        } catch (Exception e) {
            String errorMessage = "Failed to send notification, reason: " + e.getMessage();
            result.put("status", "fail");
            result.put("result", errorMessage);
            error = errorMessage;
            log.error(errorMessage, e);
        }

        try {
            Notification notificationDetails = new Notification();
            notificationDetails.setTitle(title);
            notificationDetails.setRecipient(userId);
            notificationDetails.setSendDate(Instant.now());
            notificationDetails.setSendStatus("" + result.get("status"));
            notificationDetails.setNotificationContents(requestBody);
            notificationDetails.setError(error);
            crossStorageApi.createOrUpdate(defaultRepo, notificationDetails);
        } catch (Exception e) {
            String errorMessage = "Failed to save notification details, reason: " + e.getMessage();
            result.put("status", "fail");
            result.put("result", errorMessage);
            log.error(errorMessage, e);
        }
    }
}
