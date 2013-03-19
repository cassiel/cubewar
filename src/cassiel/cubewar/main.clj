(ns cassiel.cubewar.main
  (:use [clojure.tools.cli :only [cli]])
  (:gen-class))

(defn -main
  [& args]
  (println
   (cli args
        ["--port" "Listen on this port" :default 8123 :parse-fn #(Integer. %)]
        ["--name" "The Zeroconf engine name" :default "Cubewar"]
        ["--db" "The database name":default "cubewar"])))
