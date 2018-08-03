package example;

import org.drools.core.event.DefaultProcessEventListener;
import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.process.workitem.rest.RESTWorkItemHandler;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
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

		if (processInstance.getState() == ProcessInstance.STATE_COMPLETED
				|| processInstance.getState() == ProcessInstance.STATE_ABORTED) {
			manager.completeWorkItem(workItem.getId(), null);
		} else {
			// Attach the event listener
			ProcessEventListener procListener = new DefaultProcessEventListener() {
				public void afterProcessCompleted(ProcessCompletedEvent event) {
					System.out.println("afterProcessCompleted()");
					if (processInstance.getId() == event.getProcessInstance().getId()) {
						manager.completeWorkItem(workItem.getId(), null);
						event.getKieRuntime().removeEventListener(this);
					}
					super.afterProcessCompleted(event);
				}
			};
			kieSession.addEventListener(procListener);
		}
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
