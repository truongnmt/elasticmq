package org.elasticmq.rest.sqs

import Constants._
import akka.http.scaladsl.server.Route
import org.elasticmq.rest.sqs.directives.ElasticMQDirectives
import org.joda.time.DateTime

trait SendMessageBatchDirectives {
  this: ElasticMQDirectives with SendMessageDirectives with BatchRequestsModule =>
  val SendMessageBatchPrefix = "SendMessageBatchRequestEntry"

  def sendMessageBatch(p: AnyParams): Route = {
    p.action("SendMessageBatch") {
      queueActorAndDataFromRequest(p) { (queueActor, queueData) =>
        verifyMessagesNotTooLong(p)

        val baseCreationTime = new DateTime()

        val resultsFuture = batchRequest(SendMessageBatchPrefix, p) { (messageData, id, index) =>
          val message = createMessage(messageData, queueData, index)

          doSendMessage(queueActor, message).map {
            case (message, digest, messageAttributeDigest) =>
              <SendMessageBatchResultEntry>
                <Id>{id}</Id>
                {
                  if (!messageAttributeDigest.isEmpty) <MD5OfMessageAttributes>{
                    messageAttributeDigest
                  }</MD5OfMessageAttributes>
                }
                <MD5OfMessageBody>{digest}</MD5OfMessageBody>
                <MessageId>{message.id.id}</MessageId>
              </SendMessageBatchResultEntry>
          }
        }

        resultsFuture.map { results =>
          respondWith {
            <SendMessageBatchResponse>
              <SendMessageBatchResult>
                {results}
              </SendMessageBatchResult>
              <ResponseMetadata>
                <RequestId>{EmptyRequestId}</RequestId>
              </ResponseMetadata>
            </SendMessageBatchResponse>
          }
        }
      }
    }
  }

  def verifyMessagesNotTooLong(parameters: Map[String, String]): Unit = {
    val messageLengths = for {
      parameterMap <- batchParametersMap(SendMessageBatchPrefix, parameters)
    } yield {
      parameterMap(MessageBodyParameter).length
    }

    verifyMessageNotTooLong(messageLengths.sum)
  }
}
