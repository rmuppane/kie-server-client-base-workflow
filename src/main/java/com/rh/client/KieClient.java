package com.rh.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.instance.ProcessInstanceCustomVarsList;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieClient {

	final static Logger log = LoggerFactory.getLogger(KieClient.class);

	// private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
	private static final String URL = "http://localhost:8090/rest/server";
	private static final String user = System.getProperty("username", "rhpamAdmin");
	private static final String password = System.getProperty("password", "Pa$$w0rd");

	// CONSTANTS
	private static final String CONTAINER = "base-work-flow";
	private static final String PROCESS_ID = "IPOS.base-workflow";
	private static final String QUERY_PROCESS_INSTANCES_WITH_VAR = "jbpmProcessInstancesWithVariables";

	private KieServicesClient client;

	public static void main(String[] args) {
		KieClient clientApp = new KieClient(user, password);
		System.setProperty("org.drools.server.filter.classes", "true");
		log.info("begin");

		// Long piid = clientApp.launchProcess();
		Long piid = 1l;
		Long taskId = clientApp.getTaskAsPotentialOwner("rhpamAdmin");
		clientApp.allocateUser(taskId, "user");
		
		//KieClient clientAppUser = new KieClient("user", password);
		//taskId = clientAppUser.getTaskAsPotentialOwner("user");
		//clientAppUser.completeTask(taskId, "user");

		log.info("piid {}", piid);

		log.info("end");
	}

	public KieClient(String user, String password) {
		client = getClient(user, password);
	}
	

	public Long launchProcess() {
		try {
			ProcessServicesClient processClient = client.getServicesClient(ProcessServicesClient.class);
			Map<String, Object> inputData = new HashMap<>();

			setInputData(inputData);
			return processClient.startProcess(CONTAINER, PROCESS_ID, inputData);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void setInputData(Map<String, Object> inputData) {
	}

	public long getTaskAsPotentialOwner(String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		List<TaskSummary> tasks = userTaskServicesClient.findTasksAssignedAsPotentialOwner(userName, 0, 10);
		TaskSummary ts = (TaskSummary)tasks.get(0);
		return ts.getId();
	}

	public void allocateUser(Long taskId, String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		userTaskServicesClient.claimTask(CONTAINER, taskId, userName);
		userTaskServicesClient.startTask(CONTAINER, taskId, userName);
		Map<String, Object> inputData = new HashMap<>();
		inputData.put("nextAct", "Allocate");
		userTaskServicesClient.completeAutoProgress(CONTAINER, taskId, userName, inputData);
	}
	
	
	public void reassignTeam(Long taskId, String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		userTaskServicesClient.claimTask(CONTAINER, taskId, userName);
		userTaskServicesClient.startTask(CONTAINER, taskId, userName);
		Map<String, Object> inputData = new HashMap<>();
		inputData.put("nextAct", "Reassign");
		userTaskServicesClient.completeAutoProgress(CONTAINER, taskId, userName, inputData);
	}
	
	public void completeTask(Long taskId, String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		userTaskServicesClient.claimTask(CONTAINER, taskId, userName);
		userTaskServicesClient.startTask(CONTAINER, taskId, userName);
		Map<String, Object> inputData = new HashMap<>();
		inputData.put("nextAct", "Complete");
		userTaskServicesClient.completeAutoProgress(CONTAINER, taskId, userName, inputData);
	}
	
	public void findProcessesByIds() {
		try {
			QueryServicesClient queryClient = client.getServicesClient(QueryServicesClient.class);

			QueryFilterSpec spec = new QueryFilterSpecBuilder().between("processInstanceId", 0, 10)
															   .in("variableId", Arrays.asList("dossierId", "status"))
			                                                   .get();

			queryClient.query(QUERY_PROCESS_INSTANCES_WITH_VAR, QueryServicesClient.QUERY_MAP_PI_WITH_VARS, spec, 0, 10,
			        ProcessInstanceCustomVarsList.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendSignal(String signalName, Object signalPayload) {
		ProcessServicesClient processClient = client.getServicesClient(ProcessServicesClient.class);
		processClient.signal(CONTAINER, signalName, signalPayload);
	}

	private KieServicesClient getClient() {
		KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(URL, user, password);

		// Marshalling
		config.setMarshallingFormat(MarshallingFormat.JSON);
		KieServicesClient client = KieServicesFactory.newKieServicesClient(config);

		return client;
	}
	
	private KieServicesClient getClient(String user, String password) {
		KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(URL, user, password);

		// Marshalling
		config.setMarshallingFormat(MarshallingFormat.JSON);
		KieServicesClient client = KieServicesFactory.newKieServicesClient(config);

		return client;
	}
}
