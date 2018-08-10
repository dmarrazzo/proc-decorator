package example;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.process.EventListener;

public abstract class ProcessCompletionListener implements EventListener {

	private String[] eventTypes;
	private InternalProcessRuntime processRuntime;

	public ProcessCompletionListener() {
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
			stopListening();
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
	
	public void listenTo(RuleFlowProcessInstance processInstance) {
		this.eventTypes = new String[] {"processInstanceCompleted:"+processInstance.getId()};
		processInstance.setSignalCompletion(true);
		processRuntime = (InternalProcessRuntime) processInstance.getKnowledgeRuntime().getProcessRuntime();
		for (String event : getEventTypes()) {
			processRuntime.getSignalManager().addEventListener(event, this);
		}
	}

	private void stopListening() {
		for (String event : getEventTypes()) {
			processRuntime.getSignalManager().removeEventListener(event, this);
		}		
	}

}
