(ns cassiel.cubewar.server
  "Main server."
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [tournament :as t]
                             [network :as net])))

;; Deal with incoming OSC messages. For now (this is temporary) include the
;; symbolic name of the player as part of the message.

(defmulti service (fn [& args] (nth args 1)))

;; `service1` takes the world, the dispatch argument, and a list
;; of arguments that came in with the OSC message. The result is a new world
;; and a journal.

(defmethod service :handshake
  [world & _]
  {:world world :journal {:action :handshake-reply :args []}})

(defmethod service :fire
  [world _ [player]]
  (t/fire world player))

(defmethod service :pitch-up
  [world _ [player]]
  (t/move world player c/pitch-up))

(defmethod service :yaw-right
  [world _ [player]]
  (t/move world player c/yaw-right))

(defn serve1
  "The `world-state` is an atom with map containing `:world` and `:journal`. The
   journal is effectively transient, but we need some way to return it atomically
   so that it can be transmitted. (TODO we need a way to do that safely.)"
  [world-state action args]
  (let [{j :journal}
        (swap! world-state (fn [{w :world}] (service w action args)))]
    j))

;; Actual server.

(defn start-game
  [port]

  ;; Test with strings for players - these are directly in the OSC at the moment.
  (let [arena (-> {}
                  (pl/add-player "P1" (pl/gen-player [0 0 0]))
                  (pl/add-player "P2" (pl/gen-player [1 0 0]))
                  (pl/add-player "P3" (pl/gen-player [0 1 0])))
        scoring {"P1" 50 "P2" 50 "P3" 50}
        WORLD-STATE (atom {:world {:arena arena :scoring scoring}
                           :journal []})]

    (add-watch WORLD-STATE
               :key
               (fn [k r old new] (println (:journal new))))
    {:receiver (net/start-receiver 8123 (partial serve1 WORLD-STATE))
     :state WORLD-STATE}))
