package example;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.EventListener;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

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

	public void listenTo(RuleFlowProcessInstance processInstance, RuntimeManager runtimeManager) {
		eventType = "processInstanceCompleted:" + processInstance.getId();
		eventTypes = new String[] { eventType };

		processInstance.setSignalCompletion(true);

		// The following doesn't work:
		// experiment
		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
		RuleFlowProcessInstance parentProcessInstance = (RuleFlowProcessInstance) runtimeEngine.getKieSession()
				.getProcessInstance(processInstance.getParentProcessInstanceId());

		parentProcessInstance.addEventListener(eventType, this, true);
		runtimeManager.disposeRuntimeEngine(runtimeEngine);

// The following doesn't work:
		
//		processInstance.addEventListener(eventType, this, true);

// The following works just for Singleton Runtime Manager
//		processRuntime = (InternalProcessRuntime) processInstance.getKnowledgeRuntime().getProcessRuntime();
//		processRuntime.getSignalManager().addEventListener(eventType, this);
	}

	private void stopListening() {
		processRuntime.getSignalManager().removeEventListener(eventType, this);
	}

}
