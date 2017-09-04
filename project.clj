(defproject cosla "0.3.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "0.9.2"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time/clj-time "0.11.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}
             :test {:dependencies [[midje "1.6.3"]]
                    :plugins [[lein-midje "3.1.3"]]}
             :uberjar {:aot :all}}
  :aliases {"autotest" ["with-profile" "+test" "midje" ":autotest"]
            "test"  ["with-profile" "+test" "midje"]
            "repl" ["with-profile" "+test" "repl"]}
  :repl-options {:init-ns user}
  :injections [(require 'clojure.pprint)]
  :main ^:skip-aot cosla.main
  :target-path "target/%s")
