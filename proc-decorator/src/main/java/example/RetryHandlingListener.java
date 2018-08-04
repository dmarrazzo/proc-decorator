/**
 * 
 */
package example;

import java.util.Map;

import org.jbpm.process.instance.event.SignalManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.runtime.process.EventListener;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

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

	public RetryHandlingListener(WorkItem workItem, WorkItemManager manager, WorkflowProcessInstance pi, ProcessTaskHandlerDecorator processTaskHandlerDecorator) {
		this.workItem = workItem;
		this.manager = manager;
		this.processTaskHandlerDecorator = processTaskHandlerDecorator;
		// this is the event thrown by a process when closing
		this.eventTypes = new String[] {"processInstanceCompleted:"+pi.getId()};
	}

	/* (non-Javadoc)
	 * @see org.kie.api.runtime.process.EventListener#signalEvent(java.lang.String, java.lang.Object)
	 */
	@Override
	public void signalEvent(String type, Object event) {
		System.out.format("event: %s, payload: %s\n", type, event);
		
		if (event instanceof RuleFlowProcessInstance) {
			RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) event;
			Boolean retry = (Boolean) processInstance.getVariable("retry");
			if (retry!=null && retry == true) {
				// retry
				
				// update the parameters with the values coming from the exception handling process
				Map<String, Object> parameters = workItem.getParameters();
				for (String key : parameters.keySet()) {
					Object value = processInstance.getVariable(key);
					if ( value != null) 
						parameters.put(key, value);
				}
				
				// execute again the original WIH
				processTaskHandlerDecorator.executeWorkItem(workItem, manager);
			} else {
				// skip
				manager.completeWorkItem(workItem.getId(), null);
			}
		} else {
			System.err.format("event: %s with wrong payload: %s\n", type, event);
		}
		
		// remove this event listner
		unregister();		
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
