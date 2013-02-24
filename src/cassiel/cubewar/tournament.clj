(ns cassiel.cubewar.tournament
  "Central game-play, with hit points and rounds. The functions here correspond to
   actual OSC commands coming in from the clients (after appropriate authentication).
   `tournament` also builds journal lists of actions to transmit or broadcast."
  (:require (cassiel.cubewar [view :as v]
                             [state-navigation :as n])))

(defn fire
  "Takes, and returns, a complete world state. Also returns a journal.
   If a player is not in the scoring system, it's an internal error.
   If the player is not in the cube, then they're (presumably) sitting
   out this tournament round."
  [world name]
  (let [{:keys [arena scoring]} world
        me-scored (get scoring name)
        me-playing (get arena name)]

    (cond (nil? me-scored)
          (throw (IllegalStateException. (str "player not in scoring system: " name)))

          (nil? me-playing)
          {:world world
           :journal [{:to name :type :error :error "not currently in play"}]}

          :else
          (let [victim (v/fire arena me-playing)]
            (if victim
              (let [old-score (get scoring victim)
                    new-score (dec old-score)]
                ;; Re-score the victim, remove from arena if hit-points now zero.
                {:world (assoc world
                          :scoring (assoc scoring victim new-score)
                          :arena (if (pos? new-score) arena (dissoc arena victim)))
                 :journal [{:to name :type :hit :hit victim}
                           {:to victim
                            :type :hit-by
                            :hit-by name
                            :hit-points new-score}]})
              {:world world
               :journal [{:to name :type :miss}]})))))

(defn move
  "Perform a cube move. We report `:blocked` or a new view."
  [world name f]
  (let [{:keys [arena]} world]
    (try
      (let [arena' (n/navigate arena name f)
            me (get arena' name)
            view (v/look-plane arena' me)]
        {:world (assoc world :arena arena')
         :journal [{:to name :type :view :view view}]})
      (catch IllegalArgumentException exn {:world world
                                           :journal [{:to name :type :blocked}]}))))
