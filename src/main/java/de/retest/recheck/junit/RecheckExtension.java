package de.retest.recheck.junit;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

import de.retest.recheck.RecheckLifecycle;

/**
 * This extension adds callback to automatically execute {@link RecheckLifecycle#startTest()} before each test
 * invocation, {@link RecheckLifecycle#capTest()} after each test invocation and {@link RecheckLifecycle#cap()} after
 * all tests.
 */
public class RecheckExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

	private static final HierarchyTraversalMode TRAVERSAL_MODE = HierarchyTraversalMode.TOP_DOWN;

	@Override
	public void beforeTestExecution( final ExtensionContext context ) throws Exception {
		final Consumer<RecheckLifecycle> startTest = r -> r.startTest( context.getDisplayName() );
		execute( startTest, context );
	}

	@Override
	public void afterTestExecution( final ExtensionContext context ) throws Exception {
		execute( RecheckLifecycle::capTest, context );
		execute( RecheckLifecycle::cap, context );
	}

	@Override
	public void afterAll( final ExtensionContext context ) throws Exception {
		execute( RecheckLifecycle::cap, context );
	}

	private void execute( final Consumer<RecheckLifecycle> consumer, final ExtensionContext context ) {
		final Predicate<Field> isRecheck = f -> isRecheck( f, context.getRequiredTestInstance() );
		final List<Field> fields =
				ReflectionUtils.findFields( context.getRequiredTestClass(), isRecheck, TRAVERSAL_MODE );
		fields.stream().flatMap( f -> streamRecheckField( context, f ) ).forEach( consumer );
	}

	/**
	 * Extracts the instance of a {@link RecheckLifecycle} field from the test instance
	 *
	 * @return a {@link Stream} containing the instance or an empty {@link Stream} otherwise
	 */
	private Stream<RecheckLifecycle> streamRecheckField( final ExtensionContext context, final Field field ) {
		final Object testInstance = context.getRequiredTestInstance();
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
