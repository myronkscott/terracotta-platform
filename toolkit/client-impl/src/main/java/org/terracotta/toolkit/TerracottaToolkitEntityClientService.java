package org.terracotta.toolkit;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;

/**
 */
public class TerracottaToolkitEntityClientService implements EntityClientService<Toolkit, ToolkitConfig, EntityMessage, EntityResponse> {

  @Override
  public boolean handlesEntityType(Class<Toolkit> type) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public byte[] serializeConfiguration(ToolkitConfig c) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ToolkitConfig deserializeConfiguration(byte[] bytes) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Toolkit create(EntityClientEndpoint<EntityMessage, EntityResponse> ece) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
