(ns cassiel.cubewar.tournament
  "Central game-play, with hit points and rounds.
   `tournament` also builds journal lists of actions to transmit or broadcast."
  (:require (cassiel.cubewar [view :as v]
                             [state-navigation :as n])))

(defn fire
  "Takes, and returns, a complete world state. Also returns a journal."
  [world name]
  (let [{:keys [cube scoring]} world
        res (v/fire cube (get cube name))]
    (if res
      {:world world
       ;; TODO: score the hit
       :journal [{:to name :type :hit :hit res}
                 {:to res :type :hit-by :hit-by name}]}
      {:world world
       :journal [{:to name :type :miss}]})))

(defn move
  "Perform a cube move. We report `:blocked` or a new view."
  [world name f]
  (let [{:keys [cube]} world]
    (try
      (let [cube' (n/navigate cube name f)
            me (get cube' name)
            view (v/look-plane cube' me)]
        {:world (assoc world :cube cube')
         :journal [{:to name :type :view :view view}]})
      (catch IllegalArgumentException exn {:world world
                                           :journal [{:to name :type :blocked}]}))))
