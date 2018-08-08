/**
 * 
 */
package example;

import java.util.Map;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.process.core.context.exception.ExceptionScope;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.context.exception.ExceptionScopeInstance;
import org.jbpm.process.instance.event.SignalManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.NodeInstance;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.EventListener;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

/**
 * @author donato
 *
 */
public class RetryHandlingListener implements EventListener {
	
	private WorkItem workItem;
	private WorkItemManager manager;
	private ProcessTaskHandlerDecorator processTaskHandlerDecorator;
	private String[] eventTypes;
	private SignalManager signalManager;
	private RuntimeManager runtimeManager;
	private Throwable cause;


	public RetryHandlingListener(WorkItem workItem, WorkItemManager manager, RuleFlowProcessInstance processInstance, ProcessTaskHandlerDecorator processTaskHandlerDecorator, Throwable cause) {
		this.workItem = workItem;
		this.manager = manager;
		this.processTaskHandlerDecorator = processTaskHandlerDecorator;
		this.runtimeManager = processTaskHandlerDecorator.runtimeManager;
		this.cause = cause;
		
		// this is the event thrown by a process when closing
		this.eventTypes = new String[] {"processInstanceCompleted:"+processInstance.getId()};
	}

	/**
	 * Handle the completion event for the exception handling process
	 * 
	 * @see org.kie.api.runtime.process.EventListener#signalEvent(java.lang.String, java.lang.Object)
	 */
	@Override
	public void signalEvent(String type, Object event) {
		// event is the closing process instance
		if (event instanceof RuleFlowProcessInstance) {
			RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) event;
			// retry is a variable used in the exception handling process to catch the willingness of retring the failing service
			Boolean retry = (Boolean) processInstance.getVariable("retry");
			
			// exception handling process raised an exception 
			// this exception is triggered by the human decision of rethrowing the exception of the failing service
			// to the process that triggered the service. The user know that the process has an exception handling logic to 
			// gracefully unwind the business logic.
			if (processInstance.getState() == ProcessInstance.STATE_ABORTED) {				
				// retrieve the process instance of the parent process containing the failing service
				RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
				RuleFlowProcessInstance parentProcessInstance = (RuleFlowProcessInstance) runtimeEngine.getKieSession().getProcessInstance(workItem.getProcessInstanceId());
	
				// retrieve the node instance of the failing servise
				long nodeInstanceId = ((WorkItemImpl) workItem).getNodeInstanceId();
				NodeInstance nodeInstance = parentProcessInstance.getNodeInstance(nodeInstanceId, true);

				// throw the exception in the context of the failing services
				String faultName = cause.getClass().getName();
				ExceptionScopeInstance contextInstance = (ExceptionScopeInstance) nodeInstance.resolveContextInstance(ExceptionScope.EXCEPTION_SCOPE, faultName);	
				contextInstance.handleException(faultName, cause);
				
				// mark the work item failed and call the abort procedure
				manager.abortWorkItem(workItem.getId());
			} 
			// user ask to retry
			else if (retry!=null && retry == true) {				
				// update the parameters with the values coming from the exception handling process
				Map<String, Object> parameters = workItem.getParameters();
				for (String key : parameters.keySet()) {
					Object value = processInstance.getVariable(key);
					if ( value != null) 
						parameters.put(key, value);
				}
				
				// execute again the original WIH
				processTaskHandlerDecorator.executeWorkItem(workItem, manager);
			} 
			// user ask to skip the failing service
			else {
				// mark done
				manager.completeWorkItem(workItem.getId(), null);
			}
			// remove this event listner
			unregister();		

		} else {
			System.err.format("event: %s with wrong payload: %s\n", type, event);
		}		
	}

	/* (non-Javadoc)
	 * @see org.kie.api.runtime.process.EventListener#getEventTypes()
	 */
	@Override
	public String[] getEventTypes() {
		return eventTypes;
	}

	/*
	 * register this event listner
	 */
	public void register(SignalManager signalManager) {
		this.signalManager = signalManager;
		for (String eventType : eventTypes) {
			signalManager.addEventListener(eventType, this);
		}
	}

	/*
	 * unregister this event listner
	 */
	private void unregister() {
		for (String eventType : eventTypes) {
			signalManager.removeEventListener(eventType, this);
		}
	}

}
