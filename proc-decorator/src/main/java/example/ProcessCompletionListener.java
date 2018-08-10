package example;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.process.EventListener;

public abstract class ProcessCompletionListener implements EventListener {

	private RuleFlowProcessInstance processInstance;
	private String[] eventTypes;
	private InternalProcessRuntime processRuntime;

	public ProcessCompletionListener(RuleFlowProcessInstance processInstance) {
		this.processInstance = processInstance;
		this.eventTypes = new String[] {"processInstanceCompleted:"+processInstance.getId()};
	}
	
	@Override
	public void signalEvent(String type, Object event) {
		if (event instanceof RuleFlowProcessInstance) {
			RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) event;
			
			if (processInstance.getState() == ProcessInstance.STATE_COMPLETED) {
				processCompleted(processInstance);
			} else {
				processAborted(processInstance);
			}
			unregister();
		} else {
			System.err.format("event: %s with wrong payload: %s\n", type, event);
		}
	}

	public abstract void processCompleted(RuleFlowProcessInstance processInstance);
	public abstract void processAborted(RuleFlowProcessInstance processInstance);
	
	@Override
	public String[] getEventTypes() {
		return eventTypes;
	}
	
	public void register() {
		processInstance.setSignalCompletion(true);
		processRuntime = (InternalProcessRuntime) processInstance.getKnowledgeRuntime().getProcessRuntime();
		for (String event : getEventTypes()) {
			processRuntime.getSignalManager().addEventListener(event, this);
		}
	}

	private void unregister() {
		for (String event : getEventTypes()) {
			processRuntime.getSignalManager().removeEventListener(event, this);
		}		
	}

}
