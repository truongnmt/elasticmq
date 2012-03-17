package org.elasticmq.replication

import org.elasticmq.replication.message.{ReplicationMessage, ReplicationMessageMarshaller}
import org.jgroups.blocks.{MessageDispatcher, ResponseMode, RequestOptions}
import org.jgroups.Message

class JGroupsReplicationMessageSender(messageMarshaller: ReplicationMessageMarshaller,
                                      defaultCommandReplicationMode: CommandReplicationMode,
                                      messageDispatcher: MessageDispatcher)
  extends ReplicationMessageSender {

  def broadcast(replicationMessage: ReplicationMessage) {
    broadcast(replicationMessage, defaultCommandReplicationMode)
  }

  def broadcastDoNotWait(replicationMessage: ReplicationMessage) {
    broadcast(replicationMessage, DoNotWaitReplicationMode)
  }

  private def broadcast(replicationMessage: ReplicationMessage, commandReplicationMode: CommandReplicationMode) {
    val requestOptions = new RequestOptions(responseModeForReplicationMode(commandReplicationMode), 0)
    
    // The val _ is needed because of type inferencing problems
    val _ = messageDispatcher.castMessage(
      null,
      new Message(null, messageMarshaller.serialize(replicationMessage)),
      requestOptions)
  }

  private def responseModeForReplicationMode(commandReplicationMode: CommandReplicationMode): ResponseMode =
    commandReplicationMode match {
      case DoNotWaitReplicationMode => ResponseMode.GET_NONE
      case WaitForAnyReplicationMode => ResponseMode.GET_FIRST
      case WaitForMajorityReplicationMode => ResponseMode.GET_MAJORITY
      case WaitForAllReplicationMode => ResponseMode.GET_ALL
    }
}
