(ns cassiel.cubewar.tournament
  "Central game-play, with hit points and rounds."
  (:require (cassiel.cubewar [view :as v]))
  )

(defn fire
  "A named player fires a shot. Return :miss or [:hit playername]."
  [state name]
  (let [p (get state name)
        view (rest (v/look-ahead state p))
        ;; Look for first non-:empty, non-:wall if any.
        target (some #(when-not (#{:empty :wall} %) %) view)]
    (or target :miss)))
