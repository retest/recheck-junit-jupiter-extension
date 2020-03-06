package de.retest.recheck.junit.jupiter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
			context.getTestInstances() //
					.map( TestInstances::getAllInstances ) //
					.orElse( Collections.emptyList() ) //
					.forEach( action );
		} else {
			context.getTestInstance().ifPresent( action );
		}
	}

	private void execute( final Consumer<RecheckLifecycle> consumer, final Object testInstance,
			final Class<?> testClass ) {
		final Predicate<Field> isRecheck = field -> isRecheck( field, testInstance );
		ReflectionUtils.findFields( testClass, isRecheck, TRAVERSAL_MODE ).stream() //
				.map( field -> getRecheckLifecycle( field, testInstance ) ) //
				.forEach( consumer );
	}

	private RecheckLifecycle getRecheckLifecycle( final Field field, final Object testInstance ) {
		final boolean accessibility = unlock( field );
		try {
			return (RecheckLifecycle) field.get( testInstance );
		} catch ( IllegalArgumentException | IllegalAccessException e ) {
			throw new IllegalStateException( e );
		} finally {
			lock( field, accessibility );
		}
	}

	private boolean isRecheck( final Field field, final Object testInstance ) {
		final boolean accessibility = unlock( field );
		try {
			return RecheckLifecycle.class.isInstance( field.get( testInstance ) );
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
