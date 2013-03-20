(ns cassiel.cubewar.main
  (:require (cassiel.cubewar [server :as srv]
                             [manifest :as m])
            (clojure.tools.nrepl [server :as ns]))
  (:use [clojure.tools.cli :only [cli]])
  (:gen-class))

(defn -main
  [& args]
  (let [[{:keys [port nrepl name db-name]} rest usage]
        (cli args
             ["--port" "Listen on this port" :default m/DEFAULT-SERVICE-PORT
              :parse-fn #(Integer. %)]
             ["--nrepl" "NREPL on this port" :default m/DEFAULT-NREPL-PORT
              :parse-fn #(Integer. %)]
             ["--name" "The Zeroconf engine name" :default m/DEFAULT-SERVICE-NAME]
             ["--db-name" "The database name":default m/DEFAULT-DB-NAME])]

    (srv/start-game name port db-name)
    (ns/start-server :port nrepl)
    (dorun (repeatedly #(do (println "Alive at " (str (java.util.Date.)))
                            (Thread/sleep (* 60 1000)))))))
