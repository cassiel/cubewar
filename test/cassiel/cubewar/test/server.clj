(ns cassiel.cubewar.test.server
  "(Non-network) server tests"
  (:use clojure.test)
  (:require (cassiel.cubewar [players :as pl]
                             [server :as srv]))
  (:import [net.loadbang.osc.comms IPTransmitter]))

(def state-n
  (-> {}
      (pl/add-player :P1 (pl/gen-player [0 0 0]))
      (pl/add-player :P2 (pl/gen-player [1 0 0]))
      (pl/add-player :P3 (pl/gen-player [0 1 0]))))

(def world-n {:arena state-n
              :scoring {:P1 50 :P3 50}
              :names->destinations {}
              :sources->names {}})

(deftest housekeeping
  (testing "attach: world state"
    (let [FULL-STATE (atom {:world world-n :journal []})
          _ (srv/serve1 FULL-STATE
                        {:host "localhost" :port 9999}
                        :attach [:P1 9998])]
      (is (= :P1 (-> @FULL-STATE
                     (:world)
                     (:sources->names)
                     (get {:host "localhost" :port 9999}))))
      (let [r (-> @FULL-STATE
                  (:world)
                  (:names->transmitters)
                  (:P1))]
        (is (isa? (class r) IPTransmitter))
        (is (= "localhost" (-> r (.getAddress) (.getHostName))))
        (is (= 9998 (.getPort r))))))

  (testing "attach: journal"
    (let [FULL-STATE (atom {:world world-n :journal []})]
      (is (= [{:to :P1 :action :attached :args ["localhost" 9998]}]
             (srv/serve1 FULL-STATE
                         {:host "localhost" :port 9999}
                         :attach [:P1 9998]))))))
