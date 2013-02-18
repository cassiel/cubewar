(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube]
                             [players :as pl])))

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

(pl/add-player {} 'PLAYER (pl/gen-player [0 0 0]))

(some identity {:a 3 :b 5})

(map identity {:a 3 :b 5})

(not nil)
