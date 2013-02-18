(ns cassiel.cubewar.players
  "Player set (basic functionality at this stage)."
  (:require (cassiel.cubewar [cube :as c])))

(defn gen-player
  "Generate a player at a given start position. For now, always point inertial-forward."
  [pos0]
  (fn [pos] (map + pos0 pos)))

(defn add-player
  "Add a player to a map from names to player functions. Fails if a player with this name
   is present, or any player is already at the location."
  [state name p]
  (if (some (partial = name) (keys state))
    (throw (IllegalStateException. "player already in cube"))
    (let [pos (p [0 0 0])
          current-positions (map #(% [0 0 0]) (vals state))]
      (if (some (partial = pos) current-positions)
        (throw (IllegalStateException. "cell already occupied"))
        (assoc state name p)))))
