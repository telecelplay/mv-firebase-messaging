package org.meveo.firebase;

import java.util.HashMap;
import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.Notification;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNotification extends Script {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteNotification.class);
	private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
	private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
	private final Repository defaultRepo = repositoryService.findDefaultRepository();

	private String notificationId;
	private final Map<String, Object> result = new HashMap<>();

	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}

	public Map<String, Object> getResult() {
		return result;
	}

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);

		try{
			crossStorageApi.remove(defaultRepo, notificationId, Notification.class);
			result.put("status", "success");
			result.put("result", "Successfully deleted notification with id: " + notificationId);
		}catch (Exception e){
			String errorMessage = "Failed to remove notification with id: " + notificationId;
			LOG.error(errorMessage, e);
			result.put("status", "fail");
			result.put("result", errorMessage);
		}
	}
}
