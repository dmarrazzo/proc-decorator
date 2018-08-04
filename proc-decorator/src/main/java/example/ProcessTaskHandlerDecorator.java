package example;

import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.event.SignalManager;
import org.jbpm.process.workitem.rest.RESTWorkItemHandler;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
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
		ProcessInstance processInstance = (ProcessInstance) kieSession.createProcessInstance(processId,
				workItem.getParameters());

		WorkflowProcessInstanceImpl piImpl = (WorkflowProcessInstanceImpl) processInstance;

		long parentInstanceId = workItem.getProcessInstanceId();
		piImpl.setMetaData("ParentProcessInstanceId", parentInstanceId);
		piImpl.setParentProcessInstanceId(parentInstanceId);
		piImpl.setSignalCompletion(true);

		// Start the subprocess
		kieSession.startProcessInstance(processInstance.getId());

		// Manage the subprocess completion
		RetryHandlingListener retryHandlingListener = new RetryHandlingListener(workItem, manager, piImpl, this);
				
		InternalProcessRuntime processRuntime = (InternalProcessRuntime) processInstance.getKnowledgeRuntime().getProcessRuntime();
		SignalManager signalManager = processRuntime.getSignalManager();
		retryHandlingListener.register(signalManager);
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
