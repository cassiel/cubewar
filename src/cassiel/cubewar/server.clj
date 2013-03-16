(ns cassiel.cubewar.server
  "Main server."
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as c]
                             [players :as pl]
                             [tournament :as t]
                             [network :as net])
            (cassiel.zeroconf [server :as zs]))
  (:use [slingshot.slingshot :only [throw+]]))

(defprotocol GAME
  "State of a game."

  (examine [this] "Look at the world state.")
  (interact [this port action args] "Interact with the server as a pretend client")
  (close [this] "Close the game server."))

(defn retrieve-player
  [world origin]
  (get (:origins->names world) origin))

(defn retrieve-transmitter
  [world player]
  (or
   (get (:names->transmitters world) player)
   (throw+ {:type ::NO-TRANSMITTER :player (or player "<null>")})))

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
      (t/journalise
       (assoc (t/attach world player-name)
         :origins->names origins->names
         :names->transmitters names->transmitters)
       {:to player-name
        :action :attached
        :args {:host (:host origin) :port back-port}}))

    :detach
    ;; TODO: check for prior attachment.
    (let [tx (retrieve-transmitter world player)]
      (.close tx)
      (assoc (t/detach world player)
        :names->transmitters (dissoc (:names->transmitters world) player)
        :origins->names (dissoc (:origins->names world) origin)))

    :start-round
    (t/start-round world)

    :handshake
    (t/journalise world {:to player :action :handshake-reply})

    :fire (t/fire world player)

    ;; Anything else: look it up as a manoeuvre function.
    (t/move world player action)))

(defn serve1
  "The journal is effectively transient, but we need some way to return it atomically
   so that it can be transmitted. (TODO we need a way to do that safely.)"
  [world origin action args]

  (:journal
   (swap! world
          (fn [w]
            (let [w' (dissoc w :journal)]
              ;; TODO (here and watcher): use Slingshot catch.
              (try
                (service w' origin (retrieve-player w' origin) action args)
                (catch Exception exn
                  (do
                    (println "SERVICE exception: " exn)
                    (.printStackTrace exn)
                    w'))))))))

;; Actual server.

(defn start-game
  [name port]

  ;; Test with strings for players - these are directly in the OSC at the moment.
  (let [WORLD (atom {:arena {}
                     :scoring {}
                     :origins->names {}
                     :names->transmitters {}})]

    ;; The watcher runs through the journal, picking out the destination transmitter via `:to`
    ;; (or all of them for a broadcast), making a `Message` out of the rest of each entry,
    ;; and transmitting.
    ;; TODO: think about broadcasts to players in-arena vs. all connected players.
    (letfn [(watcher [k r old new]
              (try
                (do
                  (println (:journal new))
                  (doseq [x (:journal new)]
                    (let [to (:to x)
                          txs (if (= to m/BROADCAST)
                                (vals (:names->transmitters new))
                                [(retrieve-transmitter new to)])
                          msg (net/make-message (:action x) (:args x))]
                      (doseq [tx txs] (.transmit tx msg)))))
                (catch Exception exn
                  (println "WATCHER exception: " exn)
                  (.printStackTrace exn))))]

      (add-watch WORLD :key watcher))

    (let [r (net/start-receiver port (partial serve1 WORLD))
          zeroconf (zs/server :type "_cubewar._udp.local."
                              :name name
                              :port port
                              :text (str "Cubewar server "
                                         (System/getProperty "cubewar.version")
                                         " on "
                                         (System/getProperty "os.name")
                                         \space
                                         (System/getProperty "os.version")))
          _ (zs/open zeroconf)]
      (reify GAME
        (examine [this] @WORLD)
        (interact [this port action args] (serve1 WORLD
                                                  {:host "localhost" :port port}
                                                  action
                                                  args))
        (close [this]
          (zs/close zeroconf)
          (.close r))))))
