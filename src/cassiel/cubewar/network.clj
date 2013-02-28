(ns cassiel.cubewar.network
  "OSC networking utilities."
  (:import [java.net InetAddress]
           [net.loadbang.osc.comms UDPTransmitter UDPReceiver]
           [net.loadbang.osc.data Bundle Message]
           [net.loadbang.osc.exn CommsException]
           [clojure.lang Keyword]))

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
  (let [m (Message. (dekeyword action "/"))]
    (reduce (fn [m [k a]]
              (-> m
                  (.addString (dekeyword k ""))
                  (.addString ":"))
              (condp instance? a
                Number (.addInteger m a)
                Keyword (.addString m (dekeyword a ""))
                String (.addString m a))) m args)))

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
