package de.retest.recheck.junit.jupiter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

import de.retest.recheck.RecheckLifecycle;
import de.retest.recheck.util.ReflectionUtilities;

/**
 * This extension adds callback to automatically execute {@link RecheckLifecycle#startTest()} before each test
 * invocation, {@link RecheckLifecycle#capTest()} after each test invocation and {@link RecheckLifecycle#cap()} after
 * all tests.
 */
public class RecheckExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

	private static final HierarchyTraversalMode TRAVERSAL_MODE = HierarchyTraversalMode.TOP_DOWN;

	@Override
	public void beforeTestExecution( final ExtensionContext context ) throws Exception {
		final Consumer<RecheckLifecycle> startTest = r -> r.startTest( toTestName( context.getDisplayName() ) );
		execute( startTest, context );
	}

	private String toTestName( final String displayName ) {
		if ( displayName.endsWith( "()" ) ) {
			return displayName.substring( 0, displayName.length() - 2 );
		}
		return displayName;
	}

	@Override
	public void afterTestExecution( final ExtensionContext context ) throws Exception {
		try {
			execute( RecheckLifecycle::capTest, context );
		} finally {
			execute( RecheckLifecycle::cap, context );
		}

	}

	private void execute( final Consumer<RecheckLifecycle> consumer, final ExtensionContext context ) {
		execute( consumer, context.getRequiredTestInstance(), context.getRequiredTestClass() );
	}

	@Override
	public void afterAll( final ExtensionContext context ) throws Exception {
		executeAll( RecheckLifecycle::cap, context );
	}

	private void executeAll( final Consumer<RecheckLifecycle> consumer, final ExtensionContext context ) {
		final Class<?> testClass = context.getRequiredTestClass();
		final Consumer<Object> action = testInstance -> execute( consumer, testInstance, testClass );
		if ( ReflectionUtilities.hasMethod( context.getClass(), "getTestInstances" ) ) {
			context.getTestInstances().map( TestInstances::getAllInstances ).orElse( Collections.emptyList() )
					.forEach( action );
		} else {
			context.getTestInstance().ifPresent( action::accept );
		}
	}

	private void execute( final Consumer<RecheckLifecycle> consumer, final Object testInstance,
			final Class<?> testClass ) {
		final Predicate<Field> isRecheck = f -> isRecheck( f, testInstance );
		final List<Field> fields = ReflectionUtils.findFields( testClass, isRecheck, TRAVERSAL_MODE );
		fields.stream().flatMap( f -> streamRecheckField( f, testInstance ) ).forEach( consumer );
	}

	/**
	 * Extracts the instance of a {@link RecheckLifecycle} field from the test instance
	 *
	 * @return a {@link Stream} containing the instance or an empty {@link Stream} otherwise
	 */
	private Stream<RecheckLifecycle> streamRecheckField( final Field field, final Object testInstance ) {
		final boolean accessibility = unlock( field );
		try {
			return streamRecheckInstance( field, testInstance );
		} catch ( IllegalArgumentException | IllegalAccessException e ) {
			throw new IllegalStateException( e );
		} finally {
			lock( field, accessibility );
		}
	}

	/**
	 * @return a {@link Stream} containing an instance of {@link RecheckLifecycle} or an empty {@link Stream} otherwise
	 */
	private Stream<RecheckLifecycle> streamRecheckInstance( final Field field, final Object testInstance )
			throws IllegalArgumentException, IllegalAccessException {
		if ( isRecheck( field, testInstance ) ) {
			return Stream.of( (RecheckLifecycle) field.get( testInstance ) );
		}
		return Stream.empty();
	}

	private boolean isRecheck( final Field field, final Object ofTestInstance ) {
		final boolean accessibility = unlock( field );
		try {
			return RecheckLifecycle.class.isInstance( field.get( ofTestInstance ) );
		} catch ( IllegalArgumentException | IllegalAccessException e ) {
			throw new IllegalStateException( e );
		} finally {
			lock( field, accessibility );
		}
	}

	private void lock( final Field field, final boolean accessibility ) {
		field.setAccessible( accessibility );
	}

	private boolean unlock( final Field field ) {
		final boolean accessibility = field.isAccessible();
		field.setAccessible( true );
		return accessibility;
	}

}
