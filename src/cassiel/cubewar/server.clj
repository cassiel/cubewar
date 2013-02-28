(ns cassiel.cubewar.server
  "Main server."
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [tournament :as t]
                             [network :as net])))

(defn retrieve-player
  [world origin]
  (get (:origins->names world) origin))

(defn retrieve-transmitter
  [world player]
  (or
   (get (:names->transmitters world) player)
   (throw (IllegalStateException. (str "no transmitter found for player " (or player "<null>"))))))

;; `service` takes the world, the dispatch argument, and a list
;; of arguments that came in with the OSC message. The result is a new world.

;; Attach a player. `origins->names` maps incoming `{:host, :port}` to name.
;; `names->transmitters` maps names to actual transmitters.

(defn service
  [world origin player action args]
  (case action
    :attach
    ;; TODO: check for prior attachment.
    (let [[player-name back-port] args
          origins->names (assoc (:origins->names world)
                           origin
                           player-name)
          names->transmitters (assoc (:names->transmitters world)
                                player-name
                                (net/start-transmitter (:host origin) back-port))]
      (assoc world
        :origins->names origins->names
        :names->transmitters names->transmitters
        :scoring (assoc (:scoring world) player-name 0)
        :journal [{:to player-name :action :attached :args [(:host origin) back-port]}]))

    :detach
    ;; TODO: check for prior attachment.
    (let [tx (retrieve-transmitter world player)]
      (.close tx)
      (assoc world
        :arena (dissoc (:arena world) player)
        :scoring (dissoc (:scoring world) player)
        :names->transmitters (dissoc (:names->transmitters world) player)
        :origins->names (dissoc (:origins->names world) origin)
        :journal []))

    :handshake
    (assoc world :journal [{:to player :action :handshake-reply :args []}])

    :fire (t/fire world player)
    :pitch-up (t/move world player c/pitch-up)
    :yaw-right (t/move world player c/yaw-right)

    (throw (IllegalArgumentException. (str "unrecognised action: " action))))
  )

(defn serve1
  "The journal is effectively transient, but we need some way to return it atomically
   so that it can be transmitted. (TODO we need a way to do that safely.)"
  [world origin action args]

  (:journal
   (swap! world
          (fn [w]
            (try
              (service w origin (retrieve-player w origin) action args)
              (catch Exception exn
                (do
                  (println "SERVICE exception: " exn)
                  (.printStackTrace exn)
                  w)))))))

;; Actual server.

(defn start-game
  [port]

  ;; Test with strings for players - these are directly in the OSC at the moment.
  (let [arena (-> {}
                  (pl/add-player "P1" (pl/gen-player [0 0 0]))
                  (pl/add-player "P2" (pl/gen-player [1 0 0]))
                  (pl/add-player "P3" (pl/gen-player [0 1 0])))
        scoring {"P1" 50 "P2" 50 "P3" 50}
        WORLD (atom {:arena arena
                     :scoring scoring
                     :origins->names {}
                     :names->transmitters {}})]

    ;; The watcher runs through the journal, picking out the destination transmitter via `:to`,
    ;; making a `Message` out of the rest of each entry, and transmitting.
    ;; TODO: deal with wildcards - also think about broadcasts to players in-arena vs.
    ;; all connected players.
    (letfn [(watcher [k r old new]
              (try
                (do
                  (println (:journal new))
                  (doseq [x (:journal new)]
                    (let [tx (retrieve-transmitter new (:to x))
                          msg (net/make-message (:action x) (:args x))]
                      (.transmit tx msg))))
                (catch Exception exn
                  (println "WATCHER exception: " exn)
                  (.printStackTrace exn))))]

      (add-watch WORLD :key watcher))

    {:receiver (net/start-receiver port (partial serve1 WORLD))
     :world WORLD}))
