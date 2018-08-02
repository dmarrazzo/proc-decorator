package example;

import org.jbpm.bpmn2.handler.AbstractExceptionHandlingTaskHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class ProcessTaskHandlerDecorator extends AbstractExceptionHandlingTaskHandler {

	public ProcessTaskHandlerDecorator(Class<? extends WorkItemHandler> originalTaskHandlerClass) {
		super(originalTaskHandlerClass);
	}

	@Override
	public void handleExecuteException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.out.println("ProcessTaskHandlerDecorator.handleExecuteException()");
	}

	@Override
	public void handleAbortException(Throwable cause, WorkItem workItem, WorkItemManager manager) {
		System.err.println("ProcessTaskHandlerDecorator.handleAbortException()");
	}

}
