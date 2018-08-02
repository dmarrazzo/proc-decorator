package example;

import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
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
		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) runtimeEngine.getKieSession()
				.startProcess(processId);

		((ProcessInstanceImpl) processInstance).setMetaData("ParentProcessInstanceId", workItem.getProcessInstanceId());

		// Attach the event listener
		EventListener eventListener = new EventListener() {

			@Override
			public void signalEvent(String type, Object event) {
				if (("processInstanceCompleted:" + processInstance.getId()).equals(type)) {
					processInstanceCompleted((ProcessInstance) event, workItem, manager);
				}
			}

			@Override
			public String[] getEventTypes() {
				return new String[] { "processInstanceCompleted:" + processInstance.getId() };
			}
		};
		processInstance.addEventListener("processInstanceCompleted:" + processInstance.getId(), eventListener, true);

	}

	protected void processInstanceCompleted(ProcessInstance event, WorkItem workItem, WorkItemManager manager) {
		manager.completeWorkItem(workItem.getId(), null);
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
