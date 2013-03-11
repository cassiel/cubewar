(defproject eu.cassiel/cubewar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/cassiel/cubewar"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [slingshot "0.10.3"]
                 [net.loadbang/net.loadbang.osc "1.5.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.hsqldb/hsqldb "2.2.9"]
                 [eu.cassiel/clojure-zeroconf "1.1.0"]]
  :plugins [[lein-ring "0.8.2"]
            [lein-marginalia "0.7.1"]]
  :ring {:handler cassiel.cubewar.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
