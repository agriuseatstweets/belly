(ns belly.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [belly.kafka :as kafka]
            [belly.pubsub :as pubsub]))


(defn get-writer []
  (let [q (env :belly-queue)]
    (cond
      (= q "kafka") kafka/write
      (= q "pubsub") pubsub/write
      (throw (RuntimeException. (str "We don't have a queue that matches: " q))))))

(defn -main  []
  ((get-writer) (Integer/parseInt (env :belly-size))))
