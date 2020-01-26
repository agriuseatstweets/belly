(ns belly.gcsfs
  (:require [clojure.java.io :refer [input-stream]]
            [environ.core :refer [env]]
            [clojure.string :refer [join]])

  (:import (com.google.auth.oauth2 GoogleCredentials)
           (com.google.api.gax.core FixedCredentialsProvider)
           java.time.Instant
           java.nio.charset.Charset
           (com.google.cloud.storage
            Blob
            BlobId
            BlobInfo
            Storage$BlobTargetOption
            StorageOptions))
)

(defn credentials-provider []
  (FixedCredentialsProvider/create
   (GoogleCredentials/fromStream
    (input-stream
     (env :google-application-credentials)))))


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

(defn upload-tweets [tweets sep]
  (upload
   (env :belly-location)
   (.toString (Instant/now))
   (join sep tweets)))
