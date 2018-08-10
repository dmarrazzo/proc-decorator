package example;

import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.workitem.rest.RESTWorkItemHandler;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class ProcessTaskHandlerDecorator extends AbstractExceptionHandlingTaskHandler {

	protected RuntimeManager runtimeManager;
	private String processId;

	public ProcessTaskHandlerDecorator(Class<? extends WorkItemHandler> originalTaskHandlerClass,
			RuntimeManager runtimeManager) {
		super(originalTaskHandlerClass);
		this.runtimeManager = runtimeManager;
	}

	public ProcessTaskHandlerDecorator(RuntimeManager runtimeManager) {
		super(RESTWorkItemHandler.class);
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

		// Create the kiesession
		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
		KieSession kieSession = runtimeEngine.getKieSession();

		// Create the subprocess
		RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) kieSession.createProcessInstance(processId,
				workItem.getParameters());

		long parentInstanceId = workItem.getProcessInstanceId();
		processInstance.setMetaData("ParentProcessInstanceId", parentInstanceId);
		processInstance.setParentProcessInstanceId(parentInstanceId);

		// Start the subprocess
		kieSession.startProcessInstance(processInstance.getId());

		// Manage the subprocess completion
		if (processInstance.getState() == ProcessInstance.STATE_COMPLETED
				|| processInstance.getState() == ProcessInstance.STATE_ABORTED) {
			manager.completeWorkItem(workItem.getId(), null);
		} else {
			ErrorHandlingCompletion errorHandlingCompletion = new ErrorHandlingCompletion(runtimeManager, this, manager, workItem, cause);
			errorHandlingCompletion.listenTo(processInstance);
		}
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
