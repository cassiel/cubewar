(ns cassiel.cubewar.server
  "Main server."
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as c]
                             [players :as pl]
                             [tournament :as t]
                             [db :as db]
                             [network :as net])
            (cassiel.zeroconf [server :as zs]))
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defprotocol GAME
  "State of a game."

  (examine [this] "Look at the world state.")
  (interact [this port action args] "Interact with the server as a pretend client")
  (clear [this] "Reset the state to empty.")
  (close [this] "Close the game server."))

(defn retrieve-player
  [world origin]
  (get (:origins->names world) origin))

(defn retrieve-transmitter
  [world player]
  (or
   (get (:names->transmitters world) player)
   (throw+ {:type ::NO-TRANSMITTER :player (or player "<null>")})))

(defn- attach
  "Attach a player, setting up the name and network maps. Produce a journal
   with the specified action keyword."
  [world origin player-name back-port action]
  (let [host (:host origin)
        origins->names (assoc (:origins->names world)
                         origin
                         player-name)
        txs (:names->transmitters world)]
    (if (some #(and (= host (-> % (.getAddress) (.getHostName)))
                    (= back-port (.getPort %)))
              (vals txs))
      (throw+ {:type ::MACHINE-IN-USE})
      (let [names->transmitters (assoc (:names->transmitters world)
                                  player-name
                                  (net/start-transmitter host back-port))]
        (t/journalise
         (assoc (t/attach world player-name)
           :origins->names origins->names
           :names->transmitters names->transmitters)
         {:to player-name
          :action action
          :args {:host (:host origin) :port back-port}})))))

;; `service` takes the world, the dispatch argument, and a list
;; of arguments that came in with the OSC message. The result is a new world.

;; Attach a player. `origins->names` maps incoming `{:host, :port}` to name.
;; `names->transmitters` maps names to actual transmitters.

(defn service
  [world origin player action args]
  (case action
    :attach
    (let [[player-name back-port] args]
      (attach world origin player-name back-port :attached))

    :login
    ;; TODO: check for prior attachment.
    (let [db (:db world)
          [player-name password back-port] args
          _ (db/authenticate db player-name password)]
      (if ((:scoring world) player-name)
        (throw+ {:type ::ALREADY-LOGGED-IN})
        (attach world origin player-name back-port :logged-in)))

    :login-new
    ;; TODO: check for prior attachment.
    (let [db (:db world)
          [player-name password rgb back-port] args
          _ (db/add-user db player-name password rgb)]
      (attach world origin player-name back-port :logged-in))

    :detach
    ;; TODO: check for prior detachment...?
    (let [tx (retrieve-transmitter world player)]
      (.close tx)
      (assoc (t/detach world player)
        :names->transmitters (dissoc (:names->transmitters world) player)
        :origins->names (dissoc (:origins->names world) origin)))

    :start-round
    (t/start-round world)

    :handshake
    (t/journalise world {:to player :action :handshake-reply})

    :fire
    (t/fire world player)

    :kick
    (t/kick world)

    ;; Anything else: look it up as a manoeuvre function.
    (t/move world player action)))

;; We painted ourself into a corner slightly, in that all comms go through the
;; journalling mechanism which only works for attached players, so there's no
;; way to reply when not authenticated. So, `serve1` is able to catch exceptions
;; and report them back to the origin point.

(defn serve1
  "The journal is effectively transient, but we need some way to return it atomically
   so that it can be transmitted. (TODO we need a way to do that safely.)"
  [world handler origin action args]

  (:journal
   (swap! world
          (fn [w]
            (let [w' (dissoc w :journal)
                  player-opt (retrieve-player w' origin)]
              ;; TODO (here and watcher): use Slingshot catch.
              (try
                (service w' origin player-opt action args)
                (catch
                    Exception
                    exn
                  (handler w' exn origin player-opt args))))))))

;; Actual server.

(defn handler
  "Handler has been lifted out to help with unit tests."
  [world exn origin player-opt args]
  (let [_ (println "Attempting to HANDLE: " (.getMessage exn))
        {:keys [host _]} origin
        tx-opt (when player-opt (retrieve-transmitter world player-opt))
        ;; This is a horrible hack: when we aren't attached, we don't
        ;; have any information about the back-port, so let's blindly
        ;; use the last argument (works for `:attach`).
        tx (or tx-opt
               (do
                 (println "ATTEMPTING Transmitter on " host " " (last args))
                 (when (last args)      ; Bullet-proofing - need to track this down.
                   (net/start-transmitter host (last args)))))]
    (println "SERVICE exception: " exn)
    (.printStackTrace exn)
    (println "Transmitting back to: " tx)
    (if tx
      (.transmit tx (net/make-message
                     :error
                     {:message (.getMessage exn)}))
      (println "No transmitter for handler."))
    world))

(defn- start-state
  "The starting state for the server, setting up DB if/as required."
  [db-name]
  (let [db (db/file-db db-name)
        ;; TEMPORARY: initialise each time.
        _ (when m/INITIALISE-DB-ON-START (db/initialize db))]
    {:arena {}
     :scoring {}
     :origins->names {}
     :names->transmitters {}
     :db db
     ;; In some test code we don't have DB entries for RGB.
     :rgb-fn (fn [w n]
               (or (db/lookup-rgb db n)
                   0xFFFFFF))
     :banner-fn (fn [w n]
                  (let [i (get (:scoring w) n)]
                    (format "%s %d"
                            (apply str (repeat i \_))
                            i)))}))

(defn start-game
  [name port db-name]

  ;; Test with strings for players - these are directly in the OSC at the moment.
  (let [WORLD (atom (start-state db-name))]

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
                                (try+
                                 [(retrieve-transmitter new to)]
                                 ;; Another catch for `Observer`.
                                 (catch [:type ::NO-TRANSMITTER] _ [])))
                          msg (net/make-message (:action x) (:args x))]
                      (doseq [tx txs]
                        ;; The `when` check is 0 again - for `Observer` which we
                        ;; send to regardless of whether it's attached. (I don't
                        ;; think we need this one.)
                        (when tx (.transmit tx msg))))))
                (catch Exception exn
                  (println "WATCHER exception: " exn)
                  (.printStackTrace exn))))]

      (add-watch WORLD :key watcher))

    (let [r (net/start-receiver port (partial serve1 WORLD handler))
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
                                                  handler
                                                  {:host "localhost" :port port}
                                                  action
                                                  args))
        (clear [this]
          (reset! WORLD (start-state)))

        (close [this]
          (zs/close zeroconf)
          (.close r))))))
