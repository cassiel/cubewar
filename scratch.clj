(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as nav])))

(
 (reduce
  (fn [m [k v]] (assoc m k v))
  {}
  (for [x (range m/CUBE-SIZE)
        y (range m/CUBE-SIZE)
        z (range m/CUBE-SIZE)]
    [[x y z] (keyword (str "C" (+ x (* m/CUBE-SIZE (+ y (* m/CUBE-SIZE z))))))]
    ))
 [0 1 0])

(cube/inertial-cube [0 0 0])

(cube/forward [0 0 0])

(cube/pitch-up [0 1 0])

((comp cube/inertial-cube cube/forward cube/yaw-right) [0 0 0])

(map #(% [0 0 0]) (vals {'PLAYER (fn [[x y z]] :X)}))

(def state0
  (pl/add-player {} 'PLAYER (pl/gen-player [0 0 0])))

(def state1
  (pl/add-player {} 'PLAYER (comp (pl/gen-player [0 0 0]) cube/forward)))

(v/look state0 (pl/gen-player [0 0 0]) [0 0 0])

(v/look state0 (comp (pl/gen-player [0 0 0]) cube/forward) [0 0 0])
(v/look state0 (comp (pl/gen-player [0 0 0]) cube/pitch-up) [0 0 0])


(map (fn [[name f]] {:name name :pos (f [0 0 0])})
     state1)

state0

(
 (get
  (nav/navigate
   state0
   'PLAYER
   cube/forward
   )
  'PLAYER)
 [0 0 0])

(#{1 2} 1)
