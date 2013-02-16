(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube])))

(keyword (str "A" 34))


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

(apply hash-map [[1 2] "X" [3 4] "Y"])


(flatten [[1 2] [3 4]])

(cube/inertial-cube [0 0 0])

(cube/forward [0 0 0])

((comp cube/inertial-cube cube/forward cube/yaw-right) [0 0 0])
