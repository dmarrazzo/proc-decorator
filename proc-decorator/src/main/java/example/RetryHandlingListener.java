/**
 * 
 */
package example;

import java.util.Map;

import org.jbpm.process.instance.event.SignalManager;
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
	private WorkflowProcessInstance pi;

	public RetryHandlingListener(WorkItem workItem, WorkItemManager manager, WorkflowProcessInstance pi, ProcessTaskHandlerDecorator processTaskHandlerDecorator) {
		this.workItem = workItem;
		this.manager = manager;
		this.processTaskHandlerDecorator = processTaskHandlerDecorator;
		//this.eventTypes = new String[] {"retry:"+pi.getId(),"skip:"+pi.getId()};
		this.eventTypes = new String[] {"processInstanceCompleted:"+pi.getId()};
	    this.pi = pi;
	}

	/* (non-Javadoc)
	 * @see org.kie.api.runtime.process.EventListener#signalEvent(java.lang.String, java.lang.Object)
	 */
	@Override
	public void signalEvent(String type, Object event) {
		if (event != null && event instanceof Boolean && ((Boolean)event)) {
			//retry
			manager.completeWorkItem(workItem.getId(), null);
			Map<String, Object> parameters = workItem.getParameters();
			for (String key : parameters.keySet()) {
				Object value = pi.getVariable(key);
				if ( value != null) 
					parameters.put(key, value);
			}
			processTaskHandlerDecorator.executeWorkItem(workItem, manager);
		} else {
			//skip
			manager.completeWorkItem(workItem.getId(), null);
		}
	}

	/* (non-Javadoc)
	 * @see org.kie.api.runtime.process.EventListener#getEventTypes()
	 */
	@Override
	public String[] getEventTypes() {
		return eventTypes;
	}

	public void register(SignalManager signalManager) {
		for (String eventType : eventTypes) {
			signalManager.addEventListener(eventType, this);
		}
	}

}
