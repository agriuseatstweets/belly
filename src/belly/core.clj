(ns belly.core
  (:gen-class)
  (:require [clojure.java.io :refer [input-stream]]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [join]])
  (:import (com.google.auth.oauth2 GoogleCredentials)
           (com.google.api.gax.core FixedCredentialsProvider)
           (com.google.pubsub.v1 PubsubMessage ProjectTopicName)
           (com.google.protobuf ByteString)
           java.time.Instant
           java.nio.charset.Charset
           (com.google.cloud.storage
            Blob
            BlobId
            BlobInfo
            Storage$BlobTargetOption
            StorageOptions)
           (com.google.pubsub.v1
            ProjectSubscriptionName
            AcknowledgeRequest
            PullRequest)
           (com.google.cloud.pubsub.v1.stub
            GrpcSubscriberStub
            SubscriberStub
            SubscriberStubSettings)))

(defn credentials-provider []
  (FixedCredentialsProvider/create
   (GoogleCredentials/fromStream
    (input-stream
     (env :google-application-credentials)))))

(defn subscriber-settings []
  (-> (SubscriberStubSettings/newBuilder)
      (.setCredentialsProvider (credentials-provider))
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

(defn blob-info [bucket filename]
  (-> (BlobInfo/newBuilder (BlobId/of bucket filename))
      (.setContentType "text/plain")
      (.build)))

(defn utf [s] (.getBytes s (Charset/forName "UTF-8")))

(defn upload [bucket filename s]
  (-> (StorageOptions/newBuilder)
      (.setCredentials (.getCredentials (credentials-provider)))
      (.build)
      (.getService)
      (.create (blob-info bucket filename) (utf s) (make-array Storage$BlobTargetOption 0))))

(defn upload-tweets [tweets]
  (upload 
   (env :belly-location) 
   (.toString (Instant/now)) 
   (join "" (map get-data tweets))))

(defn write [n p]
  (let [subscriber (make-subscriber)]
    (try
      (let [tweet-groups (remove nil? (map #(pull subscriber %) (repeat p n)))
            tweets (flatten tweet-groups)]
        (if (empty? tweets)
          (log/warn "No tweets to write!")
          (do
            (log/info (str "Belly writing: " (count tweets)))
            (upload-tweets tweets)
            (doall (map #(ack subscriber %) tweet-groups)))))
      (finally (.close subscriber)))))

(defn -main  []
  (write 1000 (Integer/parseInt (env :belly-size))))
