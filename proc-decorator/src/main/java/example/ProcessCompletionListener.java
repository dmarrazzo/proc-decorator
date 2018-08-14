package example;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.process.EventListener;

public abstract class ProcessCompletionListener implements EventListener {

	private String[] eventTypes;
	private InternalProcessRuntime processRuntime;
	private String eventType;

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
		this.eventType = "processInstanceCompleted:" + processInstance.getId();
		this.eventTypes = new String[] { eventType };

		processInstance.setSignalCompletion(true);
		processRuntime = (InternalProcessRuntime) processInstance.getKnowledgeRuntime().getProcessRuntime();
		processRuntime.getSignalManager().addEventListener(eventType, this);
	}

	private void stopListening() {
		processRuntime.getSignalManager().removeEventListener(eventType, this);
	}

}
