(defproject eu.cassiel/cubewar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/cassiel/cubewar"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.5"]
                 [net.loadbang/net.loadbang.osc "1.4.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.hsqldb/hsqldb "2.2.9"]]
  :plugins [[lein-ring "0.8.2"]
            [lein-marginalia "0.7.1"]]
  :ring {:handler cassiel.cubewar.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
