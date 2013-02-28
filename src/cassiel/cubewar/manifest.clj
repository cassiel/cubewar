;; Manifest constants.

(ns cassiel.cubewar.manifest)

;; Inner dimensions of cube.
(def CUBE-SIZE 3)

;; Width of view seen by player. Should be odd, so that player is centered.
(def VIEW-WIDTH 3)

;; Depth of view seen by player.
(def VIEW-DEPTH 3)

;; Depth of fire (count includes origin position).
(def FIRE-DEPTH VIEW-DEPTH)

;; Special symbol for broadcast messages.
(def BROADCAST :*)
