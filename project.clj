(defproject bellyback "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [spootnik/kinsky "0.1.23"]
                 [com.google.cloud/google-cloud-pubsub "1.59.0"]
                 [com.google.cloud/google-cloud-storage "1.62.0"]
                 [environ "1.1.0"]]
  :main ^:skip-aot belly.core
  :resource-paths ["src/resources"]
  :plugins [[lein-environ "1.1.0"]]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :resource-paths ["test/resources"]}})
