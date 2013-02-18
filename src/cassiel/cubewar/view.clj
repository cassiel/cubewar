(ns cassiel.cubewar.view
  "Generate view from a player's perspective."
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl])))

(defn look
  "Examine a cell coordinate from a player's perspective. The player will only see
   itself at [0 0 0] if it's in the state.
   Possible results: :empty, :wall, [:occupied name]."
  [state p pos]
  (if (c/wall? pos)
    :wall
    (let [p (pl/player-at state pos)]
      (if p
        [:player p]
        :empty))))
