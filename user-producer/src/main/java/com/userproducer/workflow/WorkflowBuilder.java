package com.userproducer.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Workflow orchestration engine with Saga pattern support. Features: retry,
 * compensation, async steps, conditional flows, parallel execution.
 * </pre>
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WorkflowBuilder {

	// ============================================
	// INTERFACES FOR EXTENSIBILITY
	// ============================================

	/** Hook into step execution lifecycle (before/after/error) */
	public interface StepInterceptor {
		default <T> void beforeStep(WorkflowStep<T> step, WorkflowContext context) {
		}

		default <T> void afterStep(WorkflowStep<T> step, WorkflowContext context, T result) {
		}

		default <T> void onStepError(WorkflowStep<T> step, WorkflowContext context, Exception error) {
		}
	}

	/** Listen to workflow lifecycle events */
	public interface WorkflowListener {
		default void onWorkflowStart(String workflowName, WorkflowContext context) {
		}

		default void onWorkflowComplete(String workflowName, WorkflowContext context, Object result) {
		}

		default void onWorkflowError(String workflowName, WorkflowContext context, Exception error) {
		}

		default void onCompensationStart(String workflowName, WorkflowContext context) {
		}

		default void onCompensationComplete(String workflowName, WorkflowContext context, int succeeded, int failed) {
		}
	}

	/** Collect execution metrics for monitoring */
	public interface MetricsCollector {
		default void recordStepExecution(String stepName, Duration duration, boolean success) {
		}

		default void recordRetry(String stepName, int attempt) {
		}

		default void recordCompensation(String stepName, boolean success) {
		}
	}

	/** Ensure step idempotency (prevent duplicate execution) */
	public interface IdempotencyChecker {
		boolean isStepExecuted(String workflowId, String stepId);

		void markStepExecuted(String workflowId, String stepId);
	}

	// ============================================
	// CONSTANTS & CONFIG
	// ============================================

	/** Default configuration values */
	public static final class Config {
		public static final int DEFAULT_MAX_RETRIES = 3;
		public static final long DEFAULT_RETRY_DELAY_MS = 1000;
		public static final boolean DEFAULT_ENABLE_LOGGING = true;
		public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
		public static final Duration DEFAULT_COMPENSATION_TIMEOUT = Duration.ofSeconds(60);
		public static final int DEFAULT_MAX_COMPENSATION_RETRIES = 1;
	}

	// ============================================
	// TYPED CONTEXT KEYS
	// ============================================

	/** Predefined context keys for type-safe data access */
	public enum ContextKey {
		WORKFLOW_ID("workflow_id"), EXECUTION_ID("execution_id"), WORKFLOW_NAME("workflow_name"),
		LAST_RESULT("last_result"), FINAL_RESULT("final_result"), STEP_RESULT_PREFIX("step_result_"),
		CURRENT_ITEM("current_item"), ITEM_INDEX("item_index"), ERROR_CONTEXT("error_context"),
		START_TIME("start_time"), METRICS("metrics_data");

		private final String value;

		ContextKey(String value) {
			this.value = value;
		}

		public String key() {
			return value;
		}

		public String withSuffix(String suffix) {
			return value + suffix;
		}
	}

	// ============================================
	// STEP EXECUTION STRATEGIES
	// ============================================

	/**
	 * Customize how steps are executed (e.g., for testing or special requirements)
	 */
	public interface StepExecutor {
		<T> CompletableFuture<T> execute(WorkflowStep<T> step, WorkflowContext context);

		<T> CompletableFuture<Void> compensate(WorkflowStep<T> step, T result, WorkflowContext context);
	}

	/** Default executor with metrics collection */
	public static class DefaultStepExecutor implements StepExecutor {
		private final ExecutorService executor;
		private final MetricsCollector metricsCollector;

		public DefaultStepExecutor(ExecutorService executor, MetricsCollector metricsCollector) {
			this.executor = executor != null ? executor : ForkJoinPool.commonPool();
			this.metricsCollector = metricsCollector;
		}

		@Override
		public <T> CompletableFuture<T> execute(WorkflowStep<T> step, WorkflowContext context) {
			return CompletableFuture.supplyAsync(() -> {
				Instant start = Instant.now();
				try {
					T result = step.action.get();
					Duration duration = Duration.between(start, Instant.now());
					if (metricsCollector != null) {
						metricsCollector.recordStepExecution(step.name, duration, true);
					}
					return result;
				} catch (Exception e) {
					Duration duration = Duration.between(start, Instant.now());
					if (metricsCollector != null) {
						metricsCollector.recordStepExecution(step.name, duration, false);
					}
					throw new CompletionException(e);
				}
			}, executor);
		}

		@Override
		public <T> CompletableFuture<Void> compensate(WorkflowStep<T> step, T result, WorkflowContext context) {
			if (step.compensation == null) {
				return CompletableFuture.completedFuture(null);
			}

			return CompletableFuture.runAsync(() -> {
				@SuppressWarnings("unused")
				Instant start = Instant.now();
				try {
					step.compensation.accept(result);
					if (metricsCollector != null) {
						metricsCollector.recordCompensation(step.name, true);
					}
				} catch (Exception e) {
					if (metricsCollector != null) {
						metricsCollector.recordCompensation(step.name, false);
					}
					throw new CompletionException(e);
				}
			}, executor);
		}
	}

	// ============================================
	// ASYNC STEP SUPPORT
	// ============================================

	/** Async workflow step that returns CompletableFuture */
	public static final class AsyncWorkflowStep<T> {
		public final String id;
		public final String name;
		public final Function<WorkflowContext, CompletableFuture<T>> asyncAction;
		public final Function<T, CompletableFuture<Void>> asyncCompensation;
		public final int maxRetries;
		public final Duration retryDelay;
		public final Duration timeout;
		public final boolean isCritical;
		public final boolean isIdempotent;

		private AsyncWorkflowStep(Builder<T> builder) {
			this.id = Objects.requireNonNull(builder.id, "Step ID cannot be null");
			this.name = Objects.requireNonNull(builder.name, "Step name cannot be null");
			this.asyncAction = Objects.requireNonNull(builder.asyncAction, "Async action cannot be null");
			this.asyncCompensation = builder.asyncCompensation;
			this.maxRetries = builder.maxRetries;
			this.retryDelay = builder.retryDelay;
			this.timeout = builder.timeout;
			this.isCritical = builder.isCritical;
			this.isIdempotent = builder.isIdempotent;
		}

		/** Create async step builder */
		public static <T> Builder<T> builder(String name, Function<WorkflowContext, CompletableFuture<T>> asyncAction) {
			return new Builder<>(name, asyncAction);
		}

		/** Builder for async steps */
		public static class Builder<T> {
			private String id;
			private final String name;
			private final Function<WorkflowContext, CompletableFuture<T>> asyncAction;
			private Function<T, CompletableFuture<Void>> asyncCompensation = null;
			private int maxRetries = Config.DEFAULT_MAX_RETRIES;
			private Duration retryDelay = Duration.ofMillis(Config.DEFAULT_RETRY_DELAY_MS);
			private Duration timeout = Config.DEFAULT_TIMEOUT;
			private boolean isCritical = false;
			private boolean isIdempotent = false;

			public Builder(String name, Function<WorkflowContext, CompletableFuture<T>> asyncAction) {
				this.name = Objects.requireNonNull(name, "Step name cannot be null");
				this.asyncAction = Objects.requireNonNull(asyncAction, "Async action cannot be null");
				this.id = UUID.randomUUID().toString();
			}

			public Builder<T> id(String id) {
				this.id = Objects.requireNonNull(id, "Step ID cannot be null");
				return this;
			}

			public Builder<T> asyncCompensation(Function<T, CompletableFuture<Void>> asyncCompensation) {
				this.asyncCompensation = asyncCompensation;
				return this;
			}

			public Builder<T> maxRetries(int maxRetries) {
				this.maxRetries = maxRetries;
				return this;
			}

			public Builder<T> retryDelay(Duration retryDelay) {
				this.retryDelay = retryDelay;
				return this;
			}

			public Builder<T> timeout(Duration timeout) {
				this.timeout = timeout;
				return this;
			}

			public Builder<T> critical() {
				this.isCritical = true;
				return this;
			}

			public Builder<T> idempotent() {
				this.isIdempotent = true;
				return this;
			}

			public AsyncWorkflowStep<T> build() {
				return new AsyncWorkflowStep<>(this);
			}
		}
	}

	// ============================================
	// WORKFLOW STEP (Enhanced)
	// ============================================

	/** A single step in the workflow with action and optional compensation */
	public static final class WorkflowStep<T> {
		public final String id;
		public final String name;
		public final Supplier<T> action;
		public final Consumer<T> compensation;
		public final int maxRetries;
		public final Duration retryDelay;
		public final Duration timeout;
		public final boolean isCritical;
		public final boolean isIdempotent;
		public final boolean isAsync;
		public final Map<String, Object> metadata;

		private WorkflowStep(Builder<T> builder) {
			this.id = Objects.requireNonNull(builder.id, "Step ID cannot be null");
			this.name = Objects.requireNonNull(builder.name, "Step name cannot be null");
			this.action = Objects.requireNonNull(builder.action, "Step action cannot be null");
			this.compensation = builder.compensation;
			this.maxRetries = builder.maxRetries;
			this.retryDelay = builder.retryDelay;
			this.timeout = builder.timeout;
			this.isCritical = builder.isCritical;
			this.isIdempotent = builder.isIdempotent;
			this.isAsync = builder.isAsync;
			this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
		}

		/** Create step builder */
		public static <T> Builder<T> builder(String name, Supplier<T> action) {
			return new Builder<>(name, action);
		}

		/** Step builder with fluent API */
		public static class Builder<T> {
			private String id;
			private final String name;
			private final Supplier<T> action;
			private Consumer<T> compensation = null;
			private int maxRetries = Config.DEFAULT_MAX_RETRIES;
			private Duration retryDelay = Duration.ofMillis(Config.DEFAULT_RETRY_DELAY_MS);
			private Duration timeout = Config.DEFAULT_TIMEOUT;
			private boolean isCritical = false;
			private boolean isIdempotent = false;
			private boolean isAsync = false;
			private final Map<String, Object> metadata = new HashMap<>();

			public Builder(String name, Supplier<T> action) {
				this.name = Objects.requireNonNull(name, "Step name cannot be null");
				this.action = Objects.requireNonNull(action, "Step action cannot be null");
				this.id = UUID.randomUUID().toString();
			}

			public Builder<T> id(String id) {
				this.id = Objects.requireNonNull(id, "Step ID cannot be null");
				return this;
			}

			public Builder<T> compensation(Consumer<T> compensation) {
				this.compensation = compensation;
				return this;
			}

			public Builder<T> maxRetries(int maxRetries) {
				this.maxRetries = maxRetries;
				return this;
			}

			public Builder<T> retryDelay(Duration retryDelay) {
				this.retryDelay = retryDelay;
				return this;
			}

			public Builder<T> timeout(Duration timeout) {
				this.timeout = timeout;
				return this;
			}

			public Builder<T> critical() {
				this.isCritical = true;
				return this;
			}

			public Builder<T> idempotent() {
				this.isIdempotent = true;
				return this;
			}

			public Builder<T> async() {
				this.isAsync = true;
				return this;
			}

			public Builder<T> metadata(String key, Object value) {
				this.metadata.put(key, value);
				return this;
			}

			public WorkflowStep<T> build() {
				return new WorkflowStep<>(this);
			}
		}
	}

	// ============================================
	// WORKFLOW CONTEXT (Enhanced)
	// ============================================

	/** Shared context for workflow execution, stores data and execution trace */
	public static final class WorkflowContext {
		private final Map<String, Object> data = new ConcurrentHashMap<>();
		private final List<String> executionTrace = Collections.synchronizedList(new ArrayList<>());
		private final String contextId;
		private final Instant createdAt;
		private final AtomicInteger version = new AtomicInteger(0);
		private final Map<String, Object> stepResults = new ConcurrentHashMap<>();
		private final Map<String, Object> stepErrors = new ConcurrentHashMap<>();
		private final Set<String> executedStepIds = ConcurrentHashMap.newKeySet();

		public WorkflowContext() {
			this.contextId = UUID.randomUUID().toString();
			this.createdAt = Instant.now();
			this.data.put(ContextKey.WORKFLOW_ID.key(), contextId);
			this.data.put(ContextKey.START_TIME.key(), createdAt);
		}

		public WorkflowContext(Map<String, Object> initialData) {
			this();
			if (initialData != null) {
				data.putAll(initialData);
			}
		}

		/** Get value from context */
		@SuppressWarnings("unchecked")
		public <T> T get(String key) {
			if (key == null)
				return null;
			return (T) data.get(key);
		}

		/** Get value or default if not found */
		@SuppressWarnings("unchecked")
		public <T> T get(String key, T defaultValue) {
			if (key == null)
				return defaultValue;
			T value = (T) data.get(key);
			return value != null ? value : defaultValue;
		}

		/** Get value using typed ContextKey */
		@SuppressWarnings("unchecked")
		public <T> T get(ContextKey key) {
			if (key == null)
				return null;
			return (T) data.get(key.key());
		}

		/** Get value with default using ContextKey */
		@SuppressWarnings("unchecked")
		public <T> T get(ContextKey key, T defaultValue) {
			if (key == null)
				return defaultValue;
			T value = (T) data.get(key.key());
			return value != null ? value : defaultValue;
		}

		/** Store value in context */
		public void put(String key, Object value) {
			if (key == null) {
				log.warn("Attempted to put value with null key, ignoring");
				return;
			}
			data.put(key, value);
			version.incrementAndGet();
		}

		/** Store value using typed ContextKey */
		public void put(ContextKey key, Object value) {
			if (key == null) {
				log.warn("Attempted to put value with null ContextKey, ignoring");
				return;
			}
			data.put(key.key(), value);
			version.incrementAndGet();
		}

		/** Remove value from context */
		public void remove(String key) {
			if (key != null) {
				data.remove(key);
				version.incrementAndGet();
			}
		}

		/** Remove value using ContextKey */
		public void remove(ContextKey key) {
			if (key != null) {
				data.remove(key.key());
				version.incrementAndGet();
			}
		}

		/** Check if key exists */
		public boolean containsKey(String key) {
			return key != null && data.containsKey(key);
		}

		/** Check if ContextKey exists */
		public boolean containsKey(ContextKey key) {
			return key != null && data.containsKey(key.key());
		}

		/** Get all context keys */
		public Set<String> keySet() {
			return new HashSet<>(data.keySet());
		}

		/** Clear all context data */
		public void clear() {
			data.clear();
			executionTrace.clear();
			stepResults.clear();
			stepErrors.clear();
			executedStepIds.clear();
			version.incrementAndGet();
		}

		/** Add message to execution trace */
		public void trace(String message) {
			executionTrace.add(String.format("[%s] %s", Instant.now(), message));
		}

		/** Get execution trace */
		public List<String> getTrace() {
			return new ArrayList<>(executionTrace);
		}

		/** Get context snapshot (copy) */
		public Map<String, Object> snapshot() {
			return new HashMap<>(data);
		}

		/** Get unique context ID */
		public String getContextId() {
			return contextId;
		}

		/** Get context creation time */
		public Instant getCreatedAt() {
			return createdAt;
		}

		/** Get context version (increments on each modification) */
		public int getVersion() {
			return version.get();
		}

		/** Store step execution result */
		public void recordStepResult(String stepId, Object result) {
			if (stepId != null) {
				stepResults.put(stepId, result);
				executedStepIds.add(stepId);
			}
		}

		/** Store step execution error */
		public void recordStepError(String stepId, Throwable cause) {
			if (stepId != null) {
				stepErrors.put(stepId, cause);
			}
		}

		/** Get step result by step ID */
		@SuppressWarnings("unchecked")
		public <T> T getStepResult(String stepId) {
			return (T) stepResults.get(stepId);
		}

		/** Get step error by step ID */
		@SuppressWarnings("unchecked")
		public <T extends Exception> T getStepError(String stepId) {
			return (T) stepErrors.get(stepId);
		}

		/** Check if step was executed */
		public boolean isStepExecuted(String stepId) {
			return executedStepIds.contains(stepId);
		}

		/** Get all executed step IDs */
		public Set<String> getExecutedStepIds() {
			return new HashSet<>(executedStepIds);
		}

		/** Get all step results */
		public Map<String, Object> getAllStepResults() {
			return new HashMap<>(stepResults);
		}
	}

	// ============================================
	// WORKFLOW BUILDER CORE (Enhanced)
	// ============================================

	private final WorkflowContext context;
	private final Queue<Object> steps = new ConcurrentLinkedQueue<>(); // Can hold both sync and async steps
	private final List<Object> executedSteps = Collections.synchronizedList(new ArrayList<>());
	private final AtomicInteger stepCounter = new AtomicInteger(0);
	private final List<Exception> compensationErrors = Collections.synchronizedList(new ArrayList<>());
	private final List<StepInterceptor> interceptors = Collections.synchronizedList(new ArrayList<>());
	private final List<WorkflowListener> listeners = Collections.synchronizedList(new ArrayList<>());

	private String workflowName = "Workflow";
	private boolean enableLogging = Config.DEFAULT_ENABLE_LOGGING;
	private boolean skipCompensationOnFailure = false;
	private ExecutorService asyncExecutor;
	private StepExecutor stepExecutor;
	private MetricsCollector metricsCollector;
	private IdempotencyChecker idempotencyChecker;
	private volatile boolean isExecuting = false;
	private CompletableFuture<?> currentExecutionFuture;

	// CONSTRUCTORS
	public WorkflowBuilder() {
		this.context = new WorkflowContext();
		this.asyncExecutor = null;
		initializeContext();
	}

	public WorkflowBuilder(ExecutorService executor) {
		this.context = new WorkflowContext();
		this.asyncExecutor = executor;
		initializeContext();
	}

	public WorkflowBuilder(Map<String, Object> initialContext) {
		this.context = new WorkflowContext(initialContext);
		this.asyncExecutor = null;
		initializeContext();
	}

	private void initializeContext() {
		context.put(ContextKey.WORKFLOW_NAME, workflowName);
		context.put(ContextKey.EXECUTION_ID, UUID.randomUUID().toString());
	}

	// ============================================
	// CONFIGURATION METHODS
	// ============================================

	/** Set workflow name */
	public WorkflowBuilder name(String workflowName) {
		validateNotExecuting();
		this.workflowName = workflowName != null ? workflowName : "Workflow";
		context.put(ContextKey.WORKFLOW_NAME, this.workflowName);
		return this;
	}

	/** Enable/disable logging */
	public WorkflowBuilder log(boolean enabled) {
		validateNotExecuting();
		this.enableLogging = enabled;
		return this;
	}

	/** Skip compensation on failure */
	public WorkflowBuilder skipCompensation(boolean skip) {
		validateNotExecuting();
		this.skipCompensationOnFailure = skip;
		return this;
	}

	/** Add data to context */
	public WorkflowBuilder with(String key, Object value) {
		validateNotExecuting();
		context.put(key, value);
		return this;
	}

	/** Add data to context using ContextKey */
	public WorkflowBuilder with(ContextKey key, Object value) {
		validateNotExecuting();
		context.put(key, value);
		return this;
	}

	/** Set custom executor */
	public WorkflowBuilder withExecutor(ExecutorService executor) {
		validateNotExecuting();
		this.asyncExecutor = executor;
		return this;
	}

	/** Set custom step executor */
	public WorkflowBuilder withStepExecutor(StepExecutor stepExecutor) {
		validateNotExecuting();
		this.stepExecutor = stepExecutor;
		return this;
	}

	/** Set metrics collector */
	public WorkflowBuilder withMetricsCollector(MetricsCollector metricsCollector) {
		validateNotExecuting();
		this.metricsCollector = metricsCollector;
		return this;
	}

	/** Set idempotency checker */
	public WorkflowBuilder withIdempotencyChecker(IdempotencyChecker idempotencyChecker) {
		validateNotExecuting();
		this.idempotencyChecker = idempotencyChecker;
		return this;
	}

	/** Add step interceptor */
	public WorkflowBuilder addInterceptor(StepInterceptor interceptor) {
		validateNotExecuting();
		if (interceptor != null) {
			interceptors.add(interceptor);
		}
		return this;
	}

	/** Add workflow listener */
	public WorkflowBuilder addListener(WorkflowListener listener) {
		validateNotExecuting();
		if (listener != null) {
			listeners.add(listener);
		}
		return this;
	}

	/** Get value from context */
	public <T> T get(String key) {
		return context.get(key);
	}

	/** Get value using ContextKey */
	public <T> T get(ContextKey key) {
		return context.get(key);
	}

	// ============================================
	// STEP DEFINITION METHODS
	// ============================================

	/** Add a workflow step */
	public <T> WorkflowBuilder step(WorkflowStep<T> workflowStep) {
		validateNotExecuting();
		Objects.requireNonNull(workflowStep, "WorkflowStep cannot be null");
		steps.add(workflowStep);
		return this;
	}

	/** Add async workflow step */
	public <T> WorkflowBuilder step(AsyncWorkflowStep<T> asyncStep) {
		validateNotExecuting();
		Objects.requireNonNull(asyncStep, "AsyncWorkflowStep cannot be null");
		steps.add(asyncStep);
		return this;
	}

	/** Quick step definition */
	public <T> WorkflowBuilder step(String name, Supplier<T> action) {
		return step(WorkflowStep.builder(name, action).build());
	}

	/** Step with compensation */
	public <T> WorkflowBuilder step(String name, Supplier<T> action, Consumer<T> compensation) {
		return step(WorkflowStep.builder(name, action).compensation(compensation).build());
	}

	/** Async step */
	public <T> WorkflowBuilder asyncStep(String name, Function<WorkflowContext, CompletableFuture<T>> asyncAction) {
		return step(AsyncWorkflowStep.builder(name, asyncAction).build());
	}

	/** Async step with async compensation */
	public <T> WorkflowBuilder asyncStep(String name, Function<WorkflowContext, CompletableFuture<T>> asyncAction,
			Function<T, CompletableFuture<Void>> asyncCompensation) {
		return step(AsyncWorkflowStep.builder(name, asyncAction).asyncCompensation(asyncCompensation).build());
	}

	/** Runnable step (no return value) */
	public WorkflowBuilder step(String name, Runnable action) {
		return step(name, () -> {
			action.run();
			return null;
		});
	}

	/** Runnable step with compensation */
	public WorkflowBuilder step(String name, Runnable action, Runnable compensation) {
		return step(name, () -> {
			action.run();
			return null;
		}, r -> compensation.run());
	}

	// ============================================
	// CONDITIONAL FLOW METHODS
	// ============================================

	/** Execute block when condition is true */
	public WorkflowBuilder when(Supplier<Boolean> condition, Consumer<WorkflowBuilder> thenBlock) {
		return step("Condition check", () -> condition.get()).processResult(isTrue -> {
			if (Boolean.TRUE.equals(isTrue)) {
				executeConditionalBlock(thenBlock, "ConditionTrue");
			}
			return null;
		});
	}

	/** Execute block when context value equals expected value */
	public WorkflowBuilder when(String conditionKey, Object expectedValue, Consumer<WorkflowBuilder> thenBlock) {
		return when(() -> expectedValue.equals(context.get(conditionKey)), thenBlock);
	}

	/** Process last step result */
	public <T> WorkflowBuilder processResult(Function<T, ?> resultProcessor) {
		return step("Process result", () -> {
			T lastResult = context.get(ContextKey.LAST_RESULT);
			return resultProcessor.apply(lastResult);
		});
	}

	/** Get last step result */
	@SuppressWarnings("unchecked")
	public <T> T getLastResult() {
		return (T) context.get(ContextKey.LAST_RESULT);
	}

	/** Get specific step result */
	@SuppressWarnings("unchecked")
	public <T> T getStepResult(String stepId) {
		if (stepId == null)
			return null;
		return (T) context.getStepResult(stepId);
	}

	// ============================================
	// FLOW CONTROL
	// ============================================

	/** If-then conditional */
	public WorkflowBuilder ifThen(Supplier<Boolean> condition, Consumer<WorkflowBuilder> ifBlock) {
		return step("If-Then condition", () -> condition.get()).processResult(isTrue -> {
			if (Boolean.TRUE.equals(isTrue)) {
				executeConditionalBlock(ifBlock, "IfTrue");
			}
			return null;
		});
	}

	/** If-then-else conditional */
	public WorkflowBuilder ifThenElse(Supplier<Boolean> condition, Consumer<WorkflowBuilder> ifBlock,
			Consumer<WorkflowBuilder> elseBlock) {
		return step("If-Then-Else condition", () -> condition.get()).processResult(isTrue -> {
			if (Boolean.TRUE.equals(isTrue)) {
				executeConditionalBlock(ifBlock, "IfTrue");
			} else if (elseBlock != null) {
				executeConditionalBlock(elseBlock, "Else");
			}
			return null;
		});
	}

	// ============================================
	// LOOPING METHODS
	// ============================================

	/** Repeat block N times */
	public WorkflowBuilder repeat(int times, Consumer<WorkflowBuilder> repeatBlock) {
		return step("Repeat " + times + " times", () -> {
			for (int i = 0; i < times; i++) {
				executeConditionalBlock(repeatBlock, "Repeat-" + i);
			}
			return null;
		});
	}

	/** For-each loop */
	public <T> WorkflowBuilder forEach(Supplier<Collection<T>> collectionSupplier, Consumer<T> itemProcessor) {
		return step("For-Each loop", () -> {
			Collection<T> collection = collectionSupplier.get();
			if (collection != null) {
				int index = 0;
				for (T item : collection) {
					context.put(ContextKey.CURRENT_ITEM, item);
					context.put(ContextKey.ITEM_INDEX, index++);
					executeItemProcessing(item, itemProcessor);
				}
			}
			return null;
		});
	}

	/** Async for-each loop */
	public <T> WorkflowBuilder forEachAsync(Supplier<Collection<T>> collectionSupplier,
			Function<T, CompletableFuture<Void>> itemProcessor) {
		return asyncStep("For-Each Async loop", ctx -> {
			Collection<T> collection = collectionSupplier.get();
			if (collection == null || collection.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			List<CompletableFuture<Void>> futures = collection.stream().map(item -> {
				ctx.put(ContextKey.CURRENT_ITEM, item);
				return itemProcessor.apply(item).exceptionally(e -> {
					log.error("Error processing item: {}", e.getMessage());
					return null;
				});
			}).collect(Collectors.toList());

			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> null);
		});
	}

	// ============================================
	// ERROR HANDLING METHODS
	// ============================================

	/** Execute fallback if main action fails */
	public WorkflowBuilder withFallback(Supplier<?> mainAction, Supplier<?> fallbackAction) {
		return step("With fallback", () -> {
			try {
				return mainAction.get();
			} catch (Exception e) {
				log.warn("Main action failed, using fallback: {}", e.getMessage());
				return fallbackAction.get();
			}
		});
	}

	/** Async fallback */
	public <T> WorkflowBuilder withFallbackAsync(Function<WorkflowContext, CompletableFuture<T>> mainAction,
			Function<WorkflowContext, CompletableFuture<T>> fallbackAction) {
		return asyncStep("With async fallback", ctx -> {
			return mainAction.apply(ctx).exceptionallyCompose(e -> {
				log.warn("Main async action failed, using fallback: {}", e.getMessage());
				return fallbackAction.apply(ctx);
			});
		});
	}

	/** Step with timeout */
	public WorkflowBuilder withTimeout(String name, Supplier<?> action, Duration timeout) {
		return step("Timeout: " + name, () -> {
			CompletableFuture<?> future = CompletableFuture.supplyAsync(action, getAsyncExecutor());
			try {
				return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				future.cancel(true);
				throw new WorkflowTimeoutException("Step timed out after " + timeout, name);
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		});
	}

	// ============================================
	// UTILITY METHODS
	// ============================================

	/** Add log step */
	public WorkflowBuilder log(String message, Object... args) {
		return step("Log: " + message, () -> {
			String formatted = String.format(message, args);
			context.trace(formatted);

			if (enableLogging) {
				log.info("[Workflow:{}] {}", workflowName, formatted);
			}
			return formatted;
		});
	}

	/** Execute branches in parallel */
	@SafeVarargs
	public final WorkflowBuilder parallel(Consumer<WorkflowBuilder>... branches) {
		return asyncStep("Parallel execution", ctx -> {
			List<CompletableFuture<Object>> futures = Arrays.stream(branches).map(branch -> {
				WorkflowBuilder subBuilder = createSubBuilder("ParallelBranch");
				ctx.snapshot().forEach(subBuilder.context::put);
				branch.accept(subBuilder);
				return subBuilder.executeAsync();
			}).collect(Collectors.toList());

			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> null);
		});
	}

	// ============================================
	// EXECUTION METHODS
	// ============================================

	/** Execute workflow asynchronously */
	public <T> CompletableFuture<T> executeAsync() {
		validateNotExecuting();
		isExecuting = true;

		currentExecutionFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return execute();
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		}, getAsyncExecutor());

		@SuppressWarnings("unchecked")
		CompletableFuture<T> typedFuture = (CompletableFuture<T>) currentExecutionFuture;
		return typedFuture.whenComplete((result, error) -> {
			isExecuting = false;
		});
	}

	/** Execute workflow synchronously */
	@SuppressWarnings("unchecked")
	public <T> T execute() {
		validateNotExecuting();
		isExecuting = true;

		try {
			notifyWorkflowStart();
			logStart();

			while (!steps.isEmpty()) {
				Object currentStep = steps.poll();
				if (currentStep == null)
					continue;

				if (currentStep instanceof WorkflowStep) {
					executeSyncStep((WorkflowStep<?>) currentStep);
				} else if (currentStep instanceof AsyncWorkflowStep) {
					executeAsyncStep((AsyncWorkflowStep<?>) currentStep);
				}
			}

			Object finalResult = context.get(ContextKey.FINAL_RESULT);
			logSuccess();
			notifyWorkflowComplete(finalResult);

			return (T) finalResult;

		} catch (Exception e) {
			logFailure(e);
			notifyWorkflowError(e);

			if (!skipCompensationOnFailure) {
				performCompensation();
			}

			throwEnhancedException(e);

		} finally {
			isExecuting = false;
		}

		return null;
	}

	// ============================================
	// PRIVATE EXECUTION ENGINE
	// ============================================

	private <T> void executeSyncStep(WorkflowStep<T> step) throws Exception {
		// Check idempotency
		if (step.isIdempotent && idempotencyChecker != null) {
			if (idempotencyChecker.isStepExecuted(context.getContextId(), step.id)) {
				log.info("Skipping idempotent step '{}' as already executed", step.name);
				return;
			}
		}

		int stepNumber = stepCounter.incrementAndGet();
		logStepStart(stepNumber, step.name);
		notifyBeforeStep(step);

		try {
			// Execute with retry
			T result = executeSyncStepWithRetry(step, stepNumber);

			// Store results
			if (result != null) {
				context.put(ContextKey.LAST_RESULT, result);
				context.recordStepResult(step.id, result);
				context.put(ContextKey.FINAL_RESULT, result);

				// Store with sanitized name
				if (step.name != null) {
					String safeName = step.name.replaceAll("\\s+", "_").toLowerCase();
					context.put("step_result_" + safeName, result);
				}
			}

			// Mark as executed for idempotency
			if (step.isIdempotent && idempotencyChecker != null) {
				idempotencyChecker.markStepExecuted(context.getContextId(), step.id);
			}

			executedSteps.add(step);
			logStepSuccess(stepNumber, step.name, result);
			notifyAfterStep(step, result);

		} catch (Exception e) {
			logStepFailure(stepNumber, step.name, e);
			context.recordStepError(step.id, e);
			notifyStepError(step, e);

			if (step.isCritical) {
				throw new CriticalStepException(step.name, e);
			}

			throw e;
		}
	}

	private <T> T executeSyncStepWithRetry(WorkflowStep<T> step, int stepNumber) throws Exception {
		Exception lastError = null;

		for (int attempt = 1; attempt <= step.maxRetries + 1; attempt++) {
			try {
				Instant start = Instant.now();
				T result = step.action.get();

				if (metricsCollector != null) {
					Duration duration = Duration.between(start, Instant.now());
					metricsCollector.recordStepExecution(step.name, duration, true);
				}

				return result;

			} catch (Exception e) {
				lastError = e;

				if (metricsCollector != null) {
					metricsCollector.recordRetry(step.name, attempt);
				}

				if (attempt > step.maxRetries) {
					throw e;
				}

				logRetry(step.name, attempt, step.maxRetries, e);

				if (step.retryDelay != null && !step.retryDelay.isZero()) {
					try {
						Thread.sleep(step.retryDelay.toMillis());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new WorkflowInterruptedException("Retry interrupted", ie);
					}
				}
			}
		}

		throw lastError != null ? lastError : new RuntimeException("Execution failed");
	}

	private <T> CompletableFuture<Void> executeAsyncStep(AsyncWorkflowStep<T> step) {
		int stepNumber = stepCounter.incrementAndGet();
		logStepStart(stepNumber, step.name);

		CompletableFuture<T> future;

		try {
			future = step.asyncAction.apply(context);

			if (step.timeout != null && !step.timeout.isZero()) {
				future = future.orTimeout(step.timeout.toMillis(), TimeUnit.MILLISECONDS);
			}

		} catch (Exception e) {
			logStepFailure(stepNumber, step.name, e);
			context.recordStepError(step.id, e);

			if (step.isCritical) {
				return CompletableFuture.failedFuture(new CriticalStepException(step.name, e));
			}

			return CompletableFuture.failedFuture(e);
		}

		return future.thenAccept(result -> {
			if (result != null) {
				context.put(ContextKey.LAST_RESULT, result);
				context.recordStepResult(step.id, result);
				context.put(ContextKey.FINAL_RESULT, result);
			}

			executedSteps.add(step);
			logStepSuccess(stepNumber, step.name, result);
		}).exceptionally(ex -> {
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

			logStepFailure(stepNumber, step.name, (Exception) cause);
			context.recordStepError(step.id, cause);

			if (step.isCritical) {
				throw new CompletionException(new CriticalStepException(step.name, cause));
			}

			throw new CompletionException(cause);
		});
	}

	// ============================================
	// PRIVATE HELPER METHODS
	// ============================================

	private void executeConditionalBlock(Consumer<WorkflowBuilder> block, String blockName) {
		if (block == null)
			return;

		WorkflowBuilder subBuilder = createSubBuilder(blockName);
		block.accept(subBuilder);

		// Flatten steps dari sub-workflow ke workflow utama
		subBuilder.steps.forEach(steps::add);
	}

	private WorkflowBuilder createSubBuilder(String blockName) {
		WorkflowBuilder subBuilder = new WorkflowBuilder(getAsyncExecutor()).name(workflowName + "-" + blockName)
				.log(enableLogging).skipCompensation(skipCompensationOnFailure);

		if (stepExecutor != null) {
			subBuilder.withStepExecutor(stepExecutor);
		}
		if (metricsCollector != null) {
			subBuilder.withMetricsCollector(metricsCollector);
		}

		context.snapshot().forEach(subBuilder.context::put);
		return subBuilder;
	}

	private <T> void executeItemProcessing(T item, Consumer<T> processor) {
		if (processor == null)
			return;

		executeConditionalBlock(wf -> {
			wf.context.put(ContextKey.CURRENT_ITEM, item);
			wf.step("Process item", () -> {
				processor.accept(item);
				return null;
			});
		}, "ItemProcessor");
	}

	private void validateNotExecuting() {
		if (isExecuting) {
			throw new IllegalStateException("Cannot modify workflow while it is executing");
		}
	}

	private ExecutorService getAsyncExecutor() {
		return asyncExecutor != null ? asyncExecutor : ForkJoinPool.commonPool();
	}

	@SuppressWarnings("unused")
	private StepExecutor getStepExecutor() {
		if (stepExecutor != null) {
			return stepExecutor;
		}
		return new DefaultStepExecutor(getAsyncExecutor(), metricsCollector);
	}

	// ============================================
	// COMPENSATION ENGINE (Enhanced)
	// ============================================

	@SuppressWarnings("unchecked")
	private void performCompensation() {
		if (executedSteps.isEmpty() || skipCompensationOnFailure) {
			return;
		}

		logCompensationStart();
		notifyCompensationStart();

		int successful = 0;
		int failed = 0;

		List<CompletableFuture<Void>> compensationFutures = new ArrayList<>();

		for (int i = executedSteps.size() - 1; i >= 0; i--) {
			Object step = executedSteps.get(i);

			if (step instanceof WorkflowStep) {
				@SuppressWarnings("rawtypes")
				WorkflowStep workflowStep = (WorkflowStep) step;

				if (workflowStep.compensation == null) {
					continue;
				}

				try {
					Object result = context.getStepResult(workflowStep.id);
					if (result != null) {
						// Execute compensation with retry
						executeCompensationWithRetry(workflowStep, result);
					}
					successful++;
					logCompensationStepSuccess(workflowStep.name);
				} catch (Exception e) {
					failed++;
					compensationErrors.add(e);
					logCompensationStepFailure(workflowStep.name, e);
				}
			}
		}

		// Wait for all async compensations
		if (!compensationFutures.isEmpty()) {
			try {
				CompletableFuture.allOf(compensationFutures.toArray(new CompletableFuture[0]))
						.get(Config.DEFAULT_COMPENSATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.error("Async compensation failed: {}", e.getMessage());
			}
		}

		logCompensationComplete(successful, failed);
		notifyCompensationComplete(successful, failed);
	}

	private <T> void executeCompensationWithRetry(WorkflowStep<T> step, Object result) throws Exception {
		for (int attempt = 1; attempt <= Config.DEFAULT_MAX_COMPENSATION_RETRIES + 1; attempt++) {
			try {
				@SuppressWarnings("unchecked")
				T typedResult = (T) result;
				step.compensation.accept(typedResult);
				return;
			} catch (Exception e) {
				if (attempt > Config.DEFAULT_MAX_COMPENSATION_RETRIES) {
					log.error("Compensation failed after {} attempts for step '{}'",
							Config.DEFAULT_MAX_COMPENSATION_RETRIES, step.name);
					throw new CompensationException("Compensation failed for step: " + step.name, e);
				}

				log.warn("Compensation attempt {}/{} failed for step '{}': {}", attempt,
						Config.DEFAULT_MAX_COMPENSATION_RETRIES, step.name, e.getMessage());

				try {
					Thread.sleep(Config.DEFAULT_RETRY_DELAY_MS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new WorkflowInterruptedException("Compensation interrupted", ie);
				}
			}
		}
	}

	// ============================================
	// NOTIFICATION METHODS
	// ============================================

	private void notifyWorkflowStart() {
		listeners.forEach(listener -> {
			try {
				listener.onWorkflowStart(workflowName, context);
			} catch (Exception e) {
				log.error("Error in workflow listener onWorkflowStart: {}", e.getMessage());
			}
		});
	}

	private void notifyWorkflowComplete(Object result) {
		listeners.forEach(listener -> {
			try {
				listener.onWorkflowComplete(workflowName, context, result);
			} catch (Exception e) {
				log.error("Error in workflow listener onWorkflowComplete: {}", e.getMessage());
			}
		});
	}

	private void notifyWorkflowError(Exception error) {
		listeners.forEach(listener -> {
			try {
				listener.onWorkflowError(workflowName, context, error);
			} catch (Exception e) {
				log.error("Error in workflow listener onWorkflowError: {}", e.getMessage());
			}
		});
	}

	private void notifyCompensationStart() {
		listeners.forEach(listener -> {
			try {
				listener.onCompensationStart(workflowName, context);
			} catch (Exception e) {
				log.error("Error in workflow listener onCompensationStart: {}", e.getMessage());
			}
		});
	}

	private void notifyCompensationComplete(int succeeded, int failed) {
		listeners.forEach(listener -> {
			try {
				listener.onCompensationComplete(workflowName, context, succeeded, failed);
			} catch (Exception e) {
				log.error("Error in workflow listener onCompensationComplete: {}", e.getMessage());
			}
		});
	}

	private <T> void notifyBeforeStep(WorkflowStep<T> step) {
		interceptors.forEach(interceptor -> {
			try {
				interceptor.beforeStep(step, context);
			} catch (Exception e) {
				log.error("Error in step interceptor beforeStep: {}", e.getMessage());
			}
		});
	}

	private <T> void notifyAfterStep(WorkflowStep<T> step, T result) {
		interceptors.forEach(interceptor -> {
			try {
				interceptor.afterStep(step, context, result);
			} catch (Exception e) {
				log.error("Error in step interceptor afterStep: {}", e.getMessage());
			}
		});
	}

	private <T> void notifyStepError(WorkflowStep<T> step, Exception error) {
		interceptors.forEach(interceptor -> {
			try {
				interceptor.onStepError(step, context, error);
			} catch (Exception e) {
				log.error("Error in step interceptor onStepError: {}", e.getMessage());
			}
		});
	}

	// ============================================
	// ERROR HANDLING
	// ============================================

	private void throwEnhancedException(Exception originalError) {
		if (!compensationErrors.isEmpty()) {
			throw new WorkflowException(workflowName, stepCounter.get(), originalError, compensationErrors);
		}

		if (originalError instanceof WorkflowException) {
			throw (WorkflowException) originalError;
		}

		throw new WorkflowException(workflowName, stepCounter.get(), originalError);
	}

	// ============================================
	// LOGGING METHODS (Enhanced)
	// ============================================

	private void logStart() {
		if (enableLogging) {
			log.info("🚀 Starting workflow: {} [ID: {}]", workflowName, context.getContextId());
			context.trace("Workflow started: " + workflowName);
		}
	}

	private void logSuccess() {
		if (enableLogging) {
			log.info("✅ Workflow '{}' completed successfully ({} steps)", workflowName, stepCounter.get());
			context.trace("Workflow completed successfully");
		}
	}

	private void logFailure(Exception e) {
		if (enableLogging) {
			log.error("❌ Workflow '{}' failed at step {}", workflowName, stepCounter.get(), e);
			context.trace("Workflow failed: " + e.getMessage());
		}
	}

	private void logStepStart(int stepNumber, String stepName) {
		if (enableLogging) {
			log.debug("   Step {}: {} - Starting", stepNumber, stepName);
			context.trace("Step " + stepNumber + " started: " + stepName);
		}
	}

	private void logStepSuccess(int stepNumber, String stepName, Object result) {
		if (enableLogging) {
			String resultStr = result != null
					? (result.toString().length() > 100 ? result.toString().substring(0, 100) + "..."
							: result.toString())
					: "null";

			log.debug("   Step {}: {} - Completed [Result: {}]", stepNumber, stepName, resultStr);
			context.trace("Step " + stepNumber + " completed: " + stepName);
		}
	}

	private void logStepFailure(int stepNumber, String stepName, Exception e) {
		if (enableLogging) {
			log.error("   Step {}: {} - Failed: {}", stepNumber, stepName, e.getMessage());
			context.trace("Step " + stepNumber + " failed: " + stepName + ": " + e.getMessage());
		}
	}

	private void logRetry(String stepName, int attempt, int maxRetries, Exception e) {
		if (enableLogging) {
			log.warn("   Step {}: Retry attempt {}/{} - {}", stepName, attempt, maxRetries, e.getMessage());
		}
	}

	private void logCompensationStart() {
		if (enableLogging) {
			log.info("🔄 Starting compensation for {} steps", executedSteps.size());
			context.trace("Compensation started for " + executedSteps.size() + " steps");
		}
	}

	private void logCompensationStepSuccess(String stepName) {
		if (enableLogging) {
			log.debug("   Compensation step: {} - Success", stepName);
		}
	}

	private void logCompensationStepFailure(String stepName, Exception e) {
		if (enableLogging) {
			log.error("   Compensation step: {} - Failed: {}", stepName, e.getMessage());
			context.trace("Compensation failed for step: " + stepName);
		}
	}

	private void logCompensationComplete(int successful, int failed) {
		if (enableLogging) {
			log.warn("🔄 Compensation completed: {} succeeded, {} failed", successful, failed);
			context.trace("Compensation completed: " + successful + " succeeded, " + failed + " failed");
		}
	}

	// ============================================
	// DIAGNOSTICS & UTILITIES
	// ============================================

	/** Get execution trace */
	public List<String> getExecutionTrace() {
		return context.getTrace();
	}

	/** Get context snapshot */
	public Map<String, Object> getContextSnapshot() {
		return context.snapshot();
	}

	/** Get executed step count */
	public int getExecutedStepCount() {
		return stepCounter.get();
	}

	/** Get executed step names */
	public List<String> getExecutedStepNames() {
		return executedSteps.stream().filter(step -> {
			if (step instanceof WorkflowStep) {
				return ((WorkflowStep<?>) step).name != null;
			} else if (step instanceof AsyncWorkflowStep) {
				return ((AsyncWorkflowStep<?>) step).name != null;
			}
			return false;
		}).map(step -> {
			if (step instanceof WorkflowStep) {
				return ((WorkflowStep<?>) step).name;
			} else {
				return ((AsyncWorkflowStep<?>) step).name;
			}
		}).collect(Collectors.toList());
	}

	/** Reset workflow to initial state */
	public void reset() {
		validateNotExecuting();
		context.clear();
		steps.clear();
		executedSteps.clear();
		compensationErrors.clear();
		interceptors.clear();
		stepCounter.set(0);
		currentExecutionFuture = null;
	}

	/** Check if workflow is executing */
	public boolean isExecuting() {
		return isExecuting;
	}

	/** Get workflow ID */
	public String getWorkflowId() {
		return context.getContextId();
	}

	/** Get workflow context */
	public WorkflowContext getWorkflowContext() {
		return context;
	}

	/** Cancel executing workflow */
	public void cancel() {
		if (currentExecutionFuture != null && !currentExecutionFuture.isDone()) {
			currentExecutionFuture.cancel(true);
		}
		isExecuting = false;
	}

	// ============================================
	// CUSTOM EXCEPTIONS (Enhanced)
	// ============================================

	/** Base workflow exception */
	public static class WorkflowException extends RuntimeException {
		private final String workflowName;
		private final int stepNumber;
		private final List<Exception> compensationErrors;
		private final Instant timestamp;

		public WorkflowException(String workflowName, int stepNumber, Throwable cause) {
			this(workflowName, stepNumber, cause, Collections.emptyList());
		}

		public WorkflowException(String workflowName, int stepNumber, Throwable cause,
				List<Exception> compensationErrors) {
			super(buildMessage(workflowName, stepNumber, cause, compensationErrors), cause);
			this.workflowName = workflowName;
			this.stepNumber = stepNumber;
			this.compensationErrors = compensationErrors != null ? new ArrayList<>(compensationErrors)
					: Collections.emptyList();
			this.timestamp = Instant.now();
		}

		private static String buildMessage(String workflowName, int stepNumber, Throwable cause,
				List<Exception> compensationErrors) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Workflow '%s' failed at step %d", workflowName, stepNumber));

			if (cause != null && cause.getMessage() != null) {
				sb.append(": ").append(cause.getMessage());
			}

			if (compensationErrors != null && !compensationErrors.isEmpty()) {
				sb.append(" [Compensation errors: ").append(compensationErrors.size()).append("]");
			}

			return sb.toString();
		}

		public String getWorkflowName() {
			return workflowName;
		}

		public int getStepNumber() {
			return stepNumber;
		}

		public List<Exception> getCompensationErrors() {
			return new ArrayList<>(compensationErrors);
		}

		public Instant getTimestamp() {
			return timestamp;
		}
	}

	/** Workflow interrupted exception */
	public static class WorkflowInterruptedException extends WorkflowException {
		public WorkflowInterruptedException(String message, Throwable cause) {
			super("Interrupted", 0, new InterruptedException(message), Collections.emptyList());
		}
	}

	/** Critical step failed exception */
	public static class CriticalStepException extends WorkflowException {
		public CriticalStepException(String stepName, Throwable cause) {
			super("Critical", 0, new RuntimeException("Critical step failed: " + stepName, cause),
					Collections.emptyList());
		}
	}

	/** Compensation failed exception */
	public static class CompensationException extends WorkflowException {
		public CompensationException(String message, Throwable cause) {
			super("Compensation", 0, new RuntimeException(message, cause), Collections.emptyList());
		}
	}

	/** Step timeout exception */
	public static class WorkflowTimeoutException extends WorkflowException {
		private final String stepName;

		public WorkflowTimeoutException(String message, String stepName) {
			super("Timeout", 0, new TimeoutException(message), Collections.emptyList());
			this.stepName = stepName;
		}

		public String getStepName() {
			return stepName;
		}
	}
}