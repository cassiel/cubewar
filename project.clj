(defproject eu.cassiel/cubewar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/cassiel/cubewar"
  :main cassiel.cubewar.main
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [slingshot "0.10.3"]
                 [net.loadbang/net.loadbang.osc "1.5.0"]
                 ;; Hack because we have to manually install some Maven entries:
                 [net.loadbang/net.loadbang.lib "1.9.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.hsqldb/hsqldb "2.2.9"]
                 [commons-codec/commons-codec "1.7"]
                 [eu.cassiel/clojure-zeroconf "1.1.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/tools.nrepl "0.2.2"]]
  :plugins [[lein-midje "3.0.0"]
            [lein-marginalia "0.7.1"]]
  :profiles
  {:dev {:dependencies [[midje "1.5.0"]
                        [ring-mock "0.1.3"]]}})
