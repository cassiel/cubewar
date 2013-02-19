(ns cassiel.cubewar.tournament
  "Central game-play, with hit points and rounds.
   `tournament` also builds journal lists of actions to transmit or broadcast."
  (:require (cassiel.cubewar [view :as v])))

(defn fire
  "Takes, and returns, a complete world state. Also returns a journal."
  [world name]
  (let [{:keys [cube scoring]} world
        res (v/fire cube name)]
    (if res
      {:world world
       ;; TODO: score the hit
       :journal [{:to name :type :hit :hit res}
                 {:to res :type :hit-by :hit-by name}]
       }
      {:world world
       :journal [{:to name :type :miss}]}
      )
    )
  )
