(ns belly.pubsub
  (:require [clojure.java.io :refer [input-stream]]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [join]]
            [belly.gcsfs :as gcsfs])

  (:import (com.google.pubsub.v1 PubsubMessage ProjectTopicName)
           (com.google.pubsub.v1
            ProjectSubscriptionName
            AcknowledgeRequest
            PullRequest)
           (com.google.cloud.pubsub.v1.stub
            GrpcSubscriberStub
            SubscriberStub
            SubscriberStubSettings)))

(defn subscriber-settings []
  (-> (SubscriberStubSettings/newBuilder)
      (.setCredentialsProvider (gcsfs/credentials-provider))
      (.build)))

(defn get-subscription []
  (ProjectSubscriptionName/format
   (env :google-project-id)
   (env :google-pubsub-subscription)))

(defn pull-request [messages]
  (-> (PullRequest/newBuilder)
      (.setMaxMessages messages)
      (.setReturnImmediately true)
      (.setSubscription (get-subscription))
      (.build)))

(defn make-subscriber []
  (GrpcSubscriberStub/create (subscriber-settings)))

(defn pull [subscriber n]
  (-> subscriber
      (.pullCallable)
      (.call (pull-request n))
      (.getReceivedMessagesList)
      (seq)))

(defn acknowledge-request [tweets]
  (-> (AcknowledgeRequest/newBuilder)
      (.setSubscription (get-subscription))
      (.addAllAckIds (map #(.getAckId %) tweets))
      (.build)))

(defn ack [subscriber tweets]
  (-> subscriber
      (.acknowledgeCallable)
      (.call (acknowledge-request tweets))))

(defn get-data [tweet]
  (-> tweet
      (.getMessage)
      (.getData)
      (.toStringUtf8)))


(defn write [n]
  (let [subscriber (make-subscriber)]
    (try
      (let [tweet-groups (remove nil? (map #(pull subscriber %) (repeat 1000 n)))
            tweets (map get-data (flatten tweet-groups))]
        (if (empty? tweets)
          (log/warn "No tweets to write!")
          (do
            (log/info (str "Belly writing: " (count tweets)))
            (gcsfs/upload-tweets tweets "")
            (doall (map #(ack subscriber %) tweet-groups)))))
      (finally (.close subscriber)))))
