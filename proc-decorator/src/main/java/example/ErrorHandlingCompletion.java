package example;

import java.util.Map;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.process.core.context.exception.ExceptionScope;
import org.jbpm.process.instance.context.exception.ExceptionScopeInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.NodeInstance;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class ErrorHandlingCompletion extends ProcessCompletionListener {

	private WorkItemManager manager;
	private WorkItem workItem;
	private ProcessTaskHandlerDecorator processTaskHandlerDecorator;
	private Throwable cause;
	private RuntimeManager runtimeManager;


	public ErrorHandlingCompletion(RuleFlowProcessInstance processInstance,RuntimeManager runtimeManager,
			ProcessTaskHandlerDecorator processTaskHandlerDecorator, WorkItemManager manager, WorkItem workItem,
			Throwable cause) {
		super(processInstance);
		this.runtimeManager = runtimeManager;
		this.processTaskHandlerDecorator = processTaskHandlerDecorator;
		this.manager = manager;
		this.workItem = workItem;
		this.cause = cause;
		register();
	}

	@Override
	public void processCompleted(RuleFlowProcessInstance processInstance) {
		// retry is a variable used in the exception handling process to catch the
		// willingness of retring the failing service
		Boolean retry = (Boolean) processInstance.getVariable("retry");
		if (retry != null && retry == true) {
			// update the parameters with the values coming from the exception handling
			// process
			Map<String, Object> parameters = workItem.getParameters();
			for (String key : parameters.keySet()) {
				Object value = processInstance.getVariable(key);
				if (value != null)
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
	}

	@Override
	public void processAborted(RuleFlowProcessInstance processInstance) {
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

}
