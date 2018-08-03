package example;

import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.EventListener;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class ProcessTaskHandlerDecorator extends AbstractExceptionHandlingTaskHandler {

	private RuntimeManager runtimeManager;
	private String processId;

	class SubprocessEventListener implements EventListener {

		private long processInstanceId;
		private WorkItem workItem;
		private WorkItemManager manager;

		public SubprocessEventListener(long processId, WorkItem workItem, WorkItemManager manager) {
			super();
			this.processInstanceId = processId;
			this.workItem = workItem;
			this.manager = manager;
		}

		@Override
		public void signalEvent(String type, Object event) {
			System.out.println("ProcessTaskHandlerDecorator.SubprocessEventListener.signalEvent()");
			if (("processInstanceCompleted:" + processInstanceId).equals(type)) {
				manager.completeWorkItem(workItem.getId(), null);
			}
		}

		@Override
		public String[] getEventTypes() {
			System.out.println("ProcessTaskHandlerDecorator.SubprocessEventListener.getEventTypes()");
			return new String[] { "processInstanceCompleted:" + processInstanceId };
		}

	}

	public ProcessTaskHandlerDecorator(Class<? extends WorkItemHandler> originalTaskHandlerClass,
			RuntimeManager runtimeManager) {
		super(originalTaskHandlerClass);
		this.runtimeManager = runtimeManager;
	}

	public ProcessTaskHandlerDecorator(Class<? extends WorkItemHandler> originalTaskHandlerClass,
			RuntimeManager runtimeManager, String processId) {
		super(originalTaskHandlerClass);
		this.runtimeManager = runtimeManager;
		this.processId = processId;
	}

	@Override
	public void handleExecuteException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.out.println("ProcessTaskHandlerDecorator.handleExecuteException()");

		if (processId == null)
			processId = (String) workItem.getParameter("processId");

		// Start the subprocess
		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
		KieSession kieSession = runtimeEngine.getKieSession();
		ProcessInstance processInstance = (ProcessInstance) kieSession.createProcessInstance(processId,
				workItem.getParameters());

		ProcessInstanceImpl piImpl = (ProcessInstanceImpl) processInstance;
		piImpl.setMetaData("ParentProcessInstanceId", workItem.getProcessInstanceId());
		piImpl.setParentProcessInstanceId(workItem.getProcessInstanceId());
		piImpl.setSignalCompletion(true);

		kieSession.startProcessInstance(processInstance.getId());

		// Attach the event listener
		SubprocessEventListener eventListener = new SubprocessEventListener(processInstance.getId(), workItem, manager);

		if (processInstance.getState() == ProcessInstance.STATE_COMPLETED
				|| processInstance.getState() == ProcessInstance.STATE_ABORTED) {
			manager.completeWorkItem(workItem.getId(), null);
		} else {
			WorkflowProcessInstance callingPI = (WorkflowProcessInstance) kieSession
					.getProcessInstance(workItem.getProcessInstanceId());
			callingPI.addEventListener("processInstanceCompleted:" + processInstance.getId(), eventListener, true);
//			((WorkflowProcessInstance) processInstance)
//					.addEventListener("processInstanceCompleted:" + processInstance.getId(), eventListener, true);

		}
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
