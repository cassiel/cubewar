(ns cassiel.cubewar.cube
  "Basic geometry and navigation for the cube."
  (:require (cassiel.cubewar [manifest :as m])))

;; Represent the cube in absolute coordinates as a map from [x y z] (0..n) to
;; a unique cell-id (which could be an integer, but we make it a symbol for
;; clarity). In fact, I'm not sure we ever need the symbols; all we need is
;; a "wall" predicate for the navigation and view functions.

;; TODO: remove inertial-cube entirely.

(def inertial-cube
  (reduce
   (fn [m [k v]] (assoc m k v))
   {}
   (for [x (range m/CUBE-SIZE)
         y (range m/CUBE-SIZE)
         z (range m/CUBE-SIZE)]
     [[x y z] (symbol (str "C" x y z))])))

(defn wall?
  "Is this coordinate point inside a wall?"
  [xyz]
  (or (some neg? xyz)
      (some #(>= % m/CUBE-SIZE) xyz)))

;; Navigation functions. Each takes the coordinate to access, and returns the
;; coordinate to use in the reference frame prior to the move.
;; Coordinate system is right-hand: X is positive to right, Y positive ahead,
;; Z positive upwards.

(defn forward
  "Move forward one step. There are no range/boundary checks here."
  [[x y z]]
  [x (inc y) z])

(defn pitch-up
  "Pitch up to point along previous local positive Z."
  [[x y z]]
  [x (- z) y])

(defn pitch-down
  "Pitch up to point along previous local negative Z."
  [[x y z]]
  [x z (- y)])

(defn roll-left
  "Roll to left, new Z pointing to old negative X."
  [[x y z]]
  [(- z) y x])

(defn roll-right
  "Roll to right, new Z pointing to old positive X."
  [[x y z]]
  [z y (- x)])

(defn yaw-left
  "Yaw to left, new Y pointing to old negative X."
  [[x y z]]
  [(- y) x z])

(defn yaw-right
  "Yaw to right, new Y pointing to old positive X."
  [[x y z]]
  [y (- x) z])
