package de.retest.recheck.junit.jupiter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
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
		final Consumer<RecheckLifecycle> startTest =
				lifecycle -> lifecycle.startTest( toTestName( context.getDisplayName() ) );
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

	@Override
	public void afterAll( final ExtensionContext context ) throws Exception {
		executeAll( RecheckLifecycle::cap, context );
	}

	private void executeAll( final Consumer<RecheckLifecycle> lifecycleMethod, final ExtensionContext context ) {
		final Class<?> testClass = context.getRequiredTestClass();
		final Consumer<Object> execute = testInstance -> execute( lifecycleMethod, testInstance, testClass );
		if ( ReflectionUtilities.hasMethod( context.getClass(), "getTestInstances" ) ) {
			context.getTestInstances() //
					.map( TestInstances::getAllInstances ) //
					.orElse( Collections.emptyList() ) //
					.forEach( execute );
		} else {
			context.getTestInstance().ifPresent( execute );
		}
	}

	private void execute( final Consumer<RecheckLifecycle> lifecycleMethod, final ExtensionContext context ) {
		execute( lifecycleMethod, context.getRequiredTestInstance(), context.getRequiredTestClass() );
	}

	private void execute( final Consumer<RecheckLifecycle> lifecycleMethod, final Object testInstance,
			final Class<?> testClass ) {
		final Predicate<Field> isRecheck = field -> isRecheckLifecycle( field, testInstance );
		ReflectionUtils.findFields( testClass, isRecheck, TRAVERSAL_MODE ).stream() //
				.map( field -> getRecheckLifecycle( field, testInstance ) ) //
				.forEach( lifecycleMethod );
	}

	private boolean isRecheckLifecycle( final Field field, final Object testInstance ) {
		return mapFieldValue( field, testInstance, value -> value instanceof RecheckLifecycle );
	}

	private RecheckLifecycle getRecheckLifecycle( final Field field, final Object testInstance ) {
		return mapFieldValue( field, testInstance, value -> (RecheckLifecycle) value );
	}

	private <T> T mapFieldValue( final Field field, final Object testInstance, final Function<Object, T> mapper ) {
		final boolean accessibility = unlock( field );
		try {
			final Object value = field.get( testInstance );
			return mapper.apply( value );
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
