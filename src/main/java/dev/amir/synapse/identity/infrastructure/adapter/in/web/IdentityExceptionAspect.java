package dev.amir.synapse.identity.infrastructure.adapter.in.web;

import dev.amir.synapse.identity.domain.exception.IdentityInternalException;
import dev.amir.synapse.identity.domain.exception.InvalidIdentityRequestException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public final class IdentityExceptionAspect {
  @Pointcut(
      "within(dev.amir.synapse.identity.application..*)"
          + " || within(dev.amir.synapse.identity.domain..*)"
          + " || within(dev.amir.synapse.identity.infrastructure.adapter.out..*)"
          + " || within(dev.amir.synapse.identity.infrastructure.adapter.in.persistence..*)"
          + " || within(dev.amir.synapse.identity.infrastructure.adapter.in.web.api..*)")
  void identityBoundary() {}

  @Around("identityBoundary()")
  public Object translateIdentityExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed();
    } catch (IllegalArgumentException ex) {
      throw new InvalidIdentityRequestException(
          "Invalid identity request at " + joinPoint.getSignature().toShortString(), ex);
    } catch (IllegalStateException ex) {
      throw new IdentityInternalException(
          "Identity operation failed at " + joinPoint.getSignature().toShortString(), ex);
    }
  }
}
