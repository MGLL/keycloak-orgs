package io.phasetwo.service.model.profile;

/**
 * <p>This interface represents the different contexts from where organization profile are managed.
 *
 * <p>The context is crucial to drive the conditions that should be respected when managing
 * organization profiles.
 */
public enum OrganizationProfileContext {

  /**
   * In this context, organization profile is managed by organization manager.
   */
  UPDATE_PROFILE(false),

  /**
   * In this context, organization profile is managed by platform manager or admin.
   */
  UPDATE_METADATA(true);

  private final boolean adminContext;

  OrganizationProfileContext(boolean adminContext) {
    this.adminContext = adminContext;
  }

  public boolean isAdminContext() {
    return adminContext;
  }
}
