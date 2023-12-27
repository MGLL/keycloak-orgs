package io.phasetwo.service.model;

import com.google.auto.service.AutoService;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

@AutoService(Spi.class)
public class OrganizationProfileSpi implements Spi {

  @Override
  public boolean isInternal() {
    return false;
  }

  @Override
  public String getName() {
    return "organizationProfile";
  }

  @Override
  public Class<? extends Provider> getProviderClass() {
    return OrganizationProfileProvider.class;
  }

  @Override
  public Class<? extends ProviderFactory> getProviderFactoryClass() {
    return OrganizationProfileProviderFactory.class;
  }
}
