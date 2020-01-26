(ns belly.kafka
  (:require [kinsky.client :as kc]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [belly.gcsfs :as gcsfs]))

(defn get-consumer []
  (let [c (kc/consumer {:bootstrap.servers (env :kafka-brokers)
                        :group.id          "bellyback"
                        :auto.offset.reset "earliest"
                        :max.poll.records 1000
                        :enable.auto.commit "false"}
                       (kc/string-deserializer)
                       (kc/string-deserializer))]

    (kc/subscribe! c (env :belly-topic))
    c))

(defn pull [c]
  (flatten (vals (:by-topic (kc/poll! c 100)))))

(defn pulls [n c]
  (flatten (map pull (repeat n c))))

(defn write [n]
  (let [c (get-consumer)]
    (try
      (let [messages (pulls n c)
            tweets (map #(get % :value) messages)]
        (if (empty? tweets)
          (log/warn "No tweets to write!")
          (do
            (log/info (str "Belly writing: " (count tweets)))
            (gcsfs/upload-tweets tweets "\n")
            (kc/commit! c))))
      (finally (kc/close! c)))))
