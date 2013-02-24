(ns cassiel.cubewar.server
  "Main server."
  (:require (cassiel.cubewar [tournament :as t]
                             [network :as net])))

;; Deal with incoming OSC messages. For now (this is temporary) include the
;; symbolic name of the player as part of the message.

(defmulti service (fn [& args] (nth args 1)))

;; `service1` takes (after the dispatch argument) the world and a variable list
;; of arguments that came in with the OSC message. The result is a new world
;; and a journal.

(defmethod service :handshake
  [world _ args]
  {:world world :journal {:action :handshake-reply :args []}})

(defmethod service :fire
  [world _ [player]]
  (t/fire world player)
  )

(defn serve1
  "The `world-state` is an atom with map containing `:world` and `:journal`. The
   journal is effectively transient, but we need some way to return it atomically
   so that it can be transmitted. (TODO perhaps we should just do that inside the
   `swap!`."
  [world-state action args]
  (let [{j :journal}
        (swap! world-state (fn [{w :world}] (service w action args)))]
    j))

;; Actual server.

(defn start-game
  [port]

  (let [WORLD (atom {:world {} :journal []})]
    nil)
  )
