(ns cassiel.cubewar.network
  "OSC networking utilities."
  (:import [java.net InetAddress]
           [net.loadbang.osc.comms UDPTransmitter UDPReceiver]
           [net.loadbang.osc.data Bundle Message]
           [net.loadbang.osc.exn CommsException]
           [clojure.lang Keyword]
           [java.util List Map]))

(defn dispatch-message
  "Unpack a message and call `f` with host/port, OSC address, and list of args."
  [f
   origin                               ; {:host, :port}
   ^Message m]
  (let [address (get (re-find #"^/?(.+)+$" (.getAddress m)) 1)
        args (for
                 [i (range (.getNumArguments m))]
               (.getValue (.getArgument m i)))]
    (f origin (keyword address) args)))

(defn dekeyword [k punc]
  (clojure.string/replace k ":" punc))

(defn make-message
  "Create a `Message` from a map containing action (regarded as OSC address) and arguments
   (represented as a map: we need to turn this into something that Max can unpack as a
   dictionary)."
  [action args]
  (let [msg (Message. (dekeyword action "/"))]
    (letfn [(add1 [a]
              (condp instance? a
                Number (.addInteger msg a)
                Keyword (.addString msg (dekeyword a ""))
                String (.addString msg a)
                List (doseq [x a] (add1 x))
                Map (do (.addString msg "{")
                        (add-args a)
                        (.addString msg "}"))))

            (add-args [args]
              (doseq [[k a] args]
                (-> msg
                    (.addString (dekeyword k ""))
                    (.addString ":"))
                (add1 a)))]

      (add-args args)
      msg)))

(defn start-receiver
  "Create a receiver socket given a consuming function. The receiver accepts `(.close)`."
  [port f]
  (let [rx (proxy
               [UDPReceiver]
               [port]
             (consumeMessage
               [socket _date00 m]
               (let [host (.getHostName socket)
                     port (.getPort socket)]
                 (dispatch-message f {:host host :port port} m))))
        _ (.open rx)]

    (.start (Thread. (reify Runnable
                       (run [this]
                         (try
                           (dorun (repeatedly #(.take rx)))
                           (catch CommsException _ nil))))))
    rx))

(defn start-transmitter
  "Create a transmitter to host and port. The transmitter accepts `(.close)`."
  [host port]
  (UDPTransmitter. (InetAddress/getByName host) port))
