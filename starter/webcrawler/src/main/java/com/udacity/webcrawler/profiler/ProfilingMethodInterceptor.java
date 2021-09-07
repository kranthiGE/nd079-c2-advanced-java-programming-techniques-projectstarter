package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object targetObject;
  private final ProfilingState state;

  // You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, Object targetObject, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.targetObject = Objects.requireNonNull(targetObject);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) 
    throws Throwable {
    // This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    // record the start time
    final Instant startTime = clock.instant();
    // invoke the method
    Object returnObject = null;
    try {
      returnObject = method.invoke(targetObject, args);
      // invoke toString() and equals() and hashCode() directly on the wrapped object and no need to record the timing
      if(method.getAnnotation(Profiled.class) == null || method.getDeclaringClass().equals(Object.class)){
        return returnObject;
      }
    } catch (IllegalAccessException e){
      throw e;
    } catch(InvocationTargetException e) {
      throw e.getTargetException();
    }
    finally{
      // record the calculated elapsed time to run the target Object's method
      state.record(targetObject.getClass(), method, Duration.between(startTime, clock.instant()));
    }
    return returnObject;
  }
}
