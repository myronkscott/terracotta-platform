/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.NomadClientProcessTest;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.server.NomadException;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.UUID;

import static com.terracottatech.nomad.client.Consistency.CONSISTENT;
import static com.terracottatech.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static com.terracottatech.nomad.client.Consistency.UNKNOWN_BUT_NO_CHANGE;
import static com.terracottatech.nomad.client.Consistency.UNRECOVERABLY_INCONSISTENT;
import static com.terracottatech.nomad.client.NomadTestHelper.discovery;
import static com.terracottatech.nomad.client.NomadTestHelper.matchSetOf;
import static com.terracottatech.nomad.messages.AcceptRejectResponse.accept;
import static com.terracottatech.nomad.messages.AcceptRejectResponse.reject;
import static com.terracottatech.nomad.messages.RejectionReason.DEAD;
import static com.terracottatech.nomad.messages.RejectionReason.UNACCEPTABLE;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ChangeProcessTest extends NomadClientProcessTest {
  @Mock
  private ChangeResultReceiver results;

  @After
  public void after() {
    verifyNoMoreInteractions(results);
  }

  @Test
  public void makeChange() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepared("server2");
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).committed("server1");
    verify(results).committed("server2");
    verify(results).endCommit();
    verify(results).done(CONSISTENT);
  }

  @Test
  public void discoverFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server2.discover()).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discoverFail("server2");
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  public void discoverAlreadyPrepared() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server2.discover()).thenReturn(discovery(PREPARED));

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).discoverAlreadyPrepared(eq("server2"), any(UUID.class), eq("testCreationHost"), eq("testCreationUser"));
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  public void discoverInconsistentCluster() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server2.discover()).thenReturn(discovery(ROLLED_BACK, uuid));

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).discoverClusterInconsistent(eq(uuid), matchSetOf("server1"), matchSetOf("server2"));
    verify(results).endSecondDiscovery();
    verify(results).done(UNRECOVERABLY_INCONSISTENT);
  }

  @Test
  public void discoverOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, 1L), discovery(COMMITTED, 2L));
    when(server2.discover()).thenReturn(discovery(COMMITTED));

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverOtherClient("server1", "testMutationHost", "testMutationUser");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  public void prepareFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepareFail("server2");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack("server1");
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @Test
  public void prepareFailRollbackFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenThrow(NomadException.class);
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepareFail("server2");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rollbackFail("server1");
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  public void prepareFailRollbackOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepareFail("server2");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rollbackOtherClient("server1", "lastMutationHost", "lastMutationUser");
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  public void prepareOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepareOtherClient("server2", "lastMutationHost", "lastMutationUser");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack("server1");
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @Test
  public void prepareChangeUnacceptable() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(reject(UNACCEPTABLE, "reason", "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepareChangeUnacceptable("server2", "reason");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack("server1");
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @Test
  public void commitFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenThrow(NomadException.class);
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepared("server2");
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).commitFail("server1");
    verify(results).committed("server2");
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  public void commitOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));
    when(server2.discover()).thenReturn(discovery(COMMITTED));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated("server1");
    verify(results).discoverRepeated("server2");
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared("server1");
    verify(results).prepared("server2");
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).commitOtherClient("server1", "lastMutationHost", "lastMutationUser");
    verify(results).committed("server2");
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test(timeout = 10_000L)
  public void timeout() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    doAnswer(new AnswersWithDelay(10_000L, new Returns(discovery(COMMITTED)))).when(server2).discover();

    runTest(10);

    verify(results).startDiscovery(matchSetOf("server1", "server2"));
    verify(results).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(results).discoverFail("server2");
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  private void runTest() {
    NomadClient client = new NomadClient(servers, "host", "user");
    client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));
  }

  private void runTest(long timeout) {
    NomadClient client = new NomadClient(servers, "host", "user");
    client.setTimeoutMillis(timeout);
    client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));
  }
}
