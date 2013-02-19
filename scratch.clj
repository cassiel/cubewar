(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as nav]
                             [tournament :as t])))

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

(def state-n
  (-> {}
      (pl/add-player :P1 (pl/gen-player [0 0 0]))
      (pl/add-player :P2 (pl/gen-player [1 0 0]))
      (pl/add-player :P3 (pl/gen-player [0 1 0]))))

(v/look state0 (pl/gen-player [0 0 0]) [0 0 0])

(v/look state0 (comp (pl/gen-player [0 0 0]) cube/forward) [0 0 0])
(v/look state0 (comp (pl/gen-player [0 0 0]) cube/pitch-up) [0 0 0])

(v/look-plane state-n (pl/gen-player [0 0 0]))

(map (fn [[name f]] {:name name :pos (f [0 0 0])})
     state1)

state0

(v/fire state-n :P1)

(def world-n {:cube state-n
              :scoring nil})

(t/fire world-n :P1)

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

(range -10 5)

(int (/ (- 9) 2))



(range (int (/ (- m/VIEW-WIDTH) 2))
       (inc (int (/ m/VIEW-WIDTH 2)))
       )

(repeat 4 :A)

(= [:x :x] (repeat 2 :x))


(fn [:a] "A")
